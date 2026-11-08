package com.mipatrimonio.app.domain.usecase

import com.mipatrimonio.app.core.database.dao.RecurringDao
import com.mipatrimonio.app.core.database.dao.TransactionDao
import com.mipatrimonio.app.core.database.entity.RecurringEntity
import com.mipatrimonio.app.core.database.entity.TransactionEntity
import com.mipatrimonio.app.core.model.IncomeKind
import com.mipatrimonio.app.core.model.TransactionType
import java.time.LocalDate
import javax.inject.Inject

data class RecurringProcessResult(
    val generated: Int,
    val duplicatesSkipped: Int,
)

/**
 * Genera movimientos vencidos de reglas recurrentes (catch-up, §7.6) con detección de duplicados.
 * Idempotente: no duplica (recurringId, date).
 */
class ProcessRecurringRules @Inject constructor(
    private val recurringDao: RecurringDao,
    private val transactionDao: TransactionDao,
) {

    suspend fun run(todayEpochDay: Long = LocalDate.now().toEpochDay()): RecurringProcessResult {
        val due = recurringDao.getDueUpTo(todayEpochDay)
        var generated = 0
        var skipped = 0
        val nowMillis = System.currentTimeMillis()
        for (rule in due) {
            val occ = RecurringSchedule.dueOccurrences(rule.nextDate, rule.period, todayEpochDay)
            for (date in occ.dueDatesEpochDay) {
                val exists = transactionDao.countForRecurringOnDate(rule.id, date) > 0
                if (exists) {
                    skipped++
                    continue
                }
                val type = if (rule.isSubscription) TransactionType.SUBSCRIPTION else rule.type
                val incomeKind = if (type == TransactionType.INCOME) IncomeKind.OTHER else null
                val cardId = if (type == TransactionType.CARD) rule.cardId else null
                val tx = TransactionEntity(
                    type = type,
                    incomeKind = incomeKind,
                    amountCents = rule.amountCents,
                    currency = "EUR",
                    amountBaseCents = rule.amountCents,
                    date = date,
                    merchant = rule.name,
                    note = "",
                    categoryId = rule.categoryId,
                    cardId = cardId,
                    receiptPath = null,
                    recurringId = rule.id,
                    sharedExpenseId = null,
                    createdAt = nowMillis,
                )
                transactionDao.insert(tx)
                generated++
            }
            if (occ.newNextDateEpochDay != rule.nextDate) {
                recurringDao.updateNextDate(rule.id, occ.newNextDateEpochDay)
            }
        }
        return RecurringProcessResult(generated, skipped)
    }
}
