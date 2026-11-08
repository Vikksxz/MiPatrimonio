package com.mipatrimonio.app.feature.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mipatrimonio.app.core.database.dao.CategoryDao
import com.mipatrimonio.app.core.database.dao.SavingsDao
import com.mipatrimonio.app.core.database.dao.TransactionDao
import com.mipatrimonio.app.core.model.TransactionType
import com.mipatrimonio.app.domain.usecase.CategoryExpense
import com.mipatrimonio.app.domain.usecase.FixedVariableSummary
import com.mipatrimonio.app.domain.usecase.MonthlyAnalysis
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MonthTotals(val incomeCents: Long, val expenseCents: Long)

data class AnalysisUiState(
    val loading: Boolean = true,
    val currentMonth: YearMonth = YearMonth.now(),
    val previousMonth: YearMonth = YearMonth.now().minusMonths(1),
    val currentTotals: MonthTotals = MonthTotals(0, 0),
    val previousTotals: MonthTotals = MonthTotals(0, 0),
    val categories: List<CategoryExpense> = emptyList(),
    val fixedVariable: FixedVariableSummary? = null,
)

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val savingsDao: SavingsDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalysisUiState())
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            val now = YearMonth.now()
            val prev = now.minusMonths(1)
            val nowFrom = now.atDay(1).toEpochDay()
            val nowTo = now.plusMonths(1).atDay(1).toEpochDay()
            val prevFrom = prev.atDay(1).toEpochDay()
            val prevTo = prev.plusMonths(1).atDay(1).toEpochDay()

            val currentTx = transactionDao.getByMonth(nowFrom, nowTo)
            val prevTx = transactionDao.getByMonth(prevFrom, prevTo)
            val categories = categoryDao.getAll()
            val catMap = categories.associateBy { it.id }
            val savings = savingsDao.getMonthlyContributed(nowFrom, nowTo) ?: 0L

            fun totals(txs: List<com.mipatrimonio.app.core.database.entity.TransactionEntity>): MonthTotals {
                var inc = 0L; var exp = 0L
                txs.forEach { tx ->
                    if (tx.type == TransactionType.INCOME) inc += tx.amountBaseCents else exp += tx.amountBaseCents
                }
                return MonthTotals(inc, exp)
            }

            _uiState.value = AnalysisUiState(
                loading = false,
                currentMonth = now,
                previousMonth = prev,
                currentTotals = totals(currentTx),
                previousTotals = totals(prevTx),
                categories = MonthlyAnalysis.expensesByCategory(now, currentTx, catMap, otherLabel = "Otros gastos"),
                fixedVariable = MonthlyAnalysis.fixedVsVariable(now, currentTx, catMap, savings),
            )
        }
    }
}
