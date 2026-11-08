package com.mipatrimonio.app.feature.recurring

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mipatrimonio.app.core.database.dao.RecurringDao
import com.mipatrimonio.app.core.notifications.RecurringNotifier
import com.mipatrimonio.app.domain.usecase.ProcessRecurringRules
import com.mipatrimonio.app.feature.alerts.AlertsWorker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate

@HiltWorker
class RecurringWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val processRecurringRules: ProcessRecurringRules,
    private val recurringDao: RecurringDao,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val today = LocalDate.now()
        val todayEpoch = today.toEpochDay()
        return try {
            processRecurringRules.run(todayEpoch)
            // Aviso previo de suscripciones (§7.6): notificar cuando faltan exactamente remindDaysBefore días.
            RecurringNotifier.ensureChannels(applicationContext)
            val active = recurringDao.getDueUpTo(today.plusDays(31).toEpochDay())
            active.filter { it.isSubscription && it.active }.forEach { rule ->
                val next = LocalDate.ofEpochDay(rule.nextDate)
                val daysUntil = java.time.temporal.ChronoUnit.DAYS.between(today, next).toInt()
                if (daysUntil == rule.remindDaysBefore) {
                    RecurringNotifier.notifyUpcoming(applicationContext, rule, next)
                }
            }
            // §12: tras ejecutar RecurringWorker, lanzar comprobaciones de alertas.
            AlertsWorker.enqueueNow(applicationContext)
            Result.success()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val NAME = "recurring_daily"
    }
}
