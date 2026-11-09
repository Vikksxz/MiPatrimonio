package com.mipatrimonio.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mipatrimonio.app.core.settings.HomeBlockConfig
import com.mipatrimonio.app.core.settings.HomeDashboardConfig
import com.mipatrimonio.app.core.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeSettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val config: StateFlow<List<HomeBlockConfig>> = settingsRepository.homeDashboardConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeDashboardConfig.DEFAULT)

    fun setVisible(blockKey: String, visible: Boolean) {
        update { list -> list.map { if (it.block.key == blockKey) it.copy(visible = visible) else it } }
    }

    fun move(blockKey: String, delta: Int) {
        update { list ->
            val idx = list.indexOfFirst { it.block.key == blockKey }
            if (idx < 0) return@update list
            val target = (idx + delta).coerceIn(0, list.lastIndex)
            if (target == idx) return@update list
            val mutable = list.toMutableList()
            val item = mutable.removeAt(idx)
            mutable.add(target, item)
            mutable
        }
    }

    private fun update(transform: (List<HomeBlockConfig>) -> List<HomeBlockConfig>) {
        viewModelScope.launch {
            settingsRepository.setHomeDashboardConfig(transform(config.value))
        }
    }
}
