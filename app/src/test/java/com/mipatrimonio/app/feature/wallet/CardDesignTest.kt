package com.mipatrimonio.app.feature.wallet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comprueba que los seis diseños de tarjeta cumplen MiPatrimonio.md §5.5
 * y que los identificadores aceptados por `CardEntity.theme` (§6) son esos.
 */
class CardDesignTest {

    private val validThemes = listOf("cyan", "magenta", "green", "amber", "violet", "graphite")

    @Test
    fun hay_exactamente_seis_disenos() {
        assertEquals(6, CardDesign.DESIGNS.size)
    }

    @Test
    fun los_identificadores_son_los_de_la_especificacion() {
        assertEquals(validThemes, CardDesign.DESIGNS.map { it.id })
    }

    @Test
    fun los_identificadores_no_se_repiten() {
        val ids = CardDesign.DESIGNS.map { it.id }
        assertEquals(ids.size, ids.distinct().size)
    }

    @Test
    fun cada_diseno_tiene_nombre_y_tres_colores() {
        CardDesign.DESIGNS.forEach { design ->
            assertNotNull(design.nameRes)
            assertTrue("inicio opaco", design.startColor ushr 24 == 0xFFL)
            assertTrue("fin opaco", design.endColor ushr 24 == 0xFFL)
            assertTrue("acento opaco", design.accentColor ushr 24 == 0xFFL)
        }
    }

    @Test
    fun el_degradado_y_el_acento_coinciden_con_la_tabla() {
        val esperado = mapOf(
            "cyan" to Triple(0xFF0B1B3A, 0xFF1D0B3A, 0xFF22D3EE),
            "magenta" to Triple(0xFF2A0B2E, 0xFF0B1B3A, 0xFFFF4FD8),
            "green" to Triple(0xFF06241A, 0xFF0B1B3A, 0xFF39FF88),
            "amber" to Triple(0xFF2A1A06, 0xFF1A0B2E, 0xFFFFB020),
            "violet" to Triple(0xFF1D0B3A, 0xFF0B0B24, 0xFFB26BFF),
            "graphite" to Triple(0xFF1A2030, 0xFF0A0D14, 0xFF8FA0C4),
        )
        CardDesign.DESIGNS.forEach { design ->
            val colores = esperado.getValue(design.id)
            assertEquals(design.id, colores.first, design.startColor)
            assertEquals(design.id, colores.second, design.endColor)
            assertEquals(design.id, colores.third, design.accentColor)
        }
    }

    @Test
    fun get_by_id_desconocido_devuelve_el_primero() {
        assertEquals(CardDesign.DESIGNS.first(), CardDesign.getById("desconocido"))
    }

    @Test
    fun get_by_id_devuelve_el_diseno_pedido() {
        assertTrue(CardDesign.getById("graphite").id == "graphite")
    }

    @Test
    fun los_seis_bancos_de_la_hoja_estan_definidos() {
        assertEquals(6, BankOption.BANKS.size)
        assertEquals(
            listOf("caixabank", "bbva", "santander", "ing", "revolut", "other"),
            BankOption.BANKS.map { it.id },
        )
    }
}
