package com.mipatrimonio.app.core.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.mipatrimonio.app.R
import com.mipatrimonio.app.core.money.Money
import com.mipatrimonio.app.domain.usecase.MonthlySummary
import java.io.File
import java.io.FileOutputStream
import java.time.format.TextStyle
import java.util.Locale

/** Genera informe PDF del resumen mensual (§13) con PdfDocument de Android. */
object PdfReportExporter {

    fun generate(context: Context, summary: MonthlySummary, fileName: String): File {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 puntos
        val page = doc.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val titlePaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 20f
            color = 0xFF111111.toInt()
        }
        val labelPaint = Paint().apply {
            textSize = 12f
            color = 0xFF444444.toInt()
        }
        val valuePaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 13f
            color = 0xFF111111.toInt()
        }

        var y = 50f
        val x = 40f
        val locale = Locale.forLanguageTag("es-ES")
        val monthLabel = summary.month.month.getDisplayName(TextStyle.FULL, locale)
            .replaceFirstChar { it.uppercase(locale) } + " " + summary.month.year

        canvas.drawText(context.getString(R.string.export_pdf_title), x, y, titlePaint)
        y += 28f
        canvas.drawText(monthLabel, x, y, labelPaint)
        y += 36f

        fun row(label: String, value: String) {
            canvas.drawText(label, x, y, labelPaint)
            canvas.drawText(value, x + 260f, y, valuePaint)
            y += 22f
        }

        row(context.getString(R.string.export_pdf_income), Money.format(summary.incomeCents))
        row(context.getString(R.string.export_pdf_cash), Money.format(summary.cashCents))
        row(context.getString(R.string.export_pdf_card), Money.format(summary.cardCents))
        row(context.getString(R.string.export_pdf_subscriptions), Money.format(summary.subscriptionCents))
        row(context.getString(R.string.export_pdf_total_expenses), Money.format(summary.expenseCents))
        row(context.getString(R.string.export_pdf_savings), Money.format(summary.savingsCents))
        y += 8f
        row(context.getString(R.string.export_pdf_available), Money.format(summary.availableCents))

        summary.projection?.let { p ->
            if (p.visible) {
                y += 8f
                row(context.getString(R.string.export_pdf_projection), Money.format(p.projectedExpenseCents))
            }
        }

        y += 28f
        val notePaint = Paint().apply { textSize = 10f; color = 0xFF888888.toInt() }
        canvas.drawText(context.getString(R.string.export_pdf_footer), x, y, notePaint)

        doc.finishPage(page)

        val dir = ExportFileSharer.exportsDir(context)
        val file = File(dir, fileName)
        FileOutputStream(file).use { out -> doc.writeTo(out) }
        doc.close()
        return file
    }
}
