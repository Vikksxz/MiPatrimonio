package com.mipatrimonio.app.feature.wallet

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.mipatrimonio.app.core.database.AppDatabase
import android.net.Uri
import com.mipatrimonio.app.R
import com.mipatrimonio.app.core.backup.BackupManager
import com.mipatrimonio.app.core.database.dao.BudgetDao
import com.mipatrimonio.app.core.database.dao.CardDao
import com.mipatrimonio.app.core.database.dao.CategoryDao
import com.mipatrimonio.app.core.database.dao.RecurringDao
import com.mipatrimonio.app.core.database.dao.SavingsDao
import com.mipatrimonio.app.core.database.dao.TransactionDao
import com.mipatrimonio.app.core.database.entity.CardEntity
import com.mipatrimonio.app.core.database.entity.CategoryEntity
import com.mipatrimonio.app.core.database.entity.RecurringEntity
import com.mipatrimonio.app.core.export.CsvExporter
import com.mipatrimonio.app.core.export.ExportFileSharer
import com.mipatrimonio.app.core.export.MonthlyReportPdfExporter
import com.mipatrimonio.app.core.export.PdfReportExporter
import com.mipatrimonio.app.core.model.TransactionType
import com.mipatrimonio.app.core.settings.SettingsRepository
import com.mipatrimonio.app.domain.usecase.GetMonthlySummary
import com.mipatrimonio.app.domain.usecase.MonthlyReportBuilder
import com.mipatrimonio.app.feature.reminders.CashReminderWorker
import kotlinx.coroutines.flow.first
import java.io.File
import java.time.YearMonth
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Estado y operaciones de las tarjetas de la Cartera (T1.6).
 * La lógica de persistencia vive aquí, no en los Composables.
 */
@HiltViewModel
class WalletViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val cardDao: CardDao,
    private val recurringDao: RecurringDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val savingsDao: SavingsDao,
    private val budgetDao: BudgetDao,
    private val settingsRepository: SettingsRepository,
    private val getMonthlySummary: GetMonthlySummary,
    private val backupManager: BackupManager,
) : ViewModel() {

    private val _cards = MutableStateFlow<List<CardEntity>>(emptyList())
    val cards = _cards.asStateFlow()

    private val _recurrings = MutableStateFlow<List<RecurringEntity>>(emptyList())
    val recurrings = _recurrings.asStateFlow()

    private val _expenseCategories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val expenseCategories = _expenseCategories.asStateFlow()

    private val _cashReminderEnabled = MutableStateFlow(false)
    val cashReminderEnabled = _cashReminderEnabled.asStateFlow()

    private val _backupBusy = MutableStateFlow(false)
    val backupBusy = _backupBusy.asStateFlow()

    init {
        viewModelScope.launch {
            cardDao.observeAll().collect { cards ->
                _cards.value = cards
            }
        }
        viewModelScope.launch {
            recurringDao.observeActive().collect { _recurrings.value = it }
        }
        viewModelScope.launch {
            categoryDao.observeByIncome(false).collect { _expenseCategories.value = it }
        }
        viewModelScope.launch {
            settingsRepository.cashReminderEnabled.collect { _cashReminderEnabled.value = it }
        }
    }

    /**
     * Inserta o actualiza una tarjeta. Si la foto cambió, borra del almacenamiento
     * privado la imagen anterior ([obsoletePhotoPath]).
     */
    fun saveCard(card: CardEntity, obsoletePhotoPath: String? = null) {
        viewModelScope.launch {
            if (card.id == 0L) {
                cardDao.insert(card)
            } else {
                cardDao.update(card)
            }
            if (!obsoletePhotoPath.isNullOrBlank() && obsoletePhotoPath != card.photoPath) {
                withContext(Dispatchers.IO) {
                    CardPhotoStore.delete(context, obsoletePhotoPath)
                }
            }
        }
    }

    fun deleteCard(card: CardEntity) {
        viewModelScope.launch {
            database.withTransaction {
                cardDao.delete(card)
                database.transactionDao().nullifyCardId(card.id)
            }
            withContext(Dispatchers.IO) {
                CardPhotoStore.delete(context, card.photoPath)
            }
        }
    }

    fun saveRecurring(entity: RecurringEntity) {
        viewModelScope.launch {
            if (entity.id == 0L) recurringDao.insert(entity) else recurringDao.update(entity)
        }
    }

    fun deleteRecurring(entity: RecurringEntity) {
        viewModelScope.launch { recurringDao.delete(entity) }
    }

    fun setCashReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setCashReminderEnabled(enabled)
            if (enabled) CashReminderWorker.schedule(context) else CashReminderWorker.cancel(context)
        }
    }

    fun exportCsv(onReady: (File, String) -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            try {
                val (file, mime) = withContext(Dispatchers.IO) {
                    val txs = transactionDao.getAll()
                    val cats = categoryDao.getAll().associateBy { it.id }
                    val typeLabel: (TransactionType) -> String = { t ->
                        when (t) {
                            TransactionType.CASH -> context.getString(R.string.type_cash)
                            TransactionType.CARD -> context.getString(R.string.type_card)
                            TransactionType.SUBSCRIPTION -> context.getString(R.string.type_subscription)
                            TransactionType.INCOME -> context.getString(R.string.type_income)
                        }
                    }
                    val csv = CsvExporter.export(txs, cats, typeLabel)
                    val out = File(ExportFileSharer.exportsDir(context), "mipatrimonio-movimientos.csv")
                    out.writeText(csv, Charsets.UTF_8)
                    out to "text/csv"
                }
                onReady(file, mime)
            } catch (_: Exception) {
                onError()
            }
        }
    }

    fun exportPdf(onReady: (File, String) -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            try {
                val (file, mime) = withContext(Dispatchers.IO) {
                    val month = YearMonth.now()
                    val summary = getMonthlySummary(month).first()
                    val name = "mipatrimonio-resumen-${month.year}-${month.monthValue.toString().padStart(2, '0')}.pdf"
                    val out = PdfReportExporter.generate(context, summary, name)
                    out to "application/pdf"
                }
                onReady(file, mime)
            } catch (_: Exception) {
                onError()
            }
        }
    }

    fun generateMonthlyReport(onReady: (File, String) -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            try {
                val (file, mime) = withContext(Dispatchers.IO) {
                    val target = YearMonth.now().minusMonths(1)
                    val from = target.minusMonths(1).atDay(1).toEpochDay()
                    val to = target.plusMonths(1).atDay(1).toEpochDay()
                    val txs = transactionDao.getByMonth(from, to)
                    val categories = categoryDao.getAll()
                    val plans = savingsDao.getAllPlans()
                    val contributions = savingsDao.getAllContributions()
                    val report = MonthlyReportBuilder.build(target, txs, categories, plans, contributions)
                    val name = "informe-mensual-${target.year}-${target.monthValue.toString().padStart(2, '0')}.pdf"
                    val out = MonthlyReportPdfExporter.generate(context, report, name)
                    out to "application/pdf"
                }
                onReady(file, mime)
            } catch (_: Exception) {
                onError()
            }
        }
    }

    fun defaultBackupFileName(): String = backupManager.defaultFileName()

    fun createBackup(uri: Uri, password: CharArray, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            _backupBusy.value = true
            val ok = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        backupManager.exportToStream(password, out)
                    } ?: return@withContext false
                    true
                } catch (_: Exception) {
                    false
                } finally {
                    password.fill('\u0000')
                }
            }
            _backupBusy.value = false
            onDone(ok)
        }
    }

    fun restoreBackup(uri: Uri, password: CharArray, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            _backupBusy.value = true
            val ok = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        backupManager.importFromStream(password, input)
                    } ?: return@withContext false
                    true
                } catch (_: Exception) {
                    false
                } finally {
                    password.fill('\u0000')
                }
            }
            _backupBusy.value = false
            onDone(ok)
        }
    }
}
