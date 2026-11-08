package com.mipatrimonio.app.domain.usecase

import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests de estimación de ahorro (T3.1, §7.2). Importes en céntimos (Long). */
class EstimateSavingsTest {

    private val now = YearMonth.of(2026, 11)

    @Test
    fun calculatesRemainingMonthsAndTargetDate() {
        val r = EstimateSavings.estimate(
            targetCents = 120_000, // 1200 €
            contributedCents = 0,
            monthlyQuotaCents = 10_000, // 100 €/mes
            freeMonthlyCents = 50_000,
            initialEstimatedMonths = 12,
            now = now,
        )
        assertEquals(120_000L, r.remainingCents)
        assertEquals(12, r.monthsEstimated)
        assertEquals(YearMonth.of(2027, 11), r.targetYearMonth)
        assertEquals(SavingsStatus.ON_TRACK, r.status)
        assertFalse(r.goalReached)
    }

    @Test
    fun goalReachedWhenContributedCoversTarget() {
        val r = EstimateSavings.estimate(
            targetCents = 50_000,
            contributedCents = 50_000,
            monthlyQuotaCents = 5_000,
            freeMonthlyCents = 10_000,
            initialEstimatedMonths = 10,
            now = now,
        )
        assertEquals(0L, r.remainingCents)
        assertEquals(0, r.monthsEstimated)
        assertTrue(r.goalReached)
        assertEquals(SavingsStatus.AHEAD, r.status)
    }

    @Test
    fun behindWhenQuotaExceedsFreeMonthly() {
        val r = EstimateSavings.estimate(
            targetCents = 100_000,
            contributedCents = 20_000,
            monthlyQuotaCents = 30_000,
            freeMonthlyCents = 20_000, // no llega
            initialEstimatedMonths = 4,
            now = now,
        )
        assertEquals(SavingsStatus.BEHIND, r.status)
        assertEquals(80_000L, r.remainingCents)
    }

    @Test
    fun aheadWhenFasterThanInitialEstimate() {
        val r = EstimateSavings.estimate(
            targetCents = 100_000,
            contributedCents = 60_000, // queda 40000
            monthlyQuotaCents = 10_000, // 4 meses
            freeMonthlyCents = 50_000,
            initialEstimatedMonths = 10,
            now = now,
        )
        assertEquals(4, r.monthsEstimated)
        assertEquals(SavingsStatus.AHEAD, r.status)
    }

    @Test
    fun updatesWhenTargetOrQuotaChange() {
        val base = EstimateSavings.estimate(120_000, 0, 10_000, 50_000, 12, now)
        val biggerTarget = EstimateSavings.estimate(240_000, 0, 10_000, 50_000, base.monthsEstimated, now)
        val biggerQuota = EstimateSavings.estimate(120_000, 0, 20_000, 50_000, base.monthsEstimated, now)
        assertEquals(24, biggerTarget.monthsEstimated)
        assertEquals(6, biggerQuota.monthsEstimated)
    }

    @Test
    fun initialMonthsHelperMatches() {
        assertEquals(12, EstimateSavings.initialMonths(120_000, 0, 10_000))
        assertEquals(0, EstimateSavings.initialMonths(50_000, 50_000, 5_000))
        assertEquals(1, EstimateSavings.initialMonths(1, 0, 10_000))
    }

    @Test
    fun negativeContributionsDoNotBreak() {
        val r = EstimateSavings.estimate(100_000, -5_000, 10_000, 50_000, 10, now)
        assertEquals(105_000L, r.remainingCents)
        assertEquals(11, r.monthsEstimated)
    }

    @Test
    fun quotaStepIs25Euros() {
        assertEquals(2_500L, EstimateSavings.QUOTA_STEP_CENTS)
    }
}
