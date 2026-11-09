package com.mipatrimonio.app.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SharedSplitTest {

    @Test
    fun splitEvenlyExact() {
        val result = SharedSplit.splitEvenly(3000L, listOf(1L, 2L, 3L))
        assertEquals(1000L, result[1L])
        assertEquals(1000L, result[2L])
        assertEquals(1000L, result[3L])
        assertEquals(3000L, result.values.sumOf { it })
    }

    @Test
    fun splitEvenlyRemainderGoesToPreferredThenOrder() {
        // 100 / 3 = 33 r 1 -> extra céntimo a preferPersonId
        val result = SharedSplit.splitEvenly(100L, listOf(1L, 2L, 3L), preferPersonId = 2L)
        assertEquals(33L, result[1L])
        assertEquals(34L, result[2L])
        assertEquals(33L, result[3L])
        assertEquals(100L, result.values.sumOf { it })
    }

    @Test
    fun splitEvenlyRemainderWithoutPreferredStartsAtFirst() {
        val result = SharedSplit.splitEvenly(101L, listOf(1L, 2L, 3L))
        assertEquals(34L, result[1L])
        assertEquals(34L, result[2L])
        assertEquals(33L, result[3L])
        assertEquals(101L, result.values.sumOf { it })
    }

    @Test
    fun splitEvenlyPreferredNotParticipantFallsBackToFirst() {
        val result = SharedSplit.splitEvenly(100L, listOf(1L, 2L, 3L), preferPersonId = 99L)
        assertEquals(34L, result[1L])
        assertEquals(100L, result.values.sumOf { it })
    }

    @Test
    fun validateCustomSharesAcceptsExactSum() {
        SharedSplit.validateCustomShares(5000L, mapOf(1L to 2000L, 2L to 3000L))
    }

    @Test
    fun validateCustomSharesRejectsWrongSum() {
        assertThrows(IllegalArgumentException::class.java) {
            SharedSplit.validateCustomShares(5000L, mapOf(1L to 2000L, 2L to 2000L))
        }
    }

    @Test
    fun validateCustomSharesRejectsNonPositiveShare() {
        assertThrows(IllegalArgumentException::class.java) {
            SharedSplit.validateCustomShares(5000L, mapOf(1L to 0L, 2L to 5000L))
        }
    }
}
