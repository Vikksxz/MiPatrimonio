package com.mipatrimonio.app.domain.usecase

import com.mipatrimonio.app.core.database.entity.SharedExpenseEntity
import com.mipatrimonio.app.core.database.entity.SharedShareEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedBalancesTest {

    private fun expense(id: Long, totalCents: Long, paidBy: Long) =
        SharedExpenseEntity(id = id, title = "Gasto $id", totalCents = totalCents, paidByPersonId = paidBy, date = 20000L)

    private fun share(expenseId: Long, personId: Long, cents: Long, settled: Boolean = false) =
        SharedShareEntity(sharedExpenseId = expenseId, personId = personId, shareCents = cents, settled = settled)

    @Test
    fun twoPeopleOnePaysEqualSplit() {
        // A(1) paga 20€, partes A=10, B=10 -> A +10, B -10, "B debe a A 10€"
        val expenses = mapOf(1L to expense(1L, 2000L, paidBy = 1L))
        val shares = listOf(share(1L, 1L, 1000L), share(1L, 2L, 1000L))
        val net = SharedBalances.netBalances(expenses, shares)
        assertEquals(1000L, net[1L])
        assertEquals(-1000L, net[2L])
        val transfers = SharedBalances.whoOwesWhom(net)
        assertEquals(1, transfers.size)
        assertEquals(DebtTransfer(fromPersonId = 2L, toPersonId = 1L, amountCents = 1000L), transfers[0])
    }

    @Test
    fun threePeopleEqualSplit() {
        // A paga 30€, 10 cada uno -> A +20, B -10, C -10
        val expenses = mapOf(1L to expense(1L, 3000L, paidBy = 1L))
        val shares = listOf(share(1L, 1L, 1000L), share(1L, 2L, 1000L), share(1L, 3L, 1000L))
        val net = SharedBalances.netBalances(expenses, shares)
        assertEquals(2000L, net[1L])
        assertEquals(-1000L, net[2L])
        assertEquals(-1000L, net[3L])
        val transfers = SharedBalances.whoOwesWhom(net)
        assertEquals(2000L, transfers.sumOf { it.amountCents })
        assertTrue(transfers.all { it.toPersonId == 1L })
    }

    @Test
    fun customUnequalSplit() {
        val expenses = mapOf(1L to expense(1L, 5000L, paidBy = 1L))
        val shares = listOf(share(1L, 1L, 2000L), share(1L, 2L, 3000L))
        val net = SharedBalances.netBalances(expenses, shares)
        assertEquals(3000L, net[1L])
        assertEquals(-3000L, net[2L])
    }

    @Test
    fun partiallySettledShareExcluded() {
        // B liquida su parte; solo C sigue debiendo
        val expenses = mapOf(1L to expense(1L, 3000L, paidBy = 1L))
        val shares = listOf(
            share(1L, 1L, 1000L),
            share(1L, 2L, 1000L, settled = true),
            share(1L, 3L, 1000L),
        )
        val net = SharedBalances.netBalances(expenses, shares)
        assertEquals(1000L, net[1L])
        assertEquals(null, net[2L])
        assertEquals(-1000L, net[3L])
        val transfers = SharedBalances.whoOwesWhom(net)
        assertEquals(listOf(DebtTransfer(3L, 1L, 1000L)), transfers)
    }

    @Test
    fun fullySettledProducesNoBalances() {
        val expenses = mapOf(1L to expense(1L, 3000L, paidBy = 1L))
        val shares = listOf(
            share(1L, 1L, 1000L, settled = true),
            share(1L, 2L, 1000L, settled = true),
            share(1L, 3L, 1000L, settled = true),
        )
        val net = SharedBalances.netBalances(expenses, shares)
        assertTrue(net.isEmpty())
        assertTrue(SharedBalances.whoOwesWhom(net).isEmpty())
    }

    @Test
    fun threeDebtorsOneCreditorGreedyTotalsSquare() {
        // A acreedor +600 (pagó gastos); B -100, C -200, D -300
        val balances = mapOf(1L to 600L, 2L to -100L, 3L to -200L, 4L to -300L)
        val transfers = SharedBalances.whoOwesWhom(balances)
        assertEquals(600L, transfers.sumOf { it.amountCents })
        assertTrue(transfers.all { it.toPersonId == 1L })
        val byFrom = transfers.groupBy { it.fromPersonId }.mapValues { (_, v) -> v.sumOf { it.amountCents } }
        assertEquals(100L, byFrom[2L])
        assertEquals(200L, byFrom[3L])
        assertEquals(300L, byFrom[4L])
    }

    @Test
    fun multipleExpensesDifferentPayers() {
        // A paga 20 (A10,B10), B paga 20 (A10,B10) -> se compensan: net 0
        val expenses = mapOf(1L to expense(1L, 2000L, 1L), 2L to expense(2L, 2000L, 2L))
        val shares = listOf(
            share(1L, 1L, 1000L), share(1L, 2L, 1000L),
            share(2L, 1L, 1000L), share(2L, 2L, 1000L),
        )
        val net = SharedBalances.netBalances(expenses, shares)
        assertTrue(net.isEmpty())
    }
}
