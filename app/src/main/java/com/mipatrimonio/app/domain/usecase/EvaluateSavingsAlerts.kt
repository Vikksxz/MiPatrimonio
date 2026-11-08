package com.mipatrimonio.app.domain.usecase

import android.content.Context
import com.mipatrimonio.app.core.database.dao.SavingsDao
import com.mipatrimonio.app.core.notifications.SavingsNotifier
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.YearMonth
import javax.inject.Inject
import kotlin.math.abs

/** Evalúa planes de ahorro activos y envía avisos según §7.3. */
class EvaluateSavingsAlerts @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val savingsDao: SavingsDao,
) {
    suspend fun run(now: YearMonth = YearMonth.now()) {
        val plans = savingsDao.getActivePlans()
        for (plan in plans) {
            val contributed = savingsDao.getContributed(plan.id) ?: 0L
            val estimate = EstimateSavings.estimate(
                targetCents = plan.targetCents,
                contributedCents = contributed,
                monthlyQuotaCents = plan.monthlyQuotaCents,
                // Neutral, coherente con UI actual (sin media 3 meses implementada).
                freeMonthlyCents = plan.monthlyQuotaCents,
                initialEstimatedMonths = plan.initialEstimatedMonths,
                now = now,
            )
            val newState = estimate.status.name
            val stateChanged = newState != plan.lastNotifiedState
            val monthsChanged = plan.lastNotifiedMonths < 0 || abs(estimate.monthsEstimated - plan.lastNotifiedMonths) >= 1
            if (stateChanged || monthsChanged) {
                when (estimate.status) {
                    SavingsStatus.AHEAD -> SavingsNotifier.notifyAhead(
                        context, plan.id, plan.name, estimate.monthsEstimated, estimate.targetYearMonth
                    )
                    SavingsStatus.BEHIND -> SavingsNotifier.notifyBehind(context, plan.id, plan.name)
                    SavingsStatus.ON_TRACK -> { /* sin mensaje específico en §7.3 */ }
                }
                savingsDao.updateNotifiedState(plan.id, newState, estimate.monthsEstimated)
            }
        }
    }
}
