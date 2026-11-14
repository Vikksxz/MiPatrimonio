package com.mipatrimonio.app.domain.usecase

import android.util.Log
import com.mipatrimonio.app.core.database.dao.SavingsDao
import com.mipatrimonio.app.core.database.dao.TagDao
import com.mipatrimonio.app.core.database.dao.TransactionDao
import com.mipatrimonio.app.core.database.entity.TransactionEntity
import com.mipatrimonio.app.core.model.IncomeKind
import com.mipatrimonio.app.core.model.TransactionType
import com.mipatrimonio.app.feature.widget.WidgetUpdater
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlin.math.roundToLong

/**
 * Datos de un movimiento introducido a mano (T1.7) o prellenado desde OCR (T2.5).
 * El importe siempre es positivo: el signo lo da [type] (MiPatrimonio.md §6).
 * [receiptPath] es la ruta relativa del ticket cifrado en almacenamiento privado.
 * Multimoneda (T4.6, §7.8): [rateToBase] convierte a EUR base cuando [currency] != EUR.
 * [sharedExpenseId] enlaza el movimiento con un gasto compartido (T4.7).
 */
data class NewTransaction(
    val type: TransactionType,
    val amountCents: Long,
    val merchant: String,
    val date: LocalDate,
    val currency: String = "EUR",
    val rateToBase: Double? = null,
    val incomeKind: IncomeKind? = null,
    val categoryId: Long? = null,
    val cardId: Long? = null,
    val note: String = "",
    val receiptPath: String? = null,
    val sharedExpenseId: Long? = null,
    val tagIds: List<Long> = emptyList(),
)

/**
 * Construcción y validación de un movimiento (función pura, testeable sin Room).
 * Reglas de MiPatrimonio.md §6, §7.8 y §8.3:
 * - importe > 0;
 * - [TransactionEntity.incomeKind] solo si `type == INCOME`;
 * - [TransactionEntity.cardId] solo si `type == CARD`;
 * - EUR: `amountBaseCents == amountCents`; otra moneda: `amountBaseCents = round(amountCents × rateToBase)`.
 */
object TransactionFactory {

    fun create(
        input: NewTransaction,
        createdAtMillis: Long,
    ): TransactionEntity {
        validate(input)
        val currency = normalizedCurrency(input)
        return TransactionEntity(
            type = input.type,
            incomeKind = if (input.type == TransactionType.INCOME) input.incomeKind else null,
            amountCents = input.amountCents,
            currency = currency,
            amountBaseCents = baseCents(input),
            date = input.date.toEpochDay(),
            merchant = input.merchant.trim(),
            note = input.note.trim(),
            categoryId = input.categoryId,
            cardId = if (input.type == TransactionType.CARD) input.cardId else null,
            receiptPath = input.receiptPath?.trim()?.takeIf { it.isNotBlank() },
            sharedExpenseId = input.sharedExpenseId,
            createdAt = createdAtMillis,
        )
    }

    /**
     * Aplica los cambios de [input] sobre [existing] tocando **solo** los campos
     * editables del formulario. Conserva id, fecha de creación, ticket, vínculos
     * (recurrente/compartido) y cualquier campo futuro en lugar de reconstruir la
     * entidad desde cero.
     */
    fun applyTo(existing: TransactionEntity, input: NewTransaction): TransactionEntity {
        validate(input)
        val currency = normalizedCurrency(input)
        return existing.copy(
            type = input.type,
            incomeKind = if (input.type == TransactionType.INCOME) input.incomeKind else null,
            amountCents = input.amountCents,
            currency = currency,
            amountBaseCents = baseCents(input),
            date = input.date.toEpochDay(),
            merchant = input.merchant.trim(),
            note = input.note.trim(),
            categoryId = input.categoryId,
            cardId = if (input.type == TransactionType.CARD) input.cardId else null,
        )
    }

    private fun normalizedCurrency(input: NewTransaction): String = input.currency.trim().uppercase()

    private fun baseCents(input: NewTransaction): Long {
        if (normalizedCurrency(input) == "EUR") return input.amountCents
        val rate = input.rateToBase
        require(rate != null && rate > 0.0) { "El tipo de cambio es obligatorio para monedas distintas de EUR" }
        return (input.amountCents.toDouble() * rate).roundToLong()
    }

    private fun validate(input: NewTransaction) {
        require(input.amountCents > 0) { "El importe debe ser mayor que 0" }
        require(input.type == TransactionType.INCOME || input.incomeKind == null) {
            "incomeKind solo se usa con INCOME"
        }
        if (input.type == TransactionType.INCOME) {
            require(input.incomeKind != null) { "Un ingreso necesita incomeKind" }
        }
        require(input.type == TransactionType.CARD || input.cardId == null) {
            "cardId solo se usa con CARD"
        }
        val currency = normalizedCurrency(input)
        require(currency.isNotBlank()) { "La moneda no puede estar vacía" }
        if (currency != "EUR") {
            val rate = input.rateToBase
            require(rate != null && rate > 0.0) { "El tipo de cambio es obligatorio para monedas distintas de EUR" }
        }
    }
}

class AvailableWouldBecomeNegativeException : IllegalStateException("available_would_be_negative")
class IncomeEditInconsistentException : IllegalStateException("income_edit_inconsistent")

/**
 * Caso de uso `AddTransaction` (T1.7, T6.1): valida y guarda un movimiento.
 * Bloquea gastos/suscripciones que dejen el disponible < 0 (§7.1).
 */
class AddTransaction @Inject constructor(
    private val transactionDao: TransactionDao,
    private val tagDao: TagDao,
    private val savingsDao: SavingsDao,
    private val evaluateBudgetAlerts: EvaluateBudgetAlerts,
    private val widgetUpdater: WidgetUpdater,
) {

    suspend operator fun invoke(
        input: NewTransaction,
        createdAtMillis: Long = System.currentTimeMillis(),
    ): Long {
        val entity = TransactionFactory.create(input, createdAtMillis)
        if (entity.type != TransactionType.INCOME) {
            val month = YearMonth.from(entity.dateAsLocalDate())
            val available = availableForMonth(month)
            if (available - entity.amountBaseCents < 0L) throw AvailableWouldBecomeNegativeException()
        }
        val id = transactionDao.insert(entity)
        if (input.tagIds.isNotEmpty()) tagDao.replaceTagsForTransaction(id, input.tagIds)
        if (entity.type != TransactionType.INCOME) {
            try {
                evaluateBudgetAlerts.run()
            } catch (e: Exception) {
                Log.w("AddTransaction", "No se pudieron evaluar alertas de presupuesto", e)
            }
        }
        widgetUpdater.update()
        return id
    }

    private suspend fun availableForMonth(month: YearMonth): Long {
        val from = month.atDay(1).toEpochDay()
        val to = month.plusMonths(1).atDay(1).toEpochDay()
        val txs = transactionDao.getByMonth(from, to)
        val savings = savingsDao.getMonthlyContributed(from, to) ?: 0L
        return SavingsContributionRules.availableCents(month, txs, savings)
    }
}

/**
 * Caso de uso `UpdateTransaction` (T1.8, T6.1): edita un movimiento respetando reglas de ahorro/disponible.
 */
class UpdateTransaction @Inject constructor(
    private val transactionDao: TransactionDao,
    private val tagDao: TagDao,
    private val savingsDao: SavingsDao,
    private val evaluateBudgetAlerts: EvaluateBudgetAlerts,
    private val widgetUpdater: WidgetUpdater,
) {

    suspend operator fun invoke(existing: TransactionEntity, input: NewTransaction): Long {
        val current = transactionDao.getById(existing.id)
            ?: error("El movimiento ya no existe")
        val updated = TransactionFactory.applyTo(current, input)

        validateSavingsConstraints(current, updated)

        val rows = transactionDao.update(updated)
        check(rows == 1) { "El movimiento ya no existe" }
        tagDao.replaceTagsForTransaction(existing.id, input.tagIds)
        if (updated.type != TransactionType.INCOME) {
            try {
                evaluateBudgetAlerts.run()
            } catch (e: Exception) {
                Log.w("UpdateTransaction", "No se pudieron evaluar alertas de presupuesto", e)
            }
        }
        widgetUpdater.update()
        return existing.id
    }

    private suspend fun validateSavingsConstraints(old: TransactionEntity, new: TransactionEntity) {
        // Edición de ingreso vinculado: importe base >= total aportado con ese origen y disponible consistente.
        if (old.type == TransactionType.INCOME || new.type == TransactionType.INCOME) {
            val linkedCount = savingsDao.countLinkedContributions(old.id)
            val linkedSum = savingsDao.sumLinkedContributionAmountCents(old.id)
            if (linkedCount > 0) {
                if (new.type != TransactionType.INCOME || new.amountBaseCents < linkedSum) {
                    throw IncomeEditInconsistentException()
                }
                if (!isAvailableConsistentAfterUpdate(old, new, strict = true)) throw IncomeEditInconsistentException()
                return
            }
        }

        // Gastos/suscripciones/movimientos que reduzcan disponible: bloquear si empeora/cruza a < 0.
        if (!isAvailableConsistentAfterUpdate(old, new, strict = false)) {
            throw AvailableWouldBecomeNegativeException()
        }
    }

    /**
     * strict=true (ingreso vinculado): disponible tras edición no puede ser < 0.
     * strict=false: permite mejoras/correcciones si ya estaba < 0, pero bloquea si cruza a < 0 o empeora estando < 0.
     */
    private suspend fun isAvailableConsistentAfterUpdate(
        old: TransactionEntity,
        new: TransactionEntity,
        strict: Boolean,
    ): Boolean {
        val months = linkedSetOf(
            YearMonth.from(old.dateAsLocalDate()),
            YearMonth.from(new.dateAsLocalDate()),
        )
        for (month in months) {
            val before = availableForMonth(month)
            val after = availableForMonthAfterUpdate(month, old, new)
            if (strict) {
                if (after < 0L) return false
            } else {
                if (before >= 0L && after < 0L) return false
                if (before < 0L && after < before) return false
            }
        }
        return true
    }

    private suspend fun availableForMonthAfterUpdate(month: YearMonth, old: TransactionEntity, new: TransactionEntity): Long {
        val from = month.atDay(1).toEpochDay()
        val to = month.plusMonths(1).atDay(1).toEpochDay()
        val txs = transactionDao.getByMonth(from, to).toMutableList()
        // Reemplazar old por new en el conjunto simulado
        val idx = txs.indexOfFirst { it.id == old.id }
        if (idx >= 0) txs[idx] = new else txs.add(new)
        val savings = savingsDao.getMonthlyContributed(from, to) ?: 0L
        // Aportaciones no cambian aquí; disponible usa txs simuladas
        return SavingsContributionRules.availableCents(month, txs, savings)
    }

    private suspend fun availableForMonth(month: YearMonth): Long {
        val from = month.atDay(1).toEpochDay()
        val to = month.plusMonths(1).atDay(1).toEpochDay()
        val txs = transactionDao.getByMonth(from, to)
        val savings = savingsDao.getMonthlyContributed(from, to) ?: 0L
        return SavingsContributionRules.availableCents(month, txs, savings)
    }
}

private fun TransactionEntity.dateAsLocalDate(): LocalDate = LocalDate.ofEpochDay(date)
