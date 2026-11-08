package com.mipatrimonio.app.domain.usecase

import com.mipatrimonio.app.core.model.RecurrencePeriod
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests de generación de recurrentes con huecos de días (T3.5, §7.6). */
class RecurringScheduleTest {

    private fun d(y: Int, m: Int, day: Int) = LocalDate.of(y, m, day).toEpochDay()

    @Test
    fun monthlyAdvance() {
        val next = RecurringSchedule.advance(LocalDate.of(2026, 1, 15), RecurrencePeriod.MONTHLY)
        assertEquals(LocalDate.of(2026, 2, 15), next)
    }

    @Test
    fun monthlyAdvanceClampsMonthEnd() {
        val next = RecurringSchedule.advance(LocalDate.of(2026, 1, 31), RecurrencePeriod.MONTHLY)
        assertEquals(LocalDate.of(2026, 2, 28), next)
    }

    @Test
    fun weeklyQuarterlyYearlyAdvance() {
        assertEquals(LocalDate.of(2026, 1, 8), RecurringSchedule.advance(LocalDate.of(2026, 1, 1), RecurrencePeriod.WEEKLY))
        assertEquals(LocalDate.of(2026, 4, 1), RecurringSchedule.advance(LocalDate.of(2026, 1, 1), RecurrencePeriod.QUARTERLY))
        assertEquals(LocalDate.of(2027, 1, 1), RecurringSchedule.advance(LocalDate.of(2026, 1, 1), RecurrencePeriod.YEARLY))
    }

    @Test
    fun dueOccurrences_catchesUpGaps() {
        // nextDate = 1 ene, hoy = 10 mar -> genera ene,feb,mar (mensual) con huecos cubiertos
        val r = RecurringSchedule.dueOccurrences(d(2026, 1, 1), RecurrencePeriod.MONTHLY, d(2026, 3, 10))
        assertEquals(listOf(d(2026, 1, 1), d(2026, 2, 1), d(2026, 3, 1)), r.dueDatesEpochDay)
        assertEquals(d(2026, 4, 1), r.newNextDateEpochDay)
    }

    @Test
    fun dueOccurrences_noDueWhenFuture() {
        val r = RecurringSchedule.dueOccurrences(d(2026, 12, 1), RecurrencePeriod.MONTHLY, d(2026, 11, 8))
        assertTrue(r.dueDatesEpochDay.isEmpty())
        assertEquals(d(2026, 12, 1), r.newNextDateEpochDay)
    }

    @Test
    fun dueOccurrences_dueTodayIncluded() {
        val r = RecurringSchedule.dueOccurrences(d(2026, 11, 8), RecurrencePeriod.WEEKLY, d(2026, 11, 8))
        assertEquals(listOf(d(2026, 11, 8)), r.dueDatesEpochDay)
        assertEquals(d(2026, 11, 15), r.newNextDateEpochDay)
    }

    @Test
    fun dueOccurrences_multipleWeeklyGaps() {
        val r = RecurringSchedule.dueOccurrences(d(2026, 11, 1), RecurrencePeriod.WEEKLY, d(2026, 11, 22))
        assertEquals(4, r.dueDatesEpochDay.size) // 1,8,15,22
        assertEquals(d(2026, 11, 29), r.newNextDateEpochDay)
    }

    @Test
    fun annualCostByPeriod() {
        val amount = 999L // 9,99 €
        assertEquals(51948L, RecurringSchedule.annualCostCents(amount, RecurrencePeriod.WEEKLY))
        assertEquals(11988L, RecurringSchedule.annualCostCents(amount, RecurrencePeriod.MONTHLY))
        assertEquals(3996L, RecurringSchedule.annualCostCents(amount, RecurrencePeriod.QUARTERLY))
        assertEquals(999L, RecurringSchedule.annualCostCents(amount, RecurrencePeriod.YEARLY))
    }
}
