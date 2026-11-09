package com.mipatrimonio.app.feature.documents

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mipatrimonio.app.core.database.dao.DocumentDao
import com.mipatrimonio.app.core.notifications.DocumentExpiryNotifier
import com.mipatrimonio.app.core.settings.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

@HiltWorker
class DocumentExpiryWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val documentDao: DocumentDao,
    private val settingsRepository: SettingsRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val today = LocalDate.now()
            val limit = today.plusDays(NOTIFY_DAYS_BEFORE).toEpochDay()
            val expiring = documentDao.observeExpiringBefore(limit).first()
                .filter { it.expiresAt != null && it.expiresAt >= today.toEpochDay() }
            val notified = settingsRepository.notifiedExpiringDocumentIds()
            val fmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es-ES"))
            val newIds = mutableSetOf<Long>()
            expiring.forEach { doc ->
                if (doc.id in notified) return@forEach
                val label = LocalDate.ofEpochDay(doc.expiresAt!!).format(fmt)
                DocumentExpiryNotifier.notify(applicationContext, doc.id, doc.title, label)
                newIds += doc.id
            }
            settingsRepository.addNotifiedExpiringDocumentIds(newIds)
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val NAME = "document_expiry_daily_check"
        private const val NOTIFY_DAYS_BEFORE = 7L

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<DocumentExpiryWorker>(24, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
