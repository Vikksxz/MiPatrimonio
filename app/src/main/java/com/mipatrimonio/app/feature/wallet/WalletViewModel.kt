package com.mipatrimonio.app.feature.wallet

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mipatrimonio.app.core.database.dao.CardDao
import com.mipatrimonio.app.core.database.entity.CardEntity
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
    private val cardDao: CardDao,
) : ViewModel() {

    private val _cards = MutableStateFlow<List<CardEntity>>(emptyList())
    val cards = _cards.asStateFlow()

    init {
        viewModelScope.launch {
            cardDao.observeAll().collect { cards ->
                _cards.value = cards
            }
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
            cardDao.delete(card)
            withContext(Dispatchers.IO) {
                CardPhotoStore.delete(context, card.photoPath)
            }
        }
    }
}
