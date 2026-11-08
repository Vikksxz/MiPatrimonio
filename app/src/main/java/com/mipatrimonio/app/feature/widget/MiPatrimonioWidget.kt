package com.mipatrimonio.app.feature.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.mipatrimonio.app.R
import com.mipatrimonio.app.core.money.Money
import com.mipatrimonio.app.domain.usecase.GetMonthlySummary
import java.time.YearMonth

class MiPatrimonioWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = runCatching { loadData(context) }.getOrElse { WidgetData("—", "—", "—", null, null) }
        provideContent {
            WidgetContent(data)
        }
    }

    private suspend fun loadData(context: Context): WidgetData {
        val ep = WidgetEntryPoint.get(context)
        val month = YearMonth.now()
        val from = month.atDay(1).toEpochDay()
        val to = month.plusMonths(1).atDay(1).toEpochDay()
        val txs = ep.transactionDao().getByMonth(from, to)
        val recentEmpty = emptyList<com.mipatrimonio.app.core.database.entity.TransactionEntity>()
        val saved = ep.savingsDao().getMonthlyContributed(from, to) ?: 0L
        val plans = ep.savingsDao().getAllPlans()
        val active = plans.firstOrNull { it.isActive }
        val planData = if (active != null) {
            val contributed = ep.savingsDao().getContributed(active.id) ?: 0L
            val progress = if (active.targetCents > 0L) {
                (contributed.toDouble() / active.targetCents.toDouble()).coerceIn(0.0, 1.0).toFloat()
            } else 0f
            PlanData(active.name, progress)
        } else null
        val summary = GetMonthlySummary.build(month, txs, recentEmpty, saved, null, emptyList())
        val projection = if (summary.projection?.visible == true) {
            Money.format(summary.projection.projectedExpenseCents)
        } else null
        return WidgetData(
            available = Money.format(summary.availableCents),
            expenses = Money.format(summary.expenseCents),
            income = Money.format(summary.incomeCents),
            projection = projection,
            plan = planData,
        )
    }
}

private data class PlanData(val name: String, val progress: Float)
private data class WidgetData(
    val available: String,
    val expenses: String,
    val income: String,
    val projection: String?,
    val plan: PlanData?,
)

private val Bg = ColorProvider(R.color.widget_bg)
private val Panel = ColorProvider(R.color.widget_panel)
private val Ink = ColorProvider(R.color.widget_ink)
private val Muted = ColorProvider(R.color.widget_muted)
private val Cyan = ColorProvider(R.color.widget_cyan)
private val Track = ColorProvider(R.color.widget_track)



@Composable
private fun WidgetContent(data: WidgetData) {
    val context = LocalContext.current
    val addIntent = Intent(Intent.ACTION_VIEW, Uri.parse("mipatrimonio://add")).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    Column(
        modifier = GlanceModifier.fillMaxSize().background(Bg).padding(12.dp),
        verticalAlignment = Alignment.Top,
        horizontalAlignment = Alignment.Start,
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = context.getString(R.string.widget_available_label),
                    style = TextStyle(color = Muted, fontSize = 11.sp),
                    maxLines = 1,
                )
                Text(
                    text = data.available,
                    style = TextStyle(color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                )
            }
            if (data.projection != null) {
                Spacer(GlanceModifier.width(6.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = context.getString(R.string.widget_projection),
                        style = TextStyle(color = Muted, fontSize = 10.sp),
                        maxLines = 1,
                    )
                    Text(
                        text = data.projection,
                        style = TextStyle(color = Cyan, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                        maxLines = 1,
                    )
                }
            }
        }
        Spacer(GlanceModifier.height(4.dp))
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = context.getString(R.string.home_expenses),
                    style = TextStyle(color = Muted, fontSize = 10.sp),
                )
                Text(
                    text = data.expenses,
                    style = TextStyle(color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Medium),
                    maxLines = 1,
                )
            }
            Spacer(GlanceModifier.width(8.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = context.getString(R.string.home_income),
                    style = TextStyle(color = Muted, fontSize = 10.sp),
                )
                Text(
                    text = data.income,
                    style = TextStyle(color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Medium),
                    maxLines = 1,
                )
            }
        }
        Spacer(GlanceModifier.height(6.dp))
        if (data.plan != null) {
            Text(
                text = data.plan.name,
                style = TextStyle(color = Cyan, fontSize = 11.sp),
                maxLines = 1,
            )
            Spacer(GlanceModifier.height(3.dp))
            LinearProgressIndicator(
                progress = data.plan.progress,
                modifier = GlanceModifier.fillMaxWidth().height(6.dp),
                color = Cyan,
                backgroundColor = Track,
            )
        } else {
            Text(
                text = context.getString(R.string.widget_no_plan),
                style = TextStyle(color = Muted, fontSize = 11.sp),
                maxLines = 1,
            )
        }
        Spacer(GlanceModifier.height(8.dp))
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(Panel)
                .padding(horizontal = 10.dp, vertical = 7.dp)
                .clickable(actionStartActivity(addIntent)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = context.getString(R.string.widget_add_expense),
                style = TextStyle(color = Cyan, fontSize = 12.sp, fontWeight = FontWeight.Medium),
            )
        }
    }
}
