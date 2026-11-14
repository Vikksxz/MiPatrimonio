package com.mipatrimonio.app.feature.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mipatrimonio.app.core.database.dao.SavingsDao
import com.mipatrimonio.app.feature.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class LinkedContributionsViewModel @Inject constructor(
    private val savingsDao: SavingsDao,
    private val widgetUpdater: WidgetUpdater,
) : ViewModel() {

    private val incomeIdState = kotlinx.coroutines.flow.MutableStateFlow<Long?>(null)

    val contributions: StateFlow<List<SavingsDao.ContributionWithPlan>> = incomeIdState
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else savingsDao.observeContributionsWithPlanBySource(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setIncomeId(id: Long?) {
        incomeIdState.value = id
    }

    fun deleteContribution(contributionId: Long) {
        viewModelScope.launch {
            val c = savingsDao.getContributionById(contributionId) ?: return@launch
            val total = savingsDao.getContributed(c.planId) ?: 0L
            if (total - c.amountCents < 0L) return@launch
            savingsDao.deleteContributionById(contributionId)
            widgetUpdater.update()
        }
    }
}