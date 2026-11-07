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
import com.mipatrimonio.app.domain.usecase.AddTransaction
import com.mipatrimonio.app.domain.usecase.NewTransaction
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
        if (saving) return
        saving = true
        saveFailed = false
        viewModelScope.launch {
            runCatching { addTransaction(input) }
                .onSuccess { id ->
                    // `saving` sigue true hasta que la hoja se cierre: evita un segundo insert.
                    onSaved(id)
                }
                .onFailure { error ->
                    saving = false
                    if (error is CancellationException) throw error
                    saveFailed = true
                }
        }
    }
}
