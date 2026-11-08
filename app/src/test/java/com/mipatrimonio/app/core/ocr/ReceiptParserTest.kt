package com.mipatrimonio.app.core.ocr

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * T2.4 (§9.5): al menos 8 textos de ejemplo realistas.
 * Importes verificados en céntimos (Long).
 */
class ReceiptParserTest {

    private val today = LocalDate.of(2026, 11, 8)

    @Test
    fun supermercadoMercadona() {
        val lines = listOf(
            "MERCADONA S.A.",
            "C/ MAYOR 12 MADRID",
            "CIF A-46103834",
            "07/11/2026 18:32",
            "LECHE ENTERA 1L   0,89",
            "PAN INTEGRAL      1,20",
            "MANZANAS 1KG      2,35",
            "IVA 4%            0,18",
            "TOTAL           4,44 EUR",
            "TARJETA",
            "GRACIAS POR SU VISITA",
        )
        val r = ReceiptParser.parse(lines, today)
        assertEquals(444L, r.amountCents)
        assertEquals(FieldConfidence.HIGH, r.amountConfidence)
        assertEquals(LocalDate.of(2026, 11, 7), r.date)
        assertEquals("Mercadona", r.merchant)
        assertEquals(FieldConfidence.HIGH, r.merchantConfidence)
    }

    @Test
    fun gasolinera() {
        val lines = listOf(
            "REPSOL",
            "E.S. AUTOPISTA A-3",
            "TEL 912345678",
            "08/11/2026 09:15",
            "GASOLEO A    42,50 L",
            "PRECIO/L      1,529",
            "TOTAL A PAGAR  65,00 EUR",
        )
        val r = ReceiptParser.parse(lines, today)
        assertEquals(6500L, r.amountCents)
        assertEquals("Repsol", r.merchant)
        assertEquals(LocalDate.of(2026, 11, 8), r.date)
    }

    @Test
    fun restaurante() {
        val lines = listOf(
            "LA TABERNA DEL PUERTO",
            "AVDA. DEL MAR 5",
            "06/11/2026 14:05",
            "MENU DIA x2     24,00",
            "BEBIDAS          4,50",
            "BASE IMPONIBLE  26,09",
            "IVA 10%          2,61",
            "TOTAL           28,70",
            "GRACIAS POR SU VISITA",
        )
        val r = ReceiptParser.parse(lines, today)
        assertEquals(2870L, r.amountCents)
        assertEquals(FieldConfidence.HIGH, r.amountConfidence)
        assertEquals("La Taberna Del Puerto", r.merchant)
    }

    @Test
    fun ticketConIvaDesglosado() {
        val lines = listOf(
            "CARREFOUR EXPRESS",
            "FACTURA SIMPLIFICADA",
            "05/11/2026",
            "ARTICULO A       10,00",
            "ARTICULO B        5,00",
            "BASE 10%         13,64",
            "CUOTA IVA 10%     1,36",
            "BASE 4%           0,00",
            "CUOTA IVA 4%      0,00",
            "TOTAL IMPUESTOS   1,36",
            "TOTAL (EUR)      15,00",
        )
        val r = ReceiptParser.parse(lines, today)
        // Debe elegir TOTAL (EUR) 15,00, no BASE/CUOTA/IVA
        assertEquals(1500L, r.amountCents)
        assertEquals("Carrefour", r.merchant)
    }

    @Test
    fun ticketConCambioEnEfectivo() {
        val lines = listOf(
            "PANADERIA SAN JOSE",
            "C/ SOL 3",
            "04/11/2026 08:10",
            "BARRA PAN        1,20",
            "BOLLERIA         2,30",
            "TOTAL            3,50",
            "EFECTIVO        10,00",
            "CAMBIO           6,50",
        )
        val r = ReceiptParser.parse(lines, today)
        // Debe elegir TOTAL 3,50, no EFECTIVO/CAMBIO
        assertEquals(350L, r.amountCents)
        assertEquals(FieldConfidence.HIGH, r.amountConfidence)
    }

    @Test
    fun ticketFechaDdMmYy() {
        val lines = listOf(
            "LIDL SUPERMERCADOS",
            "03-11-26 19:44",
            "LECHE            0,95",
            "YOGUR            1,85",
            "TOTAL            2,80",
        )
        val r = ReceiptParser.parse(lines, today)
        assertEquals(LocalDate.of(2026, 11, 3), r.date)
        assertEquals(280L, r.amountCents)
        assertEquals("Lidl", r.merchant)
    }

    @Test
    fun ticketSinPalabraTotal() {
        val lines = listOf(
            "CAFETERIA CENTRAL",
            "C/ GRAN VIA 1",
            "02/11/2026 11:00",
            "CAFE             1,50",
            "TOSTADA          2,20",
            "ZUMO             2,80",
            "                 6,50",
        )
        val r = ReceiptParser.parse(lines, today)
        // Sin palabra clave -> mayor en tercio inferior, baja confianza
        assertEquals(650L, r.amountCents)
        assertEquals(FieldConfidence.LOW, r.amountConfidence)
        assertEquals(LocalDate.of(2026, 11, 2), r.date)
    }

    @Test
    fun ticketSinPalabraClaveUsaMayorDelTercioInferior() {
        val lines = listOf(
            "TIENDA BARRIO",
            "01/11/2026",
            "PRODUCTO A      20,00",
            "PRODUCTO B       5,00",
            "PRODUCTO C       3,00",
            "SUMA            28,00",
        )
        val r = ReceiptParser.parse(lines, today)
        // Sin TOTAL/IMPORTE/A PAGAR -> mayor en tercio inferior, baja confianza
        assertEquals(2800L, r.amountCents)
        assertEquals(FieldConfidence.LOW, r.amountConfidence)
        assertNotNull(r.merchant)
    }
}