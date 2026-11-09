package com.mipatrimonio.app.domain.usecase

import com.mipatrimonio.app.core.database.entity.TransactionEntity
import com.mipatrimonio.app.core.model.IncomeKind
import com.mipatrimonio.app.core.model.TransactionType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Tests de la construcción y validación de movimientos manuales (T1.7).
 * No hay céntimos negativos: el signo lo da el tipo.
 */
class AddTransactionTest {

    private val date = LocalDate.of(2026, 11, 6)
    private val createdAt = 1_791_331_200_000L

    private fun cash(
        amountCents: Long = 1234,
        cardId: Long? = null,
        incomeKind: IncomeKind? = null,
        merchant: String = "  Mercadona  ",
        note: String = "  compra semanal  ",
    ) = NewTransaction(
        type = TransactionType.CASH,
        amountCents = amountCents,
        merchant = merchant,
        date = date,
        categoryId = 3L,
        cardId = cardId,
        incomeKind = incomeKind,
        note = note,
    )

    @Test
    fun cashTransactionKeepsAmountInCentsAndEpochDay() {
        val entity = TransactionFactory.create(cash(), createdAt)

        assertEquals(TransactionType.CASH, entity.type)
        assertEquals(1234L, entity.amountCents)
        assertEquals(1234L, entity.amountBaseCents)
        assertEquals("EUR", entity.currency)
        assertEquals(date.toEpochDay(), entity.date)
        assertEquals(createdAt, entity.createdAt)
        assertEquals(3L, entity.categoryId)
        assertNull(entity.cardId)
        assertNull(entity.incomeKind)
        assertEquals(0L, entity.id)
    }

    @Test
    fun merchantAndNoteAreTrimmed() {
        val entity = TransactionFactory.create(cash(), createdAt)

        assertEquals("Mercadona", entity.merchant)
        assertEquals("compra semanal", entity.note)
    }

    @Test
    fun incomeRequiresIncomeKind() {
        val input = NewTransaction(
            type = TransactionType.INCOME,
            amountCents = 250_000,
            merchant = "Nómina",
            date = date,
            categoryId = 11L,
        )

        assertThrows(IllegalArgumentException::class.java) {
            TransactionFactory.create(input, createdAt)
        }
    }

    @Test
    fun incomeKeepsIncomeKind() {
        val input = NewTransaction(
            type = TransactionType.INCOME,
            amountCents = 250_000,
            merchant = "Nómina",
            date = date,
            incomeKind = IncomeKind.PAYROLL,
            categoryId = 11L,
        )

        val entity = TransactionFactory.create(input, createdAt)

        assertEquals(TransactionType.INCOME, entity.type)
        assertEquals(IncomeKind.PAYROLL, entity.incomeKind)
        assertEquals(250_000L, entity.amountBaseCents)
        assertNull(entity.cardId)
    }

    @Test
    fun incomeKindIsRejectedForExpenses() {
        assertThrows(IllegalArgumentException::class.java) {
            TransactionFactory.create(cash(incomeKind = IncomeKind.OTHER), createdAt)
        }
    }

    @Test
    fun cardIdOnlyKeptForCardType() {
        val input = cash().copy(type = TransactionType.CARD, cardId = 7L)

        val entity = TransactionFactory.create(input, createdAt)

        assertEquals(7L, entity.cardId)
        assertEquals(TransactionType.CARD, entity.type)
    }

    @Test
    fun cardIdIsRejectedForNonCardType() {
        assertThrows(IllegalArgumentException::class.java) {
            TransactionFactory.create(cash(cardId = 7L), createdAt)
        }
    }

    @Test
    fun cardTypeAllowsNoCard() {
        val input = cash().copy(type = TransactionType.CARD, cardId = null)

        val entity = TransactionFactory.create(input, createdAt)

        assertNull(entity.cardId)
    }

    @Test
    fun amountMustBePositive() {
        assertThrows(IllegalArgumentException::class.java) {
            TransactionFactory.create(cash(amountCents = 0), createdAt)
        }
        assertThrows(IllegalArgumentException::class.java) {
            TransactionFactory.create(cash(amountCents = -100), createdAt)
        }
    }

    @Test
    fun subscriptionTypeKeepsNoCardAndNoIncomeKind() {
        val input = cash().copy(type = TransactionType.SUBSCRIPTION)

        val entity = TransactionFactory.create(input, createdAt)

        assertEquals(TransactionType.SUBSCRIPTION, entity.type)
        assertNull(entity.cardId)
        assertNull(entity.incomeKind)
    }

    @Test
    fun nonEurCurrencyRequiresRate() {
        val input = cash().copy(currency = "USD", rateToBase = null)

        assertThrows(IllegalArgumentException::class.java) {
            TransactionFactory.create(input, createdAt)
        }
        assertThrows(IllegalArgumentException::class.java) {
            TransactionFactory.create(cash().copy(currency = "USD", rateToBase = 0.0), createdAt)
        }
    }

    @Test
    fun nonEurCurrencyComputesBaseCentsWithRate() {
        val input = cash(amountCents = 1000).copy(currency = "USD", rateToBase = 0.92)
        val entity = TransactionFactory.create(input, createdAt)
        assertEquals("USD", entity.currency)
        assertEquals(1000L, entity.amountCents)
        assertEquals(920L, entity.amountBaseCents)
    }

    @Test
    fun nonEurCurrencyRoundsBaseCents() {
        val input = cash(amountCents = 1001).copy(currency = "USD", rateToBase = 0.925)
        val entity = TransactionFactory.create(input, createdAt)
        // 1001 * 0.925 = 925.925 -> 926
        assertEquals(926L, entity.amountBaseCents)
    }

    @Test
    fun currencyIsNormalizedToUppercase() {
        val input = cash(amountCents = 500).copy(currency = " usd ", rateToBase = 1.0)
        val entity = TransactionFactory.create(input, createdAt)
        assertEquals("USD", entity.currency)
        assertEquals(500L, entity.amountBaseCents)
    }

    @Test
    fun blankMerchantIsAllowedAndStoredEmpty() {
        val entity = TransactionFactory.create(cash(merchant = "   "), createdAt)

        assertEquals("", entity.merchant)
    }

    @Test
    fun applyToKeepsIdentityAndLinksButAppliesNewValues() {
        val existing = TransactionEntity(
            id = 42L,
            type = TransactionType.CARD,
            amountCents = 500,
            amountBaseCents = 500,
            date = date.toEpochDay(),
            merchant = "Antiguo",
            categoryId = 1L,
            cardId = 7L,
            receiptPath = "/ticket.jpg",
            recurringId = 3L,
            sharedExpenseId = 9L,
            createdAt = createdAt,
        )
        val input = NewTransaction(
            type = TransactionType.CASH,
            amountCents = 1999,
            merchant = "Nuevo",
            date = date.plusDays(1),
            categoryId = 2L,
            note = "cambio",
        )

        val updated = TransactionFactory.applyTo(existing, input)

        assertEquals(42L, updated.id)
        assertEquals(createdAt, updated.createdAt)
        assertEquals("/ticket.jpg", updated.receiptPath)
        assertEquals(3L, updated.recurringId)
        assertEquals(9L, updated.sharedExpenseId)
        assertEquals(TransactionType.CASH, updated.type)
        assertEquals(1999L, updated.amountCents)
        assertEquals(1999L, updated.amountBaseCents)
        assertEquals("Nuevo", updated.merchant)
        assertEquals(date.plusDays(1).toEpochDay(), updated.date)
        assertEquals(2L, updated.categoryId)
        assertEquals("cambio", updated.note)
        assertNull(updated.cardId)
        assertNull(updated.incomeKind)
    }

    @Test
    fun applyToValidatesTheNewData() {
        val existing = TransactionFactory.create(cash(), createdAt)

        assertThrows(IllegalArgumentException::class.java) {
            TransactionFactory.applyTo(existing, cash(amountCents = 0))
        }
    }
}
