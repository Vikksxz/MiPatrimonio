package com.mipatrimonio.app.feature.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mipatrimonio.app.R
import com.mipatrimonio.app.core.database.entity.TransactionEntity
import com.mipatrimonio.app.core.model.TransactionType
import com.mipatrimonio.app.core.money.Money
import com.mipatrimonio.app.core.settings.HomeBlock
import com.mipatrimonio.app.core.ui.components.AnimatedAmount
import com.mipatrimonio.app.core.ui.components.EmptyState
import com.mipatrimonio.app.core.ui.components.NeonButton
import com.mipatrimonio.app.core.ui.components.NeonButtonText
import com.mipatrimonio.app.core.ui.components.NeonCard
import com.mipatrimonio.app.core.ui.components.ProgressNeonBar
import com.mipatrimonio.app.core.ui.components.ScreenTitle
import com.mipatrimonio.app.core.ui.components.accentColor
import com.mipatrimonio.app.core.ui.components.label
import com.mipatrimonio.app.core.ui.motion.MotionDurations
import com.mipatrimonio.app.core.ui.motion.rememberAnimationsEnabled
import com.mipatrimonio.app.core.ui.motion.rememberStaggeredEnter
import com.mipatrimonio.app.core.ui.motion.staggerDelayMillis
import com.mipatrimonio.app.core.ui.theme.MiPatrimonioTheme
import com.mipatrimonio.app.core.ui.theme.NeonColor
import com.mipatrimonio.app.core.ui.theme.NeonShape
import com.mipatrimonio.app.core.ui.theme.NeonType
import com.mipatrimonio.app.domain.usecase.ActivePlanSummary
import com.mipatrimonio.app.domain.usecase.BudgetLevel
import com.mipatrimonio.app.domain.usecase.GetMonthlySummary
import com.mipatrimonio.app.domain.usecase.HomeBudgetSummary
import com.mipatrimonio.app.domain.usecase.MonthlySummary
import com.mipatrimonio.app.domain.usecase.SavingsStatus
import com.mipatrimonio.app.feature.savings.toMonthYearLabel
import kotlin.math.roundToInt
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

private val MonthFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("LLLL yyyy", Locale.forLanguageTag("es-ES"))

private val ShortDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es-ES"))

/**
 * Inicio (T1.9, MiPatrimonio.md §8.1; T5.4): saldo protagonista, conteos animados,
 * bloques con entrada escalonada y cromo calmado (1–2 momentos con brillo).
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    onOpenSavings: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshMonth()
    }
    MiPatrimonioTheme {
        HomeContent(state = state, modifier = modifier, onOpenSavings = onOpenSavings)
    }
}

@Composable
fun HomeContent(
    state: HomeUiState,
    modifier: Modifier = Modifier,
    onOpenSavings: () -> Unit = {},
) {
    val summary = state.summary
    val month = summary?.month ?: YearMonth.now()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            ScreenTitle(text = stringResource(R.string.home_title))
            Text(
                text = formatMonth(month),
                color = NeonColor.muted,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }

        if (!state.loaded || summary == null) {
            Spacer(modifier = Modifier.height(24.dp))
            return@Column
        }

        var renderIndex = 0
        state.dashboardConfig.forEach { cfg ->
            if (!cfg.visible) return@forEach
            val willRender = when (cfg.block) {
                HomeBlock.BALANCE, HomeBlock.SAVINGS_PLAN, HomeBlock.BREAKDOWN, HomeBlock.RECENT -> true
                HomeBlock.ALERTS -> summary.budgets.isNotEmpty()
                HomeBlock.PROJECTION -> summary.projection?.visible == true
            }
            if (!willRender) return@forEach

            Spacer(modifier = Modifier.height(if (renderIndex == 0) 16.dp else 24.dp))
            val (alpha, offsetY) = rememberStaggeredEnter(index = renderIndex)
            val blockModifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    this.alpha = alpha
                    translationY = offsetY.toFloat()
                }
            when (cfg.block) {
                HomeBlock.BALANCE -> Box(blockModifier) { BalanceCard(summary) }
                HomeBlock.SAVINGS_PLAN -> Box(blockModifier) { ActiveSavingsPanel(plan = summary.activePlan, onOpenSavings = onOpenSavings) }
                HomeBlock.BREAKDOWN -> Box(blockModifier) { BreakdownCard(summary) }
                HomeBlock.ALERTS -> Box(blockModifier) { BudgetsCard(budgets = summary.budgets) }
                HomeBlock.PROJECTION -> Box(blockModifier) { ProjectionCard(projectedExpenseCents = summary.projection!!.projectedExpenseCents) }
                HomeBlock.RECENT -> Box(blockModifier) { RecentSection(recent = summary.recent) }
            }
            renderIndex++
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/** Tarjeta de saldo (§8.1): protagonista por cifra, cromo calmado (brillo contenido). */
@Composable
private fun BalanceCard(summary: MonthlySummary) {
    val shape = RoundedCornerShape(NeonShape.panelRadius)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = shape,
                clip = false,
                ambientColor = NeonColor.neonCyan.copy(alpha = 0.10f),
                spotColor = NeonColor.neonCyan.copy(alpha = 0.10f),
            )
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        NeonColor.balanceGradientStart,
                        NeonColor.balanceGradientEnd,
                    ),
                ),
                shape = shape,
            )
            .border(width = 1.dp, color = NeonColor.balanceBorder, shape = shape)
            .padding(20.dp),
    ) {
        Column {
            Text(
                text = stringResource(R.string.home_available),
                color = NeonColor.muted,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            AnimatedAmount(
                cents = summary.availableCents,
                signed = summary.availableCents < 0L,
                isExpense = summary.availableCents < 0L,
                color = NeonColor.neonCyan,
                style = MaterialTheme.typography.displayMedium,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniPanel(
                    label = stringResource(R.string.home_income),
                    amount = summary.incomeCents,
                    color = NeonColor.neonGreen,
                    modifier = Modifier.weight(1f),
                )
                MiniPanel(
                    label = stringResource(R.string.home_expenses),
                    amount = summary.expenseCents,
                    color = NeonColor.expenseRed,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun MiniPanel(
    label: String,
    amount: Long,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(NeonShape.inputRadius))
            .background(NeonColor.panelAlt.copy(alpha = 0.7f))
            .border(
                width = 1.dp,
                color = NeonColor.balanceBorder.copy(alpha = 0.45f),
                shape = RoundedCornerShape(NeonShape.inputRadius),
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            color = NeonColor.muted,
            style = MaterialTheme.typography.labelMedium,
        )
        Spacer(modifier = Modifier.height(2.dp))
        AnimatedAmount(
            cents = amount,
            color = color,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

/** Barra segmentada por tipo con leyenda (T5.4: cromo calmado, reveal suave). */
@Composable
private fun BreakdownCard(summary: MonthlySummary) {
    NeonCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.home_breakdown_title),
            color = NeonColor.ink,
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(12.dp))
        SegmentedBar(summary)
        Spacer(modifier = Modifier.height(14.dp))
        LegendRow(TransactionType.CASH, summary.cashCents)
        LegendRow(TransactionType.CARD, summary.cardCents)
        LegendRow(TransactionType.SUBSCRIPTION, summary.subscriptionCents)
        LegendRow(TransactionType.INCOME, summary.incomeCents)
    }
}

@Composable
private fun SegmentedBar(summary: MonthlySummary) {
    val segments = listOf(
        TransactionType.CASH,
        TransactionType.CARD,
        TransactionType.SUBSCRIPTION,
    )
    val weights = GetMonthlySummary.segmentWeights(
        cashCents = summary.cashCents,
        cardCents = summary.cardCents,
        subscriptionCents = summary.subscriptionCents,
    )
    val animationsEnabled = rememberAnimationsEnabled()
    val reveal = remember { Animatable(if (animationsEnabled) 0f else 1f) }
    LaunchedEffect(animationsEnabled) {
        if (!animationsEnabled) {
            reveal.snapTo(1f)
        } else {
            delay(staggerDelayMillis(1).toLong())
            reveal.animateTo(1f, animationSpec = tween(durationMillis = MotionDurations.Long))
        }
    }
    val shape = RoundedCornerShape(5.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .graphicsLayer { alpha = 0.6f + 0.4f * reveal.value },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        segments.forEachIndexed { index, type ->
            val color = type.accentColor()
            Box(
                modifier = Modifier
                    .weight(weights[index])
                    .fillMaxHeight()
                    .clip(shape)
                    .background(color.copy(alpha = 0.9f)),
            )
        }
    }
}

/** Proyección a fin de mes como bloque independiente (§8.1, T4.8). */
@Composable
private fun ProjectionCard(projectedExpenseCents: Long) {
    NeonCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.home_projection_label),
            color = NeonColor.muted,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(4.dp))
        AnimatedAmount(
            cents = projectedExpenseCents,
            color = NeonColor.ink,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@Composable
private fun LegendRow(type: TransactionType, amount: Long) {
    val color = type.accentColor()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(color),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = type.label(),
            color = NeonColor.muted,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        AnimatedAmount(
            cents = amount,
            color = color,
            style = MaterialTheme.typography.titleSmall,
        )
    }
}

/** Últimos movimientos (§8.1): barra vertical de color del tipo, fecha · tipo e importe. */
@Composable
private fun RecentSection(recent: List<TransactionEntity>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.home_recent_title),
            color = NeonColor.ink,
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (recent.isEmpty()) {
            EmptyState(message = stringResource(R.string.home_no_recent))
        } else {
            recent.forEach { transaction ->
                RecentRow(transaction)
            }
        }
    }
}

@Composable
private fun RecentRow(transaction: TransactionEntity) {
    val accent = transaction.type.accentColor()
    val isExpense = transaction.type != TransactionType.INCOME
    val shape = RoundedCornerShape(3.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(6.dp)
                .height(40.dp)
                .clip(shape)
                .background(accent),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.merchant.ifBlank {
                    stringResource(R.string.transaction_untitled)
                },
                color = NeonColor.ink,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(
                    R.string.home_recent_date_type,
                    formatShortDate(transaction.date),
                    transaction.type.label(),
                ),
                color = NeonColor.muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        AnimatedAmount(
            cents = transaction.amountCents,
            currency = transaction.currency,
            signed = true,
            isExpense = isExpense,
            color = accent,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

private fun formatMonth(month: YearMonth): String = month.atDay(1).format(MonthFormatter)

private fun formatShortDate(epochDay: Long): String =
    LocalDate.ofEpochDay(epochDay).format(ShortDateFormatter)

/** Presupuestos en Inicio (T3.4, T5.4): barras legibles, cromo calmado. */
@Composable
private fun BudgetsCard(budgets: List<HomeBudgetSummary>) {
    NeonCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.home_budgets_title),
            color = NeonColor.ink,
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(10.dp))
        budgets.forEachIndexed { index, b ->
            if (index > 0) Spacer(modifier = Modifier.height(10.dp))
            BudgetUrgencyRow(b)
        }
    }
}

@Composable
private fun BudgetUrgencyRow(b: HomeBudgetSummary) {
    val barColor = when (b.level) {
        BudgetLevel.OK -> NeonColor.neonCyan
        BudgetLevel.WARNING -> NeonColor.neonAmber
        BudgetLevel.OVER -> NeonColor.expenseRed
    }
    val progress = if (b.limitCents > 0L) {
        (b.spentCents.toDouble() / b.limitCents.toDouble()).toFloat().coerceIn(0f, 1f)
    } else 0f

    val animationsEnabled = rememberAnimationsEnabled()
    val urgentPulse = if (animationsEnabled && b.level != BudgetLevel.OK) {
        val t = rememberInfiniteTransition(label = "budgetUrgency")
        val p by t.animateFloat(
            initialValue = 0.75f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = if (b.level == BudgetLevel.OVER) 900 else 1200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "pulse",
        )
        p
    } else 1f

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = b.categoryName, color = NeonColor.ink, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(
                text = "${b.percent}% · ${Money.format(b.spentCents)}/${Money.format(b.limitCents)}",
                color = barColor,
                style = NeonType.tabular(MaterialTheme.typography.labelSmall),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        val shape = RoundedCornerShape(6.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(shape)
                .background(NeonColor.track),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(10.dp)
                    .clip(shape)
                    .background(barColor.copy(alpha = if (b.level == BudgetLevel.OK) 0.85f else urgentPulse)),
            )
        }
    }
}

/** Panel del plan activo en Inicio (§8.1, T3.1, T5.4): momento protagonista (glow contenido). */
@Composable
private fun ActiveSavingsPanel(plan: ActivePlanSummary?, onOpenSavings: () -> Unit) {
    if (plan == null) {
        NeonButton(onClick = onOpenSavings, modifier = Modifier.fillMaxWidth()) {
            NeonButtonText(text = stringResource(R.string.savings_create_plan))
        }
        return
    }
    val progress = if (plan.targetCents > 0L) {
        (plan.contributedCents.toDouble() / plan.targetCents.toDouble()).toFloat().coerceIn(0f, 1f)
    } else 0f
    val percent = (progress * 100f).roundToInt()

    NeonCard(
        glowColor = if (progress >= 1f) NeonColor.neonGreen.copy(alpha = 0.22f) else NeonColor.neonGreen.copy(alpha = 0.14f),
        modifier = Modifier.fillMaxWidth().clickable { onOpenSavings() },
    ) {
        Column(modifier = Modifier.padding(4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.home_savings_plan_active),
                    color = NeonColor.muted,
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = "$percent%",
                    color = NeonColor.neonCyan,
                    style = NeonType.tabular(MaterialTheme.typography.titleMedium),
                )
            }
            Text(text = plan.name, color = NeonColor.ink, style = MaterialTheme.typography.titleMedium)
            ProgressNeonBar(progress = progress, modifier = Modifier.fillMaxWidth())
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AnimatedAmount(cents = plan.contributedCents, color = NeonColor.ink, style = MaterialTheme.typography.bodySmall)
                    Text(text = " / ${Money.format(plan.targetCents)}", color = NeonColor.muted, style = NeonType.tabular(MaterialTheme.typography.bodySmall))
                }
                val monthsText = if (plan.estimate.goalReached) {
                    stringResource(R.string.savings_reaches_now)
                } else {
                    val tpl = if (plan.estimate.monthsEstimated == 1) R.string.home_savings_month_short else R.string.home_savings_months_short
                    stringResource(tpl, plan.estimate.monthsEstimated, plan.estimate.targetYearMonth.toMonthYearLabel())
                }
                Text(text = monthsText, color = NeonColor.muted, style = MaterialTheme.typography.bodySmall)
            }
            val (statusText, statusColor, statusBg) = when {
                plan.estimate.goalReached -> Triple(stringResource(R.string.savings_reaches_now), NeonColor.neonGreen, NeonColor.neonGreen.copy(alpha = 0.12f))
                plan.estimate.status == SavingsStatus.BEHIND -> Triple(stringResource(R.string.savings_status_behind), NeonColor.neonAmber, NeonColor.neonAmber.copy(alpha = 0.12f))
                plan.estimate.status == SavingsStatus.AHEAD -> Triple(stringResource(R.string.savings_status_ahead), NeonColor.neonGreen, NeonColor.neonGreen.copy(alpha = 0.12f))
                else -> Triple(stringResource(R.string.savings_status_on_track), NeonColor.neonGreen, NeonColor.neonGreen.copy(alpha = 0.12f))
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(NeonShape.chipRadius))
                    .background(statusBg)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(text = statusText, style = MaterialTheme.typography.bodySmall, color = statusColor)
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF070A12)
@Composable
private fun HomeContentPreview() {
    val month = YearMonth.of(2026, 11)
    val sample = listOf(
        sampleTransaction(
            id = 3,
            type = TransactionType.CARD,
            amountCents = 4599,
            merchant = "Librería Nova",
            day = 7,
        ),
        sampleTransaction(
            id = 2,
            type = TransactionType.INCOME,
            amountCents = 142400,
            merchant = "Nómina noviembre",
            day = 4,
        ),
        sampleTransaction(
            id = 1,
            type = TransactionType.CASH,
            amountCents = 1250,
            merchant = "Mercadona",
            day = 2,
        ),
    )
    MiPatrimonioTheme {
        HomeContent(
            state = HomeUiState(
                summary = MonthlySummary(
                    month = month,
                    incomeCents = 142400,
                    cashCents = 1250,
                    cardCents = 4599,
                    subscriptionCents = 999,
                    recent = sample,
                ),
                loaded = true,
            ),
        )
    }
}

private fun sampleTransaction(
    id: Long,
    type: TransactionType,
    amountCents: Long,
    merchant: String,
    day: Int,
): TransactionEntity = TransactionEntity(
    id = id,
    type = type,
    amountCents = amountCents,
    currency = "EUR",
    amountBaseCents = amountCents,
    date = LocalDate.of(2026, 11, day).toEpochDay(),
    merchant = merchant,
    createdAt = 0L,
)
