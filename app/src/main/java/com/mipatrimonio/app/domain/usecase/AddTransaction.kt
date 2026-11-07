package com.mipatrimonio.app.domain.usecase

import com.mipatrimonio.app.core.database.dao.TransactionDao
import com.mipatrimonio.app.core.database.entity.TransactionEntity
import com.mipatrimonio.app.core.model.IncomeKind
import com.mipatrimonio.app.core.model.TransactionType
import java.time.LocalDate
import javax.inject.Inject

/**
 * Datos de un movimiento introducido a mano (T1.7).
 * El importe siempre es positivo: el signo lo da [type] (MiPatrimonio.md §6).
 */
data class NewTransaction(
    val type: TransactionType,
    val amountCents: Long,
    val merchant: String,
    val date: LocalDate,
    val currency: String = "EUR",
    val incomeKind: IncomeKind? = null,
    val categoryId: Long? = null,
    val cardId: Long? = null,
    val note: String = "",
)

/**
 * Construcción y validación de un movimiento (función pura, testeable sin Room).
 * Reglas de MiPatrimonio.md §6:
 * - importe > 0;
 * - [TransactionEntity.incomeKind] solo si `type == INCOME`;
 * - [TransactionEntity.cardId] solo si `type == CARD`;
 * - `amountBaseCents == amountCents` cuando la moneda es EUR.
 */
object TransactionFactory {

    fun create(
        input: NewTransaction,
        createdAtMillis: Long,
    ): TransactionEntity {
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
        require(input.currency.isNotBlank()) { "La moneda no puede estar vacía" }
        // Multimoneda (T4.6): hasta que exista conversión solo se admite EUR.
        require(input.currency == "EUR") { "Solo se admite EUR hasta T4.6" }

        // Multimoneda (T4.6): hasta entonces amountBaseCents == amountCents.
        val amountBaseCents = input.amountCents

        return TransactionEntity(
            type = input.type,
            incomeKind = if (input.type == TransactionType.INCOME) input.incomeKind else null,
            amountCents = input.amountCents,
            currency = input.currency,
            amountBaseCents = amountBaseCents,
            date = input.date.toEpochDay(),
            merchant = input.merchant.trim(),
            note = input.note.trim(),
            categoryId = input.categoryId,
            cardId = if (input.type == TransactionType.CARD) input.cardId else null,
            createdAt = createdAtMillis,
        )
    }
}

/**
 * Caso de uso `AddTransaction` (T1.7): valida y guarda un movimiento manual.
 * La conversión a moneda base se ampliará en T4.6 (multimoneda).
 */
class AddTransaction @Inject constructor(
    private val transactionDao: TransactionDao,
) {

    suspend operator fun invoke(
        input: NewTransaction,
        createdAtMillis: Long = System.currentTimeMillis(),
    ): Long {
        val entity = TransactionFactory.create(input, createdAtMillis)
        return transactionDao.insert(entity)
    }
}
