package com.mipatrimonio.app.domain.usecase

import com.mipatrimonio.app.core.database.dao.SavingsDao
import com.mipatrimonio.app.core.database.dao.TransactionDao
import com.mipatrimonio.app.core.database.entity.TransactionEntity
import com.mipatrimonio.app.core.model.TransactionType
import com.mipatrimonio.app.feature.widget.WidgetUpdater
import javax.inject.Inject

class IncomeHasLinkedContributionsException(val incomeId: Long) : IllegalStateException("income_has_linked_contributions")

/**
 * Caso de uso `DeleteTransaction` (T1.8, T6.1): borra un movimiento si las reglas lo permiten.
 * Un ingreso con aportaciones vinculadas no se puede borrar (§7.1, §8.2).
 */
class DeleteTransaction @Inject constructor(
    private val transactionDao: TransactionDao,
    private val savingsDao: SavingsDao,
    private val widgetUpdater: WidgetUpdater,
) {

    suspend operator fun invoke(transaction: TransactionEntity) {
        if (transaction.type == TransactionType.INCOME) {
            val linked = savingsDao.countLinkedContributions(transaction.id)
            if (linked > 0) throw IncomeHasLinkedContributionsException(transaction.id)
        }
        transactionDao.delete(transaction)
        widgetUpdater.update()
    }
}

class RestoreWouldMakeAvailableNegativeException : IllegalStateException("restore_would_make_available_negative")

/**
 * Caso de uso `RestoreTransaction` (T1.8, T6.1): deshace un borrado reinsertando el
 * movimiento con su id original, respetando la regla de disponible no negativo.
 */
class RestoreTransaction @Inject constructor(
    private val transactionDao: TransactionDao,
    private val savingsDao: SavingsDao,
    private val widgetUpdater: WidgetUpdater,
) {

    suspend operator fun invoke(transaction: TransactionEntity) {
        if (transaction.type != TransactionType.INCOME) {
            val month = java.time.YearMonth.from(java.time.LocalDate.ofEpochDay(transaction.date))
            val from = month.atDay(1).toEpochDay()
            val to = month.plusMonths(1).atDay(1).toEpochDay()
            val txs = transactionDao.getByMonth(from, to).filterNot { it.id == transaction.id } + transaction
            val savings = savingsDao.getMonthlyContributed(from, to) ?: 0L
            val availableAfter = SavingsContributionRules.availableCents(month, txs, savings)
            if (availableAfter < 0L) throw RestoreWouldMakeAvailableNegativeException()
        }
        transactionDao.restore(transaction)
        widgetUpdater.update()
    }
}
