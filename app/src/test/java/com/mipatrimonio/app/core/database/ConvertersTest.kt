package com.mipatrimonio.app.core.database

import com.mipatrimonio.app.core.model.CardKind
import com.mipatrimonio.app.core.model.DocumentKind
import com.mipatrimonio.app.core.model.IncomeKind
import com.mipatrimonio.app.core.model.RecurrencePeriod
import com.mipatrimonio.app.core.model.TransactionType
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun localDateToEpochDayAndBack() {
        val date = LocalDate.of(2026, 11, 3)
        val epoch = converters.toEpochDay(date)
        assertEquals(date.toEpochDay(), epoch)
        assertEquals(date, converters.fromEpochDay(epoch))
        assertNull(converters.fromEpochDay(null))
        assertNull(converters.toEpochDay(null))
    }

    @Test
    fun instantToEpochMillisAndBack() {
        val instant = Instant.parse("2026-11-03T10:15:30Z")
        val millis = converters.toEpochMillis(instant)
        assertEquals(instant.toEpochMilli(), millis)
        assertEquals(instant, converters.fromEpochMillis(millis))
        assertNull(converters.fromEpochMillis(null))
        assertNull(converters.toEpochMillis(null))
    }

    @Test
    fun localDateTimeToEpochMillisAndBack() {
        val ldt = LocalDateTime.of(2026, 11, 3, 10, 15, 30)
        val millis = converters.toEpochMillisLDT(ldt)
        assertEquals(ldt.atZone(ZoneOffset.UTC).toInstant().toEpochMilli(), millis)
        assertEquals(ldt, converters.fromEpochMillisLDT(millis))
    }

    @Test
    fun transactionTypeToStringAndBack() {
        assertEquals("CASH", converters.transactionTypeToString(TransactionType.CASH))
        assertEquals(TransactionType.CASH, converters.stringToTransactionType("CASH"))
        assertNull(converters.transactionTypeToString(null))
        assertNull(converters.stringToTransactionType(null))
    }

    @Test
    fun incomeKindToStringAndBack() {
        assertEquals("PAYROLL", converters.incomeKindToString(IncomeKind.PAYROLL))
        assertEquals(IncomeKind.PAYROLL, converters.stringToIncomeKind("PAYROLL"))
    }

    @Test
    fun cardKindToStringAndBack() {
        assertEquals("DEBIT", converters.cardKindToString(CardKind.DEBIT))
        assertEquals(CardKind.DEBIT, converters.stringToCardKind("DEBIT"))
    }

    @Test
    fun documentKindToStringAndBack() {
        assertEquals("PAYSLIP", converters.documentKindToString(DocumentKind.PAYSLIP))
        assertEquals(DocumentKind.PAYSLIP, converters.stringToDocumentKind("PAYSLIP"))
    }

    @Test
    fun recurrencePeriodToStringAndBack() {
        assertEquals("MONTHLY", converters.recurrencePeriodToString(RecurrencePeriod.MONTHLY))
        assertEquals(RecurrencePeriod.MONTHLY, converters.stringToRecurrencePeriod("MONTHLY"))
    }
}