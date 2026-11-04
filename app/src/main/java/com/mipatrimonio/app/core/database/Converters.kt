package com.mipatrimonio.app.core.database

import androidx.room.TypeConverter
import com.mipatrimonio.app.core.model.CardKind
import com.mipatrimonio.app.core.model.DocumentKind
import com.mipatrimonio.app.core.model.IncomeKind
import com.mipatrimonio.app.core.model.RecurrencePeriod
import com.mipatrimonio.app.core.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Conversores Room (MiPatrimonio.md §6).
 * - Fechas: LocalDate ↔ epochDay (Long), Instant/LocalDateTime ↔ epochMillis (Long).
 * - Enums ↔ String (nombre de la constante, no ordinal).
 * NUNCA usar Double/Float para importes.
 */
class Converters {

    // java.time
    @TypeConverter
    fun fromEpochDay(value: Long?): LocalDate? = value?.let { LocalDate.ofEpochDay(it) }

    @TypeConverter
    fun toEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun fromEpochMillis(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }

    @TypeConverter
    fun toEpochMillis(instant: Instant?): Long? = instant?.toEpochMilli()

    @TypeConverter
    fun fromEpochMillisLDT(value: Long?): LocalDateTime? = value?.let { Instant.ofEpochMilli(it).atZone(java.time.ZoneOffset.UTC).toLocalDateTime() }

    @TypeConverter
    fun toEpochMillisLDT(dateTime: LocalDateTime?): Long? = dateTime?.let { it.atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli() }

    // Enums -> String
    @TypeConverter
    fun transactionTypeToString(t: TransactionType?): String? = t?.name

    @TypeConverter
    fun stringToTransactionType(s: String?): TransactionType? = s?.let { TransactionType.valueOf(it) }

    @TypeConverter
    fun incomeKindToString(k: IncomeKind?): String? = k?.name

    @TypeConverter
    fun stringToIncomeKind(s: String?): IncomeKind? = s?.let { IncomeKind.valueOf(it) }

    @TypeConverter
    fun cardKindToString(k: CardKind?): String? = k?.name

    @TypeConverter
    fun stringToCardKind(s: String?): CardKind? = s?.let { CardKind.valueOf(it) }

    @TypeConverter
    fun documentKindToString(k: DocumentKind?): String? = k?.name

    @TypeConverter
    fun stringToDocumentKind(s: String?): DocumentKind? = s?.let { DocumentKind.valueOf(it) }

    @TypeConverter
    fun recurrencePeriodToString(p: RecurrencePeriod?): String? = p?.name

    @TypeConverter
    fun stringToRecurrencePeriod(s: String?): RecurrencePeriod? = s?.let { RecurrencePeriod.valueOf(it) }
}