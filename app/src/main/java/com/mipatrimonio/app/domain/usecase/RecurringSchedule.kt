package com.mipatrimonio.app.domain.usecase

import com.mipatrimonio.app.core.model.RecurrencePeriod
import java.time.LocalDate

/**
 * Lógica pura de recurrencias (§7.6, T3.5). Fechas en epochDay (Long) / LocalDate.
 */
object RecurringSchedule {

    /** Avanza una fecha según el periodo. */
    fun advance(date: LocalDate, period: RecurrencePeriod): LocalDate = when (period) {
        RecurrencePeriod.WEEKLY -> date.plusWeeks(1)
        RecurrencePeriod.MONTHLY -> date.plusMonths(1)
        RecurrencePeriod.QUARTERLY -> date.plusMonths(3)
        RecurrencePeriod.YEARLY -> date.plusYears(1)
    }

    /**
     * Genera las ocurrencias vencidas (catch-up por huecos de días, §7.6).
     * Devuelve las fechas (epochDay) a generar, en orden, y el nuevo nextDate (epochDay)
     * que queda tras avanzar más allá de [today].
     */
    fun dueOccurrences(
        nextDateEpochDay: Long,
        period: RecurrencePeriod,
        todayEpochDay: Long,
    ): DueResult {
        val today = LocalDate.ofEpochDay(todayEpochDay)
        var cursor = LocalDate.ofEpochDay(nextDateEpochDay)
        val dates = mutableListOf<Long>()
        // Seguridad ante datos corruptos: límite razonable.
        var guard = 0
        while (!cursor.isAfter(today) && guard++ < 500) {
            dates += cursor.toEpochDay()
            cursor = advance(cursor, period)
        }
        return DueResult(dueDatesEpochDay = dates, newNextDateEpochDay = cursor.toEpochDay())
    }

    /** Coste anual de una suscripción (§7.6). */
    fun annualCostCents(amountCents: Long, period: RecurrencePeriod): Long = when (period) {
        RecurrencePeriod.WEEKLY -> amountCents * 52
        RecurrencePeriod.MONTHLY -> amountCents * 12
        RecurrencePeriod.QUARTERLY -> amountCents * 4
        RecurrencePeriod.YEARLY -> amountCents
    }

    data class DueResult(val dueDatesEpochDay: List<Long>, val newNextDateEpochDay: Long)
}
