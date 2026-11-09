package com.mipatrimonio.app.domain.usecase

import com.mipatrimonio.app.core.database.entity.SharedExpenseEntity
import com.mipatrimonio.app.core.database.entity.SharedShareEntity

/** Transferencia sugerida: [fromPersonId] debe a [toPersonId] [amountCents]. */
data class DebtTransfer(
    val fromPersonId: Long,
    val toPersonId: Long,
    val amountCents: Long,
)

/**
 * Saldos y liquidaciones de gastos compartidos (T4.7, MiPatrimonio.md §7.9).
 * Todo en céntimos [Long]. Funciones puras, sin I/O.
 *
 * Formulación: por cada parte NO liquidada, el pagador del gasto recibe crédito (+parte)
 * y la persona de la parte recibe débito (−parte). La parte propia del pagador se anula.
 * Así, saldo neto(persona) = pagado − su parte (sobre lo pendiente). Σ saldos = 0.
 */
object SharedBalances {

    /** Saldos netos por persona (positivo = le deben, negativo = debe). Solo shares no liquidados. */
    fun netBalances(
        expensesById: Map<Long, SharedExpenseEntity>,
        shares: List<SharedShareEntity>,
    ): Map<Long, Long> {
        val net = mutableMapOf<Long, Long>()
        for (share in shares) {
            if (share.settled) continue
            val expense = expensesById[share.sharedExpenseId] ?: continue
            val payer = expense.paidByPersonId
            val person = share.personId
            val amount = share.shareCents
            if (amount <= 0L) continue
            net[payer] = (net[payer] ?: 0L) + amount
            net[person] = (net[person] ?: 0L) - amount
        }
        // Eliminar saldos neutros para limpiar el resultado
        return net.filterValues { it != 0L }
    }

    /**
     * "Quién debe a quién" (§7.9): ordena deudores y acreedores y compensa mayor con mayor.
     * [balances] es el mapa de saldos netos (ignora entradas 0). Devuelve transferencias con importe > 0.
     */
    fun whoOwesWhom(balances: Map<Long, Long>): List<DebtTransfer> {
        val debtors = balances.filter { it.value < 0L }
            .map { it.key to -it.value } // (id, amount owed > 0)
            .sortedByDescending { it.second }
            .toMutableList()
        val creditors = balances.filter { it.value > 0L }
            .map { it.key to it.value }
            .sortedByDescending { it.second }
            .toMutableList()
        val transfers = mutableListOf<DebtTransfer>()
        var i = 0
        var j = 0
        while (i < debtors.size && j < creditors.size) {
            val (debtorId, debt) = debtors[i]
            val (creditorId, credit) = creditors[j]
            val amount = minOf(debt, credit)
            if (amount > 0L) {
                transfers += DebtTransfer(fromPersonId = debtorId, toPersonId = creditorId, amountCents = amount)
            }
            val remainingDebt = debt - amount
            val remainingCredit = credit - amount
            if (remainingDebt == 0L) i++ else debtors[i] = debtorId to remainingDebt
            if (remainingCredit == 0L) j++ else creditors[j] = creditorId to remainingCredit
        }
        return transfers
    }
}
