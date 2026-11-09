package com.mipatrimonio.app.feature.documents

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mipatrimonio.app.core.database.dao.DocumentDao
import com.mipatrimonio.app.core.database.entity.DocumentEntity
import com.mipatrimonio.app.core.model.DocumentKind
import com.mipatrimonio.app.core.ocr.DocumentClassifier
import com.mipatrimonio.app.core.ocr.OcrService
import com.mipatrimonio.app.core.ocr.PdfBitmapRenderer
import com.mipatrimonio.app.core.ocr.SuggestedDocumentKind
import com.mipatrimonio.app.core.storage.EncryptedDocumentStore
import com.mipatrimonio.app.feature.add.PayrollDraft
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DocumentsUiState(
    val documents: List<DocumentEntity> = emptyList(),
    val query: String = "",
    val saving: Boolean = false,
    val error: Boolean = false,
    val pendingPayrollDraft: PayrollDraft? = null,
    val pendingDuplicateWarning: Boolean = false,
)

@HiltViewModel
class DocumentsViewModel @Inject constructor(
    private val documentDao: DocumentDao,
    private val store: EncryptedDocumentStore,
    private val ocrService: OcrService,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val saving = MutableStateFlow(false)
    private val error = MutableStateFlow(false)
    private val pendingPayrollDraft = MutableStateFlow<PayrollDraft?>(null)
    private val pendingDuplicateWarning = MutableStateFlow(false)

    private val baseState = combine(documentDao.observeAll(), query, saving, error) { docs, q, s, e ->
        val filtered = if (q.isBlank()) docs else docs.filter { it.title.lowercase().contains(q.trim().lowercase()) }
        Triple(filtered, q, Pair(s, e))
    }
    val uiState: StateFlow<DocumentsUiState> = combine(
        baseState, pendingPayrollDraft, pendingDuplicateWarning,
    ) { base, draft, dup ->
        DocumentsUiState(base.first, base.second, base.third.first, base.third.second, draft, dup)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DocumentsUiState())

    fun setQuery(value: String) { query.value = value }
    fun clearError() { error.value = false }

    fun onFilePicked(uri: Uri) {
        if (saving.value) return
        viewModelScope.launch {
            saving.value = true
            error.value = false
            try {
                val outcome = withContext(Dispatchers.IO) { analyzeUri(uri) }
                when (outcome) {
                    is PickOutcome.Payroll -> {
                        val dups = findPayrollDuplicates(outcome.draft.date, outcome.draft.displayName.removeSuffix(".pdf"))
                        if (dups.isNotEmpty()) {
                            pendingPayrollDraft.value = outcome.draft
                            pendingDuplicateWarning.value = true
                        } else {
                            pendingPayrollDraft.value = outcome.draft
                        }
                    }
                    is PickOutcome.PlainDocument -> {
                        // Ya guardado en IO
                    }
                }
            } catch (_: Exception) {
                error.value = true
            } finally {
                saving.value = false
            }
        }
    }

    fun confirmDuplicateAndShowDraft() {
        pendingDuplicateWarning.value = false
    }

    fun dismissPendingPayroll(deleteFile: Boolean = true) {
        val draft = pendingPayrollDraft.value
        pendingPayrollDraft.value = null
        pendingDuplicateWarning.value = false
        if (deleteFile && draft != null) {
            viewModelScope.launch(Dispatchers.IO) { runCatching { File(draft.sourceFilePath).delete() } }
        }
    }

    fun onPayrollSaved() {
        dismissPendingPayroll(deleteFile = false) // savePayroll ya borra el temporal
    }

    fun updateDocument(document: DocumentEntity, title: String, kind: DocumentKind, expiresAt: Long?) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    documentDao.update(
                        document.copy(
                            title = title.trim().ifBlank { document.title },
                            kind = kind,
                            expiresAt = expiresAt,
                        ),
                    )
                }
            } catch (_: Exception) { error.value = true }
        }
    }

    fun delete(document: DocumentEntity) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    documentDao.delete(document)
                    store.delete(document.filePath)
                }
            } catch (_: Exception) { error.value = true }
        }
    }

    private sealed interface PickOutcome {
        data class Payroll(val draft: PayrollDraft) : PickOutcome
        data object PlainDocument : PickOutcome
    }

    private suspend fun analyzeUri(uri: Uri): PickOutcome {
        val displayName = queryDisplayName(uri) ?: "Documento"
        val mime = context.contentResolver.getType(uri) ?: "application/pdf"
        val isPdf = mime == "application/pdf" || displayName.endsWith(".pdf", true)
        // Copiar a temp para OCR y posible guardado posterior
        val temp = copyToTemp(uri, if (isPdf) ".pdf" else ".bin")
        val bitmap = if (isPdf) PdfBitmapRenderer.renderFirstPage(temp) else decodeBitmap(temp) ?: return savePlain(uri, displayName, mime)
        val lines = try { ocrService.recognize(bitmap).lines } finally { bitmap.recycle() }
        val classification = DocumentClassifier.classify(lines)
        if (classification.suggested == SuggestedDocumentKind.PAYROLL || classification.payrollScore >= 2) {
            val thumb = if (isPdf) runCatching {
                val b = PdfBitmapRenderer.renderFirstPage(temp)
                thumbnailOf(b).also { b.recycle() }
            }.getOrNull() else decodeBitmap(temp)?.let { b -> thumbnailOf(b).also { b.recycle() } }
            return PickOutcome.Payroll(
                PayrollDraft(
                    sourceFilePath = temp.absolutePath,
                    mimeType = if (isPdf) "application/pdf" else mime,
                    displayName = displayName,
                    amountCents = classification.suggestedNetCents,
                    date = classification.suggestedDate ?: LocalDate.now(),
                    thumbnail = thumb,
                )
            )
        }
        // No es nómina reconocida: guardar como documento directamente (comportamiento previo)
        runCatching { temp.delete() }
        return savePlain(uri, displayName, mime)
    }

    private suspend fun savePlain(uri: Uri, displayName: String, mime: String): PickOutcome {
        val relativePath = context.contentResolver.openInputStream(uri)?.use { store.save(it) }
            ?: throw IllegalStateException("No se pudo abrir el archivo")
        documentDao.insert(
            DocumentEntity(
                kind = DocumentKind.PAYSLIP,
                title = displayName.removeSuffix(".pdf").ifBlank { "Documento" },
                filePath = relativePath,
                mimeType = mime,
                date = LocalDate.now().toEpochDay(),
                createdAt = System.currentTimeMillis(),
            )
        )
        return PickOutcome.PlainDocument
    }

    private suspend fun findPayrollDuplicates(date: LocalDate, title: String): List<DocumentEntity> {
        val t = title.trim()
        return documentDao.getAllByKind(DocumentKind.PAYSLIP).filter { d ->
            d.date == date.toEpochDay() || (t.isNotBlank() && d.title.trim().equals(t, true))
        }
    }

    private fun copyToTemp(uri: Uri, suffix: String): File {
        val dir = File(context.cacheDir, "doc_inbox").also { it.mkdirs() }
        val f = File(dir, "${UUID.randomUUID()}$suffix")
        context.contentResolver.openInputStream(uri)?.use { i -> FileOutputStream(f).use { o -> i.copyTo(o) } }
            ?: error("open")
        return f
    }

    private fun decodeBitmap(file: File): Bitmap? =
        android.graphics.BitmapFactory.decodeFile(file.absolutePath)

    private fun thumbnailOf(source: Bitmap): Bitmap {
        val s = 480f / maxOf(source.width, source.height).coerceAtLeast(1)
        val scale = minOf(s, 1f)
        return Bitmap.createScaledBitmap(source, (source.width * scale).toInt().coerceAtLeast(1), (source.height * scale).toInt().coerceAtLeast(1), true)
    }

    private fun queryDisplayName(uri: Uri): String? = try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    } catch (_: Exception) { null }
}
