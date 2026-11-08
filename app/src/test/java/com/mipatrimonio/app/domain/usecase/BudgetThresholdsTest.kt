package com.mipatrimonio.app.domain.usecase

import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests de umbrales de presupuesto (T3.4, §7.4). Importes en céntimos (Long). */
class BudgetThresholdsTest {

    private val ym = YearMonth.of(2026, 11)
    private val ymKey = 202611

    @Test
    fun under79PercentIsOkAndNoNotify() {
        val s = BudgetThresholds.evaluate(7_900, 10_000, ym, -1, -1)
        assertEquals(BudgetLevel.OK, s.level)
        assertEquals(79, s.percent)
        assertFalse(s.notify80)
        assertFalse(s.notify100)
        assertEquals(-1, s.newNotified80Month)
    }

    @Test
    fun exactly80PercentIsWarningAndNotifiesOnce() {
        val s = BudgetThresholds.evaluate(8_000, 10_000, ym, -1, -1)
        assertEquals(BudgetLevel.WARNING, s.level)
        assertEquals(80, s.percent)
        assertTrue(s.notify80)
        assertFalse(s.notify100)
        assertEquals(ymKey, s.newNotified80Month)
    }

    @Test
    fun between80And99IsWarning() {
        val s = BudgetThresholds.evaluate(9_900, 10_000, ym, ymKey, -1)
        assertEquals(BudgetLevel.WARNING, s.level)
        assertEquals(99, s.percent)
        assertFalse(s.notify80) // ya notificado este mes
        assertFalse(s.notify100)
    }

    @Test
    fun exactly100PercentIsOverAndNotifies100() {
        val s = BudgetThresholds.evaluate(10_000, 10_000, ym, ymKey, -1)
        assertEquals(BudgetLevel.OVER, s.level)
        assertEquals(100, s.percent)
        assertFalse(s.notify80)
        assertTrue(s.notify100)
        assertEquals(ymKey, s.newNotified100Month)
    }

    @Test
    fun over100PercentNotifiesOnlyIfNotNotifiedThisMonth() {
        val s = BudgetThresholds.evaluate(15_000, 10_000, ym, ymKey, ymKey)
        assertEquals(BudgetLevel.OVER, s.level)
        assertEquals(150, s.percent)
        assertFalse(s.notify80)
        assertFalse(s.notify100)
    }

    @Test
    fun newMonthAllowsNotifyAgain() {
        val dec = YearMonth.of(2026, 12)
        val s = BudgetThresholds.evaluate(9_000, 10_000, dec, ymKey, -1)
        assertTrue(s.notify80) // notificado en nov, no en dic
        assertEquals(202612, s.newNotified80Month)
    }

    @Test
    fun zeroLimitIsSafe() {
        val s = BudgetThresholds.evaluate(5_000, 0, ym, -1, -1)
        assertEquals(BudgetLevel.OVER, s.level)
        assertTrue(s.percent >= 100)
    }

    @Test
    fun zeroSpentIsOk() {
        val s = BudgetThresholds.evaluate(0, 10_000, ym, -1, -1)
        assertEquals(BudgetLevel.OK, s.level)
        assertEquals(0, s.percent)
        assertFalse(s.notify80)
    }
}
