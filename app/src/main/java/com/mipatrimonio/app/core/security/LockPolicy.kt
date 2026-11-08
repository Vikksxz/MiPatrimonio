package com.mipatrimonio.app.core.security

/**
 * Política de bloqueo (§14), separada de Android para poder probarla:
 * - Si el bloqueo está desactivado, nunca se bloquea.
 * - La primera vez de la sesión siempre pide autenticación.
 * - Después, solo si ha pasado el tiempo configurado en segundo plano.
 * - "Inmediato" (0 minutos) bloquea a la mínima vuelta.
 */
object LockPolicy {

    fun shouldLock(
        enabled: Boolean,
        timeoutMinutes: Int,
        elapsedMillis: Long?,
        unlockedInSession: Boolean,
    ): Boolean {
        if (!enabled) return false
        if (!unlockedInSession) return true
        if (elapsedMillis == null) return false
        return elapsedMillis >= timeoutMinutes * 60_000L
    }
}
