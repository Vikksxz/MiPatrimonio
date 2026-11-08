package com.mipatrimonio.app.domain.usecase

import java.time.YearMonth
import kotlin.math.ceil
import kotlin.math.max

/** Estado del plan de ahorro (§7.2). */
enum class SavingsStatus { ON_TRACK, AHEAD, BEHIND }

data class SavingsEstimate(
    val contributedCents: Long,
    val remainingCents: Long,
    val monthsEstimated: Int,
    val targetYearMonth: YearMonth,
    val status: SavingsStatus,
    val goalReached: Boolean,
)

/**
 * Estimación pura de ahorro (§7.2). Sin acceso a BD.
 * [contributedCents] suma de aportaciones (puede incluir retiradas negativas).
 * [monthlyQuotaCents] cuota mensual (> 0).
 * [freeMonthlyCents] dinero libre mensual estimado para evaluar si el plan es asumible.
 * [initialEstimatedMonths] estimación de referencia al crear/editar (para AHEAD).
 */
object EstimateSavings {

    const val QUOTA_STEP_CENTS = 2_500L // 25 €

    fun estimate(
        targetCents: Long,
        contributedCents: Long,
        monthlyQuotaCents: Long,
        freeMonthlyCents: Long,
        initialEstimatedMonths: Int,
        now: YearMonth = YearMonth.now(),
    ): SavingsEstimate {
        val safeTarget = max(0L, targetCents)
        val contributed = contributedCents
        val remaining = max(0L, safeTarget - contributed)
        val quota = max(1L, monthlyQuotaCents)
        val goalReached = remaining == 0L
        val months = if (goalReached) 0 else ceil(remaining.toDouble() / quota.toDouble()).toInt().coerceAtLeast(1)
        val targetMonth = now.plusMonths(months.toLong())
        val status = when {
            goalReached -> SavingsStatus.AHEAD
            monthlyQuotaCents > freeMonthlyCents -> SavingsStatus.BEHIND
            months < initialEstimatedMonths -> SavingsStatus.AHEAD
            else -> SavingsStatus.ON_TRACK
        }
        return SavingsEstimate(
            contributedCents = contributed,
            remainingCents = remaining,
            monthsEstimated = months,
            targetYearMonth = targetMonth,
            status = status,
            goalReached = goalReached,
        )
    }

    /** Meses estimados al fijar objetivo/cuota (para guardar como initialEstimatedMonths). */
    fun initialMonths(targetCents: Long, contributedCents: Long, monthlyQuotaCents: Long): Int {
        val remaining = max(0L, max(0L, targetCents) - contributedCents)
        if (remaining == 0L) return 0
        val quota = max(1L, monthlyQuotaCents)
        return ceil(remaining.toDouble() / quota.toDouble()).toInt().coerceAtLeast(1)
    }
}
