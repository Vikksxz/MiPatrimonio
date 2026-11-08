package com.mipatrimonio.app.feature.add

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mipatrimonio.app.core.database.dao.CardDao
import com.mipatrimonio.app.core.database.dao.CategoryDao
import com.mipatrimonio.app.core.database.entity.CardEntity
import com.mipatrimonio.app.core.database.entity.CategoryEntity
import com.mipatrimonio.app.core.database.entity.TransactionEntity
import com.mipatrimonio.app.domain.usecase.AddTransaction
import com.mipatrimonio.app.domain.usecase.DeleteTransaction
import com.mipatrimonio.app.domain.usecase.NewTransaction
import com.mipatrimonio.app.domain.usecase.UpdateTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Estado del formulario de revisión del botón + (T1.7).
 * Ofrece tarjetas y categorías a la hoja y guarda mediante [AddTransaction].
 */
@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    categoryDao: CategoryDao,
    cardDao: CardDao,
    private val addTransaction: AddTransaction,
    private val updateTransaction: UpdateTransaction,
    private val deleteTransaction: DeleteTransaction,
) : ViewModel() {

    val cards: StateFlow<List<CardEntity>> = cardDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = categoryDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var saveFailed by mutableStateOf(false)
        private set

    var saving by mutableStateOf(false)
        private set

    fun reset() {
        saveFailed = false
        saving = false
    }

    fun save(input: NewTransaction, onSaved: (Long) -> Unit) {
        if (!begin()) return
        viewModelScope.launch {
            runCatching { addTransaction(input) }
                .onSuccess(onSaved)
                .onFailure(::fail)
        }
    }

    fun update(existing: TransactionEntity, input: NewTransaction, onSaved: (Long) -> Unit) {
        if (!begin()) return
        viewModelScope.launch {
            runCatching { updateTransaction(existing, input) }
                .onSuccess(onSaved)
                .onFailure(::fail)
        }
    }

    fun delete(existing: TransactionEntity, onDeleted: () -> Unit) {
        if (!begin()) return
        viewModelScope.launch {
            runCatching { deleteTransaction(existing) }
                .onSuccess { onDeleted() }
                .onFailure(::fail)
        }
    }

    /** Marca la operación como en curso; false si ya había una en marcha. */
    private fun begin(): Boolean {
        if (saving) return false
        saving = true
        saveFailed = false
        return true
    }

    private fun fail(error: Throwable) {
        saving = false
        if (error is CancellationException) throw error
        saveFailed = true
    }
}
