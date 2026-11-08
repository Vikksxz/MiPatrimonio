package com.mipatrimonio.app.core.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests de la política de bloqueo (§14, T1.10). */
class LockPolicyTest {

    @Test
    fun disabledNeverLocks() {
        assertFalse(
            LockPolicy.shouldLock(
                enabled = false,
                timeoutMinutes = 0,
                elapsedMillis = 60_000,
                unlockedInSession = false,
            ),
        )
    }

    @Test
    fun firstForegroundAlwaysLocks() {
        assertTrue(
            LockPolicy.shouldLock(
                enabled = true,
                timeoutMinutes = 5,
                elapsedMillis = null,
                unlockedInSession = false,
            ),
        )
    }

    @Test
    fun noBackgroundTimeDoesNotLock() {
        assertFalse(
            LockPolicy.shouldLock(
                enabled = true,
                timeoutMinutes = 0,
                elapsedMillis = null,
                unlockedInSession = true,
            ),
        )
    }

    @Test
    fun immediateTimeoutLocksAfterAnyBackgroundTime() {
        assertTrue(
            LockPolicy.shouldLock(
                enabled = true,
                timeoutMinutes = 0,
                elapsedMillis = 1,
                unlockedInSession = true,
            ),
        )
    }

    @Test
    fun oneMinuteTimeoutHonoursTheBoundary() {
        assertFalse(
            LockPolicy.shouldLock(
                enabled = true,
                timeoutMinutes = 1,
                elapsedMillis = 59_999,
                unlockedInSession = true,
            ),
        )
        assertTrue(
            LockPolicy.shouldLock(
                enabled = true,
                timeoutMinutes = 1,
                elapsedMillis = 60_000,
                unlockedInSession = true,
            ),
        )
    }

    @Test
    fun fiveMinuteTimeoutHonoursTheBoundary() {
        assertFalse(
            LockPolicy.shouldLock(
                enabled = true,
                timeoutMinutes = 5,
                elapsedMillis = 299_999,
                unlockedInSession = true,
            ),
        )
        assertTrue(
            LockPolicy.shouldLock(
                enabled = true,
                timeoutMinutes = 5,
                elapsedMillis = 300_000,
                unlockedInSession = true,
            ),
        )
    }
}
