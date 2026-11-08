package com.mipatrimonio.app.feature.reminders

import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class CashReminderWorkerTest {

    private val zone = ZoneId.of("Europe/Madrid")

    @Test
    fun delayTo21_before21_schedulesSameDay() {
        val now = ZonedDateTime.of(2026, 11, 8, 20, 0, 0, 0, zone)
        val d = CashReminderWorker.initialDelayToHour(21, now)
        assertEquals(Duration.ofHours(1), d)
    }

    @Test
    fun delayTo21_after21_schedulesNextDay() {
        val now = ZonedDateTime.of(2026, 11, 8, 21, 30, 0, 0, zone)
        val d = CashReminderWorker.initialDelayToHour(21, now)
        assertEquals(Duration.ofHours(23).plusMinutes(30), d)
    }

    @Test
    fun delayTo21_at21_schedulesNextDay() {
        val now = ZonedDateTime.of(2026, 11, 8, 21, 0, 0, 0, zone)
        val d = CashReminderWorker.initialDelayToHour(21, now)
        assertEquals(Duration.ofHours(24), d)
    }
}
