package com.mipatrimonio.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mipatrimonio.app.domain.usecase.GetMonthlySummary
import com.mipatrimonio.app.domain.usecase.MonthlySummary
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Estado de Inicio (T1.9). `loaded` evita enseñar ceros en el primer frame. */
data class HomeUiState(
    val summary: MonthlySummary? = null,
    val loaded: Boolean = false,
)

/** Inicio (T1.9): resumen del mes actual y últimos movimientos. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getMonthlySummary: GetMonthlySummary,
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now())

    val uiState: StateFlow<HomeUiState> = month
        .flatMapLatest { currentMonth -> getMonthlySummary(currentMonth) }
        .map { summary -> HomeUiState(summary = summary, loaded = true) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(),
        )

    /** Recalcula el mes al volver a Inicio: evita quedarse en el mes anterior. */
    fun refreshMonth() {
        month.value = YearMonth.now()
    }
}
