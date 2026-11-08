package com.mipatrimonio.app.feature.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.mipatrimonio.app.core.ui.components.MovementChipType
import com.mipatrimonio.app.core.ui.components.NeonCard
import com.mipatrimonio.app.core.ui.components.NeonChip
import com.mipatrimonio.app.core.ui.components.ScreenTitle
import com.mipatrimonio.app.core.ui.components.accentColor
import com.mipatrimonio.app.core.ui.components.label
import com.mipatrimonio.app.core.ui.theme.MiPatrimonioTheme
import com.mipatrimonio.app.core.ui.theme.NeonColor
import com.mipatrimonio.app.feature.add.ReviewSheet
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

private val DayFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE d MMM yyyy", Locale.forLanguageTag("es-ES"))

private val FilterTypes = listOf(
    TransactionType.CASH,
    TransactionType.CARD,
    TransactionType.SUBSCRIPTION,
    TransactionType.INCOME,
)

/**
 * Movimientos (T1.8, MiPatrimonio.md §8.2): buscador, filtros por tipo, lista
 * agrupada por día, edición al tocar un movimiento y deslizar para borrar con
 * deshacer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    modifier: Modifier = Modifier,
    viewModel: TransactionsViewModel = hiltViewModel(),
    onOpenAnalysis: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<TransactionEntity?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    MiPatrimonioTheme {
        TransactionsContent(
            state = state,
            actionFailed = viewModel.actionFailed,
            modifier = modifier,
            onActionFailureShown = viewModel::consumeActionFailed,
            onQueryChange = viewModel::setQuery,
            onTypeFilterChange = viewModel::setTypeFilter,
            onTagFilterChange = viewModel::setTagFilter,
            onEdit = { editing = it },
            onDelete = viewModel::delete,
            onRestore = viewModel::restore,
            onOpenAnalysis = onOpenAnalysis,
        )

        val current = editing
        if (current != null) {
            ModalBottomSheet(
                onDismissRequest = { editing = null },
                sheetState = sheetState,
                containerColor = NeonColor.panel,
                contentColor = NeonColor.ink,
                scrimColor = NeonColor.bg.copy(alpha = 0.72f),
            ) {
                ReviewSheet(
                    initialType = current.type,
                    editing = current,
                    onSaved = { editing = null },
                    onDeleted = { editing = null },
                    onDismiss = { editing = null },
                )
            }
        }
    }
}

@Composable
fun TransactionsContent(
    state: TransactionsUiState,
    modifier: Modifier = Modifier,
    actionFailed: Boolean = false,
    onActionFailureShown: () -> Unit = {},
    onQueryChange: (String) -> Unit = {},
    onTypeFilterChange: (TransactionType?) -> Unit = {},
    onTagFilterChange: (Long?) -> Unit = {},
    onEdit: (TransactionEntity) -> Unit = {},
    onDelete: (TransactionEntity) -> Unit = {},
    onRestore: (TransactionEntity) -> Unit = {},
    onOpenAnalysis: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val deletedMessage = stringResource(R.string.transaction_deleted)
    val undoLabel = stringResource(R.string.common_undo)
    val actionFailedMessage = stringResource(R.string.transactions_action_failed)

    LaunchedEffect(actionFailed) {
        if (actionFailed) {
            snackbarHostState.showSnackbar(actionFailedMessage)
            onActionFailureShown()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ScreenTitle(text = stringResource(R.string.tab_transactions))
                TextButton(onClick = onOpenAnalysis) {
                    Text(stringResource(R.string.transactions_open_analysis), color = NeonColor.neonCyan)
                }
            }

            SearchField(
                query = state.query,
                onQueryChange = onQueryChange,
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item(key = "filter-all") {
                    NeonChip(
                        text = stringResource(R.string.transactions_filter_all),
                        accent = NeonColor.neonCyan,
                        tint = NeonColor.neonCyan.copy(alpha = 0.14f),
                        labelColor = NeonColor.neonCyan,
                        selected = state.typeFilter == null,
                        onClick = { onTypeFilterChange(null) },
                    )
                }
                items(items = FilterTypes, key = { it.name }) { type ->
                    NeonChip(
                        text = type.label(),
                        type = type.chipType(),
                        selected = state.typeFilter == type,
                        onClick = {
                            onTypeFilterChange(if (state.typeFilter == type) null else type)
                        },
                    )
                }
            }

            if (state.availableTags.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item(key = "tag-all") {
                        NeonChip(
                            text = stringResource(R.string.transactions_filter_tag_all),
                            accent = NeonColor.neonCyan,
                            tint = NeonColor.neonCyan.copy(alpha = 0.14f),
                            labelColor = NeonColor.neonCyan,
                            selected = state.tagFilterId == null,
                            onClick = { onTagFilterChange(null) },
                        )
                    }
                    items(items = state.availableTags, key = { "tag-${it.id}" }) { tag ->
                        val selected = state.tagFilterId == tag.id
                        NeonChip(
                            text = tag.name,
                            accent = if (selected) NeonColor.neonCyan else NeonColor.muted,
                            tint = if (selected) NeonColor.neonCyan.copy(alpha = 0.18f) else NeonColor.panelAlt,
                            labelColor = if (selected) NeonColor.neonCyan else NeonColor.chipText,
                            selected = selected,
                            onClick = { onTagFilterChange(if (selected) null else tag.id) },
                        )
                    }
                }
            }

            when {
                !state.loaded -> Spacer(modifier = Modifier.weight(1f))
                state.groups.isEmpty() && !state.hasAnyTransactions -> EmptyState(
                    modifier = Modifier.weight(1f),
                )
                state.groups.isEmpty() -> NoResultsState(modifier = Modifier.weight(1f))
                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    state.groups.forEach { group ->
                        item(key = "day-${group.epochDay}") {
                            Text(
                                text = formatDay(group.epochDay),
                                color = NeonColor.muted,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                            )
                        }
                        items(items = group.transactions, key = { it.id }) { transaction ->
                            SwipeableTransactionRow(
                                transaction = transaction,
                                onClick = { onEdit(transaction) },
                                onDelete = {
                                    onDelete(transaction)
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = deletedMessage,
                                            actionLabel = undoLabel,
                                            duration = SnackbarDuration.Long,
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            onRestore(transaction)
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = NeonColor.panelAlt,
                contentColor = NeonColor.ink,
                actionContentColor = NeonColor.neonCyan,
                dismissActionContentColor = NeonColor.muted,
            )
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = {
            Text(
                text = stringResource(R.string.transactions_search_hint),
                color = NeonColor.muted,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = NeonColor.muted,
            )
        },
        trailingIcon = if (query.isEmpty()) {
            null
        } else {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.transactions_search_clear),
                        tint = NeonColor.muted,
                    )
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NeonColor.neonCyan,
            unfocusedBorderColor = NeonColor.lineInput,
            focusedTextColor = NeonColor.ink,
            unfocusedTextColor = NeonColor.ink,
            cursorColor = NeonColor.neonCyan,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableTransactionRow(
    transaction: TransactionEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState()

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = { DeleteBackground() },
        onDismiss = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) onDelete()
        },
    ) {
        TransactionRow(transaction = transaction, onClick = onClick)
    }
}

@Composable
private fun RowScope.DeleteBackground() {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(NeonColor.expenseRed.copy(alpha = 0.18f))
            .padding(end = 20.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Delete,
            // Decorativo: la acción de borrar ya se anuncia desde la fila y la hoja.
            contentDescription = null,
            tint = NeonColor.expenseRed,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun TransactionRow(
    transaction: TransactionEntity,
    onClick: () -> Unit,
) {
    val accent = transaction.type.accentColor()
    val isExpense = transaction.type != TransactionType.INCOME

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
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
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
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
}

@Composable
private fun NoResultsState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        NeonCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.transactions_no_results),
                color = NeonColor.ink,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.transactions_no_results_hint),
                color = NeonColor.muted,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

private fun TransactionType.chipType(): MovementChipType = when (this) {
    TransactionType.CASH -> MovementChipType.CASH
    TransactionType.CARD -> MovementChipType.CARD
    TransactionType.SUBSCRIPTION -> MovementChipType.SUBSCRIPTION
    TransactionType.INCOME -> MovementChipType.INCOME
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
    val sample = listOf(
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
    )
    MiPatrimonioTheme {
        TransactionsContent(
            state = TransactionsUiState(
                groups = TransactionsViewModel.buildDayGroups(sample, "", null),
                hasAnyTransactions = true,
                loaded = true,
            ),
        )
    }
}
