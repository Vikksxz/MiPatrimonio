package com.mipatrimonio.app.feature.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mipatrimonio.app.core.database.dao.BudgetDao
import com.mipatrimonio.app.core.database.dao.CategoryDao
import com.mipatrimonio.app.core.database.dao.SavingsDao
import com.mipatrimonio.app.core.database.dao.TransactionDao
import com.mipatrimonio.app.core.database.entity.BudgetEntity
import com.mipatrimonio.app.core.database.entity.CategoryEntity
import com.mipatrimonio.app.core.database.entity.PlanContributionEntity
import com.mipatrimonio.app.core.database.entity.SavingsPlanEntity
import com.mipatrimonio.app.core.model.TransactionType
import com.mipatrimonio.app.domain.usecase.BudgetStatus
import com.mipatrimonio.app.domain.usecase.BudgetThresholds
import com.mipatrimonio.app.domain.usecase.EstimateSavings
import com.mipatrimonio.app.domain.usecase.SavingsEstimate
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlanItemUi(
    val plan: SavingsPlanEntity,
    val contributedCents: Long,
    val estimate: SavingsEstimate,
)

data class BudgetItemUi(
    val budget: BudgetEntity,
    val category: CategoryEntity?,
    val status: BudgetStatus,
)

data class SavingsUiState(
    val plans: List<PlanItemUi> = emptyList(),
    val activePlan: PlanItemUi? = null,
    val budgets: List<BudgetItemUi> = emptyList(),
    val expenseCategories: List<CategoryEntity> = emptyList(),
    val loaded: Boolean = false,
)

@HiltViewModel
class SavingsViewModel @Inject constructor(
    private val savingsDao: SavingsDao,
    private val budgetDao: BudgetDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
) : ViewModel() {

    private val month: YearMonth = YearMonth.now()
    private val monthFrom: Long = month.atDay(1).toEpochDay()
    private val monthToExclusive: Long = month.plusMonths(1).atDay(1).toEpochDay()

    private val plansFlow = savingsDao.observePlansWithContributions()

    private val budgetsFlow = combine(
        budgetDao.observeWithCategory(),
        transactionDao.observeByMonth(monthFrom, monthToExclusive),
        categoryDao.observeByIncome(false),
    ) { budgetsWithCat, monthTx, expenseCats ->
        val spentByCategory = mutableMapOf<Long, Long>()
        monthTx.forEach { tx ->
            val catId = tx.categoryId ?: return@forEach
            if (tx.type == TransactionType.INCOME) return@forEach
            spentByCategory[catId] = (spentByCategory[catId] ?: 0L) + tx.amountBaseCents
        }
        val items = budgetsWithCat.map { bwc ->
            val spent = spentByCategory[bwc.budget.categoryId] ?: 0L
            val status = BudgetThresholds.evaluate(spent, bwc.budget.limitCents, month, bwc.budget.notified80Month, bwc.budget.notified100Month)
            BudgetItemUi(budget = bwc.budget, category = bwc.category, status = status)
        }.sortedBy { it.category?.name ?: "" }
        // Persistir marcas de umbral alcanzado (una vez por mes/umbral, §7.4). T3.6 enviará notificaciones.
        items.forEach { item ->
            val b = item.budget
            val s = item.status
            if (s.newNotified80Month != b.notified80Month || s.newNotified100Month != b.notified100Month) {
                viewModelScope.launch { budgetDao.updateNotifiedMonths(b.id, s.newNotified80Month, s.newNotified100Month) }
            }
        }
        Triple(items, expenseCats, spentByCategory)
    }

    val uiState: StateFlow<SavingsUiState> = combine(plansFlow, budgetsFlow) { all, budgetData ->
        val items = all.map { pwc ->
            val contributed = pwc.contributions.sumOf { it.amountCents }
            val estimate = EstimateSavings.estimate(
                targetCents = pwc.plan.targetCents,
                contributedCents = contributed,
                monthlyQuotaCents = pwc.plan.monthlyQuotaCents,
                // Neutral: no false "BEHIND" sin datos reales de ingreso/gasto (T3.1).
                freeMonthlyCents = pwc.plan.monthlyQuotaCents,
                initialEstimatedMonths = pwc.plan.initialEstimatedMonths,
                now = YearMonth.now(),
            )
            PlanItemUi(pwc.plan, contributed, estimate)
        }.sortedBy { it.plan.priority }
        val active = items.firstOrNull { it.plan.isActive } ?: items.firstOrNull()
        SavingsUiState(
            plans = items,
            activePlan = active,
            budgets = budgetData.first,
            expenseCategories = budgetData.second,
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SavingsUiState())

    fun createPlan(name: String, targetCents: Long) {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || targetCents <= 0L) return
        viewModelScope.launch {
            val nowMillis = System.currentTimeMillis()
            val existing = uiState.value.plans
            val priority = (existing.maxOfOrNull { it.plan.priority } ?: 0) + 1
            val initialQuota = EstimateSavings.QUOTA_STEP_CENTS * 4 // 100 € por defecto
            val initialMonths = EstimateSavings.initialMonths(targetCents, 0L, initialQuota)
            val isFirst = existing.isEmpty()
            val id = savingsDao.insertPlan(
                SavingsPlanEntity(
                    name = trimmed,
                    targetCents = targetCents,
                    monthlyQuotaCents = initialQuota,
                    priority = priority,
                    isActive = isFirst,
                    initialEstimatedMonths = initialMonths,
                    createdAt = nowMillis,
                )
            )
            if (isFirst && id > 0L) savingsDao.setActivePlan(id)
        }
    }

    fun updatePlan(planId: Long, name: String, targetCents: Long) {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || targetCents <= 0L) return
        viewModelScope.launch {
            val current = uiState.value.plans.firstOrNull { it.plan.id == planId } ?: return@launch
            val contributed = current.contributedCents
            val quota = current.plan.monthlyQuotaCents
            val initialMonths = EstimateSavings.initialMonths(targetCents, contributed, quota)
            savingsDao.updatePlan(
                current.plan.copy(
                    name = trimmed,
                    targetCents = targetCents,
                    initialEstimatedMonths = initialMonths,
                )
            )
        }
    }

    fun changeQuota(planId: Long, deltaCents: Long) {
        viewModelScope.launch {
            val current = uiState.value.plans.firstOrNull { it.plan.id == planId } ?: return@launch
            val newQuota = (current.plan.monthlyQuotaCents + deltaCents).coerceAtLeast(EstimateSavings.QUOTA_STEP_CENTS)
            if (newQuota == current.plan.monthlyQuotaCents) return@launch
            val initialMonths = EstimateSavings.initialMonths(
                current.plan.targetCents,
                current.contributedCents,
                newQuota,
            )
            savingsDao.updatePlan(
                current.plan.copy(
                    monthlyQuotaCents = newQuota,
                    initialEstimatedMonths = initialMonths,
                )
            )
        }
    }

    fun addContribution(planId: Long, amountCents: Long) {
        if (amountCents <= 0L) return
        viewModelScope.launch {
            val todayEpochDay = LocalDate.now().toEpochDay()
            savingsDao.insertContribution(
                PlanContributionEntity(planId = planId, amountCents = amountCents, date = todayEpochDay)
            )
        }
    }

    fun setActive(planId: Long) {
        viewModelScope.launch { savingsDao.setActivePlan(planId) }
    }

    fun deletePlan(planId: Long) {
        viewModelScope.launch {
            val wasActive = uiState.value.plans.firstOrNull { it.plan.id == planId }?.plan?.isActive == true
            savingsDao.deletePlanWithContributions(planId)
            if (wasActive) {
                val next = uiState.value.plans.firstOrNull { it.plan.id != planId }
                if (next != null) savingsDao.setActivePlan(next.plan.id)
            }
        }
    }

    /** Reordena prioridad (T3.3): intercambia con el vecino según [up]. */
    fun movePlan(planId: Long, up: Boolean) {
        viewModelScope.launch {
            val ordered = uiState.value.plans.sortedBy { it.plan.priority }
            val index = ordered.indexOfFirst { it.plan.id == planId }
            if (index < 0) return@launch
            val targetIndex = if (up) index - 1 else index + 1
            if (targetIndex !in ordered.indices) return@launch
            val a = ordered[index].plan
            val b = ordered[targetIndex].plan
            // Intercambio de prioridad (valores distintos para mantener orden estable).
            val aPri = a.priority
            val bPri = b.priority
            val (newA, newB) = if (aPri == bPri) {
                val base = index + 1
                if (up) a.copy(priority = base - 1) to b.copy(priority = base + 1)
                else a.copy(priority = base + 1) to b.copy(priority = base - 1)
            } else {
                a.copy(priority = bPri) to b.copy(priority = aPri)
            }
            savingsDao.updatePlan(newA)
            savingsDao.updatePlan(newB)
        }
    }

    // --- Presupuestos (T3.4) ---
    fun saveBudget(categoryId: Long, limitCents: Long, existingId: Long? = null) {
        if (limitCents <= 0L || categoryId <= 0L) return
        viewModelScope.launch {
            val existing = existingId?.let { uiState.value.budgets.firstOrNull { b -> b.budget.id == it }?.budget }
                ?: budgetDao.getByCategory(categoryId)
            val entity = if (existing != null) {
                // Al cambiar límite/categoría, reiniciar avisos del mes actual según nueva evaluación.
                existing.copy(categoryId = categoryId, limitCents = limitCents, notified80Month = -1, notified100Month = -1)
            } else {
                BudgetEntity(categoryId = categoryId, limitCents = limitCents)
            }
            budgetDao.upsert(entity)
        }
    }

    fun deleteBudget(budgetId: Long) {
        viewModelScope.launch {
            val b = uiState.value.budgets.firstOrNull { it.budget.id == budgetId }?.budget ?: return@launch
            budgetDao.delete(b)
        }
    }
}

fun YearMonth.toMonthYearLabel(locale: Locale = Locale.forLanguageTag("es-ES")): String {
    val month = month.getDisplayName(TextStyle.SHORT, locale)
    return "$month ${year}"
}
