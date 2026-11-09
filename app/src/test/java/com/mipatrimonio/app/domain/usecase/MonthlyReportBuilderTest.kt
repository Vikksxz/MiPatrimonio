package com.mipatrimonio.app.domain.usecase

import com.mipatrimonio.app.core.database.entity.CategoryEntity
import com.mipatrimonio.app.core.database.entity.PlanContributionEntity
import com.mipatrimonio.app.core.database.entity.SavingsPlanEntity
import com.mipatrimonio.app.core.database.entity.TransactionEntity
import com.mipatrimonio.app.core.model.TransactionType
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MonthlyReportBuilderTest {

    private val month = YearMonth.of(2026, 10)
    private val prev = YearMonth.of(2026, 9)

    private fun tx(
        type: TransactionType,
        amountCents: Long,
        date: LocalDate,
        merchant: String = "Shop",
        categoryId: Long? = null,
        id: Long = 0L,
    ) = TransactionEntity(
        id = id,
        type = type,
        amountCents = amountCents,
        amountBaseCents = amountCents,
        currency = "EUR",
        merchant = merchant,
        date = date.toEpochDay(),
        categoryId = categoryId,
        cardId = null,
        incomeKind = null,
        note = "",
        receiptPath = null,
        recurringId = null,
        createdAt = 0L,
    )

    @Test
    fun `totales por tipo y comparativa con mes anterior`() {
        val txs = listOf(
            tx(TransactionType.INCOME, 200000, month.atDay(1), "Nomina"),
            tx(TransactionType.CASH, 5000, month.atDay(2), "Pan"),
            tx(TransactionType.CARD, 3000, month.atDay(3), "Super"),
            tx(TransactionType.SUBSCRIPTION, 1000, month.atDay(4), "Sub"),
            tx(TransactionType.CARD, 2000, prev.atDay(5), "Old"),
        )
        val report = MonthlyReportBuilder.build(month, txs, emptyList(), emptyList(), emptyList())
        assertEquals(200000L, report.incomeCents)
        assertEquals(9000L, report.expenseCents)
        assertEquals(5000L, report.cashCents)
        assertEquals(3000L, report.cardCents)
        assertEquals(1000L, report.subscriptionCents)
        assertEquals(2000L, report.previousExpenseCents)
        assertEquals(7000L, report.expenseDeltaCents)
        assertEquals(191000L, report.availableCents)
    }

    @Test
    fun `top 5 comercios por gasto excluye ingresos`() {
        val txs = listOf(
            tx(TransactionType.CARD, 1000, month.atDay(1), "A"),
            tx(TransactionType.CARD, 5000, month.atDay(2), "B"),
            tx(TransactionType.CASH, 2000, month.atDay(3), "A"),
            tx(TransactionType.INCOME, 9999, month.atDay(4), "B"),
            tx(TransactionType.SUBSCRIPTION, 300, month.atDay(5), "C"),
        )
        val top = MonthlyReportBuilder.topMerchants(txs, 5)
        assertEquals("B", top[0].merchant)
        assertEquals(5000L, top[0].amountCents)
        assertEquals("A", top[1].merchant)
        assertEquals(3000L, top[1].amountCents)
        assertEquals("C", top[2].merchant)
        assertTrue(top.none { it.merchant == "Nomina" })
    }

    @Test
    fun `consejo por categoria que sube mas del 30 por ciento`() {
        val ocio = CategoryEntity(id = 1, name = "Ocio", colorHex = "#fff", icon = "i", isIncome = false, isFixed = false, isDefault = false)
        val txs = listOf(
            tx(TransactionType.CARD, 13000, month.atDay(1), "Cine", categoryId = 1),
            tx(TransactionType.CARD, 10000, prev.atDay(1), "Cine", categoryId = 1),
        )
        val report = MonthlyReportBuilder.build(month, txs, listOf(ocio), emptyList(), emptyList())
        val tip = report.tips.firstOrNull { it.kind == ReportTipKind.CATEGORY_UP }
        assertTrue(tip != null)
        assertEquals("Ocio", tip?.categoryName)
        assertEquals(30, tip?.percent)
    }

    @Test
    fun `aportaciones al ahorro del mes reducen disponible`() {
        val plan = SavingsPlanEntity(id = 1, name = "Viaje", targetCents = 100000, monthlyQuotaCents = 5000, priority = 0, isActive = true, initialEstimatedMonths = 0, createdAt = 0L)
        val contribs = listOf(
            PlanContributionEntity(id = 1, planId = 1, amountCents = 4000, date = month.atDay(10).toEpochDay()),
            PlanContributionEntity(id = 2, planId = 1, amountCents = 1000, date = prev.atDay(10).toEpochDay()),
        )
        val txs = listOf(tx(TransactionType.INCOME, 10000, month.atDay(1), "N"))
        val report = MonthlyReportBuilder.build(month, txs, emptyList(), listOf(plan), contribs)
        assertEquals(4000L, report.savingsContributedCents)
        assertEquals(6000L, report.availableCents)
        assertEquals(1, report.savingsPlans.size)
        assertEquals(5000L, report.savingsPlans[0].contributedCents)
    }

    @Test
    fun `mes anterior sin datos no divide por cero y delta es el gasto completo`() {
        val txs = listOf(tx(TransactionType.CARD, 50000, month.atDay(5), "Tienda"))
        val report = MonthlyReportBuilder.build(month, txs, emptyList(), emptyList(), emptyList())
        assertEquals(0L, report.previousExpenseCents)
        assertEquals(50000L, report.expenseDeltaCents)
        assertTrue(report.tips.none { it.kind == ReportTipKind.CATEGORY_UP })
    }

    @Test
    fun `consejo de categoria exige 30 por ciento y subida minima de 5 euros`() {
        val cats = listOf(
            CategoryEntity(1, "A", "#fff", "i", false, false, false),
            CategoryEntity(2, "B", "#fff", "i", false, false, false),
            CategoryEntity(3, "C", "#fff", "i", false, false, false),
        )
        val txs = listOf(
            tx(TransactionType.CARD, 1290, month.atDay(1), "x", 1),
            tx(TransactionType.CARD, 1000, prev.atDay(1), "x", 1), // 29%
            tx(TransactionType.CARD, 130, month.atDay(1), "y", 2),
            tx(TransactionType.CARD, 100, prev.atDay(1), "y", 2), // 30% pero +30c
            tx(TransactionType.CARD, 13000, month.atDay(1), "z", 3),
            tx(TransactionType.CARD, 10000, prev.atDay(1), "z", 3), // 30% y +3000c
        )
        val report = MonthlyReportBuilder.build(month, txs, cats, emptyList(), emptyList())
        val upTips = report.tips.filter { it.kind == ReportTipKind.CATEGORY_UP }
        assertEquals(1, upTips.size)
        assertEquals("C", upTips[0].categoryName)
    }

    @Test
    fun `sin reglas aplicables devuelve consejo general`() {
        val report = MonthlyReportBuilder.build(month, emptyList(), emptyList(), emptyList(), emptyList())
        assertTrue(report.tips.isNotEmpty())
        assertEquals(ReportTipKind.GENERAL, report.tips[0].kind)
    }
}
