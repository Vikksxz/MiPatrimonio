package com.mipatrimonio.app.domain.usecase

import com.mipatrimonio.app.core.database.entity.CategoryEntity
import com.mipatrimonio.app.core.database.entity.TransactionEntity
import com.mipatrimonio.app.core.model.TransactionType
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.max

/** Proyección a fin de mes (§7.5). */
data class MonthProjection(
    val visible: Boolean,
    val projectedExpenseCents: Long,
    val spentToDateCents: Long,
    val dayOfMonth: Int,
    val daysInMonth: Int,
)

/** Fijos vs variables (§7.7). */
data class FixedVariableSummary(
    val incomeCents: Long,
    val totalExpenseCents: Long,
    val fixedCents: Long,
    val variableCents: Long,
    val savingsContributedCents: Long,
    val freeAtMonthEndCents: Long,
)

data class CategoryExpense(
    val categoryId: Long?,
    val categoryName: String,
    val colorHex: String,
    val amountCents: Long,
)

object MonthlyAnalysis {

    /** §7.5: solo mostrar si día >= 3. */
    fun projection(
        month: YearMonth,
        transactions: List<TransactionEntity>,
        today: LocalDate = LocalDate.now(),
    ): MonthProjection {
        val daysInMonth = month.lengthOfMonth()
        val inMonth = transactions.filter { YearMonth.from(LocalDate.ofEpochDay(it.date)) == month }
        val spentTotal = sumExpenses(inMonth)
        val isCurrentMonth = YearMonth.from(today) == month
        val dayOfMonth = if (isCurrentMonth) today.dayOfMonth else daysInMonth
        val visible = !isCurrentMonth || dayOfMonth >= 3
        val spentToDate = if (isCurrentMonth) {
            inMonth.asSequence()
                .filter { it.type != TransactionType.INCOME && !LocalDate.ofEpochDay(it.date).isAfter(today) }
                .sumOf { it.amountBaseCents }
        } else spentTotal
        val projected = if (!visible || dayOfMonth <= 0) 0L else {
            // gastosHastaHoy / díaDelMes × díasDelMes (céntimos, entero redondeado)
            ((spentToDate.toDouble() / dayOfMonth.toDouble()) * daysInMonth.toDouble()).toLong()
        }
        return MonthProjection(
            visible = visible,
            projectedExpenseCents = projected.coerceAtLeast(0L),
            spentToDateCents = spentToDate.coerceAtLeast(0L),
            dayOfMonth = dayOfMonth,
            daysInMonth = daysInMonth,
        )
    }

    /**
     * §7.7: fijos = gastos de categorías con isFixed O movimientos recurrentes (sin doble conteo por tx).
     */
    fun fixedVsVariable(
        month: YearMonth,
        transactions: List<TransactionEntity>,
        categoriesById: Map<Long, CategoryEntity>,
        savingsContributedCents: Long,
    ): FixedVariableSummary {
        val inMonth = transactions.filter { YearMonth.from(LocalDate.ofEpochDay(it.date)) == month }
        var income = 0L
        var totalExpense = 0L
        var fixed = 0L
        inMonth.forEach { tx ->
            if (tx.type == TransactionType.INCOME) {
                income += tx.amountBaseCents
            } else {
                val amount = tx.amountBaseCents
                totalExpense += amount
                val catFixed = tx.categoryId?.let { categoriesById[it]?.isFixed } == true
                val recurring = tx.recurringId != null
                if (catFixed || recurring) fixed += amount
            }
        }
        val variable = max(0L, totalExpense - fixed)
        val free = income - fixed - savingsContributedCents - variable // = income - totalExpense - savings
        return FixedVariableSummary(
            incomeCents = income,
            totalExpenseCents = totalExpense,
            fixedCents = fixed.coerceAtLeast(0L),
            variableCents = variable,
            savingsContributedCents = savingsContributedCents,
            freeAtMonthEndCents = free,
        )
    }

    /** Gastos por categoría (mes), ordenados por importe desc. */
    fun expensesByCategory(
        month: YearMonth,
        transactions: List<TransactionEntity>,
        categoriesById: Map<Long, CategoryEntity>,
        otherLabel: String,
        otherColorHex: String = "#8C97D8",
    ): List<CategoryExpense> {
        val inMonth = transactions.filter { YearMonth.from(LocalDate.ofEpochDay(it.date)) == month }
        val byCat = mutableMapOf<Long?, Long>()
        inMonth.forEach { tx ->
            if (tx.type == TransactionType.INCOME) return@forEach
            val key = tx.categoryId
            byCat[key] = (byCat[key] ?: 0L) + tx.amountBaseCents
        }
        return byCat.map { (id, amount) ->
            val cat = id?.let { categoriesById[it] }
            CategoryExpense(
                categoryId = id,
                categoryName = cat?.name ?: otherLabel,
                colorHex = cat?.colorHex ?: otherColorHex,
                amountCents = amount,
            )
        }.sortedByDescending { it.amountCents }
    }

    private fun sumExpenses(txs: List<TransactionEntity>): Long =
        txs.asSequence().filter { it.type != TransactionType.INCOME }.sumOf { it.amountBaseCents }
}
