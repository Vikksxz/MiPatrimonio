package com.mipatrimonio.app.feature.transactions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mipatrimonio.app.core.database.dao.TransactionDao
import com.mipatrimonio.app.core.database.entity.TransactionEntity
import com.mipatrimonio.app.core.model.TransactionType
import com.mipatrimonio.app.domain.usecase.DeleteTransaction
import com.mipatrimonio.app.domain.usecase.RestoreTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Un día de la lista de movimientos (T1.8). */
data class TransactionsDayGroup(
    val epochDay: Long,
    val transactions: List<TransactionEntity>,
)

/** Estado de Movimientos: buscador, filtro por tipo y lista agrupada. */
data class TransactionsUiState(
    val groups: List<TransactionsDayGroup> = emptyList(),
    val hasAnyTransactions: Boolean = false,
    val query: String = "",
    val typeFilter: TransactionType? = null,
    val loaded: Boolean = false,
)

/**
 * Movimientos (T1.8): buscador, filtro por tipo, agrupado por día, edición
 * (desde la pantalla) y borrado con deshacer.
 */
@HiltViewModel
class TransactionsViewModel @Inject constructor(
    transactionDao: TransactionDao,
    private val deleteTransaction: DeleteTransaction,
    private val restoreTransaction: RestoreTransaction,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val typeFilter = MutableStateFlow<TransactionType?>(null)

    var actionFailed by mutableStateOf(false)
        private set

    val uiState: StateFlow<TransactionsUiState> = combine(
        transactionDao.observeAll(),
        query,
        typeFilter,
    ) { all, currentQuery, currentType ->
        TransactionsUiState(
            groups = buildDayGroups(all, currentQuery, currentType),
            hasAnyTransactions = all.isNotEmpty(),
            query = currentQuery,
            typeFilter = currentType,
            loaded = true,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TransactionsUiState(),
    )

    fun setQuery(value: String) {
        query.value = value
    }

    fun setTypeFilter(value: TransactionType?) {
        typeFilter.value = value
    }

    fun delete(transaction: TransactionEntity) {
        viewModelScope.launch {
            runCatching { deleteTransaction(transaction) }
                .onFailure { if (it is CancellationException) throw it else actionFailed = true }
        }
    }

    /** Deshacer: reinserta el movimiento con su id original. */
    fun restore(transaction: TransactionEntity) {
        viewModelScope.launch {
            runCatching { restoreTransaction(transaction) }
                .onFailure { if (it is CancellationException) throw it else actionFailed = true }
        }
    }

    /** El aviso de error ya se ha mostrado. */
    fun consumeActionFailed() {
        actionFailed = false
    }

    companion object {

        /**
         * Filtra por tipo y por texto (comercio o nota, sin distinguir mayúsculas)
         * y agrupa por día, con el más reciente primero. Función pura, testeable.
         */
        fun buildDayGroups(
            all: List<TransactionEntity>,
            query: String,
            typeFilter: TransactionType?,
        ): List<TransactionsDayGroup> {
            val term = query.trim()
            return all.asSequence()
                .filter { transaction ->
                    (typeFilter == null || transaction.type == typeFilter) &&
                        (
                            term.isEmpty() ||
                                transaction.merchant.contains(term, ignoreCase = true) ||
                                transaction.note.contains(term, ignoreCase = true)
                            )
                }
                .sortedWith(
                    compareByDescending<TransactionEntity> { it.date }
                        .thenByDescending { it.id },
                )
                .groupBy { it.date }
                .map { (day, transactions) -> TransactionsDayGroup(day, transactions) }
                .toList()
        }
    }
}
