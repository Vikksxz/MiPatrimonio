package com.mipatrimonio.app.domain.usecase

import java.time.YearMonth

/** Umbrales y estado de presupuesto (§7.4). Importes en céntimos (Long). */
enum class BudgetLevel { OK, WARNING, OVER }

data class BudgetStatus(
    val spentCents: Long,
    val limitCents: Long,
    val percent: Int, // 0..N (puede superar 100)
    val level: BudgetLevel,
    val notify80: Boolean,
    val notify100: Boolean,
    val newNotified80Month: Int,
    val newNotified100Month: Int,
)

object BudgetThresholds {

    /** Evalúa presupuesto: color/nivel y si corresponde avisar (una vez por mes y umbral). */
    fun evaluate(
        spentCents: Long,
        limitCents: Long,
        yearMonth: YearMonth,
        notified80Month: Int,
        notified100Month: Int,
    ): BudgetStatus {
        val safeLimit = if (limitCents > 0L) limitCents else 1L
        val spent = spentCents.coerceAtLeast(0L)
        val ratio = spent.toDouble() / safeLimit.toDouble()
        val percent = (ratio * 100.0).toInt().coerceAtLeast(0)
        val ym = yearMonth.year * 100 + yearMonth.monthValue
        val level = when {
            percent >= 100 -> BudgetLevel.OVER
            percent >= 80 -> BudgetLevel.WARNING
            else -> BudgetLevel.OK
        }
        val crossed80 = percent >= 80
        val crossed100 = percent >= 100
        val notify80 = crossed80 && notified80Month != ym
        val notify100 = crossed100 && notified100Month != ym
        val new80 = if (crossed80) ym else notified80Month
        val new100 = if (crossed100) ym else notified100Month
        return BudgetStatus(
            spentCents = spent,
            limitCents = limitCents.coerceAtLeast(0L),
            percent = percent,
            level = level,
            notify80 = notify80,
            notify100 = notify100,
            newNotified80Month = new80,
            newNotified100Month = new100,
        )
    }

    fun yearMonthKey(yearMonth: YearMonth): Int = yearMonth.year * 100 + yearMonth.monthValue
}
