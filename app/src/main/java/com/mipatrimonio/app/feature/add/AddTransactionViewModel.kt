package com.mipatrimonio.app.feature.add

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mipatrimonio.app.core.database.dao.CardDao
import com.mipatrimonio.app.core.database.dao.CategoryDao
import com.mipatrimonio.app.core.database.entity.CardEntity
import com.mipatrimonio.app.core.database.entity.CategoryEntity
import com.mipatrimonio.app.core.database.entity.TransactionEntity
import com.mipatrimonio.app.core.ocr.OcrService
import com.mipatrimonio.app.core.ocr.ReceiptParser
import com.mipatrimonio.app.core.storage.ReceiptImageStore
import com.mipatrimonio.app.domain.usecase.AddTransaction
import com.mipatrimonio.app.domain.usecase.DeleteTransaction
import com.mipatrimonio.app.domain.usecase.NewTransaction
import com.mipatrimonio.app.domain.usecase.UpdateTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
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
) : ViewModel() {

    val cards: StateFlow<List<CardEntity>> = cardDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = categoryDao.observeAll()
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

    /**
     * OCR + parseo del ticket (T2.5). No persiste nada hasta confirmar el guardado (§8.3/§9.1).
     * [bitmap] no se recicla aquí.
     */
    suspend fun scanReceipt(bitmap: Bitmap): ReceiptScanDraft = withContext(Dispatchers.IO) {
        val ocr = ocrService.recognize(bitmap)
        val parsed = ReceiptParser.parse(ocr.lines)
        val thumbSide = 480
        val scale = minOf(
            thumbSide.toFloat() / bitmap.width.coerceAtLeast(1),
            thumbSide.toFloat() / bitmap.height.coerceAtLeast(1),
            1f,
        )
        val thumbW = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val thumbH = (bitmap.height * scale).toInt().coerceAtLeast(1)
        val thumbnail = Bitmap.createScaledBitmap(bitmap, thumbW, thumbH, true)
        ReceiptScanDraft(
            amountCents = parsed.amountCents,
            date = parsed.date ?: LocalDate.now(),
            merchant = parsed.merchant?.takeIf { it.isNotBlank() },
            sourceBitmap = bitmap,
            thumbnail = thumbnail,
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
}
