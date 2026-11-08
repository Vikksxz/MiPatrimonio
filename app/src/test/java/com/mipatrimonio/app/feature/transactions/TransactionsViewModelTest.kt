package com.mipatrimonio.app.feature.transactions

import com.mipatrimonio.app.core.database.entity.TransactionEntity
import com.mipatrimonio.app.core.model.IncomeKind
import com.mipatrimonio.app.core.model.TransactionType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests de los filtros y la agrupación por día de Movimientos (T1.8). */
class TransactionsViewModelTest {

    private val day1 = LocalDate.of(2026, 11, 6)
    private val day2 = LocalDate.of(2026, 11, 5)

    private fun transaction(
        id: Long,
        type: TransactionType = TransactionType.CASH,
        amountCents: Long = 100,
        merchant: String = "",
        note: String = "",
        date: LocalDate = day1,
    ) = TransactionEntity(
        id = id,
        type = type,
        incomeKind = if (type == TransactionType.INCOME) IncomeKind.OTHER else null,
        amountCents = amountCents,
        amountBaseCents = amountCents,
        date = date.toEpochDay(),
        merchant = merchant,
        note = note,
        createdAt = 0L,
    )

    @Test
    fun groupsByDayWithTheMostRecentFirst() {
        val all = listOf(
            transaction(id = 1, date = day2),
            transaction(id = 2, date = day1),
            transaction(id = 3, date = day1),
        )

        val groups = TransactionsViewModel.buildDayGroups(all, "", null)

        assertEquals(2, groups.size)
        assertEquals(day1.toEpochDay(), groups[0].epochDay)
        assertEquals(day2.toEpochDay(), groups[1].epochDay)
        assertEquals(2, groups[0].transactions.size)
        assertEquals(1, groups[1].transactions.size)
    }

    @Test
    fun filtersByType() {
        val all = listOf(
            transaction(id = 1, type = TransactionType.CASH),
            transaction(id = 2, type = TransactionType.CARD),
            transaction(id = 3, type = TransactionType.CARD),
            transaction(id = 4, type = TransactionType.INCOME),
        )

        val groups = TransactionsViewModel.buildDayGroups(all, "", TransactionType.CARD)

        assertEquals(1, groups.size)
        assertEquals(listOf(3L, 2L), groups[0].transactions.map { it.id })
    }

    @Test
    fun filtersByMerchantIgnoringCase() {
        val all = listOf(
            transaction(id = 1, merchant = "Mercadona"),
            transaction(id = 2, merchant = "Netflix"),
        )

        val groups = TransactionsViewModel.buildDayGroups(all, "merca", null)

        assertEquals(listOf(1L), groups.flatMap { it.transactions }.map { it.id })
    }

    @Test
    fun filtersByNote() {
        val all = listOf(
            transaction(id = 1, merchant = "Cafe", note = "con Ana"),
            transaction(id = 2, merchant = "Cafe", note = "solo"),
        )

        val groups = TransactionsViewModel.buildDayGroups(all, "ana", null)

        assertEquals(listOf(1L), groups.flatMap { it.transactions }.map { it.id })
    }

    @Test
    fun queryIsTrimmed() {
        val all = listOf(transaction(id = 1, merchant = "Mercadona"))

        val groups = TransactionsViewModel.buildDayGroups(all, "  mercado  ", null)

        assertEquals(1, groups.flatMap { it.transactions }.size)
    }

    @Test
    fun noMatchReturnsEmptyList() {
        val all = listOf(transaction(id = 1, merchant = "Mercadona"))

        val groups = TransactionsViewModel.buildDayGroups(all, "zzz", null)

        assertTrue(groups.isEmpty())
    }

    @Test
    fun typeFilterAndSearchAreCombined() {
        val all = listOf(
            transaction(id = 1, type = TransactionType.CASH, merchant = "Cafe"),
            transaction(id = 2, type = TransactionType.CARD, merchant = "Cafe"),
            transaction(id = 3, type = TransactionType.CARD, merchant = "Mercadona"),
        )

        val groups = TransactionsViewModel.buildDayGroups(all, "cafe", TransactionType.CARD)

        assertEquals(listOf(2L), groups.flatMap { it.transactions }.map { it.id })
    }

    @Test
    fun sameDayKeepsTheHighestIdFirst() {
        val all = listOf(
            transaction(id = 1, date = day1),
            transaction(id = 5, date = day1),
            transaction(id = 3, date = day1),
        )

        val groups = TransactionsViewModel.buildDayGroups(all, "", null)

        assertEquals(listOf(5L, 3L, 1L), groups[0].transactions.map { it.id })
    }
}
