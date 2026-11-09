package com.mipatrimonio.app.feature.reports

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mipatrimonio.app.core.database.dao.CategoryDao
import com.mipatrimonio.app.core.database.dao.SavingsDao
import com.mipatrimonio.app.core.database.dao.TransactionDao
import com.mipatrimonio.app.core.export.MonthlyReportPdfExporter
import com.mipatrimonio.app.core.notifications.MonthlyReportNotifier
import com.mipatrimonio.app.core.settings.SettingsRepository
import com.mipatrimonio.app.domain.usecase.MonthlyReportBuilder
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException

@HiltWorker
class MonthlyReportWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val savingsDao: SavingsDao,
    private val settingsRepository: SettingsRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val today = LocalDate.now()
            if (today.dayOfMonth != 1) return Result.success()
            val target = YearMonth.from(today).minusMonths(1)
            val key = target.year * 100 + target.monthValue
            if (settingsRepository.lastMonthlyReportMonth() == key) return Result.success()

            val from = target.minusMonths(1).atDay(1).toEpochDay()
            val to = target.plusMonths(1).atDay(1).toEpochDay()
            val txs = transactionDao.getByMonth(from, to)
            val categories = categoryDao.getAll()
            val plans = savingsDao.getAllPlans()
            val contributions = savingsDao.getAllContributions()
            val report = MonthlyReportBuilder.build(target, txs, categories, plans, contributions)
            val fileName = "informe-mensual-${target.year}-${target.monthValue.toString().padStart(2, '0')}.pdf"
            MonthlyReportPdfExporter.generate(applicationContext, report, fileName)

            val locale = Locale.forLanguageTag("es-ES")
            val label = target.month.getDisplayName(TextStyle.FULL, locale)
                .replaceFirstChar { it.uppercase(locale) } + " " + target.year
            MonthlyReportNotifier.notify(applicationContext, label)
            settingsRepository.setLastMonthlyReportMonth(key)
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val NAME = "monthly_report_daily_check"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<MonthlyReportWorker>(24, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
