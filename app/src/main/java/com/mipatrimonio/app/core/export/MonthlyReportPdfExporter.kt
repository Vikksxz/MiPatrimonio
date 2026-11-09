package com.mipatrimonio.app.core.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.mipatrimonio.app.R
import com.mipatrimonio.app.core.money.Money
import com.mipatrimonio.app.domain.usecase.MonthlyReport
import com.mipatrimonio.app.domain.usecase.ReportTip
import com.mipatrimonio.app.domain.usecase.ReportTipKind
import java.io.File
import java.io.FileOutputStream
import java.time.format.TextStyle
import java.util.Locale

/** Informe mensual PDF (§8.7, §13) con resumen, comparativa, categorías, ahorro y consejos. */
object MonthlyReportPdfExporter {

    fun generate(context: Context, report: MonthlyReport, fileName: String): File {
        val doc = PdfDocument()
        val locale = Locale.forLanguageTag("es-ES")
        val titlePaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 20f; color = 0xFF111111.toInt()
        }
        val sectionPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 13f; color = 0xFF222222.toInt()
        }
        val labelPaint = Paint().apply { textSize = 11f; color = 0xFF444444.toInt() }
        val valuePaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 11f; color = 0xFF111111.toInt()
        }
        val smallPaint = Paint().apply { textSize = 10f; color = 0xFF555555.toInt() }

        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var page = doc.startPage(pageInfo)
        var canvas: Canvas = page.canvas
        var y = 44f
        val x = 40f
        val rightX = 300f

        fun newPageIfNeeded(needed: Float = 40f) {
            if (y + needed <= 800f) return
            doc.finishPage(page)
            val pi = PdfDocument.PageInfo.Builder(595, 842, doc.pages.size + 1).create()
            page = doc.startPage(pi)
            canvas = page.canvas
            y = 44f
        }

        fun row(label: String, value: String) {
            newPageIfNeeded(20f)
            canvas.drawText(label, x, y, labelPaint)
            canvas.drawText(value, rightX, y, valuePaint)
            y += 18f
        }

        fun section(title: String) {
            newPageIfNeeded(28f)
            y += 6f
            canvas.drawText(title, x, y, sectionPaint)
            y += 18f
        }

        val monthLabel = report.month.month.getDisplayName(TextStyle.FULL, locale)
            .replaceFirstChar { it.uppercase(locale) } + " " + report.month.year
        val prevLabel = report.previousMonth.month.getDisplayName(TextStyle.FULL, locale)
            .replaceFirstChar { it.uppercase(locale) } + " " + report.previousMonth.year

        canvas.drawText(context.getString(R.string.monthly_report_pdf_title), x, y, titlePaint)
        y += 24f
        canvas.drawText(monthLabel, x, y, labelPaint)
        y += 22f

        section(context.getString(R.string.monthly_report_pdf_summary))
        row(context.getString(R.string.export_pdf_income), Money.format(report.incomeCents))
        row(context.getString(R.string.export_pdf_cash), Money.format(report.cashCents))
        row(context.getString(R.string.export_pdf_card), Money.format(report.cardCents))
        row(context.getString(R.string.export_pdf_subscriptions), Money.format(report.subscriptionCents))
        row(context.getString(R.string.export_pdf_total_expenses), Money.format(report.expenseCents))
        row(context.getString(R.string.export_pdf_savings), Money.format(report.savingsContributedCents))
        row(context.getString(R.string.export_pdf_available), Money.format(report.availableCents))

        section(context.getString(R.string.monthly_report_pdf_compare, prevLabel))
        row(context.getString(R.string.monthly_report_pdf_prev_expenses), Money.format(report.previousExpenseCents))
        val deltaText = when {
            report.expenseDeltaCents > 0L -> "+" + Money.format(report.expenseDeltaCents)
            else -> Money.format(report.expenseDeltaCents)
        }
        row(context.getString(R.string.monthly_report_pdf_delta), deltaText)

        section(context.getString(R.string.monthly_report_pdf_by_category))
        if (report.expensesByCategory.isEmpty()) {
            row(context.getString(R.string.monthly_report_pdf_no_data), "—")
        } else {
            report.expensesByCategory.forEach { cat ->
                row(cat.name.ifBlank { "—" }, Money.format(cat.amountCents))
            }
        }

        section(context.getString(R.string.monthly_report_pdf_top_merchants))
        if (report.topMerchants.isEmpty()) {
            row(context.getString(R.string.monthly_report_pdf_no_data), "—")
        } else {
            report.topMerchants.forEach { m -> row(m.merchant, Money.format(m.amountCents)) }
        }

        section(context.getString(R.string.monthly_report_pdf_savings_plans))
        if (report.savingsPlans.isEmpty()) {
            row(context.getString(R.string.monthly_report_pdf_no_data), "—")
        } else {
            report.savingsPlans.forEach { p ->
                row(p.name, "${Money.format(p.contributedCents)} / ${Money.format(p.targetCents)}")
            }
        }

        section(context.getString(R.string.monthly_report_pdf_tips))
        if (report.tips.isEmpty()) {
            row(context.getString(R.string.monthly_report_pdf_no_data), "—")
        } else {
            report.tips.forEach { tip ->
                newPageIfNeeded(18f)
                canvas.drawText("• " + tipText(context, tip), x, y, smallPaint)
                y += 16f
            }
        }

        y += 14f
        newPageIfNeeded(20f)
        val footer = Paint().apply { textSize = 9f; color = 0xFF888888.toInt() }
        canvas.drawText(context.getString(R.string.export_pdf_footer), x, y, footer)

        doc.finishPage(page)
        val dir = ExportFileSharer.exportsDir(context)
        val file = File(dir, fileName)
        FileOutputStream(file).use { out -> doc.writeTo(out) }
        doc.close()
        return file
    }

    private fun tipText(context: Context, tip: ReportTip): String = when (tip.kind) {
        ReportTipKind.CATEGORY_UP -> context.getString(
            R.string.monthly_report_tip_category_up,
            tip.categoryName ?: "",
            tip.percent,
        )
        ReportTipKind.EXPENSE_OVER_INCOME -> context.getString(R.string.monthly_report_tip_over_income)
        ReportTipKind.SAVINGS_POSITIVE -> context.getString(R.string.monthly_report_tip_savings, Money.format(tip.amountCents))
        ReportTipKind.GENERAL -> context.getString(R.string.monthly_report_tip_general)
    }
}
