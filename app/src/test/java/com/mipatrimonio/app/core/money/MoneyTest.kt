package com.mipatrimonio.app.core.money

import org.junit.Assert.*
import org.junit.Test

class MoneyTest {

    @Test
    fun format_basic() {
        assertEquals("0,00 €", Money.format(0))
        assertEquals("1,00 €", Money.format(100))
        assertEquals("1,23 €", Money.format(123))
        assertEquals("12,34 €", Money.format(1234))
        assertEquals("123,45 €", Money.format(12345))
        assertEquals("1.234,56 €", Money.format(123456))
        assertEquals("12.345,67 €", Money.format(1234567))
        assertEquals("123.456,78 €", Money.format(12345678))
    }

    @Test
    fun format_negative() {
        assertEquals("1,00 €", Money.format(-100))
    }

    @Test
    fun formatSigned_expense() {
        assertEquals("−1,00 €", Money.formatSigned(100, true))
        assertEquals("−1.234,56 €", Money.formatSigned(123456, true))
    }

    @Test
    fun formatSigned_income() {
        assertEquals("+1,00 €", Money.formatSigned(100, false))
        assertEquals("+1.234,56 €", Money.formatSigned(123456, false))
    }

    @Test
    fun formatSigned_zero() {
        assertEquals("0,00 €", Money.formatSigned(0, true))
        assertEquals("0,00 €", Money.formatSigned(0, false))
    }

    @Test
    fun formatNumber() {
        assertEquals("1.234,56", Money.formatNumber(123456))
    }

    @Test
    fun parse_basic() {
        assertEquals(2345L, Money.parse("23,45"))
        assertEquals(2345L, Money.parse("23.45"))
        assertEquals(123456L, Money.parse("1.234,56"))
        assertEquals(123456L, Money.parse("1234,56"))
    }

    @Test
    fun parse_with_sign() {
        assertEquals(2345L, Money.parse("+23,45"))
        assertEquals(-2345L, Money.parse("-23,45"))
        assertEquals(-2345L, Money.parse("−23,45")) // U+2212
    }

    @Test
    fun parse_with_euro_symbol() {
        assertEquals(2345L, Money.parse("23,45 €"))
        assertEquals(123456L, Money.parse("1.234,56 €"))
        assertEquals(-2345L, Money.parse("−23,45 €"))
    }

    @Test
    fun parse_whitespace() {
        assertEquals(2345L, Money.parse("  23,45  "))
        assertEquals(2345L, Money.parse("\t23,45\n"))
    }

    @Test
    fun parse_thousands_separator_dot() {
        // Formato español: punto = miles, coma = decimal
        assertEquals(123456L, Money.parse("1.234,56"))
        assertEquals(1234567L, Money.parse("12.345,67"))
    }

    @Test
    fun parse_us_format_with_dot_decimal() {
        // Si solo hay puntos, asumimos decimal US
        assertEquals(123456L, Money.parse("1234.56"))
    }

    @Test
    fun parse_only_commas_as_decimal() {
        // Solo comas -> decimal
        assertEquals(1234L, Money.parse("12,34"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun parse_empty_throws() {
        Money.parse("")
    }

    @Test(expected = IllegalArgumentException::class)
    fun parse_invalid_throws() {
        Money.parse("abc")
    }

    @Test
    fun parse_multiple_dots_accepted_as_thousands() {
        // Varios puntos se interpretan como separadores de miles
        assertEquals(123456789L, Money.parse("1.234.567,89"))
    }

    @Test
    fun toEuros_and_fromEuros() {
        assertEquals(1234.56, Money.toEuros(123456), 0.001)
        assertEquals(123456L, Money.fromEuros(1234.56))
    }

    @Test
    fun round_trip_format_parse() {
        val amounts = listOf(0L, 1L, 99L, 100L, 123L, 1234L, 12345L, 123456L, 1234567L)
        amounts.forEach { cents ->
            val formatted = Money.format(cents)
            val parsed = Money.parse(formatted)
            assertEquals(cents, parsed)
        }
    }
}