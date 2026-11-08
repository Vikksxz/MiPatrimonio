package com.mipatrimonio.app.core.export

import com.mipatrimonio.app.core.database.entity.CategoryEntity
import com.mipatrimonio.app.core.database.entity.TransactionEntity
import com.mipatrimonio.app.core.model.TransactionType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Exporta movimientos a CSV delimitado por ';' (compatible con Excel ES), UTF-8 con BOM. */
object CsvExporter {

    private val dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("es-ES"))

    fun export(
        transactions: List<TransactionEntity>,
        categoriesById: Map<Long, CategoryEntity>,
        typeLabel: (TransactionType) -> String,
    ): String {
        val sb = StringBuilder()
        // BOM UTF-8 para Excel
        sb.append('\uFEFF')
        sb.append("Fecha;Tipo;Importe;Moneda;Comercio;Nota;Categoria\n")
        transactions.forEach { tx ->
            val date = LocalDate.ofEpochDay(tx.date).format(dateFmt)
            val type = typeLabel(tx.type)
            val amount = formatAmountEs(tx.amountCents)
            val cat = tx.categoryId?.let { categoriesById[it]?.name }.orEmpty()
            sb.append(escape(date)).append(';')
                .append(escape(type)).append(';')
                .append(escape(amount)).append(';')
                .append(escape(tx.currency)).append(';')
                .append(escape(tx.merchant)).append(';')
                .append(escape(tx.note)).append(';')
                .append(escape(cat)).append('\n')
        }
        return sb.toString()
    }

    internal fun formatAmountEs(cents: Long): String {
        val negative = cents < 0
        val abs = kotlin.math.abs(cents)
        val euros = abs / 100
        val dec = (abs % 100).toString().padStart(2, '0')
        val eurosStr = euros.toString().reversed().chunked(3).joinToString(".").reversed()
        val s = "$eurosStr,$dec"
        return if (negative) "-$s" else s
    }

    private fun escape(value: String): String {
        if (value.contains(';') || value.contains('"') || value.contains('\n') || value.contains('\r')) {
            return "\"" + value.replace("\"", "\"\"") + "\""
        }
        return value
    }
}
