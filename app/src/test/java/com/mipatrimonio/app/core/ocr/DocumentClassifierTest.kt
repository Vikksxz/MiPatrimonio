package com.mipatrimonio.app.core.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DocumentClassifierTest {

    @Test
    fun payrollIsSuggestedWhenKeywordsDominate() {
        val lines = listOf(
            "RECIBO DE SALARIOS",
            "NOMINA MES DE NOVIEMBRE 2026",
            "DEVENGOS 2.500,00",
            "DEDUCCIONES 400,00",
            "SEGURIDAD SOCIAL 150,00",
            "IRPF 250,00",
            "LIQUIDO A PERCIBIR 2.100,00",
        )
        val r = DocumentClassifier.classify(lines)
        assertEquals(SuggestedDocumentKind.PAYROLL, r.suggested)
        assertEquals(210000L, r.suggestedNetCents)
    }

    @Test
    fun ticketIsSuggestedWhenKeywordsDominate() {
        val lines = listOf(
            "MERCADONA",
            "FACTURA SIMPLIFICADA",
            "IVA 10% 1,20",
            "TOTAL 13,20",
            "TARJETA",
            "GRACIAS POR SU VISITA",
        )
        val r = DocumentClassifier.classify(lines)
        assertEquals(SuggestedDocumentKind.TICKET, r.suggested)
        assertNull(r.suggestedNetCents)
    }

    @Test
    fun ambiguousReturnsUnknown() {
        val lines = listOf("TOTAL 10,00", "DOCUMENTO")
        val r = DocumentClassifier.classify(lines)
        assertEquals(SuggestedDocumentKind.UNKNOWN, r.suggested)
    }

    @Test
    fun accentInsensitivePayrollDetection() {
        val lines = listOf("NÓMINA", "LÍQUIDO A PERCIBIR 1.500,00", "DEDUCCIONES 200,00", "IRPF 100,00")
        val r = DocumentClassifier.classify(lines)
        assertEquals(SuggestedDocumentKind.PAYROLL, r.suggested)
        assertEquals(150000L, r.suggestedNetCents)
    }
}
