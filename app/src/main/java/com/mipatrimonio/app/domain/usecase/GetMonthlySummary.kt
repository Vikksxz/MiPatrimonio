package com.mipatrimonio.app.domain.usecase

import com.mipatrimonio.app.core.database.dao.BudgetDao
import com.mipatrimonio.app.core.database.dao.PlanWithContributions
import com.mipatrimonio.app.core.database.dao.SavingsDao
import com.mipatrimonio.app.core.database.dao.TransactionDao
import com.mipatrimonio.app.core.database.entity.TransactionEntity
import com.mipatrimonio.app.core.model.TransactionType
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Datos del plan activo para el panel de Inicio (§8.1). */
data class ActivePlanSummary(
    val planId: Long,
    val name: String,
    val targetCents: Long,
    val contributedCents: Long,
    val monthlyQuotaCents: Long,
    val estimate: SavingsEstimate,
)

/** Resumen de presupuesto para Inicio (T3.4, §7.4/§8.4). */
data class HomeBudgetSummary(
    val categoryName: String,
    val spentCents: Long,
    val limitCents: Long,
    val percent: Int,
    val level: BudgetLevel,
)

/**
 * Resumen del mes natural para la pantalla Inicio (MiPatrimonio.md §7.1).
 * Todos los importes están en céntimos (`Long`); el signo lo da el tipo.
 */
data class MonthlySummary(
    val month: YearMonth,
    val incomeCents: Long = 0L,
    val cashCents: Long = 0L,
    val cardCents: Long = 0L,
    val subscriptionCents: Long = 0L,
    val savingsCents: Long = 0L,
    val recent: List<TransactionEntity> = emptyList(),
    val activePlan: ActivePlanSummary? = null,
    val budgets: List<HomeBudgetSummary> = emptyList(),
    val projection: MonthProjection? = null,
) {
    /** Gastos del mes: metálico + tarjeta + suscripciones. */
    val expenseCents: Long get() = cashCents + cardCents + subscriptionCents

    /** Disponible = ingresos − gastos − aportado al ahorro. */
    val availableCents: Long get() = incomeCents - expenseCents - savingsCents
}

/**
 * Caso de uso `GetMonthlySummary` (T1.9+T3.1): resumen del mes, últimos movimientos,
 * aportaciones reales al ahorro (§7.1) y plan activo para Inicio (§8.1).
 */
class GetMonthlySummary @Inject constructor(
    private val transactionDao: TransactionDao,
    private val savingsDao: SavingsDao,
    private val budgetDao: BudgetDao,
) {

    operator fun invoke(month: YearMonth = YearMonth.now()): Flow<MonthlySummary> {
        val fromEpochDay = month.atDay(1).toEpochDay()
        val toEpochDayExclusive = month.plusMonths(1).atDay(1).toEpochDay()
        return combine(
            transactionDao.observeByMonth(fromEpochDay, toEpochDayExclusive),
            transactionDao.observeRecent(RECENT_LIMIT),
            savingsDao.observeMonthlyContributed(fromEpochDay, toEpochDayExclusive),
            savingsDao.observePlansWithContributions(),
            budgetDao.observeWithCategory(),
        ) { monthTransactions, recent, monthlySaved, plans, budgetsWithCat ->
            // §7.1: Σ aportaciones del mes (puede ser negativa si hay retiradas).
            val savingsCents = monthlySaved ?: 0L
            val active = buildActivePlan(plans)
            val budgets = buildBudgetSummaries(month, monthTransactions, budgetsWithCat)
            build(month, monthTransactions, recent, savingsCents, active, budgets)
        }
    }

    companion object {

        /** §8.1: la pantalla Inicio muestra los 4 últimos movimientos. */
        const val RECENT_LIMIT = 4

        /** Compatibilidad: valor por defecto cuando no hay datos de ahorro. */
        const val SAVINGS_CENTS = 0L

        /**
         * Porcentaje mínimo que ocupa un segmento de la barra aunque su importe
         * sea 0 (§7.1). Con tres segmentos el mínimo total es 18 %.
         */
        const val MIN_SEGMENT_SHARE = 0.06f

        /** Función pura: suma por tipo, descarta lo que no sea del mes y recorta los recientes. */
        fun build(
            month: YearMonth,
            transactions: List<TransactionEntity>,
            recent: List<TransactionEntity>,
            savingsCents: Long = SAVINGS_CENTS,
            activePlan: ActivePlanSummary? = null,
            budgets: List<HomeBudgetSummary> = emptyList(),
            today: LocalDate = LocalDate.now(),
        ): MonthlySummary {
            var income = 0L
            var cash = 0L
            var card = 0L
            var subscription = 0L

            transactions.forEach { transaction ->
                if (YearMonth.from(LocalDate.ofEpochDay(transaction.date)) != month) return@forEach
                when (transaction.type) {
                    TransactionType.INCOME -> income += transaction.amountBaseCents
                    TransactionType.CASH -> cash += transaction.amountBaseCents
                    TransactionType.CARD -> card += transaction.amountBaseCents
                    TransactionType.SUBSCRIPTION -> subscription += transaction.amountBaseCents
                }
            }

            val projection = MonthlyAnalysis.projection(month, transactions, today)

            return MonthlySummary(
                month = month,
                incomeCents = income,
                cashCents = cash,
                cardCents = card,
                subscriptionCents = subscription,
                savingsCents = savingsCents,
                recent = recent.take(RECENT_LIMIT),
                activePlan = activePlan,
                budgets = budgets,
                projection = projection,
            )
        }

        /** Presupuestos con gasto del mes por categoría (§7.4). */
        fun buildBudgetSummaries(
            month: YearMonth,
            transactions: List<TransactionEntity>,
            budgetsWithCat: List<BudgetDao.BudgetWithCategory>,
        ): List<HomeBudgetSummary> {
            val spentByCategory = mutableMapOf<Long, Long>()
            transactions.forEach { tx ->
                if (YearMonth.from(LocalDate.ofEpochDay(tx.date)) != month) return@forEach
                if (tx.type == TransactionType.INCOME) return@forEach
                val catId = tx.categoryId ?: return@forEach
                spentByCategory[catId] = (spentByCategory[catId] ?: 0L) + tx.amountBaseCents
            }
            return budgetsWithCat.mapNotNull { bwc ->
                val cat = bwc.category ?: return@mapNotNull null
                val spent = spentByCategory[bwc.budget.categoryId] ?: 0L
                val st = BudgetThresholds.evaluate(spent, bwc.budget.limitCents, month, bwc.budget.notified80Month, bwc.budget.notified100Month)
                HomeBudgetSummary(
                    categoryName = cat.name,
                    spentCents = st.spentCents,
                    limitCents = st.limitCents,
                    percent = st.percent,
                    level = st.level,
                )
            }.sortedBy { it.categoryName }
        }

        /** Selecciona plan activo y calcula su estimación (§8.1/§7.2). */
        fun buildActivePlan(plans: List<PlanWithContributions>, now: YearMonth = YearMonth.now()): ActivePlanSummary? {
            val pwc = plans.firstOrNull { it.plan.isActive } ?: return null
            val contributed = pwc.contributions.sumOf { it.amountCents }
            val estimate = EstimateSavings.estimate(
                targetCents = pwc.plan.targetCents,
                contributedCents = contributed,
                monthlyQuotaCents = pwc.plan.monthlyQuotaCents,
                freeMonthlyCents = pwc.plan.monthlyQuotaCents,
                initialEstimatedMonths = pwc.plan.initialEstimatedMonths,
                now = now,
            )
            return ActivePlanSummary(
                planId = pwc.plan.id,
                name = pwc.plan.name,
                targetCents = pwc.plan.targetCents,
                contributedCents = contributed,
                monthlyQuotaCents = pwc.plan.monthlyQuotaCents,
                estimate = estimate,
            )
        }

        /**
         * Pesos de la barra por tipo (§7.1): proporcionales al gasto de metálico,
         * tarjeta y suscripciones, con [MIN_SEGMENT_SHARE] fijo para cada uno aunque
         * su importe sea 0. Siempre suman 1 y nunca son negativos.
         *
         * Es lógica de presentación, pero §7.1 pide las reglas como funciones puras
         * en `domain/usecase`; así se puede probar sin Compose.
         */
        fun segmentWeights(
            cashCents: Long,
            cardCents: Long,
            subscriptionCents: Long,
        ): List<Float> {
            val cash = cashCents.coerceAtLeast(0L)
            val card = cardCents.coerceAtLeast(0L)
            val subscription = subscriptionCents.coerceAtLeast(0L)
            val total = cash + card + subscription
            val flexible = 1f - SEGMENT_COUNT * MIN_SEGMENT_SHARE

            fun weight(amount: Long): Float {
                val share = if (total > 0L) {
                    amount.toDouble() / total.toDouble()
                } else {
                    1.0 / SEGMENT_COUNT
                }
                return MIN_SEGMENT_SHARE + flexible * share.toFloat()
            }

            return listOf(weight(cash), weight(card), weight(subscription))
        }

        private const val SEGMENT_COUNT = 3
    }
}
