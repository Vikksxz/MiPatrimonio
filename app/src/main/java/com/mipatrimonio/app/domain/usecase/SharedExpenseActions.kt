package com.mipatrimonio.app.domain.usecase

import android.util.Log
import androidx.room.withTransaction
import com.mipatrimonio.app.core.database.AppDatabase
import com.mipatrimonio.app.core.database.dao.PersonDao
import com.mipatrimonio.app.core.database.dao.SharedExpenseDao
import com.mipatrimonio.app.core.database.dao.SharedShareDao
import com.mipatrimonio.app.core.database.dao.TransactionDao
import com.mipatrimonio.app.core.database.entity.SharedExpenseEntity
import com.mipatrimonio.app.core.database.entity.SharedShareEntity
import com.mipatrimonio.app.core.database.entity.TransactionEntity
import com.mipatrimonio.app.core.model.TransactionType
import com.mipatrimonio.app.feature.widget.WidgetUpdater
import java.time.LocalDate
import javax.inject.Inject

/**
 * Casos de uso de gastos compartidos (T4.7, §7.9/§8.8).
 * Solo EUR (esquema sin moneda). Si paga "yo" (isMe), genera un movimiento normal por su parte.
 */
class AddSharedExpense @Inject constructor(
    private val database: AppDatabase,
    private val sharedExpenseDao: SharedExpenseDao,
    private val sharedShareDao: SharedShareDao,
    private val personDao: PersonDao,
    private val transactionDao: TransactionDao,
    private val evaluateBudgetAlerts: EvaluateBudgetAlerts,
    private val widgetUpdater: WidgetUpdater,
) {
    suspend operator fun invoke(
        title: String,
        totalCents: Long,
        paidByPersonId: Long,
        dateEpochDay: Long,
        shares: Map<Long, Long>, // personId -> shareCents, suma == totalCents
        createdAtMillis: Long = System.currentTimeMillis(),
    ): Long {
        SharedSplit.validateCustomShares(totalCents, shares)
        require(paidByPersonId > 0L) { "Falta quién pagó" }
        require(shares.containsKey(paidByPersonId) || shares.isNotEmpty()) { "Participantes no válidos" }
        val cleanTitle = title.trim()
        require(cleanTitle.isNotEmpty()) { "El concepto es obligatorio" }
        val expense = SharedExpenseEntity(
            title = cleanTitle,
            totalCents = totalCents,
            paidByPersonId = paidByPersonId,
            date = dateEpochDay,
        )
        val me = personDao.getMe()
        val myShare = if (me != null && paidByPersonId == me.id) shares[me.id] else null
        var expenseId = 0L
        var createdExpenseTx = false
        database.withTransaction {
            expenseId = sharedExpenseDao.insert(expense)
            sharedShareDao.insertAll(shares.map { (personId, cents) ->
                SharedShareEntity(sharedExpenseId = expenseId, personId = personId, shareCents = cents)
            })
            if (myShare != null && myShare > 0L) {
                transactionDao.insert(buildMyTransaction(cleanTitle, myShare, dateEpochDay, expenseId, createdAtMillis))
                createdExpenseTx = true
            }
        }
        if (createdExpenseTx) {
            runBudgetAlerts()
            widgetUpdater.update()
        }
        return expenseId
    }

    private fun buildMyTransaction(
        title: String,
        myShareCents: Long,
        dateEpochDay: Long,
        expenseId: Long,
        createdAtMillis: Long,
    ): TransactionEntity = TransactionFactory.create(
        NewTransaction(
            type = TransactionType.CASH,
            amountCents = myShareCents,
            merchant = title,
            date = LocalDate.ofEpochDay(dateEpochDay),
            sharedExpenseId = expenseId,
        ),
        createdAtMillis = createdAtMillis,
    )

    private suspend fun runBudgetAlerts() {
        try {
            evaluateBudgetAlerts.run()
        } catch (e: Exception) {
            Log.w("SharedExpense", "No se pudieron evaluar alertas de presupuesto", e)
        }
    }
}

class UpdateSharedExpense @Inject constructor(
    private val database: AppDatabase,
    private val sharedExpenseDao: SharedExpenseDao,
    private val sharedShareDao: SharedShareDao,
    private val personDao: PersonDao,
    private val transactionDao: TransactionDao,
    private val evaluateBudgetAlerts: EvaluateBudgetAlerts,
    private val widgetUpdater: WidgetUpdater,
) {
    suspend operator fun invoke(
        expenseId: Long,
        title: String,
        totalCents: Long,
        paidByPersonId: Long,
        dateEpochDay: Long,
        shares: Map<Long, Long>,
    ) {
        SharedSplit.validateCustomShares(totalCents, shares)
        require(expenseId > 0L) { "Gasto no válido" }
        require(paidByPersonId > 0L) { "Falta quién pagó" }
        val cleanTitle = title.trim()
        require(cleanTitle.isNotEmpty()) { "El concepto es obligatorio" }
        val existing = sharedExpenseDao.getById(expenseId) ?: error("El gasto compartido ya no existe")
        val me = personDao.getMe()
        val myShare = if (me != null && paidByPersonId == me.id) shares[me.id] else null
        var affectsTx = false
        database.withTransaction {
            val settledByPerson = sharedShareDao.getByExpense(expenseId).associate { it.personId to it.settled }
            sharedExpenseDao.update(
                existing.copy(title = cleanTitle, totalCents = totalCents, paidByPersonId = paidByPersonId, date = dateEpochDay),
            )
            sharedShareDao.deleteByExpense(expenseId)
            sharedShareDao.insertAll(shares.map { (personId, cents) ->
                SharedShareEntity(
                    sharedExpenseId = expenseId,
                    personId = personId,
                    shareCents = cents,
                    settled = settledByPerson[personId] ?: false,
                )
            })
            val linked = transactionDao.getBySharedExpenseId(expenseId)
            if (myShare != null && myShare > 0L) {
                val baseInput = NewTransaction(
                    type = linked?.type ?: TransactionType.CASH,
                    amountCents = myShareCents(myShare),
                    merchant = cleanTitle,
                    date = LocalDate.ofEpochDay(dateEpochDay),
                    categoryId = linked?.categoryId,
                    cardId = if ((linked?.type ?: TransactionType.CASH) == TransactionType.CARD) linked?.cardId else null,
                    sharedExpenseId = expenseId,
                )
                if (linked != null) {
                    transactionDao.update(TransactionFactory.applyTo(linked, baseInput))
                } else {
                    transactionDao.insert(TransactionFactory.create(baseInput, System.currentTimeMillis()))
                }
                affectsTx = true
            } else if (linked != null) {
                transactionDao.deleteBySharedExpenseId(expenseId)
                affectsTx = true
            }
        }
        if (affectsTx) {
            try { evaluateBudgetAlerts.run() } catch (e: Exception) { Log.w("SharedExpense", "Alertas", e) }
            widgetUpdater.update()
        }
    }

    private fun myShareCents(cents: Long): Long = cents
}

class DeleteSharedExpense @Inject constructor(
    private val database: AppDatabase,
    private val sharedExpenseDao: SharedExpenseDao,
    private val sharedShareDao: SharedShareDao,
    private val transactionDao: TransactionDao,
    private val evaluateBudgetAlerts: EvaluateBudgetAlerts,
    private val widgetUpdater: WidgetUpdater,
) {
    suspend operator fun invoke(expenseId: Long) {
        val existing = sharedExpenseDao.getById(expenseId) ?: return
        var hadTx = false
        database.withTransaction {
            hadTx = transactionDao.getBySharedExpenseId(expenseId) != null
            transactionDao.deleteBySharedExpenseId(expenseId)
            sharedShareDao.deleteByExpense(expenseId)
            sharedExpenseDao.delete(existing)
        }
        if (hadTx) {
            try { evaluateBudgetAlerts.run() } catch (e: Exception) { Log.w("SharedExpense", "Alertas", e) }
            widgetUpdater.update()
        }
    }
}

class SettleSharedExpense @Inject constructor(
    private val database: AppDatabase,
    private val sharedExpenseDao: SharedExpenseDao,
    private val sharedShareDao: SharedShareDao,
    private val personDao: PersonDao,
    private val transactionDao: TransactionDao,
    private val evaluateBudgetAlerts: EvaluateBudgetAlerts,
    private val widgetUpdater: WidgetUpdater,
) {
    /**
     * Liquida (o reactiva) una parte [personId] de [expenseId]. Si [personId] es null, aplica a todas.
     * Al liquidar (settled=true) genera movimiento si afecta a "yo" (§opción 2): cobro → INCOME(OTHER),
     * pago → CASH. Al reactivar no borra movimientos (historial financiero).
     */
    suspend operator fun invoke(expenseId: Long, personId: Long?, settled: Boolean = true) {
        val expense = sharedExpenseDao.getById(expenseId) ?: return
        val shares = sharedShareDao.getByExpense(expenseId)
        val targets = if (personId != null) shares.filter { it.personId == personId } else shares
        if (targets.isEmpty()) return
        val me = personDao.getMe()
        val peopleById = personDao.getAll().associateBy { it.id }
        val createdTxs = mutableListOf<TransactionEntity>()
        database.withTransaction {
            targets.forEach { share ->
                if (share.settled == settled) return@forEach
                sharedShareDao.setSettled(expenseId, share.personId, settled)
                if (settled && me != null) {
                    settlementTransaction(expense, share, me.id, peopleById[share.personId]?.name)?.let {
                        transactionDao.insert(it)
                        createdTxs += it
                    }
                }
            }
        }
        if (createdTxs.isNotEmpty()) {
            try { evaluateBudgetAlerts.run() } catch (e: Exception) { Log.w("SharedExpense", "Alertas", e) }
            widgetUpdater.update()
        }
    }

    private fun settlementTransaction(
        expense: SharedExpenseEntity,
        share: SharedShareEntity,
        meId: Long,
        otherName: String?,
    ): TransactionEntity? {
        val name = otherName ?: "—"
        return when {
            // Yo pagué el gasto y esta persona me devuelve su parte -> cobro
            expense.paidByPersonId == meId && share.personId != meId && share.shareCents > 0L ->
                TransactionFactory.create(
                    NewTransaction(
                        type = TransactionType.INCOME,
                        incomeKind = com.mipatrimonio.app.core.model.IncomeKind.OTHER,
                        amountCents = share.shareCents,
                        merchant = "Liquidación: ${expense.title} ($name)",
                        date = LocalDate.now(),
                        note = "Gasto compartido liquidado",
                    ),
                    createdAtMillis = System.currentTimeMillis(),
                )
            // Otro pagó y yo liquido mi parte -> pago
            expense.paidByPersonId != meId && share.personId == meId && share.shareCents > 0L ->
                TransactionFactory.create(
                    NewTransaction(
                        type = TransactionType.CASH,
                        amountCents = share.shareCents,
                        merchant = "Liquidación: ${expense.title}",
                        date = LocalDate.now(),
                        note = "Gasto compartido liquidado",
                    ),
                    createdAtMillis = System.currentTimeMillis(),
                )
            else -> null
        }
    }
}
