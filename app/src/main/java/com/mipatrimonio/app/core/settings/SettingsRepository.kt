package com.mipatrimonio.app.core.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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

/** Bloques del dashboard de Inicio (§16). Orden y visibilidad configurables. */
enum class HomeBlock(val key: String) {
    BALANCE("balance"),
    SAVINGS_PLAN("plan"),
    BREAKDOWN("breakdown"),
    ALERTS("alerts"),
    PROJECTION("projection"),
    RECENT("recent"),
    ;

    companion object {
        val DEFAULT_ORDER = entries.toList()
        fun fromKey(key: String): HomeBlock? = entries.firstOrNull { it.key == key }
    }
}

data class HomeBlockConfig(val block: HomeBlock, val visible: Boolean = true)

object HomeDashboardConfig {
    private const val SEP = ","
    private const val KV = ":"

    val DEFAULT: List<HomeBlockConfig> = HomeBlock.DEFAULT_ORDER.map { HomeBlockConfig(it, true) }

    fun encode(config: List<HomeBlockConfig>): String =
        config.joinToString(SEP) { "${it.block.key}$KV${if (it.visible) 1 else 0}" }

    fun decode(raw: String?): List<HomeBlockConfig> {
        if (raw.isNullOrBlank()) return DEFAULT
        val parsed = raw.split(SEP).mapNotNull { part ->
            val pieces = part.split(KV)
            if (pieces.size != 2) return@mapNotNull null
            val block = HomeBlock.fromKey(pieces[0]) ?: return@mapNotNull null
            HomeBlockConfig(block, pieces[1] == "1")
        }.toMutableList()
        // Asegurar que todos los bloques existen (por si se añaden en futuras versiones)
        val present = parsed.map { it.block }.toSet()
        HomeBlock.entries.forEach { b -> if (b !in present) parsed.add(HomeBlockConfig(b, true)) }
        return parsed
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

    /** Recordatorio nocturno de gastos en metálico (§12, T3.8). Desactivado por defecto. */
    val cashReminderEnabled: Flow<Boolean> = context.settingsDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences -> preferences[KEY_CASH_REMINDER_ENABLED] ?: false }

    suspend fun setCashReminderEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[KEY_CASH_REMINDER_ENABLED] = enabled
        }
    }

    /** Último informe mensual auto-generado (YYYYMM del mes informado), 0 si ninguno. */
    suspend fun lastMonthlyReportMonth(): Int =
        context.settingsDataStore.data.first()[KEY_LAST_MONTHLY_REPORT] ?: 0

    suspend fun setLastMonthlyReportMonth(yyyymm: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[KEY_LAST_MONTHLY_REPORT] = yyyymm
        }
    }

    /** Configuración del dashboard de Inicio: orden y visibilidad de bloques (§16). */
    val homeDashboardConfig: Flow<List<HomeBlockConfig>> = context.settingsDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences -> HomeDashboardConfig.decode(preferences[KEY_HOME_DASHBOARD]) }

    suspend fun setHomeDashboardConfig(config: List<HomeBlockConfig>) {
        context.settingsDataStore.edit { preferences ->
            preferences[KEY_HOME_DASHBOARD] = HomeDashboardConfig.encode(config)
        }
    }

    /** IDs de documentos ya notificados por vencimiento próximo (T4.8). */
    suspend fun notifiedExpiringDocumentIds(): Set<Long> =
        context.settingsDataStore.data.first()[KEY_NOTIFIED_EXPIRING_DOCS]
            ?.split(",")?.mapNotNull { it.toLongOrNull() }?.toSet().orEmpty()

    suspend fun addNotifiedExpiringDocumentIds(ids: Set<Long>) {
        if (ids.isEmpty()) return
        context.settingsDataStore.edit { preferences ->
            val current = preferences[KEY_NOTIFIED_EXPIRING_DOCS]
                ?.split(",")?.mapNotNull { it.toLongOrNull() }?.toMutableSet() ?: mutableSetOf()
            current.addAll(ids)
            preferences[KEY_NOTIFIED_EXPIRING_DOCS] = current.joinToString(",")
        }
    }

    private companion object {
        val KEY_LOCK_ENABLED = booleanPreferencesKey("lock_enabled")
        val KEY_LOCK_TIMEOUT_MINUTES = intPreferencesKey("lock_timeout_minutes")
        val KEY_HIDE_CONTENT = booleanPreferencesKey("hide_content")
        val KEY_CASH_REMINDER_ENABLED = booleanPreferencesKey("cash_reminder_enabled")
        val KEY_LAST_MONTHLY_REPORT = intPreferencesKey("last_monthly_report_month")
        val KEY_HOME_DASHBOARD = stringPreferencesKey("home_dashboard_config")
        val KEY_NOTIFIED_EXPIRING_DOCS = stringPreferencesKey("notified_expiring_doc_ids")
    }
}
