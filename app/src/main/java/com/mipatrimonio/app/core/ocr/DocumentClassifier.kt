package com.mipatrimonio.app.core.ocr

import com.mipatrimonio.app.core.money.Money
import java.text.Normalizer
import java.time.LocalDate
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
    /** Fecha sugerida detectada en el documento, o null. */
    val suggestedDate: LocalDate? = null,
)

/**
 * Clasifica texto OCR como ticket o nómina por palabras clave (§9.4).
 * Extrae líquido y fecha de nóminas con heurísticas flexibles (confirmación siempre del usuario).
 */
object DocumentClassifier {

    private val PAYROLL_KEYWORDS = listOf(
        "NOMINA", "RECIBO DE SALARIOS", "DEVENGOS", "LIQUIDO A PERCIBIR",
        "LIQUIDO TOTAL", "NETO A PERCIBIR", "NETO A COBRAR", "TOTAL A PERCIBIR",
        "A INGRESAR", "IMPORTE LIQUIDO", "LIQUIDO", "NETO",
        "DEDUCCIONES", "SEGURIDAD SOCIAL", "IRPF", "BASE DE COTIZACION", "TOTAL A DEDUCIR",
    )
    private val TICKET_KEYWORDS = listOf(
        "TICKET", "FACTURA SIMPLIFICADA", "IVA", "CAMBIO", "TOTAL",
        "GRACIAS POR SU VISITA", "TARJETA",
    )
    private val NET_KEY_MARKERS = listOf(
        "LIQUIDO A PERCIBIR", "LIQUIDO TOTAL", "NETO A PERCIBIR", "NETO A COBRAR",
        "TOTAL A PERCIBIR", "A INGRESAR", "IMPORTE LIQUIDO", "LIQUIDO", "NETO A",
        "ABONO", "TRANSFERENCIA",
    )
    private val AMOUNT_REGEX = Regex("""(\d{1,3}(?:[.\s]\d{3})*|\d+)[.,]\d{2}""")
    private val DATE_REGEX = Regex("""\b(\d{1,2})[/\-.](\d{1,2})[/\-.](\d{2,4})\b""")
    private val CLEAR_MARGIN = 2

    fun classify(lines: List<String>, today: LocalDate = LocalDate.now()): DocumentClassification {
        val normalized = lines.map { normalize(it) }
        val payrollScore = score(normalized, PAYROLL_KEYWORDS)
        val ticketScore = score(normalized, TICKET_KEYWORDS)
        val suggested = when {
            payrollScore >= ticketScore + CLEAR_MARGIN && payrollScore > 0 -> SuggestedDocumentKind.PAYROLL
            ticketScore >= payrollScore + CLEAR_MARGIN && ticketScore > 0 -> SuggestedDocumentKind.TICKET
            else -> SuggestedDocumentKind.UNKNOWN
        }
        val net = if (payrollScore > 0 || suggested == SuggestedDocumentKind.PAYROLL) findNetPay(normalized) else null
        val date = findDate(normalized, today)
        return DocumentClassification(suggested, payrollScore, ticketScore, net, date)
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
            if (NET_KEY_MARKERS.none { line.contains(it) }) continue
            val amount = extractLastAmount(line)
                ?: lines.getOrNull(index + 1)?.let { extractLastAmount(it) }
                ?: lines.getOrNull(index + 2)?.let { extractLastAmount(it) }
            if (amount != null) return amount
        }
        // Fallback nómina: si hay "DEVENGOS/DEDUCCIONES" pero no marcador claro, no adivinar.
        return null
    }

    private fun findDate(lines: List<String>, today: LocalDate): LocalDate? {
        val minDate = today.minusYears(5)
        val maxDate = today.plusDays(1)
        var found: LocalDate? = null
        for (line in lines) {
            val match = DATE_REGEX.find(line) ?: continue
            val d = match.groupValues[1].toIntOrNull() ?: continue
            val m = match.groupValues[2].toIntOrNull() ?: continue
            val yRaw = match.groupValues[3].toIntOrNull() ?: continue
            val y = if (yRaw < 100) 2000 + yRaw else yRaw
            val date = try { LocalDate.of(y, m, d) } catch (_: Exception) { continue }
            if (!date.isBefore(minDate) && !date.isAfter(maxDate)) {
                // Preferir una fecha plausible "de nómina" (no futura); quedarnos con la última válida vista.
                found = date
            }
        }
        return found
    }

    private fun extractLastAmount(line: String): Long? =
        AMOUNT_REGEX.findAll(line).lastOrNull()?.let { match ->
            runCatching { Money.parse(match.value) }.getOrNull()?.takeIf { it > 0 }
        }
}
