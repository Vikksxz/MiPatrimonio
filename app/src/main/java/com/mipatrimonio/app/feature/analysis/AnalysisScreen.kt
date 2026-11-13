package com.mipatrimonio.app.feature.analysis

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mipatrimonio.app.R
import com.mipatrimonio.app.core.money.Money
import com.mipatrimonio.app.core.ui.components.AnimatedAmount
import com.mipatrimonio.app.core.ui.components.NeonCard
import com.mipatrimonio.app.core.ui.components.ScreenTitle
import com.mipatrimonio.app.core.ui.motion.MotionDurations
import com.mipatrimonio.app.core.ui.motion.rememberAnimationsEnabled
import com.mipatrimonio.app.core.ui.theme.MiPatrimonioTheme
import com.mipatrimonio.app.core.ui.theme.NeonColor
import com.mipatrimonio.app.core.ui.theme.NeonType
import com.mipatrimonio.app.domain.usecase.CategoryExpense
import com.mipatrimonio.app.domain.usecase.FixedVariableSummary
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun AnalysisScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AnalysisViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MiPatrimonioTheme {
        AnalysisContent(state = state, onBack = onBack, modifier = modifier)
    }
}

@Composable
fun AnalysisContent(
    state: AnalysisUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.loading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = NeonColor.neonCyan)
        }
        return
    }
    val locale = Locale.forLanguageTag("es-ES")
    val curLabel = state.currentMonth.month.getDisplayName(TextStyle.FULL, locale).replaceFirstChar { it.uppercase(locale) } + " " + state.currentMonth.year
    val prevLabel = state.previousMonth.month.getDisplayName(TextStyle.FULL, locale).replaceFirstChar { it.uppercase(locale) } + " " + state.previousMonth.year
    val hasPreviousToCompare = state.previousTotals.incomeCents != 0L || state.previousTotals.expenseCents != 0L

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.analysis_back), color = NeonColor.neonCyan)
            }
            ScreenTitle(text = stringResource(R.string.analysis_title))
        }

        NeonCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.analysis_comparison), color = NeonColor.ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.analysis_total_expenses),
                        color = NeonColor.muted,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    AnimatedAmount(
                        cents = state.currentTotals.expenseCents,
                        isExpense = true,
                        color = NeonColor.ink,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    )
                    Text(curLabel, color = NeonColor.muted, style = MaterialTheme.typography.bodySmall)
                }

                if (hasPreviousToCompare) {
                    ComparisonRow(curLabel, state.currentTotals.incomeCents, state.currentTotals.expenseCents)
                    ComparisonRow(prevLabel, state.previousTotals.incomeCents, state.previousTotals.expenseCents)
                    val diff = state.currentTotals.expenseCents - state.previousTotals.expenseCents
                    val diffColor = when {
                        diff > 0 -> NeonColor.neonAmber
                        diff < 0 -> NeonColor.neonGreen
                        else -> NeonColor.muted
                    }
                    Text(
                        text = stringResource(R.string.analysis_difference) + ": " + (if (diff > 0) "+" else "") + Money.format(diff),
                        color = diffColor,
                        style = NeonType.tabular(MaterialTheme.typography.bodyMedium),
                        fontWeight = FontWeight.Medium,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(R.string.analysis_no_compare),
                            color = NeonColor.ink,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = stringResource(R.string.analysis_no_compare_hint),
                            color = NeonColor.muted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }

        NeonCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.analysis_by_category), color = NeonColor.ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (state.categories.isEmpty()) {
                    Text(stringResource(R.string.analysis_no_expenses), color = NeonColor.muted, style = MaterialTheme.typography.bodySmall)
                } else {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            DonutChart(categories = state.categories, modifier = Modifier.size(140.dp))
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            state.categories.take(8).forEach { cat ->
                                CategoryLegendRow(cat)
                            }
                        }
                    }
                }
            }
        }

        state.fixedVariable?.let { fv ->
            NeonCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.analysis_fixed_variable), color = NeonColor.ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    FixedVariableRow(stringResource(R.string.analysis_income), fv.incomeCents, NeonColor.neonGreen)
                    FixedVariableRow(stringResource(R.string.analysis_fixed), fv.fixedCents, NeonColor.neonAmber)
                    FixedVariableRow(stringResource(R.string.analysis_variable), fv.variableCents, NeonColor.neonCyan)
                    FixedVariableRow(
                        stringResource(R.string.analysis_free_at_month_end),
                        fv.freeAtMonthEndCents,
                        if (fv.freeAtMonthEndCents >= 0) NeonColor.neonGreen else NeonColor.expenseRed,
                        emphasize = true,
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ComparisonRow(label: String, income: Long, expense: Long) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = NeonColor.muted, style = MaterialTheme.typography.labelLarge)
        Text(
            "${stringResource(R.string.analysis_income)}: ${Money.format(income)} · ${stringResource(R.string.analysis_expenses)}: ${Money.format(expense)}",
            color = NeonColor.ink,
            style = NeonType.tabular(MaterialTheme.typography.bodySmall),
        )
    }
}

@Composable
private fun DonutChart(categories: List<CategoryExpense>, modifier: Modifier = Modifier) {
    val animationsEnabled = rememberAnimationsEnabled()
    var started by remember { mutableStateOf(!animationsEnabled) }
    LaunchedEffect(categories) {
        if (animationsEnabled) {
            started = false
            started = true
        } else {
            started = true
        }
    }
    val progress by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(durationMillis = if (animationsEnabled) 700 else 0),
        label = "donutProgress",
    )
    val total = categories.sumOf { it.amountCents }.coerceAtLeast(1L)
    Canvas(modifier = modifier) {
        val stroke = size.minDimension * 0.22f
        var start = -90f
        categories.forEach { cat ->
            val sweep = (cat.amountCents.toFloat() / total.toFloat()) * 360f * progress
            val color = parseHexColor(cat.colorHex)
            drawArc(
                color = color,
                startAngle = start,
                sweepAngle = sweep.coerceAtMost(360f * progress),
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Butt),
            )
            start += (cat.amountCents.toFloat() / total.toFloat()) * 360f
        }
    }
}

@Composable
private fun CategoryLegendRow(cat: CategoryExpense) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(parseHexColor(cat.colorHex)))
        Spacer(Modifier.width(8.dp))
        Text(cat.categoryName, color = NeonColor.ink, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1)
        Text(
            Money.format(cat.amountCents),
            color = NeonColor.muted,
            style = NeonType.tabular(MaterialTheme.typography.bodySmall),
        )
    }
}

@Composable
private fun FixedVariableRow(label: String, amount: Long, color: Color, emphasize: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = NeonColor.muted, style = MaterialTheme.typography.bodyMedium)
        Text(
            Money.formatSigned(amount, amount < 0),
            color = color,
            style = NeonType.tabular((if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium)),
            fontWeight = if (emphasize) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

private fun parseHexColor(hex: String): Color {
    return try {
        val clean = hex.removePrefix("#")
        val argb = when (clean.length) {
            6 -> 0xFF000000 or clean.toLong(16)
            8 -> clean.toLong(16)
            else -> 0xFF8C97D8
        }
        Color(argb.toInt())
    } catch (_: Exception) {
        Color(0xFF8C97D8)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF070A12)
@Composable
private fun AnalysisContentPreview() {
    MiPatrimonioTheme {
        AnalysisContent(
            state = AnalysisUiState(
                loading = false,
                currentTotals = MonthTotals(300000, 52200),
                previousTotals = MonthTotals(300000, 48000),
                categories = listOf(
                    CategoryExpense(1, "Comida", "#3CFFD0", 20000),
                    CategoryExpense(2, "Ocio", "#FF3DCE", 15000),
                ),
                fixedVariable = FixedVariableSummary(300000, 52200, 20000, 32200, 5000, 242800),
            ),
            onBack = {},
        )
    }
}
