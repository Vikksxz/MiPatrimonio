package com.mipatrimonio.app.core.security

import android.os.SystemClock
import com.mipatrimonio.app.core.settings.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Estado de bloqueo de toda la app (§14). Es un singleton porque el ciclo de
 * vida lo controla `MainActivity` y lo consume la composición.
 */
@Singleton
class AppLockManager @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {

    private val _isLocked = MutableStateFlow(true)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    /** `false` hasta que se ha leído DataStore: evita enseñar la pantalla de bloqueo a quien la tiene desactivada. */
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private var unlockedInSession = false
    private var backgroundedAtMillis: Long? = null
    private var authInProgress = false

    /** Se llama al volver a primer plano (onStart). */
    suspend fun onForeground() {
        // El diálogo de huella/PIN abre otra actividad: no rebloquear al volver.
        if (authInProgress) return

        val settings = settingsRepository.current()
        val elapsed = backgroundedAtMillis?.let { SystemClock.elapsedRealtime() - it }
        val shouldLock = settings.enabled && (
            _isLocked.value ||
                LockPolicy.shouldLock(
                    enabled = true,
                    timeoutMinutes = settings.timeout.minutes,
                    elapsedMillis = elapsed,
                    unlockedInSession = unlockedInSession,
                )
            )

        backgroundedAtMillis = null
        if (shouldLock) {
            _isLocked.value = true
        } else {
            unlockedInSession = true
            _isLocked.value = false
        }
        _ready.value = true
    }

    /** Se llama al pasar a segundo plano (onStop). */
    fun onBackground() {
        if (authInProgress) return
        if (!_isLocked.value) {
            backgroundedAtMillis = SystemClock.elapsedRealtime()
        }
    }

    fun onAuthStarted() {
        authInProgress = true
    }

    fun onAuthFinished() {
        authInProgress = false
    }

    fun unlock() {
        authInProgress = false
        backgroundedAtMillis = null
        unlockedInSession = true
        _isLocked.value = false
    }
}
