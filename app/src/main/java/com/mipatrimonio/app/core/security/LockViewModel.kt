package com.mipatrimonio.app.core.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Estado de bloqueo expuesto a la composición (T1.10). */
data class LockUiState(
    val ready: Boolean = false,
    val locked: Boolean = true,
)

@HiltViewModel
class LockViewModel @Inject constructor(
    private val appLockManager: AppLockManager,
) : ViewModel() {

    val state: StateFlow<LockUiState> = combine(
        appLockManager.ready,
        appLockManager.isLocked,
    ) { ready, locked ->
        LockUiState(ready = ready, locked = locked)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LockUiState(),
    )

    fun onAuthStarted() = appLockManager.onAuthStarted()

    fun onAuthFinished() = appLockManager.onAuthFinished()

    fun unlock() = appLockManager.unlock()
}
