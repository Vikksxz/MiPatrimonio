package com.mipatrimonio.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mipatrimonio.app.core.settings.HomeBlockConfig
import com.mipatrimonio.app.core.settings.HomeDashboardConfig
import com.mipatrimonio.app.core.settings.SettingsRepository
import com.mipatrimonio.app.domain.usecase.GetMonthlySummary
import com.mipatrimonio.app.domain.usecase.MonthlySummary
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

/** Estado de Inicio (T1.9, T4.8). `loaded` evita enseñar ceros en el primer frame. */
data class HomeUiState(
    val summary: MonthlySummary? = null,
    val dashboardConfig: List<HomeBlockConfig> = HomeDashboardConfig.DEFAULT,
    val loaded: Boolean = false,
)

/** Inicio (T1.9): resumen del mes actual y últimos movimientos. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getMonthlySummary: GetMonthlySummary,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now())

    private val summaryFlow = month.flatMapLatest { currentMonth -> getMonthlySummary(currentMonth) }

    val uiState: StateFlow<HomeUiState> = combine(
        summaryFlow,
        settingsRepository.homeDashboardConfig,
    ) { summary, config ->
        HomeUiState(summary = summary, dashboardConfig = config, loaded = true)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    /** Recalcula el mes al volver a Inicio: evita quedarse en el mes anterior. */
    fun refreshMonth() {
        month.value = YearMonth.now()
    }
}
