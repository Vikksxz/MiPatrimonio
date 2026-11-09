package com.mipatrimonio.app.feature.shared

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mipatrimonio.app.core.database.dao.PersonDao
import com.mipatrimonio.app.core.database.dao.SharedExpenseDao
import com.mipatrimonio.app.core.database.dao.SharedShareDao
import com.mipatrimonio.app.core.database.entity.PersonEntity
import com.mipatrimonio.app.core.database.entity.SharedExpenseEntity
import com.mipatrimonio.app.core.database.entity.SharedShareEntity
import com.mipatrimonio.app.domain.usecase.AddSharedExpense
import com.mipatrimonio.app.domain.usecase.DebtTransfer
import com.mipatrimonio.app.domain.usecase.DeleteSharedExpense
import com.mipatrimonio.app.domain.usecase.SettleSharedExpense
import com.mipatrimonio.app.domain.usecase.SharedBalances
import com.mipatrimonio.app.domain.usecase.UpdateSharedExpense
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SharedExpenseUi(
    val expense: SharedExpenseEntity,
    val shares: List<SharedShareEntity>,
)

data class SharedExpensesState(
    val people: List<PersonEntity> = emptyList(),
    val expenses: List<SharedExpenseUi> = emptyList(),
    val balances: Map<Long, Long> = emptyMap(),
    val transfers: List<DebtTransfer> = emptyList(),
)

@HiltViewModel
class SharedExpensesViewModel @Inject constructor(
    private val personDao: PersonDao,
    private val sharedExpenseDao: SharedExpenseDao,
    private val sharedShareDao: SharedShareDao,
    private val addSharedExpense: AddSharedExpense,
    private val updateSharedExpense: UpdateSharedExpense,
    private val deleteSharedExpense: DeleteSharedExpense,
    private val settleSharedExpense: SettleSharedExpense,
) : ViewModel() {

    val state: StateFlow<SharedExpensesState> = combine(
        personDao.observeAll(),
        sharedExpenseDao.observeAll(),
        sharedShareDao.observeAll(),
    ) { people, expenses, shares ->
        val sharesByExpense = shares.groupBy { it.sharedExpenseId }
        val expenseUis = expenses.map { SharedExpenseUi(it, sharesByExpense[it.id].orEmpty()) }
        val balances = SharedBalances.netBalances(expenses.associateBy { it.id }, shares)
        val transfers = SharedBalances.whoOwesWhom(balances)
        SharedExpensesState(people = people, expenses = expenseUis, balances = balances, transfers = transfers)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SharedExpensesState())

    init {
        viewModelScope.launch { ensureMe() }
    }

    private suspend fun ensureMe() {
        withContext(Dispatchers.IO) {
            if (personDao.getMe() == null) {
                // "Yo" se crea como persona propia por defecto; el nombre puede editarse después.
                personDao.insert(PersonEntity(name = "Yo", isMe = true))
            }
        }
    }

    fun addPerson(name: String, onDone: (Long) -> Unit = {}, onError: () -> Unit = {}) {
        val clean = name.trim()
        if (clean.isEmpty()) { onError(); return }
        viewModelScope.launch {
            val id = withContext(Dispatchers.IO) { personDao.insert(PersonEntity(name = clean, isMe = false)) }
            onDone(id)
        }
    }

    fun addExpense(
        title: String,
        totalCents: Long,
        paidByPersonId: Long,
        dateEpochDay: Long,
        shares: Map<Long, Long>,
        onDone: () -> Unit = {},
        onError: (String) -> Unit = {},
    ) {
        viewModelScope.launch {
            try {
                addSharedExpense(title, totalCents, paidByPersonId, dateEpochDay, shares)
                onDone()
            } catch (e: Exception) {
                onError(e.message.orEmpty())
            }
        }
    }

    fun updateExpense(
        expenseId: Long,
        title: String,
        totalCents: Long,
        paidByPersonId: Long,
        dateEpochDay: Long,
        shares: Map<Long, Long>,
        onDone: () -> Unit = {},
        onError: (String) -> Unit = {},
    ) {
        viewModelScope.launch {
            try {
                updateSharedExpense(expenseId, title, totalCents, paidByPersonId, dateEpochDay, shares)
                onDone()
            } catch (e: Exception) {
                onError(e.message.orEmpty())
            }
        }
    }

    fun deleteExpense(expenseId: Long) {
        viewModelScope.launch { deleteSharedExpense(expenseId) }
    }

    fun settleExpense(expenseId: Long, personId: Long?, settled: Boolean) {
        viewModelScope.launch { settleSharedExpense(expenseId, personId, settled) }
    }
}
