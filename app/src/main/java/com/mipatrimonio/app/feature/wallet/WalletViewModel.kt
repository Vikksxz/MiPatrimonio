package com.mipatrimonio.app.feature.wallet

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.mipatrimonio.app.core.database.AppDatabase
import com.mipatrimonio.app.core.database.dao.CardDao
import com.mipatrimonio.app.core.database.dao.CategoryDao
import com.mipatrimonio.app.core.database.dao.RecurringDao
import com.mipatrimonio.app.core.database.entity.CardEntity
import com.mipatrimonio.app.core.database.entity.CategoryEntity
import com.mipatrimonio.app.core.database.entity.RecurringEntity
import com.mipatrimonio.app.core.model.TransactionType
import com.mipatrimonio.app.core.settings.SettingsRepository
import com.mipatrimonio.app.feature.reminders.CashReminderWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Estado y operaciones de las tarjetas de la Cartera (T1.6).
 * La lógica de persistencia vive aquí, no en los Composables.
 */
@HiltViewModel
class WalletViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val cardDao: CardDao,
    private val recurringDao: RecurringDao,
    private val categoryDao: CategoryDao,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _cards = MutableStateFlow<List<CardEntity>>(emptyList())
    val cards = _cards.asStateFlow()

    private val _recurrings = MutableStateFlow<List<RecurringEntity>>(emptyList())
    val recurrings = _recurrings.asStateFlow()

    private val _expenseCategories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val expenseCategories = _expenseCategories.asStateFlow()

    private val _cashReminderEnabled = MutableStateFlow(false)
    val cashReminderEnabled = _cashReminderEnabled.asStateFlow()

    init {
        viewModelScope.launch {
            cardDao.observeAll().collect { cards ->
                _cards.value = cards
            }
        }
        viewModelScope.launch {
            recurringDao.observeActive().collect { _recurrings.value = it }
        }
        viewModelScope.launch {
            categoryDao.observeByIncome(false).collect { _expenseCategories.value = it }
        }
        viewModelScope.launch {
            settingsRepository.cashReminderEnabled.collect { _cashReminderEnabled.value = it }
        }
    }

    /**
     * Inserta o actualiza una tarjeta. Si la foto cambió, borra del almacenamiento
     * privado la imagen anterior ([obsoletePhotoPath]).
     */
    fun saveCard(card: CardEntity, obsoletePhotoPath: String? = null) {
        viewModelScope.launch {
            if (card.id == 0L) {
                cardDao.insert(card)
            } else {
                cardDao.update(card)
            }
            if (!obsoletePhotoPath.isNullOrBlank() && obsoletePhotoPath != card.photoPath) {
                withContext(Dispatchers.IO) {
                    CardPhotoStore.delete(context, obsoletePhotoPath)
                }
            }
        }
    }

    fun deleteCard(card: CardEntity) {
        viewModelScope.launch {
            database.withTransaction {
                cardDao.delete(card)
                database.transactionDao().nullifyCardId(card.id)
            }
            withContext(Dispatchers.IO) {
                CardPhotoStore.delete(context, card.photoPath)
            }
        }
    }

    fun saveRecurring(entity: RecurringEntity) {
        viewModelScope.launch {
            if (entity.id == 0L) recurringDao.insert(entity) else recurringDao.update(entity)
        }
    }

    fun deleteRecurring(entity: RecurringEntity) {
        viewModelScope.launch { recurringDao.delete(entity) }
    }

    fun setCashReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setCashReminderEnabled(enabled)
            if (enabled) CashReminderWorker.schedule(context) else CashReminderWorker.cancel(context)
        }
    }
}
