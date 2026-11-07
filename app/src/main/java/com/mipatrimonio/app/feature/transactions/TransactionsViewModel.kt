package com.mipatrimonio.app.feature.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mipatrimonio.app.core.database.dao.TransactionDao
import com.mipatrimonio.app.core.database.entity.TransactionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Lista de movimientos (T1.7: lista básica; T1.8 añade buscador, filtros y edición).
 */
@HiltViewModel
class TransactionsViewModel @Inject constructor(
    transactionDao: TransactionDao,
) : ViewModel() {

    val transactions: StateFlow<List<TransactionEntity>> = transactionDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
