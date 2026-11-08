package com.mipatrimonio.app.core.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Tiempo que la app puede estar en segundo plano antes de volver a pedir el bloqueo. */
enum class LockTimeout(val minutes: Int) {
    IMMEDIATE(0),
    ONE_MINUTE(1),
    FIVE_MINUTES(5),
    ;

    companion object {
        fun fromMinutes(minutes: Int): LockTimeout =
            entries.firstOrNull { it.minutes == minutes } ?: ONE_MINUTE
    }
}

/** Ajustes de seguridad y privacidad (§14). Valores por defecto: todo activado. */
data class LockSettings(
    val enabled: Boolean = true,
    val timeout: LockTimeout = LockTimeout.ONE_MINUTE,
    val hideContent: Boolean = true,
)

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Ajustes persistentes en DataStore (§14). Por ahora guarda el bloqueo y la
 * ocultación de contenido; la pantalla de Ajustes llegará con T4.8.
 */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    val settings: Flow<LockSettings> = context.settingsDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences ->
            LockSettings(
                enabled = preferences[KEY_LOCK_ENABLED] ?: true,
                timeout = LockTimeout.fromMinutes(
                    preferences[KEY_LOCK_TIMEOUT_MINUTES] ?: LockTimeout.ONE_MINUTE.minutes,
                ),
                hideContent = preferences[KEY_HIDE_CONTENT] ?: true,
            )
        }

    suspend fun current(): LockSettings = settings.first()

    suspend fun setLockEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[KEY_LOCK_ENABLED] = enabled
        }
    }

    suspend fun setLockTimeout(timeout: LockTimeout) {
        context.settingsDataStore.edit { preferences ->
            preferences[KEY_LOCK_TIMEOUT_MINUTES] = timeout.minutes
        }
    }

    suspend fun setHideContent(hide: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[KEY_HIDE_CONTENT] = hide
        }
    }

    private companion object {
        val KEY_LOCK_ENABLED = booleanPreferencesKey("lock_enabled")
        val KEY_LOCK_TIMEOUT_MINUTES = intPreferencesKey("lock_timeout_minutes")
        val KEY_HIDE_CONTENT = booleanPreferencesKey("hide_content")
    }
}
