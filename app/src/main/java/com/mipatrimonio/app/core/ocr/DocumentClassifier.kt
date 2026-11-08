package com.mipatrimonio.app.core.ocr

import com.mipatrimonio.app.core.money.Money
import java.text.Normalizer
import java.util.Locale

/** Tipo sugerido para un documento recibido (§9.4). */
enum class SuggestedDocumentKind { TICKET, PAYROLL, UNKNOWN }

/** Resultado de la clasificación automática (§9.4). */
data class DocumentClassification(
    val suggested: SuggestedDocumentKind,
    val payrollScore: Int,
    val ticketScore: Int,
    /** Importe líquido sugerido (céntimos) si se detecta nómina, o null. */
    val suggestedNetCents: Long? = null,
)

/**
 * Clasifica texto OCR como ticket o nómina por palabras clave (§9.4).
 * Sin acentos, en mayúsculas. El usuario siempre confirma.
 */
object DocumentClassifier {

    private val PAYROLL_KEYWORDS = listOf(
        "NOMINA", "RECIBO DE SALARIOS", "DEVENGOS", "LIQUIDO A PERCIBIR",
        "DEDUCCIONES", "SEGURIDAD SOCIAL", "IRPF", "BASE DE COTIZACION", "TOTAL A DEDUCIR",
    )
    private val TICKET_KEYWORDS = listOf(
        "TICKET", "FACTURA SIMPLIFICADA", "IVA", "CAMBIO", "TOTAL",
        "GRACIAS POR SU VISITA", "TARJETA",
    )
    private val AMOUNT_REGEX = Regex("""(\d{1,3}(?:[.\s]\d{3})*|\d+)[.,]\d{2}""")
    private val CLEAR_MARGIN = 2

    fun classify(lines: List<String>): DocumentClassification {
        val normalized = lines.map { normalize(it) }
        val payrollScore = score(normalized, PAYROLL_KEYWORDS)
        val ticketScore = score(normalized, TICKET_KEYWORDS)
        val suggested = when {
            payrollScore >= ticketScore + CLEAR_MARGIN && payrollScore > 0 -> SuggestedDocumentKind.PAYROLL
            ticketScore >= payrollScore + CLEAR_MARGIN && ticketScore > 0 -> SuggestedDocumentKind.TICKET
            else -> SuggestedDocumentKind.UNKNOWN
        }
        val net = if (payrollScore > 0) findNetPay(normalized) else null
        return DocumentClassification(suggested, payrollScore, ticketScore, net)
    }

    internal fun normalize(text: String): String {
        val upper = text.uppercase(Locale.ROOT)
        val decomposed = Normalizer.normalize(upper, Normalizer.Form.NFD)
        return decomposed.replace(Regex("\\p{M}+"), "").replace(Regex("""\s+"""), " ").trim()
    }

    private fun score(lines: List<String>, keywords: List<String>): Int =
        keywords.count { kw -> lines.any { it.contains(kw) } }

    private fun findNetPay(lines: List<String>): Long? {
        for ((index, line) in lines.withIndex()) {
            if (!line.contains("LIQUIDO A PERCIBIR") && !line.contains("LIQUIDO")) continue
            // Buscar importe en la misma línea o en la siguiente
            val amount = extractLastAmount(line) ?: lines.getOrNull(index + 1)?.let { extractLastAmount(it) }
            if (amount != null) return amount
        }
        return null
    }

    private fun extractLastAmount(line: String): Long? =
        AMOUNT_REGEX.findAll(line).lastOrNull()?.let { match ->
            runCatching { Money.parse(match.value) }.getOrNull()?.takeIf { it > 0 }
        }
}
