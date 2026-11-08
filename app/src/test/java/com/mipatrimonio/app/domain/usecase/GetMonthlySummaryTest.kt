package com.mipatrimonio.app.domain.usecase

import com.mipatrimonio.app.core.database.entity.TransactionEntity
import com.mipatrimonio.app.core.model.TransactionType
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests del resumen mensual de Inicio (T1.9, MiPatrimonio.md §7.1). */
class GetMonthlySummaryTest {

    private val month = YearMonth.of(2026, 11)

    private fun transaction(
        type: TransactionType,
        amountCents: Long,
        day: Int,
        month: YearMonth = this.month,
        id: Long = 0L,
    ) = TransactionEntity(
        id = id,
        type = type,
        amountCents = amountCents,
        amountBaseCents = amountCents,
        date = month.atDay(day).toEpochDay(),
        merchant = "Comercio $id",
        createdAt = 0L,
    )

    @Test
    fun sumsEachTypeOfTheMonth() {
        val summary = GetMonthlySummary.build(
            month = month,
            transactions = listOf(
                transaction(TransactionType.INCOME, 142_400, day = 4),
                transaction(TransactionType.CASH, 1_250, day = 7),
                transaction(TransactionType.CARD, 4_599, day = 7),
                transaction(TransactionType.SUBSCRIPTION, 999, day = 10),
            ),
            recent = emptyList(),
        )

        assertEquals(142_400L, summary.incomeCents)
        assertEquals(1_250L, summary.cashCents)
        assertEquals(4_599L, summary.cardCents)
        assertEquals(999L, summary.subscriptionCents)
        assertEquals(6_848L, summary.expenseCents)
        assertEquals(135_552L, summary.availableCents)
    }

    @Test
    fun savingsAreSubtractedFromAvailable() {
        val summary = GetMonthlySummary.build(
            month = month,
            transactions = listOf(
                transaction(TransactionType.INCOME, 200_000, day = 1),
                transaction(TransactionType.CASH, 50_000, day = 2),
            ),
            recent = emptyList(),
            savingsCents = 25_000,
        )

        assertEquals(25_000L, summary.savingsCents)
        assertEquals(125_000L, summary.availableCents)
    }

    @Test
    fun transactionsFromOtherMonthsAreIgnored() {
        val summary = GetMonthlySummary.build(
            month = month,
            transactions = listOf(
                transaction(TransactionType.INCOME, 100_000, day = 1),
                transaction(TransactionType.CASH, 500_000, day = 15, month = YearMonth.of(2026, 10)),
                transaction(TransactionType.CARD, 700_000, day = 1, month = YearMonth.of(2026, 12)),
            ),
            recent = emptyList(),
        )

        assertEquals(100_000L, summary.incomeCents)
        assertEquals(0L, summary.cashCents)
        assertEquals(0L, summary.cardCents)
        assertEquals(0L, summary.subscriptionCents)
        assertEquals(100_000L, summary.availableCents)
    }

    @Test
    fun recentIsTrimmedToFourMovements() {
        val recent = (1..6).map { id ->
            transaction(TransactionType.CASH, 100, day = 1, id = id.toLong())
        }

        val summary = GetMonthlySummary.build(
            month = month,
            transactions = emptyList(),
            recent = recent,
        )

        assertEquals(GetMonthlySummary.RECENT_LIMIT, summary.recent.size)
        assertEquals(listOf(1L, 2L, 3L, 4L), summary.recent.map { it.id })
    }

    @Test
    fun emptyMonthKeepsZeroesAndNoSign() {
        val summary = GetMonthlySummary.build(
            month = month,
            transactions = emptyList(),
            recent = emptyList(),
        )

        assertEquals(0L, summary.incomeCents)
        assertEquals(0L, summary.expenseCents)
        assertEquals(0L, summary.availableCents)
        assertTrue(summary.recent.isEmpty())
    }

    @Test
    fun overspendingMakesAvailableNegative() {
        val summary = GetMonthlySummary.build(
            month = month,
            transactions = listOf(
                transaction(TransactionType.INCOME, 10_000, day = 1),
                transaction(TransactionType.CARD, 15_000, day = 2),
            ),
            recent = emptyList(),
        )

        assertEquals(-5_000L, summary.availableCents)
    }

    @Test
    fun amountsUseBaseCents() {
        val foreign = transaction(TransactionType.CARD, 20_000, day = 3)
            .copy(amountCents = 20_000, amountBaseCents = 18_000)

        val summary = GetMonthlySummary.build(
            month = month,
            transactions = listOf(foreign),
            recent = emptyList(),
        )

        assertEquals(18_000L, summary.cardCents)
    }

    @Test
    fun monthIsKeptInTheSummary() {
        val summary = GetMonthlySummary.build(
            month = month,
            transactions = emptyList(),
            recent = emptyList(),
        )

        assertEquals(LocalDate.of(2026, 11, 1).let(YearMonth::from), summary.month)
    }

    @Test
    fun segmentWeightsShareTheWholeBarEvenWithoutExpenses() {
        val weights = GetMonthlySummary.segmentWeights(
            cashCents = 0,
            cardCents = 0,
            subscriptionCents = 0,
        )

        assertEquals(3, weights.size)
        assertEquals(1f, weights.sum(), 0.0001f)
        weights.forEach { weight ->
            assertEquals(1f / 3f, weight, 0.0001f)
        }
    }

    @Test
    fun segmentWeightsAreProportionalToEachExpense() {
        val weights = GetMonthlySummary.segmentWeights(
            cashCents = 5_000,
            cardCents = 3_000,
            subscriptionCents = 2_000,
        )

        assertEquals(1f, weights.sum(), 0.0001f)
        // El mínimo fijo (6 %) se reparte por igual y el 82 % restante es proporcional.
        assertEquals(0.06f + 0.82f * 0.5f, weights[0], 0.0001f)
        assertEquals(0.06f + 0.82f * 0.3f, weights[1], 0.0001f)
        assertEquals(0.06f + 0.82f * 0.2f, weights[2], 0.0001f)
        assertTrue(weights[0] > weights[1])
        assertTrue(weights[1] > weights[2])
    }

    @Test
    fun segmentWeightsKeepZeroTypesVisible() {
        val weights = GetMonthlySummary.segmentWeights(
            cashCents = 12_500,
            cardCents = 0,
            subscriptionCents = 0,
        )

        assertEquals(1f, weights.sum(), 0.0001f)
        assertTrue(weights[1] >= GetMonthlySummary.MIN_SEGMENT_SHARE)
        assertTrue(weights[2] >= GetMonthlySummary.MIN_SEGMENT_SHARE)
        assertTrue(weights[0] > weights[1])
    }

    @Test
    fun segmentWeightsIgnoreNegativeAmounts() {
        val weights = GetMonthlySummary.segmentWeights(
            cashCents = -500,
            cardCents = 1_000,
            subscriptionCents = 0,
        )

        assertEquals(1f, weights.sum(), 0.0001f)
        assertTrue(weights.all { it >= GetMonthlySummary.MIN_SEGMENT_SHARE })
    }

    @Test
    fun monthRangeExcludesTheFirstDayOfTheNextMonth() {
        val nextMonth = month.plusMonths(1)
        val summary = GetMonthlySummary.build(
            month = month,
            transactions = listOf(
                transaction(TransactionType.CASH, 1_000, day = 30),
                transaction(TransactionType.CASH, 9_999, day = 1, month = nextMonth),
            ),
            recent = emptyList(),
        )

        assertEquals(1_000L, summary.cashCents)
    }

    @Test
    fun decemberDoesNotIncludeJanuaryOfTheNextYear() {
        val december = YearMonth.of(2026, 12)
        val summary = GetMonthlySummary.build(
            month = december,
            transactions = listOf(
                transaction(TransactionType.CARD, 5_000, day = 31, month = december),
                transaction(TransactionType.CARD, 7_000, day = 1, month = YearMonth.of(2027, 1)),
            ),
            recent = emptyList(),
        )

        assertEquals(5_000L, summary.cardCents)
    }
}
