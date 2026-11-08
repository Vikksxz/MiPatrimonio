package com.mipatrimonio.app.core.export

import com.mipatrimonio.app.core.database.entity.CategoryEntity
import com.mipatrimonio.app.core.database.entity.TransactionEntity
import com.mipatrimonio.app.core.model.TransactionType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExporterTest {

    private val categories = mapOf(
        1L to CategoryEntity(id = 1, name = "Compras", colorHex = "#fff", icon = "x")
    )

    @Test
    fun export_hasBomHeaderAndSemicolon() {
        val txs = listOf(
            TransactionEntity(
                id = 1, type = TransactionType.CASH, amountCents = 123456,
                amountBaseCents = 123456, date = LocalDate.of(2026, 11, 8).toEpochDay(),
                merchant = "Tienda", note = "nota", categoryId = 1, createdAt = 0L,
            )
        )
        val csv = CsvExporter.export(txs, categories) { "Metálico" }
        assertTrue(csv.startsWith('\uFEFF'))
        assertTrue(csv.contains("Fecha;Tipo;Importe;Moneda;Comercio;Nota;Categoria"))
        assertTrue(csv.contains("08/11/2026;Metálico;1.234,56;EUR;Tienda;nota;Compras"))
    }

    @Test
    fun formatAmountEs_formatsThousandsAndDecimals() {
        assertEquals("1.234,56", CsvExporter.formatAmountEs(123456))
        assertEquals("0,05", CsvExporter.formatAmountEs(5))
        assertEquals("-10,00", CsvExporter.formatAmountEs(-1000))
    }

    @Test
    fun export_escapesSemicolonAndQuotes() {
        val txs = listOf(
            TransactionEntity(
                id = 1, type = TransactionType.CARD, amountCents = 100,
                amountBaseCents = 100, date = LocalDate.of(2026, 1, 1).toEpochDay(),
                merchant = "A;B", note = "dijo \"hola\"", createdAt = 0L,
            )
        )
        val csv = CsvExporter.export(txs, emptyMap()) { "Tarjeta" }
        assertTrue(csv.contains("\"A;B\""))
        assertTrue(csv.contains("\"dijo \"\"hola\"\"\""))
    }
}
