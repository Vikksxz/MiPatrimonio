package com.mipatrimonio.app.feature.alerts

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mipatrimonio.app.domain.usecase.EvaluateBudgetAlerts
import com.mipatrimonio.app.domain.usecase.EvaluateSavingsAlerts
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

@HiltWorker
class AlertsWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val evaluateBudgetAlerts: EvaluateBudgetAlerts,
    private val evaluateSavingsAlerts: EvaluateSavingsAlerts,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            evaluateBudgetAlerts.run()
            evaluateSavingsAlerts.run()
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val NAME = "alerts_daily"

        fun enqueueNow(context: Context) {
            WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<AlertsWorker>().build())
        }
    }
}
