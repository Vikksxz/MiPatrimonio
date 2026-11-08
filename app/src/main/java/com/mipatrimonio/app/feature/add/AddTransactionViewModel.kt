package com.mipatrimonio.app.feature.add

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mipatrimonio.app.core.database.dao.CardDao
import com.mipatrimonio.app.core.database.dao.CategoryDao
import com.mipatrimonio.app.core.database.dao.DocumentDao
import com.mipatrimonio.app.core.database.dao.TagDao
import com.mipatrimonio.app.core.database.entity.CardEntity
import com.mipatrimonio.app.core.database.entity.CategoryEntity
import com.mipatrimonio.app.core.database.entity.DocumentEntity
import com.mipatrimonio.app.core.database.entity.TagEntity
import com.mipatrimonio.app.core.database.entity.TransactionEntity
import com.mipatrimonio.app.core.model.DocumentKind
import com.mipatrimonio.app.core.model.IncomeKind
import com.mipatrimonio.app.core.ocr.DocumentClassification
import com.mipatrimonio.app.core.ocr.DocumentClassifier
import com.mipatrimonio.app.core.ocr.OcrService
import com.mipatrimonio.app.core.ocr.ReceiptParser
import com.mipatrimonio.app.core.storage.EncryptedDocumentStore
import com.mipatrimonio.app.core.storage.ReceiptImageStore
import com.mipatrimonio.app.domain.usecase.AddTransaction
import com.mipatrimonio.app.domain.usecase.DeleteTransaction
import com.mipatrimonio.app.domain.usecase.NewTransaction
import com.mipatrimonio.app.domain.usecase.UpdateTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.io.FileInputStream
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Borrador de ticket escaneado (T2.5): no se persiste hasta confirmar el guardado. */
data class ReceiptScanDraft(
    val amountCents: Long?,
    val date: LocalDate,
    val merchant: String?,
    val sourceBitmap: Bitmap,
    val thumbnail: Bitmap,
)

/** Borrador de nómina PDF (confirmación obligatoria): guarda documento cifrado + ingreso. */
data class PayrollDraft(
    val sourceFilePath: String,
    val mimeType: String,
    val displayName: String,
    val amountCents: Long?,
    val date: LocalDate,
    val thumbnail: Bitmap?,
)

/**
 * Estado del formulario de revisión del botón + (T1.7).
 * Ofrece tarjetas y categorías a la hoja y guarda mediante [AddTransaction].
 */
@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    categoryDao: CategoryDao,
    cardDao: CardDao,
    private val addTransaction: AddTransaction,
    private val updateTransaction: UpdateTransaction,
    private val deleteTransaction: DeleteTransaction,
    private val ocrService: OcrService,
    private val receiptImageStore: ReceiptImageStore,
    private val documentStore: EncryptedDocumentStore,
    private val documentDao: DocumentDao,
    private val tagDao: TagDao,
) : ViewModel() {

    val cards: StateFlow<List<CardEntity>> = cardDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = categoryDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val tags: StateFlow<List<TagEntity>> = tagDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var saveFailed by mutableStateOf(false)
        private set

    var saving by mutableStateOf(false)
        private set

    fun reset() {
        saveFailed = false
        saving = false
    }

    fun save(input: NewTransaction, onSaved: (Long) -> Unit) {
        if (!begin()) return
        viewModelScope.launch {
            runCatching { addTransaction(input) }
                .onSuccess(onSaved)
                .onFailure(::fail)
        }
    }

    fun update(existing: TransactionEntity, input: NewTransaction, onSaved: (Long) -> Unit) {
        if (!begin()) return
        viewModelScope.launch {
            runCatching { updateTransaction(existing, input) }
                .onSuccess(onSaved)
                .onFailure(::fail)
        }
    }

    fun delete(existing: TransactionEntity, onDeleted: () -> Unit) {
        if (!begin()) return
        viewModelScope.launch {
            runCatching { deleteTransaction(existing) }
                .onSuccess { onDeleted() }
                .onFailure(::fail)
        }
    }

    /** Marca la operación como en curso; false si ya había una en marcha. */
    private fun begin(): Boolean {
        if (saving) return false
        saving = true
        saveFailed = false
        return true
    }

    private fun fail(error: Throwable) {
        saving = false
        if (error is CancellationException) throw error
        saveFailed = true
    }

    suspend fun recognizeLines(bitmap: Bitmap): List<String> = withContext(Dispatchers.IO) {
        ocrService.recognize(bitmap).lines
    }

    /**
     * OCR + parseo del ticket (T2.5). No persiste nada hasta confirmar el guardado (§8.3/§9.1).
     * [bitmap] no se recicla aquí.
     */
    suspend fun scanReceipt(bitmap: Bitmap): ReceiptScanDraft = withContext(Dispatchers.IO) {
        val lines = ocrService.recognize(bitmap).lines
        val parsed = ReceiptParser.parse(lines)
        val thumbnail = renderThumbnail(bitmap)
        ReceiptScanDraft(
            amountCents = parsed.amountCents,
            date = parsed.date ?: LocalDate.now(),
            merchant = parsed.merchant?.takeIf { it.isNotBlank() },
            sourceBitmap = bitmap,
            thumbnail = thumbnail,
        )
    }

    private fun renderThumbnail(source: Bitmap): Bitmap {
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

    /** Guarda un movimiento con ticket escaneado: cifra la imagen y la adjunta. */
    fun saveScanned(input: NewTransaction, receiptBitmap: Bitmap, onSaved: (Long) -> Unit) {
        if (!begin()) return
        viewModelScope.launch {
            runCatching {
                val receiptPath = withContext(Dispatchers.IO) { receiptImageStore.save(receiptBitmap) }
                addTransaction(input.copy(receiptPath = receiptPath))
            }
                .onSuccess(onSaved)
                .onFailure(::fail)
        }
    }

    /** Guarda nómina confirmada: documento PDF cifrado + ingreso (nunca sin confirmación). */
    fun savePayroll(input: NewTransaction, draft: PayrollDraft, onSaved: (Long) -> Unit) {
        if (!begin()) return
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val file = File(draft.sourceFilePath)
                    val relativePath = FileInputStream(file).use { documentStore.save(it) }
                    documentDao.insert(
                        DocumentEntity(
                            kind = DocumentKind.PAYSLIP,
                            title = input.merchant.trim().ifBlank { draft.displayName.removeSuffix(".pdf") },
                            filePath = relativePath,
                            mimeType = draft.mimeType.ifBlank { "application/pdf" },
                            date = input.date.toEpochDay(),
                            createdAt = System.currentTimeMillis(),
                        ),
                    )
                    runCatching { file.delete() }
                    addTransaction(input.copy(incomeKind = IncomeKind.PAYROLL))
                }
            }
                .onSuccess(onSaved)
                .onFailure(::fail)
        }
    }

    suspend fun findPayrollDuplicates(date: LocalDate, title: String): List<DocumentEntity> =
        withContext(Dispatchers.IO) {
            val trimmed = title.trim()
            documentDao.getAllByKind(DocumentKind.PAYSLIP).filter { doc ->
                val sameDate = doc.date == date.toEpochDay()
                val sameTitle = trimmed.isNotBlank() && doc.title.trim().equals(trimmed, ignoreCase = true)
                sameDate || sameTitle
            }
        }

    suspend fun getTagIdsForTransaction(txId: Long): List<Long> = withContext(Dispatchers.IO) {
        tagDao.getTagIdsForTransaction(txId)
    }

    fun createTag(name: String, onCreated: (Long) -> Unit) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val id = withContext(Dispatchers.IO) {
                tagDao.getByName(trimmed)?.id ?: tagDao.insert(TagEntity(name = trimmed))
            }
            onCreated(id)
        }
    }
}
