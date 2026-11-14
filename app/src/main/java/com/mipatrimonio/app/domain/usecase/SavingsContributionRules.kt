package com.mipatrimonio.app.domain.usecase

import com.mipatrimonio.app.core.database.entity.PlanContributionEntity
import com.mipatrimonio.app.core.database.entity.TransactionEntity
import com.mipatrimonio.app.core.model.TransactionType
import java.time.LocalDate
import java.time.YearMonth

/** Motivos normalizados para decisiones de ahorro/disponible (T6.1). */
enum class SavingsBlockReason {
    INVALID_AMOUNT,
    INSUFFICIENT_AVAILABLE,
    MISSING_ORIGIN,
    INVALID_ORIGIN,
    INCOME_HAS_LINKED_CONTRIBUTIONS,
    INCOME_EDIT_INCONSISTENT,
    EXCEEDS_SAVED,
}

data class ContributionValidation(
    val allowed: Boolean,
    val reason: SavingsBlockReason? = null,
)

object SavingsContributionRules {

    /** Disponible de un mes: ingresos − gastos − aportaciones al ahorro (§7.1). */
    fun availableCents(
        month: YearMonth,
        transactions: List<TransactionEntity>,
        monthlyContributedCents: Long,
    ): Long {
        var income = 0L
        var expense = 0L
        transactions.forEach { tx ->
            if (YearMonth.from(LocalDate.ofEpochDay(tx.date)) != month) return@forEach
            when (tx.type) {
                TransactionType.INCOME -> income += tx.amountBaseCents
                TransactionType.CASH, TransactionType.CARD, TransactionType.SUBSCRIPTION ->
                    expense += tx.amountBaseCents
            }
        }
        return income - expense - monthlyContributedCents
    }

    /** Simula disponible de un mes tras aplicar cambios hipotéticos. */
    fun availableAfterDelta(
        month: YearMonth,
        transactions: List<TransactionEntity>,
        monthlyContributedCents: Long,
        incomeDeltaCents: Long = 0L,
        expenseDeltaCents: Long = 0L,
        contributionDeltaCents: Long = 0L,
    ): Long = availableCents(month, transactions, monthlyContributedCents) +
        incomeDeltaCents - expenseDeltaCents - contributionDeltaCents

    fun canAddContribution(
        amountCents: Long,
        currentAvailableCents: Long,
        hasValidOrigin: Boolean,
    ): ContributionValidation {
        if (amountCents <= 0L) return ContributionValidation(false, SavingsBlockReason.INVALID_AMOUNT)
        if (!hasValidOrigin) return ContributionValidation(false, SavingsBlockReason.MISSING_ORIGIN)
        // Permitido solo si queda >= 0 tras aportar.
        if (amountCents > currentAvailableCents) {
            return ContributionValidation(false, SavingsBlockReason.INSUFFICIENT_AVAILABLE)
        }
        return ContributionValidation(true)
    }

    fun canWithdraw(amountCents: Long, contributedCents: Long): ContributionValidation {
        if (amountCents <= 0L) return ContributionValidation(false, SavingsBlockReason.INVALID_AMOUNT)
        if (amountCents > contributedCents) return ContributionValidation(false, SavingsBlockReason.EXCEEDS_SAVED)
        return ContributionValidation(true)
    }

    /** Borrado de ingreso: si hay aportaciones vinculadas, no se puede borrar. */
    fun canDeleteIncome(linkedContributionCount: Int): ContributionValidation {
        if (linkedContributionCount > 0) {
            return ContributionValidation(false, SavingsBlockReason.INCOME_HAS_LINKED_CONTRIBUTIONS)
        }
        return ContributionValidation(true)
    }

    /**
     * Edición de ingreso vinculado: bloquear si el nuevo importe (base) es menor que el total
     * ya aportado con ese origen, o si el cambio deja disponible < 0 en el mes afectado.
     */
    fun canEditLinkedIncome(
        newAmountBaseCents: Long,
        linkedContributionSumCents: Long,
        availableAfterEditInMonthCents: Long,
    ): ContributionValidation {
        if (newAmountBaseCents < linkedContributionSumCents) {
            return ContributionValidation(false, SavingsBlockReason.INCOME_EDIT_INCONSISTENT)
        }
        if (availableAfterEditInMonthCents < 0L) {
            return ContributionValidation(false, SavingsBlockReason.INCOME_EDIT_INCONSISTENT)
        }
        return ContributionValidation(true)
    }

    fun totalContributed(contributions: List<PlanContributionEntity>): Long =
        contributions.sumOf { it.amountCents }

}
