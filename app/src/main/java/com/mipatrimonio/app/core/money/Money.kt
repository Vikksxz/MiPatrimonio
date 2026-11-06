package com.mipatrimonio.app.core.money

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.regex.Pattern

/**
 * Utilidades de formato y parseo de dinero (MiPatrimonio.md §7.10).
 * - Los importes se almacenan en **céntimos** (`Long`).
 * - Formato: `1.234,56 €` (miles `.`, decimal `,`, símbolo al final con espacio).
 * - Gastos: prefijo `−` (U+2212), ingresos: prefijo `+`.
 * - Parseo flexible: acepta `23,45`, `23.45`, `1.234,56`.
 */
object Money {

    private const val EURO_SYMBOL = "€"
    private val LOCALE_ES = Locale("es", "ES")
    private val MINUS_SIGN = "\u2212" // U+2212, no el guion ASCII

    private val symbols = DecimalFormatSymbols(LOCALE_ES).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }

    private val formatter = DecimalFormat("#,##0.00", symbols).apply {
        roundingMode = RoundingMode.HALF_EVEN
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    /** Formatea céntimos a String con símbolo € y separadores ES. Siempre valor absoluto. */
    fun format(cents: Long): String {
        val absCents = kotlin.math.abs(cents)
        val euros = BigDecimal(absCents).movePointLeft(2)
        return "${formatter.format(euros)} $EURO_SYMBOL"
    }

    /** Formatea con signo: gastos `−`, ingresos `+`, neutro sin signo. Cero nunca lleva signo. */
    fun formatSigned(cents: Long, isExpense: Boolean): String {
        if (cents == 0L) return format(0)
        val absCents = kotlin.math.abs(cents)
        val formatted = format(absCents)
        return when {
            isExpense -> "$MINUS_SIGN$formatted"
            cents > 0 -> "+$formatted"
            else -> formatted
        }
    }

    /** Formatea solo el número (sin €), útil para inputs. */
    fun formatNumber(cents: Long): String {
        val euros = BigDecimal(cents).movePointLeft(2)
        return formatter.format(euros)
    }

    /** Parsea String flexible a céntimos.
     * Acepta: "23,45", "23.45", "1.234,56", "1234,56", "+23,45", "−23,45", "23,45 €" */
    fun parse(input: String): Long {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) throw IllegalArgumentException("Importe vacío")

        var sign = 1
        var work = trimmed
        // Detectar signo inicial
        if (work.startsWith("+")) {
            work = work.substring(1).trim()
        } else if (work.startsWith("-") || work.startsWith(MINUS_SIGN)) {
            sign = -1
            work = work.substring(1).trim()
        }
        // Quitar símbolo € si está al final
        work = work.replace(EURO_SYMBOL, "").trim()

        // Normalizar: eliminar separadores de miles (punto), cambiar decimal coma -> punto
        // Heurística: si hay coma y punto, el último es decimal
        // Si solo hay comas -> decimal, si solo puntos -> decimal (formato US)
        val normalized = when {
            work.lastIndexOf(',') > work.lastIndexOf('.') -> {
                // Coma es decimal: quitar puntos (miles), cambiar coma por punto
                work.replace(".", "").replace(",", ".")
            }
            work.lastIndexOf('.') > work.lastIndexOf(',') -> {
                // Punto es decimal: quitar comas (miles)
                work.replace(",", "")
            }
            work.contains(',') -> {
                // Solo comas -> decimal
                work.replace(",", ".")
            }
            else -> work // Sin separadores o solo puntos (asumir decimal punto)
        }

        return try {
            (BigDecimal(normalized).movePointRight(2).longValueExact() * sign)
        } catch (e: Exception) {
            throw IllegalArgumentException("Importe inválido: '$input'")
        }
    }

    /** Convierte céntimos a euros (Double) solo para cálculos puntuales, NO para persistencia. */
    fun toEuros(cents: Long): Double = BigDecimal(cents).movePointLeft(2).toDouble()

    /** Crea céntimos desde euros (Double) - usar con cuidado, solo para entrada usuario. */
    fun fromEuros(euros: Double): Long = BigDecimal(euros).movePointRight(2).setScale(0, RoundingMode.HALF_EVEN).longValueExact()
}