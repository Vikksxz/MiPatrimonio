package com.mipatrimonio.app.domain.usecase

import com.mipatrimonio.app.core.database.entity.TransactionEntity
import com.mipatrimonio.app.core.model.TransactionType
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SavingsContributionRulesTest {

    private val month = YearMonth.of(2026, 11)

    private fun tx(
        id: Long,
        type: TransactionType,
        amountBaseCents: Long,
        date: LocalDate = month.atDay(10),
    ) = TransactionEntity(
        id = id,
        type = type,
        amountCents = amountBaseCents,
        amountBaseCents = amountBaseCents,
        currency = "EUR",
        date = date.toEpochDay(),
        merchant = "m",
        createdAt = 0L,
    )

    @Test
    fun availableCents_incomeMinusExpensesMinusSavings() {
        val txs = listOf(
            tx(1, TransactionType.INCOME, 100_00),
            tx(2, TransactionType.CASH, 30_00),
            tx(3, TransactionType.CARD, 20_00),
        )
        assertEquals(40_00L, SavingsContributionRules.availableCents(month, txs, monthlyContributedCents = 10_00))
    }

    @Test
    fun canAddContribution_allowsWhenRemainingIsZero() {
        val r = SavingsContributionRules.canAddContribution(
            amountCents = 50_00,
            currentAvailableCents = 50_00,
            hasValidOrigin = true,
        )
        assertTrue(r.allowed)
    }

    @Test
    fun canAddContribution_blocksWhenInsufficient() {
        val r = SavingsContributionRules.canAddContribution(
            amountCents = 50_01,
            currentAvailableCents = 50_00,
            hasValidOrigin = true,
        )
        assertFalse(r.allowed)
        assertEquals(SavingsBlockReason.INSUFFICIENT_AVAILABLE, r.reason)
    }

    @Test
    fun canAddContribution_requiresOrigin() {
        val r = SavingsContributionRules.canAddContribution(
            amountCents = 10_00,
            currentAvailableCents = 100_00,
            hasValidOrigin = false,
        )
        assertFalse(r.allowed)
        assertEquals(SavingsBlockReason.MISSING_ORIGIN, r.reason)
    }

    @Test
    fun canDeleteIncome_blockedWhenLinked() {
        val r = SavingsContributionRules.canDeleteIncome(linkedContributionCount = 2)
        assertFalse(r.allowed)
        assertEquals(SavingsBlockReason.INCOME_HAS_LINKED_CONTRIBUTIONS, r.reason)
    }

    @Test
    fun canDeleteIncome_allowedWhenVirgin() {
        val r = SavingsContributionRules.canDeleteIncome(linkedContributionCount = 0)
        assertTrue(r.allowed)
    }

    @Test
    fun canEditLinkedIncome_blocksWhenNewAmountBelowLinkedSum() {
        val r = SavingsContributionRules.canEditLinkedIncome(
            newAmountBaseCents = 40_00,
            linkedContributionSumCents = 50_00,
            availableAfterEditInMonthCents = 10_00,
        )
        assertFalse(r.allowed)
        assertEquals(SavingsBlockReason.INCOME_EDIT_INCONSISTENT, r.reason)
    }

    @Test
    fun canEditLinkedIncome_blocksWhenAvailableWouldBeNegative() {
        val r = SavingsContributionRules.canEditLinkedIncome(
            newAmountBaseCents = 80_00,
            linkedContributionSumCents = 50_00,
            availableAfterEditInMonthCents = -1_00,
        )
        assertFalse(r.allowed)
        assertEquals(SavingsBlockReason.INCOME_EDIT_INCONSISTENT, r.reason)
    }

    @Test
    fun canEditLinkedIncome_allowedWhenConsistent() {
        val r = SavingsContributionRules.canEditLinkedIncome(
            newAmountBaseCents = 80_00,
            linkedContributionSumCents = 50_00,
            availableAfterEditInMonthCents = 0L,
        )
        assertTrue(r.allowed)
    }

    @Test
    fun canWithdraw_blocksWhenExceedsSaved() {
        val r = SavingsContributionRules.canWithdraw(amountCents = 30_00, contributedCents = 20_00)
        assertFalse(r.allowed)
        assertEquals(SavingsBlockReason.EXCEEDS_SAVED, r.reason)
    }
}