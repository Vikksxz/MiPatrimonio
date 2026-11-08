package com.mipatrimonio.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.mipatrimonio.app.core.notifications.AppNotificationChannels
import com.mipatrimonio.app.core.settings.SettingsRepository
import com.mipatrimonio.app.feature.alerts.AlertsWorker
import com.mipatrimonio.app.feature.recurring.RecurringWorker
import com.mipatrimonio.app.feature.reminders.CashReminderWorker
import com.mipatrimonio.app.feature.widget.WidgetUpdateWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Clase Application de la app. Punto de entrada de Hilt y configuración de WorkManager.
 * Canales de notificación creados al inicio (§12, T3.6).
 */
@HiltAndroidApp
class MiPatrimonioApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        AppNotificationChannels.createAll(this)
        scheduleRecurringWorker()
        scheduleAlertsWorker()
        scheduleCashReminderIfEnabled()
        WidgetUpdateWorker.schedule(this)
    }

    private fun scheduleCashReminderIfEnabled() {
        appScope.launch {
            if (settingsRepository.cashReminderEnabled.first()) {
                CashReminderWorker.schedule(this@MiPatrimonioApp)
            }
        }
    }

    private fun scheduleRecurringWorker() {
        val request = PeriodicWorkRequestBuilder<RecurringWorker>(24, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            RecurringWorker.NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    private fun scheduleAlertsWorker() {
        val request = PeriodicWorkRequestBuilder<AlertsWorker>(24, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            AlertsWorker.NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
