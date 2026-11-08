package com.mipatrimonio.app.core.ocr

import com.mipatrimonio.app.core.money.Money
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlin.math.max

/**
 * Heurísticas de extracción de tickets (§9.2–9.3).
 * Entrada: líneas de texto OCR. Nunca guardar sin confirmación.
 */
object ReceiptParser {

    private val AMOUNT_REGEX = Regex("""(\d{1,3}(?:[.\s]\d{3})*|\d+)[.,]\d{2}""")
    private val DATE_REGEX = Regex("""\b(\d{1,2})[/\-.](\d{1,2})[/\-.](\d{2,4})\b""")
    private val LONG_NUMBER = Regex("""\d{5,}""")

    private val TOTAL_KEYWORDS = listOf(
        "TOTAL A PAGAR", "TOTAL (EUR)", "TOTAL EUROS", "TOTAL COMPRA",
        "IMPORTE", "A PAGAR", "TOTAL",
    )
    private val AMOUNT_EXCLUDE = listOf(
        "IVA", "BASE", "CUOTA", "CAMBIO", "ENTREGADO", "EFECTIVO",
        "DESCUENTO", "AHORRO", "PUNTOS",
    )
    private val MERCHANT_EXCLUDE = listOf(
        "CIF", "NIF", "TEL", "C/", "CALLE", "AVDA", "WWW", "@",
    )
    private val KNOWN_MERCHANTS = listOf(
        "MERCADONA", "CARREFOUR", "LIDL", "DIA", "ALCAMPO", "EROSKI",
        "AHORRAMAS", "ALDI", "LEROY MERLIN", "IKEA", "REPSOL", "CEPSA", "BP", "SHELL",
    )

    fun parse(lines: List<String>, today: LocalDate = LocalDate.now()): ParsedReceipt {
        val normalized = lines.map { normalizeLine(it) }
        val (amountCents, amountConfidence) = parseAmount(normalized)
        val (date, dateConfidence) = parseDate(normalized, today)
        val (merchant, merchantConfidence) = parseMerchant(normalized)
        return ParsedReceipt(
            amountCents = amountCents,
            amountConfidence = amountConfidence,
            date = date ?: today,
            dateConfidence = if (date != null) dateConfidence else FieldConfidence.LOW,
            merchant = merchant,
            merchantConfidence = merchantConfidence,
        )
    }

    internal fun normalizeLine(line: String): String {
        val upper = line.uppercase(Locale.ROOT).replace(Regex("""\s+"""), " ").trim()
        // O -> 0 solo dentro de números: reemplazar O cuando está adyacente a dígitos o separadores numéricos
        val chars = upper.toCharArray()
        for (i in chars.indices) {
            if (chars[i] == 'O') {
                val prevNum = i > 0 && (chars[i - 1].isDigit() || chars[i - 1] == ',' || chars[i - 1] == '.' || chars[i - 1] == ' ')
                val nextNum = i + 1 < chars.size && (chars[i + 1].isDigit() || chars[i + 1] == ',' || chars[i + 1] == '.' || chars[i + 1] == ' ')
                // Heurística: si cualquier lado parece contexto numérico (dígito), convertir
                val prevDigit = i > 0 && chars[i - 1].isDigit()
                val nextDigit = i + 1 < chars.size && chars[i + 1].isDigit()
                if (prevDigit || nextDigit) chars[i] = '0'
            }
        }
        return String(chars)
    }

    private fun parseAmount(lines: List<String>): Pair<Long?, FieldConfidence> {
        data class Candidate(val cents: Long, val lineIndex: Int, val hasKeyword: Boolean)
        val candidates = mutableListOf<Candidate>()
        lines.forEachIndexed { index, line ->
            val hasKeyword = hasTotalKeyword(line)
            if (AMOUNT_EXCLUDE.any { line.contains(it) } && !hasKeyword) return@forEachIndexed
            AMOUNT_REGEX.findAll(line).forEach { match ->
                val cents = parseAmountMatch(match.value) ?: return@forEach
                candidates += Candidate(cents, index, hasKeyword)
            }
        }
        if (candidates.isEmpty()) return null to FieldConfidence.LOW

        val keywordCandidates = candidates.filter { it.hasKeyword }
        if (keywordCandidates.isNotEmpty()) {
            // §9.2: si hay varios, quedarse con el último.
            return keywordCandidates.last().cents to FieldConfidence.HIGH
        }
        // Sin palabra clave: importe mayor en el tercio inferior
        val start = max(0, (lines.size * 2) / 3)
        val bottom = candidates.filter { it.lineIndex >= start }
        val pool = bottom.ifEmpty { candidates }
        val best = pool.maxByOrNull { it.cents } ?: candidates.maxByOrNull { it.cents }
        return if (best != null) best.cents to FieldConfidence.LOW else null to FieldConfidence.LOW
    }

    private fun hasTotalKeyword(line: String): Boolean = TOTAL_KEYWORDS.any { line.contains(it) }

    private fun parseAmountMatch(raw: String): Long? {
        return try {
            val value = Money.parse(raw)
            if (value > 0) value else null
        } catch (_: Exception) {
            // Intento manual: normalizar separadores
            try {
                val cleaned = raw.replace(" ", "")
                val normalized = when {
                    cleaned.lastIndexOf(',') > cleaned.lastIndexOf('.') ->
                        cleaned.replace(".", "").replace(",", ".")
                    cleaned.contains(',') && !cleaned.contains('.') ->
                        cleaned.replace(",", ".")
                    else -> cleaned.replace(",", "")
                }
                val cents = java.math.BigDecimal(normalized).movePointRight(2).longValueExact()
                if (cents > 0) cents else null
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun parseDate(lines: List<String>, today: LocalDate): Pair<LocalDate?, FieldConfidence> {
        val minDate = today.minusYears(5)
        val maxDate = today.plusDays(1)
        for (line in lines) {
            val match = DATE_REGEX.find(line) ?: continue
            val d = match.groupValues[1].toIntOrNull() ?: continue
            val m = match.groupValues[2].toIntOrNull() ?: continue
            val yRaw = match.groupValues[3].toIntOrNull() ?: continue
            val y = if (yRaw < 100) 2000 + yRaw else yRaw
            val date = try {
                LocalDate.of(y, m, d)
            } catch (_: Exception) {
                continue
            }
            if (!date.isBefore(minDate) && !date.isAfter(maxDate)) {
                return date to FieldConfidence.HIGH
            }
        }
        return null to FieldConfidence.LOW
    }

    private fun parseMerchant(lines: List<String>): Pair<String?, FieldConfidence> {
        val candidates = lines.take(6).filter { line ->
            line.any { it.isLetter() } &&
                MERCHANT_EXCLUDE.none { line.contains(it) } &&
                !LONG_NUMBER.containsMatchIn(line)
        }
        val raw = candidates.firstOrNull() ?: return null to FieldConfidence.LOW
        val known = KNOWN_MERCHANTS.firstOrNull { raw.contains(it) }
        return if (known != null) {
            normalizeKnownMerchant(known) to FieldConfidence.HIGH
        } else {
            toTitleCase(raw) to FieldConfidence.MEDIUM
        }
    }

    private fun normalizeKnownMerchant(known: String): String = when (known) {
        "LEROY MERLIN" -> "Leroy Merlin"
        else -> known.lowercase(Locale.ROOT).replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
        }
    }

    private fun toTitleCase(s: String): String =
        s.lowercase(Locale.ROOT).split(" ").joinToString(" ") { w ->
            w.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        }.trim()
}