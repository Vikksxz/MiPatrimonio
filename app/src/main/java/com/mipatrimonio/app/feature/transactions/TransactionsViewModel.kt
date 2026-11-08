package com.mipatrimonio.app.feature.transactions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mipatrimonio.app.core.database.dao.TagDao
import com.mipatrimonio.app.core.database.dao.TransactionDao
import com.mipatrimonio.app.core.database.entity.TagEntity
import com.mipatrimonio.app.core.database.entity.TransactionEntity
import com.mipatrimonio.app.core.database.entity.TransactionTagCrossRef
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

/** Estado de Movimientos: buscador, filtros y lista agrupada. */
data class TransactionsUiState(
    val groups: List<TransactionsDayGroup> = emptyList(),
    val hasAnyTransactions: Boolean = false,
    val query: String = "",
    val typeFilter: TransactionType? = null,
    val tagFilterId: Long? = null,
    val availableTags: List<TagEntity> = emptyList(),
    val loaded: Boolean = false,
)

/**
 * Movimientos (T1.8): buscador, filtro por tipo, agrupado por día, edición
 * (desde la pantalla) y borrado con deshacer.
 */
@HiltViewModel
class TransactionsViewModel @Inject constructor(
    transactionDao: TransactionDao,
    tagDao: TagDao,
    private val deleteTransaction: DeleteTransaction,
    private val restoreTransaction: RestoreTransaction,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val typeFilter = MutableStateFlow<TransactionType?>(null)
    private val tagFilter = MutableStateFlow<Long?>(null)

    var actionFailed by mutableStateOf(false)
        private set

    private val base = combine(transactionDao.observeAll(), query, typeFilter, tagFilter) { all, q, t, tag ->
        TxFilterInput(all, q, t, tag)
    }

    val uiState: StateFlow<TransactionsUiState> = combine(
        base,
        tagDao.observeAll(),
        tagDao.observeAllCrossRefs(),
    ) { input, tags, crossRefs ->
        val txIdsForTag = if (input.tagFilter == null) null
        else crossRefs.asSequence().filter { it.tagId == input.tagFilter }.map { it.transactionId }.toSet()
        TransactionsUiState(
            groups = buildDayGroups(input.all, input.query, input.typeFilter, input.tagFilter, txIdsForTag),
            hasAnyTransactions = input.all.isNotEmpty(),
            query = input.query,
            typeFilter = input.typeFilter,
            tagFilterId = input.tagFilter,
            availableTags = tags,
            loaded = true,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TransactionsUiState(),
    )

    fun setQuery(value: String) { query.value = value }

    fun setTypeFilter(value: TransactionType?) { typeFilter.value = value }

    fun setTagFilter(value: Long?) { tagFilter.value = value }

    private data class TxFilterInput(
        val all: List<TransactionEntity>,
        val query: String,
        val typeFilter: TransactionType?,
        val tagFilter: Long?,
    )

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
         * Filtra por tipo, texto y etiqueta; agrupa por día (más reciente primero). Pura, testeable.
         */
        fun buildDayGroups(
            all: List<TransactionEntity>,
            query: String,
            typeFilter: TransactionType?,
            tagFilterId: Long? = null,
            txIdsForTag: Set<Long>? = null,
        ): List<TransactionsDayGroup> {
            val term = query.trim()
            return all.asSequence()
                .filter { transaction ->
                    (typeFilter == null || transaction.type == typeFilter) &&
                        (tagFilterId == null || txIdsForTag?.contains(transaction.id) == true) &&
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
