package com.mipatrimonio.app.feature.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mipatrimonio.app.core.database.dao.PlanWithContributions
import com.mipatrimonio.app.core.database.dao.SavingsDao
import com.mipatrimonio.app.core.database.entity.PlanContributionEntity
import com.mipatrimonio.app.core.database.entity.SavingsPlanEntity
import com.mipatrimonio.app.domain.usecase.EstimateSavings
import com.mipatrimonio.app.domain.usecase.SavingsEstimate
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlanItemUi(
    val plan: SavingsPlanEntity,
    val contributedCents: Long,
    val estimate: SavingsEstimate,
)

data class SavingsUiState(
    val plans: List<PlanItemUi> = emptyList(),
    val activePlan: PlanItemUi? = null,
    val loaded: Boolean = false,
)

@HiltViewModel
class SavingsViewModel @Inject constructor(
    private val savingsDao: SavingsDao,
) : ViewModel() {

    val uiState: StateFlow<SavingsUiState> = savingsDao.observePlansWithContributions()
        .map { all ->
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
            SavingsUiState(plans = items, activePlan = active, loaded = true)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SavingsUiState())

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
}

fun YearMonth.toMonthYearLabel(locale: Locale = Locale.forLanguageTag("es-ES")): String {
    val month = month.getDisplayName(TextStyle.SHORT, locale)
    return "$month ${year}"
}
