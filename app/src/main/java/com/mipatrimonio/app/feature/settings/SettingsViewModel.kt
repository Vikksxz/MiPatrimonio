package com.mipatrimonio.app.feature.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mipatrimonio.app.R
import com.mipatrimonio.app.core.backup.BackupManager
import com.mipatrimonio.app.core.database.dao.CategoryDao
import com.mipatrimonio.app.core.database.dao.ExchangeRateDao
import com.mipatrimonio.app.core.database.dao.SavingsDao
import com.mipatrimonio.app.core.database.dao.TransactionDao
import com.mipatrimonio.app.core.database.entity.ExchangeRateEntity
import com.mipatrimonio.app.core.export.CsvExporter
import com.mipatrimonio.app.core.export.MonthlyReportPdfExporter
import com.mipatrimonio.app.core.export.PdfReportExporter
import com.mipatrimonio.app.core.model.TransactionType
import com.mipatrimonio.app.core.settings.SettingsRepository
import com.mipatrimonio.app.domain.usecase.GetMonthlySummary
import com.mipatrimonio.app.domain.usecase.MonthlyReportBuilder
import com.mipatrimonio.app.feature.reminders.CashReminderWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val savingsDao: SavingsDao,
    private val exchangeRateDao: ExchangeRateDao,
    private val settingsRepository: SettingsRepository,
    private val getMonthlySummary: GetMonthlySummary,
    private val backupManager: BackupManager,
) : ViewModel() {

    private val _cashReminderEnabled = MutableStateFlow(false)
    val cashReminderEnabled = _cashReminderEnabled.asStateFlow()

    private val _exchangeRates = MutableStateFlow<List<ExchangeRateEntity>>(emptyList())
    val exchangeRates = _exchangeRates.asStateFlow()

    private val _backupBusy = MutableStateFlow(false)
    val backupBusy = _backupBusy.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.cashReminderEnabled.collect { _cashReminderEnabled.value = it }
        }
        viewModelScope.launch {
            exchangeRateDao.observeAll().collect { _exchangeRates.value = it }
        }
    }

    fun setCashReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setCashReminderEnabled(enabled)
            if (enabled) CashReminderWorker.schedule(context) else CashReminderWorker.cancel(context)
        }
    }

    fun saveExchangeRate(currency: String, rateText: String, onError: () -> Unit = {}, onSaved: () -> Unit = {}) {
        val code = currency.trim().uppercase()
        val rate = rateText.trim().replace(',', '.').toDoubleOrNull()
        if (code.isBlank() || code == "EUR" || rate == null || !rate.isFinite() || rate <= 0.0) {
            onError(); return
        }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                exchangeRateDao.upsert(ExchangeRateEntity(code, rate, System.currentTimeMillis()))
            }
            onSaved()
        }
    }

    fun deleteExchangeRate(currency: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { exchangeRateDao.delete(currency.trim().uppercase()) }
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
                    val out = File(exportsDir(context), "mipatrimonio-movimientos.csv")
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

    private fun exportsDir(context: Context): File =
        File(context.cacheDir, "exports").also { it.mkdirs() }
}
