package com.mipatrimonio.app.feature.transactions

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mipatrimonio.app.R
import com.mipatrimonio.app.core.database.entity.TransactionEntity
import com.mipatrimonio.app.core.model.IncomeKind
import com.mipatrimonio.app.core.model.TransactionType
import com.mipatrimonio.app.core.money.Money
import com.mipatrimonio.app.core.ui.components.NeonCard
import com.mipatrimonio.app.core.ui.components.ScreenTitle
import com.mipatrimonio.app.core.ui.theme.MiPatrimonioTheme
import com.mipatrimonio.app.core.ui.theme.NeonColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DayFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE d MMM yyyy", Locale.forLanguageTag("es-ES"))

/**
 * Movimientos (T1.7). Lista agrupada por día con el color del tipo (§8.1/§8.2).
 * En T1.8 se añaden buscador, filtros, detalle y deslizar para borrar.
 */
@Composable
fun TransactionsScreen(
    modifier: Modifier = Modifier,
    viewModel: TransactionsViewModel = hiltViewModel(),
) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()

    MiPatrimonioTheme {
        TransactionsContent(transactions = transactions, modifier = modifier)
    }
}

@Composable
fun TransactionsContent(
    transactions: List<TransactionEntity>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenTitle(text = stringResource(R.string.tab_transactions))

        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                NeonCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.transactions_empty),
                        color = NeonColor.ink,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.transactions_empty_hint),
                        color = NeonColor.muted,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        } else {
            val groups = remember(transactions) { transactions.groupBy { it.date } }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                groups.forEach { (day, dayTransactions) ->
                    item(key = "day-$day") {
                        Text(
                            text = formatDay(day),
                            color = NeonColor.muted,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                        )
                    }
                    items(items = dayTransactions, key = { it.id }) { transaction ->
                        TransactionRow(transaction = transaction)
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(transaction: TransactionEntity) {
    val accent = transaction.type.accentColor()
    val isExpense = transaction.type != TransactionType.INCOME

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
                .clip(RoundedCornerShape(3.dp))
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
                text = transaction.type.label(),
                color = NeonColor.muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = Money.formatSigned(transaction.amountCents, isExpense = isExpense),
            color = accent,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun TransactionType.label(): String = stringResource(
    when (this) {
        TransactionType.CASH -> R.string.type_cash
        TransactionType.CARD -> R.string.type_card
        TransactionType.SUBSCRIPTION -> R.string.type_subscription
        TransactionType.INCOME -> R.string.type_income
    },
)

private fun TransactionType.accentColor(): Color = when (this) {
    TransactionType.CASH -> NeonColor.MovementType.cashAccent
    TransactionType.CARD -> NeonColor.MovementType.cardAccent
    TransactionType.SUBSCRIPTION -> NeonColor.MovementType.subscriptionAccent
    TransactionType.INCOME -> NeonColor.MovementType.incomeAccent
}

private fun formatDay(epochDay: Long): String {
    val text = LocalDate.ofEpochDay(epochDay).format(DayFormatter)
    return text.replaceFirstChar { char ->
        if (char.isLowerCase()) char.titlecase(Locale.forLanguageTag("es-ES")) else char.toString()
    }
}

private fun sampleTransaction(
    id: Long,
    type: TransactionType,
    amountCents: Long,
    merchant: String,
    date: LocalDate,
): TransactionEntity = TransactionEntity(
    id = id,
    type = type,
    incomeKind = if (type == TransactionType.INCOME) IncomeKind.OTHER else null,
    amountCents = amountCents,
    amountBaseCents = amountCents,
    date = date.toEpochDay(),
    merchant = merchant,
    createdAt = 0L,
)

@Preview(showBackground = true, backgroundColor = 0xFF070A12)
@Composable
private fun TransactionsContentPreview() {
    MiPatrimonioTheme {
        TransactionsContent(
            transactions = listOf(
                sampleTransaction(
                    id = 1L,
                    type = TransactionType.CARD,
                    amountCents = 4_250,
                    merchant = "Mercadona",
                    date = LocalDate.of(2026, 11, 6),
                ),
                sampleTransaction(
                    id = 2L,
                    type = TransactionType.INCOME,
                    amountCents = 250_000,
                    merchant = "Nómina",
                    date = LocalDate.of(2026, 11, 1),
                ),
            ),
        )
    }
}
