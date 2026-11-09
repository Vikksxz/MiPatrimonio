package com.mipatrimonio.app.domain.usecase

import com.mipatrimonio.app.core.database.entity.CategoryEntity
import com.mipatrimonio.app.core.database.entity.PlanContributionEntity
import com.mipatrimonio.app.core.database.entity.SavingsPlanEntity
import com.mipatrimonio.app.core.database.entity.TransactionEntity
import com.mipatrimonio.app.core.model.TransactionType
import java.time.LocalDate
import java.time.YearMonth

/** Datos de un informe mensual (§8.7). Importes en céntimos (Long). */
data class MonthlyReport(
    val month: YearMonth,
    val previousMonth: YearMonth,
    val incomeCents: Long,
    val expenseCents: Long,
    val availableCents: Long,
    val savingsContributedCents: Long,
    val cashCents: Long,
    val cardCents: Long,
    val subscriptionCents: Long,
    val previousExpenseCents: Long,
    val expenseDeltaCents: Long,
    val expensesByCategory: List<CategoryExpenseReport>,
    val topMerchants: List<MerchantTotal>,
    val savingsPlans: List<SavingsPlanReport>,
    val tips: List<ReportTip>,
)

data class CategoryExpenseReport(
    val categoryId: Long?,
    val name: String,
    val amountCents: Long,
    val previousAmountCents: Long,
)

data class MerchantTotal(val merchant: String, val amountCents: Long)

data class SavingsPlanReport(
    val name: String,
    val contributedCents: Long,
    val targetCents: Long,
)

enum class ReportTipKind { CATEGORY_UP, EXPENSE_OVER_INCOME, SAVINGS_POSITIVE, GENERAL }

data class ReportTip(val kind: ReportTipKind, val categoryName: String? = null, val percent: Int = 0, val amountCents: Long = 0L)

object MonthlyReportBuilder {

    /** Construye el informe del [month] comparando con [previousMonth] (§8.7). Función pura. */
    fun build(
        month: YearMonth,
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        plans: List<SavingsPlanEntity>,
        contributions: List<PlanContributionEntity>,
    ): MonthlyReport {
        val previousMonth = month.minusMonths(1)
        val currentTx = transactions.filter { txMonth(it) == month }
        val prevTx = transactions.filter { txMonth(it) == previousMonth }

        val income = sumByType(currentTx, TransactionType.INCOME)
        val cash = sumByType(currentTx, TransactionType.CASH)
        val card = sumByType(currentTx, TransactionType.CARD)
        val subscription = sumByType(currentTx, TransactionType.SUBSCRIPTION)
        val expense = expenseTotal(currentTx)

        val from = month.atDay(1).toEpochDay()
        val to = month.plusMonths(1).atDay(1).toEpochDay()
        val savingsContributed = contributions.asSequence()
            .filter { it.date >= from && it.date < to }
            .sumOf { it.amountCents }

        val prevExpense = expenseTotal(prevTx)
        val delta = expense - prevExpense

        val categoriesById = categories.associateBy { it.id }
        val byCat = expenseByCategory(currentTx)
        val byCatPrev = expenseByCategory(prevTx)
        val allCatIds = (byCat.keys + byCatPrev.keys).filterNotNull().distinct()
        val expensesByCategory = allCatIds.map { id ->
            val cat = categoriesById[id]
            CategoryExpenseReport(
                categoryId = id,
                name = cat?.name ?: "",
                amountCents = byCat[id] ?: 0L,
                previousAmountCents = byCatPrev[id] ?: 0L,
            )
        }.filter { it.amountCents > 0L || it.previousAmountCents > 0L }
            .sortedByDescending { it.amountCents }

        val topMerchants = topMerchants(currentTx, 5)
        val savingsPlans = plans.map { plan ->
            val contributed = contributions.asSequence()
                .filter { it.planId == plan.id }
                .sumOf { it.amountCents }
            SavingsPlanReport(plan.name, contributed, plan.targetCents)
        }

        val tips = buildTips(income, expense, savingsContributed, expensesByCategory)

        return MonthlyReport(
            month = month,
            previousMonth = previousMonth,
            incomeCents = income,
            expenseCents = expense,
            availableCents = income - expense - savingsContributed,
            savingsContributedCents = savingsContributed,
            cashCents = cash,
            cardCents = card,
            subscriptionCents = subscription,
            previousExpenseCents = prevExpense,
            expenseDeltaCents = delta,
            expensesByCategory = expensesByCategory,
            topMerchants = topMerchants,
            savingsPlans = savingsPlans,
            tips = tips,
        )
    }

    internal fun topMerchants(transactions: List<TransactionEntity>, limit: Int): List<MerchantTotal> {
        return transactions.asSequence()
            .filter { it.type != TransactionType.INCOME }
            .map { it.merchant.trim() }
            .filter { it.isNotEmpty() }
            .groupingBy { it }
            .eachCount()
            .entries
            .asSequence()
            .map { (merchant, _) ->
                val total = transactions.asSequence()
                    .filter { it.type != TransactionType.INCOME && it.merchant.trim() == merchant }
                    .sumOf { it.amountBaseCents }
                MerchantTotal(merchant, total)
            }
            .filter { it.amountCents > 0L }
            .sortedByDescending { it.amountCents }
            .take(limit)
            .toList()
    }

    internal fun buildTips(
        incomeCents: Long,
        expenseCents: Long,
        savingsCents: Long,
        expensesByCategory: List<CategoryExpenseReport>,
    ): List<ReportTip> {
        val tips = mutableListOf<ReportTip>()
        // Regla 1: categoría que más subió (>= 30% y subida significativa)
        val up = expensesByCategory
            .filter { it.previousAmountCents > 0L && it.amountCents > it.previousAmountCents }
            .mapNotNull { cat ->
                val increase = cat.amountCents - cat.previousAmountCents
                val pct = ((increase * 100L) / cat.previousAmountCents).toInt().coerceIn(0, 9999)
                if (pct >= 30 && increase >= 500L) ReportTip(ReportTipKind.CATEGORY_UP, categoryName = cat.name, percent = pct) else null
            }
            .maxByOrNull { it.percent }
        if (up != null && up.categoryName?.isNotBlank() == true) tips += up

        // Regla 2: gastos superan ingresos
        if (expenseCents > incomeCents && expenseCents > 0L) {
            tips += ReportTip(ReportTipKind.EXPENSE_OVER_INCOME)
        }

        // Regla 3: ahorro positivo
        if (savingsCents > 0L) {
            tips += ReportTip(ReportTipKind.SAVINGS_POSITIVE, amountCents = savingsCents)
        }

        if (tips.isEmpty()) {
            tips += ReportTip(ReportTipKind.GENERAL)
        }
        return tips.take(3)
    }

    private fun txMonth(tx: TransactionEntity): YearMonth =
        YearMonth.from(LocalDate.ofEpochDay(tx.date))

    private fun sumByType(txs: List<TransactionEntity>, type: TransactionType): Long =
        txs.asSequence().filter { it.type == type }.sumOf { it.amountBaseCents }

    private fun expenseTotal(txs: List<TransactionEntity>): Long =
        txs.asSequence().filter { it.type != TransactionType.INCOME }.sumOf { it.amountBaseCents }

    private fun expenseByCategory(txs: List<TransactionEntity>): Map<Long?, Long> =
        txs.asSequence()
            .filter { it.type != TransactionType.INCOME }
            .groupBy { it.categoryId }
            .mapValues { (_, list) -> list.sumOf { it.amountBaseCents } }
}
