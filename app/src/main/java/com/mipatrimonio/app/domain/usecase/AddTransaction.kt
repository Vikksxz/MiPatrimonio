package com.mipatrimonio.app.domain.usecase

import com.mipatrimonio.app.core.database.dao.TagDao
import com.mipatrimonio.app.core.database.dao.TransactionDao
import com.mipatrimonio.app.core.database.entity.TransactionEntity
import com.mipatrimonio.app.core.model.IncomeKind
import com.mipatrimonio.app.core.model.TransactionType
import java.time.LocalDate
import javax.inject.Inject

/**
 * Datos de un movimiento introducido a mano (T1.7) o prellenado desde OCR (T2.5).
 * El importe siempre es positivo: el signo lo da [type] (MiPatrimonio.md §6).
 * [receiptPath] es la ruta relativa del ticket cifrado en almacenamiento privado.
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
    val receiptPath: String? = null,
    val tagIds: List<Long> = emptyList(),
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
        validate(input)

        return TransactionEntity(
            type = input.type,
            incomeKind = if (input.type == TransactionType.INCOME) input.incomeKind else null,
            amountCents = input.amountCents,
            currency = input.currency,
            amountBaseCents = input.amountCents,
            date = input.date.toEpochDay(),
            merchant = input.merchant.trim(),
            note = input.note.trim(),
            categoryId = input.categoryId,
            cardId = if (input.type == TransactionType.CARD) input.cardId else null,
            receiptPath = input.receiptPath?.trim()?.takeIf { it.isNotBlank() },
            createdAt = createdAtMillis,
        )
    }

    /**
     * Aplica los cambios de [input] sobre [existing] tocando **solo** los campos
     * editables del formulario. Conserva id, fecha de creación, ticket, vínculos
     * (recurrente/compartido) y cualquier campo futuro (p. ej. etiquetas o tipo
     * de cambio) en lugar de reconstruir la entidad desde cero.
     */
    fun applyTo(existing: TransactionEntity, input: NewTransaction): TransactionEntity {
        validate(input)
        return existing.copy(
            type = input.type,
            incomeKind = if (input.type == TransactionType.INCOME) input.incomeKind else null,
            amountCents = input.amountCents,
            currency = input.currency,
            amountBaseCents = input.amountCents,
            date = input.date.toEpochDay(),
            merchant = input.merchant.trim(),
            note = input.note.trim(),
            categoryId = input.categoryId,
            cardId = if (input.type == TransactionType.CARD) input.cardId else null,
        )
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
        require(input.currency.isNotBlank()) { "La moneda no puede estar vacía" }
        // Multimoneda (T4.6): hasta que exista conversión solo se admite EUR.
        require(input.currency == "EUR") { "Solo se admite EUR hasta T4.6" }
    }
}

/**
 * Caso de uso `AddTransaction` (T1.7): valida y guarda un movimiento manual.
 * La conversión a moneda base se ampliará en T4.6 (multimoneda).
 */
class AddTransaction @Inject constructor(
    private val transactionDao: TransactionDao,
    private val tagDao: TagDao,
) {

    suspend operator fun invoke(
        input: NewTransaction,
        createdAtMillis: Long = System.currentTimeMillis(),
    ): Long {
        val entity = TransactionFactory.create(input, createdAtMillis)
        val id = transactionDao.insert(entity)
        if (input.tagIds.isNotEmpty()) tagDao.replaceTagsForTransaction(id, input.tagIds)
        return id
    }
}

/**
 * Caso de uso `UpdateTransaction` (T1.8): edita un movimiento conservando id,
 * fecha de creación, ticket y vínculos (recurrente/compartido).
 */
class UpdateTransaction @Inject constructor(
    private val transactionDao: TransactionDao,
    private val tagDao: TagDao,
) {

    suspend operator fun invoke(existing: TransactionEntity, input: NewTransaction): Long {
        // Se relee la fila para no pisar datos cambiados por otra pantalla.
        val current = transactionDao.getById(existing.id)
            ?: error("El movimiento ya no existe")
        val updated = TransactionFactory.applyTo(current, input)
        val rows = transactionDao.update(updated)
        check(rows == 1) { "El movimiento ya no existe" }
        tagDao.replaceTagsForTransaction(existing.id, input.tagIds)
        return existing.id
    }
}
