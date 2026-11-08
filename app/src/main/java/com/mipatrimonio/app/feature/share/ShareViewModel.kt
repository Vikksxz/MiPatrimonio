package com.mipatrimonio.app.feature.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mipatrimonio.app.core.database.dao.DocumentDao
import com.mipatrimonio.app.core.database.entity.DocumentEntity
import com.mipatrimonio.app.core.model.DocumentKind
import com.mipatrimonio.app.core.model.IncomeKind
import com.mipatrimonio.app.core.model.TransactionType
import com.mipatrimonio.app.core.ocr.DocumentClassification
import com.mipatrimonio.app.core.ocr.DocumentClassifier
import com.mipatrimonio.app.core.ocr.OcrService
import com.mipatrimonio.app.core.ocr.PdfBitmapRenderer
import com.mipatrimonio.app.core.ocr.ReceiptParser
import com.mipatrimonio.app.core.storage.EncryptedDocumentStore
import com.mipatrimonio.app.domain.usecase.AddTransaction
import com.mipatrimonio.app.domain.usecase.NewTransaction
import com.mipatrimonio.app.feature.add.ReceiptScanDraft
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileInputStream
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SharedFileInfo(
    val inboxPath: String,
    val displayName: String,
    val mimeType: String,
)

sealed interface ShareUiState {
    data object Loading : ShareUiState
    data class Ready(
        val file: SharedFileInfo,
        val classification: DocumentClassification,
        val lines: List<String>,
    ) : ShareUiState
    data class Error(val message: String? = null) : ShareUiState
}

@HiltViewModel
class ShareViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ocrService: OcrService,
    private val store: EncryptedDocumentStore,
    private val documentDao: DocumentDao,
    private val addTransaction: AddTransaction,
) : ViewModel() {

    private val _state = MutableStateFlow<ShareUiState>(ShareUiState.Loading)
    val state: StateFlow<ShareUiState> = _state.asStateFlow()

    fun handleIncomingUri(uri: Uri, sourceLabel: String?) {
        viewModelScope.launch {
            _state.value = ShareUiState.Loading
            try {
                val ready = withContext(Dispatchers.IO) {
                    val file = copyToInbox(uri)
                    val lines = ocrLines(File(file.inboxPath), file.mimeType)
                    val classification = DocumentClassifier.classify(lines)
                    ShareUiState.Ready(file, classification, lines)
                }
                _state.value = ready
            } catch (e: Exception) {
                _state.value = ShareUiState.Error(e.message)
            }
        }
    }

    private fun copyToInbox(uri: Uri): SharedFileInfo {
        val inboxDir = File(context.filesDir, "inbox").also { it.mkdirs() }
        val displayName = queryDisplayName(uri) ?: "compartido"
        val safeName = displayName.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "compartido" }
        val target = File(inboxDir, "${UUID.randomUUID()}_$safeName")
        val mime = context.contentResolver.getType(uri)
            ?: guessMime(displayName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: throw IllegalStateException("No se pudo leer el archivo compartido")
        return SharedFileInfo(target.absolutePath, displayName, mime)
    }

    private suspend fun ocrLines(file: File, mimeType: String): List<String> {
        val bitmap = if (mimeType == "application/pdf" || file.name.endsWith(".pdf", true)) {
            PdfBitmapRenderer.renderFirstPage(file)
        } else {
            decodeImageFile(file, 1600)
        } ?: return emptyList()
        return try {
            ocrService.recognize(bitmap).lines
        } finally {
            bitmap.recycle()
        }
    }

    suspend fun buildTicketDraft(file: SharedFileInfo, lines: List<String>): ReceiptScanDraft? =
        withContext(Dispatchers.IO) {
            val source = File(file.inboxPath)
            val bitmap = if (file.mimeType == "application/pdf" || source.name.endsWith(".pdf", true)) {
                PdfBitmapRenderer.renderFirstPage(source)
            } else {
                decodeImageFile(source, 1600)
            } ?: return@withContext null
            val parsed = ReceiptParser.parse(lines)
            ReceiptScanDraft(
                amountCents = parsed.amountCents,
                date = parsed.date ?: LocalDate.now(),
                merchant = parsed.merchant?.takeIf { it.isNotBlank() },
                sourceBitmap = bitmap,
                thumbnail = thumbnailOf(bitmap),
            )
        }

    suspend fun buildPayrollDraft(
        file: SharedFileInfo,
        classification: DocumentClassification,
    ): com.mipatrimonio.app.feature.add.PayrollDraft? = withContext(Dispatchers.IO) {
        val source = File(file.inboxPath)
        if (!source.exists()) return@withContext null
        val thumb = if (file.mimeType == "application/pdf" || source.name.endsWith(".pdf", true)) {
            runCatching {
                val bmp = PdfBitmapRenderer.renderFirstPage(source)
                thumbnailOf(bmp).also { bmp.recycle() }
            }.getOrNull()
        } else {
            decodeImageFile(source, 480)
        }
        com.mipatrimonio.app.feature.add.PayrollDraft(
            sourceFilePath = file.inboxPath,
            mimeType = file.mimeType,
            displayName = file.displayName,
            amountCents = classification.suggestedNetCents,
            date = classification.suggestedDate ?: LocalDate.now(),
            thumbnail = thumb,
        )
    }

    suspend fun findPayrollDuplicates(date: LocalDate, title: String): List<DocumentEntity> =
        withContext(Dispatchers.IO) {
            val trimmed = title.trim()
            documentDao.getAllByKind(DocumentKind.PAYSLIP).filter { doc ->
                doc.date == date.toEpochDay() || (trimmed.isNotBlank() && doc.title.trim().equals(trimmed, true))
            }
        }

    private fun thumbnailOf(source: Bitmap): Bitmap {
        val thumbSide = 480
        val scale = minOf(
            thumbSide.toFloat() / source.width.coerceAtLeast(1),
            thumbSide.toFloat() / source.height.coerceAtLeast(1),
            1f,
        )
        return Bitmap.createScaledBitmap(
            source,
            (source.width * scale).toInt().coerceAtLeast(1),
            (source.height * scale).toInt().coerceAtLeast(1),
            true,
        )
    }

    fun saveAsDocument(
        file: SharedFileInfo,
        kind: DocumentKind,
        title: String,
        date: LocalDate,
        onSaved: (Long) -> Unit,
        onError: () -> Unit,
    ) {
        viewModelScope.launch {
            try {
                val id = withContext(Dispatchers.IO) {
                    val relativePath = FileInputStream(file.inboxPath).use { store.save(it) }
                    documentDao.insert(
                        DocumentEntity(
                            kind = kind,
                            title = title.ifBlank { file.displayName.removeSuffix(".pdf") },
                            filePath = relativePath,
                            mimeType = file.mimeType.ifBlank { "application/pdf" },
                            date = date.toEpochDay(),
                            createdAt = System.currentTimeMillis(),
                        ),
                    )
                }
                runCatching { File(file.inboxPath).delete() }
                onSaved(id)
            } catch (_: Exception) {
                onError()
            }
        }
    }

    fun saveAsPayrollWithIncome(
        file: SharedFileInfo,
        title: String,
        date: LocalDate,
        netCents: Long?,
        onSaved: () -> Unit,
        onError: () -> Unit,
    ) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val relativePath = FileInputStream(file.inboxPath).use { store.save(it) }
                    documentDao.insert(
                        DocumentEntity(
                            kind = DocumentKind.PAYSLIP,
                            title = title.ifBlank { "Nómina ${date.monthValue}/${date.year}" },
                            filePath = relativePath,
                            mimeType = file.mimeType.ifBlank { "application/pdf" },
                            date = date.toEpochDay(),
                            createdAt = System.currentTimeMillis(),
                        ),
                    )
                    if (netCents != null && netCents > 0) {
                        addTransaction(
                            NewTransaction(
                                type = TransactionType.INCOME,
                                amountCents = netCents,
                                merchant = title.ifBlank { "Nómina" },
                                date = date,
                                incomeKind = IncomeKind.PAYROLL,
                            ),
                        )
                    }
                }
                runCatching { File(file.inboxPath).delete() }
                onSaved()
            } catch (_: Exception) {
                onError()
            }
        }
    }

    private fun queryDisplayName(uri: Uri): String? = try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    } catch (_: Exception) {
        null
    }

    private fun guessMime(name: String): String = when {
        name.endsWith(".pdf", true) -> "application/pdf"
        name.endsWith(".png", true) -> "image/png"
        name.endsWith(".jpg", true) || name.endsWith(".jpeg", true) -> "image/jpeg"
        else -> "application/octet-stream"
    }

    private fun decodeImageFile(file: File, maxDimension: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxDimension)
        }
        val decoded = BitmapFactory.decodeFile(file.absolutePath, options) ?: return null
        return applyExifAndScale(decoded, file, maxDimension)
    }

    private fun sampleSizeFor(width: Int, height: Int, maxDimension: Int): Int {
        var sampleSize = 1
        while (width / (sampleSize * 2) >= maxDimension || height / (sampleSize * 2) >= maxDimension) sampleSize *= 2
        return sampleSize
    }

    private fun applyExifAndScale(bitmap: Bitmap, file: File, maxDimension: Int): Bitmap {
        val orientation = runCatching {
            ExifInterface(file.absolutePath).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL,
            )
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val matrix = Matrix()
        val rotated = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> { matrix.postRotate(90f); true }
            ExifInterface.ORIENTATION_ROTATE_180 -> { matrix.postRotate(180f); true }
            ExifInterface.ORIENTATION_ROTATE_270 -> { matrix.postRotate(270f); true }
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> { matrix.postScale(-1f, 1f); true }
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> { matrix.postScale(1f, -1f); true }
            else -> false
        }
        val base = if (rotated) {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also {
                if (it !== bitmap) bitmap.recycle()
            }
        } else bitmap
        val longest = maxOf(base.width, base.height)
        if (longest <= maxDimension) return base
        val ratio = maxDimension.toFloat() / longest
        val scaled = Bitmap.createScaledBitmap(
            base, (base.width * ratio).toInt().coerceAtLeast(1), (base.height * ratio).toInt().coerceAtLeast(1), true,
        )
        if (scaled !== base) base.recycle()
        return scaled
    }
}
