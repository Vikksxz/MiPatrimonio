package com.mipatrimonio.app.feature.reminders

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mipatrimonio.app.core.notifications.CashReminderNotifier
import com.mipatrimonio.app.core.settings.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

@HiltWorker
class CashReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val settingsRepository: SettingsRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            if (settingsRepository.cashReminderEnabled.first()) {
                CashReminderNotifier.notify(applicationContext)
            }
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val NAME = "cash_reminder_daily"
        private const val TARGET_HOUR = 21

        fun schedule(context: Context) {
            val delay = initialDelayToHour(TARGET_HOUR)
            val request = PeriodicWorkRequestBuilder<CashReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(NAME)
        }

        internal fun initialDelayToHour(targetHour: Int, now: ZonedDateTime = ZonedDateTime.now()): Duration {
            var next = now.withHour(targetHour).withMinute(0).withSecond(0).withNano(0)
            if (!next.isAfter(now)) next = next.plusDays(1)
            return Duration.between(now, next)
        }
    }
}
