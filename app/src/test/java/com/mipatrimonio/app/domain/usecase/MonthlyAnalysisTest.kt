package com.mipatrimonio.app.domain.usecase

import com.mipatrimonio.app.core.database.entity.CategoryEntity
import com.mipatrimonio.app.core.database.entity.TransactionEntity
import com.mipatrimonio.app.core.model.TransactionType
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MonthlyAnalysisTest {

    private fun tx(
        type: TransactionType,
        amount: Long,
        date: LocalDate,
        categoryId: Long? = null,
        recurringId: Long? = null,
        id: Long = 0,
    ) = TransactionEntity(
        id = id,
        type = type,
        amountCents = amount,
        amountBaseCents = amount,
        date = date.toEpochDay(),
        merchant = "m",
        categoryId = categoryId,
        recurringId = recurringId,
        createdAt = 0L,
    )

    @Test
    fun projection_hiddenBeforeDay3() {
        val month = YearMonth.of(2026, 11)
        val today = LocalDate.of(2026, 11, 2)
        val txs = listOf(tx(TransactionType.CASH, 1000, LocalDate.of(2026, 11, 1)))
        val p = MonthlyAnalysis.projection(month, txs, today)
        assertFalse(p.visible)
        assertEquals(1000L, p.spentToDateCents)
    }

    @Test
    fun projection_calculatesFromDay3() {
        val month = YearMonth.of(2026, 11) // 30 días
        val today = LocalDate.of(2026, 11, 10)
        val txs = listOf(
            tx(TransactionType.CASH, 3000, LocalDate.of(2026, 11, 2), id = 1),
            tx(TransactionType.CARD, 2000, LocalDate.of(2026, 11, 9), id = 2),
            tx(TransactionType.INCOME, 9999, LocalDate.of(2026, 11, 5), id = 3), // no gasto
            tx(TransactionType.CASH, 1000, LocalDate.of(2026, 11, 15), id = 4), // futuro, excluido de hastaHoy
        )
        val p = MonthlyAnalysis.projection(month, txs, today)
        assertTrue(p.visible)
        assertEquals(5000L, p.spentToDateCents) // 3000+2000, excluye futuro e ingreso
        // 5000 / 10 * 30 = 15000
        assertEquals(15000L, p.projectedExpenseCents)
    }

    @Test
    fun projection_pastMonthShowsTotal() {
        val month = YearMonth.of(2026, 10)
        val today = LocalDate.of(2026, 11, 8)
        val txs = listOf(tx(TransactionType.CASH, 12345, LocalDate.of(2026, 10, 20)))
        val p = MonthlyAnalysis.projection(month, txs, today)
        assertTrue(p.visible)
        assertEquals(12345L, p.spentToDateCents)
        assertEquals(12345L, p.projectedExpenseCents)
    }

    @Test
    fun fixedVsVariable_unionsFixedCategoryAndRecurring_noDoubleCount() {
        val month = YearMonth.of(2026, 11)
        val categories = mapOf(
            1L to CategoryEntity(id = 1, name = "Hogar", colorHex = "#fff", icon = "home", isFixed = true),
            2L to CategoryEntity(id = 2, name = "Ocio", colorHex = "#fff", icon = "fun"),
        )
        val txs = listOf(
            tx(TransactionType.CARD, 5000, LocalDate.of(2026, 11, 1), categoryId = 1, id = 1), // fijo por categoría
            tx(TransactionType.CASH, 2000, LocalDate.of(2026, 11, 2), categoryId = 2, recurringId = 9, id = 2), // fijo por recurrente
            tx(TransactionType.SUBSCRIPTION, 1000, LocalDate.of(2026, 11, 3), categoryId = 1, recurringId = 8, id = 3), // fijo por ambos -> cuenta una vez
            tx(TransactionType.CASH, 3000, LocalDate.of(2026, 11, 4), categoryId = 2, id = 4), // variable
            tx(TransactionType.INCOME, 99999, LocalDate.of(2026, 11, 5), id = 5),
        )
        val r = MonthlyAnalysis.fixedVsVariable(month, txs, categories, savingsContributedCents = 500)
        assertEquals(99999L, r.incomeCents)
        assertEquals(11000L, r.totalExpenseCents)
        assertEquals(8000L, r.fixedCents) // 5000 + 2000 + 1000
        assertEquals(3000L, r.variableCents)
        // libre = ingresos - fijos - aportadoAhorro - variables = 99999-8000-500-3000 = 88499
        assertEquals(88499L, r.freeAtMonthEndCents)
    }

    @Test
    fun expensesByCategory_groupsAndSorts() {
        val month = YearMonth.of(2026, 11)
        val categories = mapOf(
            1L to CategoryEntity(id = 1, name = "Ocio", colorHex = "#aa", icon = "x"),
            2L to CategoryEntity(id = 2, name = "Comida", colorHex = "#bb", icon = "x"),
        )
        val txs = listOf(
            tx(TransactionType.CASH, 100, LocalDate.of(2026, 11, 1), categoryId = 1, id = 1),
            tx(TransactionType.CARD, 500, LocalDate.of(2026, 11, 2), categoryId = 2, id = 2),
            tx(TransactionType.CASH, 200, LocalDate.of(2026, 11, 3), categoryId = 1, id = 3),
            tx(TransactionType.CASH, 50, LocalDate.of(2026, 11, 4), categoryId = null, id = 4),
            tx(TransactionType.INCOME, 999, LocalDate.of(2026, 11, 5), categoryId = 1, id = 5),
        )
        val r = MonthlyAnalysis.expensesByCategory(month, txs, categories, otherLabel = "Otros")
        assertEquals(3, r.size)
        assertEquals("Comida", r[0].categoryName)
        assertEquals(500L, r[0].amountCents)
        assertEquals("Ocio", r[1].categoryName)
        assertEquals(300L, r[1].amountCents)
        assertEquals("Otros", r[2].categoryName)
        assertEquals(50L, r[2].amountCents)
    }
}
