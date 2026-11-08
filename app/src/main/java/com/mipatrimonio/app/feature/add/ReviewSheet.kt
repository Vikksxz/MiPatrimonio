package com.mipatrimonio.app.feature.add

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mipatrimonio.app.R
import com.mipatrimonio.app.core.database.entity.CardEntity
import com.mipatrimonio.app.core.database.entity.CategoryEntity
import com.mipatrimonio.app.core.database.entity.TransactionEntity
import com.mipatrimonio.app.core.model.CardKind
import com.mipatrimonio.app.core.model.IncomeKind
import com.mipatrimonio.app.core.model.TransactionType
import com.mipatrimonio.app.core.money.Money
import com.mipatrimonio.app.core.ui.components.MovementChipType
import com.mipatrimonio.app.core.ui.components.NeonButton
import com.mipatrimonio.app.core.ui.components.NeonButtonText
import com.mipatrimonio.app.core.ui.components.NeonChip
import com.mipatrimonio.app.core.ui.components.ScreenTitle
import com.mipatrimonio.app.core.ui.theme.MiPatrimonioTheme
import com.mipatrimonio.app.core.ui.theme.NeonColor
import com.mipatrimonio.app.domain.usecase.NewTransaction
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ReviewDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es-ES"))

/**
 * Hoja de revisión del botón + (T1.7, MiPatrimonio.md §8.3).
 * Formulario común para gasto manual e ingreso. Se muestra dentro del
 * `ModalBottomSheet` que ya abre [AddMenuSheet].
 *
 * En T1.8 también se reutiliza como detalle/edición de un movimiento existente
 * ([editing] no nulo): precarga los datos, guarda con `UpdateTransaction` y
 * permite eliminarlo con confirmación.
 */
@Composable
fun ReviewSheet(
    initialType: TransactionType,
    onSaved: (Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    editing: TransactionEntity? = null,
    scanDraft: ReceiptScanDraft? = null,
    onDeleted: () -> Unit = {},
    viewModel: AddTransactionViewModel = hiltViewModel(),
) {
    val cards by viewModel.cards.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    LaunchedEffect(editing?.id, scanDraft?.thumbnail) {
        viewModel.reset()
    }

    ReviewContent(
        initialType = initialType,
        editing = editing,
        scanDraft = scanDraft,
        cards = cards,
        categories = categories,
        saving = viewModel.saving,
        saveFailed = viewModel.saveFailed,
        onSave = { input ->
            when {
                editing != null -> viewModel.update(editing, input, onSaved)
                scanDraft != null -> viewModel.saveScanned(input, scanDraft.sourceBitmap, onSaved)
                else -> viewModel.save(input, onSaved)
            }
        },
        onDelete = if (editing == null) {
            null
        } else {
            { viewModel.delete(editing, onDeleted) }
        },
        onDismiss = onDismiss,
        modifier = modifier,
    )
}

/** Formulario sin ViewModel, para previsualizar. */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ReviewContent(
    initialType: TransactionType,
    cards: List<CardEntity>,
    categories: List<CategoryEntity>,
    saving: Boolean,
    saveFailed: Boolean,
    onSave: (NewTransaction) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    editing: TransactionEntity? = null,
    scanDraft: ReceiptScanDraft? = null,
    onDelete: (() -> Unit)? = null,
) {
    val state = remember(initialType, editing?.id, scanDraft?.thumbnail) {
        ReviewState(initialType, editing, scanDraft)
    }
    val visibleCategories = categories.filter {
        it.isIncome == (state.type == TransactionType.INCOME)
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val fromOcr = scanDraft != null && editing == null

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenTitle(
            text = stringResource(
                when {
                    editing != null -> R.string.review_title_edit
                    state.type == TransactionType.INCOME -> R.string.review_title_income
                    else -> R.string.review_title_expense
                },
            ),
        )

        if (fromOcr) {
            Text(
                text = stringResource(R.string.scan_read_hint),
                color = NeonColor.neonCyan,
                style = MaterialTheme.typography.bodyMedium,
            )
            scanDraft?.thumbnail?.let { thumb ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.scan_attachment),
                        color = NeonColor.muted,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Image(
                        bitmap = thumb.asImageBitmap(),
                        contentDescription = stringResource(R.string.scan_attachment),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(NeonColor.bg),
                    )
                }
            }
        }

        FormSection(title = stringResource(R.string.review_type)) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    TransactionType.CASH to R.string.type_cash,
                    TransactionType.CARD to R.string.type_card,
                    TransactionType.SUBSCRIPTION to R.string.type_subscription,
                    TransactionType.INCOME to R.string.type_income,
                ).forEach { (type, labelRes) ->
                    NeonChip(
                        text = stringResource(labelRes),
                        type = type.chipType(),
                        selected = state.type == type,
                        onClick = {
                            val changesIncome =
                                (state.type == TransactionType.INCOME) != (type == TransactionType.INCOME)
                            state.type = type
                            if (changesIncome) state.categoryId = null
                            if (type != TransactionType.CARD) state.cardId = null
                            if (type != TransactionType.INCOME) state.showIncomeKindError = false
                        },
                    )
                }
            }
        }

        NeonTextField(
            value = state.amountText,
            onValueChange = {
                state.amountText = it
                state.showAmountError = false
            },
            label = stringResource(R.string.review_amount),
            isError = state.showAmountError,
            keyboardType = KeyboardType.Decimal,
        )
        if (state.showAmountError) {
            Text(
                text = stringResource(R.string.review_error_amount),
                color = NeonColor.expenseRed,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        NeonTextField(
            value = state.merchant,
            onValueChange = { state.merchant = it },
            label = stringResource(R.string.review_merchant),
        )

        FormSection(title = stringResource(R.string.review_date)) {
            NeonButton(
                onClick = { showDatePicker = true },
                secondary = true,
                modifier = Modifier.fillMaxWidth(),
            ) {
                NeonButtonText(
                    text = state.date.format(ReviewDateFormatter),
                    secondary = true,
                )
            }
        }

        if (state.type == TransactionType.CARD) {
            FormSection(title = stringResource(R.string.review_card)) {
                if (cards.isEmpty()) {
                    Text(
                        text = stringResource(R.string.review_no_cards),
                        color = NeonColor.muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        NeonChip(
                            text = stringResource(R.string.review_card_none),
                            type = MovementChipType.CARD,
                            selected = state.cardId == null,
                            onClick = { state.cardId = null },
                        )
                        cards.forEach { card ->
                            NeonChip(
                                text = card.alias.ifBlank { card.bank },
                                type = MovementChipType.CARD,
                                selected = state.cardId == card.id,
                                onClick = { state.cardId = card.id },
                            )
                        }
                    }
                }
            }
        }

        if (state.type == TransactionType.INCOME) {
            FormSection(title = stringResource(R.string.income_kind_title)) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        IncomeKind.PAYROLL to R.string.income_kind_payroll,
                        IncomeKind.OTHER to R.string.income_kind_other,
                    ).forEach { (kind, labelRes) ->
                        NeonChip(
                            text = stringResource(labelRes),
                            type = MovementChipType.INCOME,
                            selected = state.incomeKind == kind,
                            onClick = {
                                state.incomeKind = kind
                                state.showIncomeKindError = false
                            },
                        )
                    }
                }
                if (state.showIncomeKindError) {
                    Text(
                        text = stringResource(R.string.review_error_income_kind),
                        color = NeonColor.expenseRed,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        FormSection(title = stringResource(R.string.review_category)) {
            if (visibleCategories.isEmpty()) {
                Text(
                    text = stringResource(R.string.review_no_categories),
                    color = NeonColor.muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    visibleCategories.forEach { category ->
                        val accent = categoryColor(category.colorHex)
                        NeonChip(
                            text = category.name,
                            accent = accent,
                            tint = accent.copy(alpha = 0.14f),
                            labelColor = accent,
                            selected = state.categoryId == category.id,
                            onClick = { state.categoryId = category.id },
                        )
                    }
                }
            }
        }

        NeonTextField(
            value = state.note,
            onValueChange = { state.note = it },
            label = stringResource(R.string.review_note),
            singleLine = false,
            minLines = 2,
        )

        NeonButton(
            onClick = {
                if (saving) return@NeonButton
                val amountCents = parseAmount(state.amountText)
                if (amountCents == null) {
                    state.showAmountError = true
                } else if (state.type == TransactionType.INCOME && state.incomeKind == null) {
                    state.showIncomeKindError = true
                } else {
                    onSave(
                        NewTransaction(
                            type = state.type,
                            amountCents = amountCents,
                            merchant = state.merchant,
                            date = state.date,
                            incomeKind = if (state.type == TransactionType.INCOME) {
                                state.incomeKind
                            } else {
                                null
                            },
                            categoryId = state.categoryId,
                            cardId = if (state.type == TransactionType.CARD) state.cardId else null,
                            note = state.note,
                            receiptPath = state.receiptPath,
                        ),
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            NeonButtonText(
                text = stringResource(
                    if (editing == null) R.string.review_save else R.string.review_save_edit,
                ),
            )
        }

        if (saveFailed) {
            Text(
                text = stringResource(R.string.review_save_failed),
                color = NeonColor.expenseRed,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        if (onDelete != null) {
            TextButton(
                onClick = { if (!saving) showDeleteDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp),
            ) {
                Text(
                    text = stringResource(R.string.review_delete),
                    color = NeonColor.expenseRed,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }

        TextButton(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp),
        ) {
            Text(
                text = stringResource(R.string.common_cancel),
                color = NeonColor.muted,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }

    if (showDeleteDialog && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = NeonColor.panel,
            title = {
                Text(
                    text = stringResource(R.string.review_delete),
                    color = NeonColor.ink,
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.review_delete_confirm),
                    color = NeonColor.muted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                ) {
                    Text(stringResource(R.string.review_delete), color = NeonColor.expenseRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.common_cancel), color = NeonColor.muted)
                }
            },
        )
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.date
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            state.date = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                        }
                        showDatePicker = false
                    },
                ) {
                    Text(stringResource(R.string.common_accept), color = NeonColor.neonCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.common_cancel), color = NeonColor.muted)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = NeonColor.panel),
        ) {
            DatePicker(
                state = pickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = NeonColor.panel,
                    selectedDayContainerColor = NeonColor.neonCyan,
                    selectedDayContentColor = NeonColor.bg,
                    todayContentColor = NeonColor.neonCyan,
                    todayDateBorderColor = NeonColor.neonCyan,
                    weekdayContentColor = NeonColor.muted,
                ),
            )
        }
    }
}

@Stable
private class ReviewState(
    initialType: TransactionType,
    editing: TransactionEntity? = null,
    scanDraft: ReceiptScanDraft? = null,
) {
    var type by mutableStateOf(editing?.type ?: initialType)
    var amountText by mutableStateOf(
        editing?.let { Money.formatNumber(it.amountCents) }
            ?: scanDraft?.amountCents?.let { Money.formatNumber(it) }
            ?: "",
    )
    var merchant by mutableStateOf(editing?.merchant ?: scanDraft?.merchant.orEmpty())
    var date by mutableStateOf(
        editing?.let { LocalDate.ofEpochDay(it.date) } ?: scanDraft?.date ?: LocalDate.now(),
    )
    var categoryId by mutableStateOf(editing?.categoryId)
    var cardId by mutableStateOf(editing?.cardId)
    var note by mutableStateOf(editing?.note ?: "")
    var incomeKind by mutableStateOf(editing?.incomeKind)
    // En escaneo el ticket se cifra al guardar (saveScanned); en edición se conserva.
    val receiptPath: String? = editing?.receiptPath
    var showAmountError by mutableStateOf(false)
    var showIncomeKindError by mutableStateOf(false)
}

@Composable
private fun FormSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            color = NeonColor.muted,
            style = MaterialTheme.typography.labelMedium,
        )
        content()
    }
}

@Composable
private fun NeonTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label) },
        singleLine = singleLine,
        minLines = minLines,
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NeonColor.neonCyan,
            unfocusedBorderColor = NeonColor.lineInput,
            focusedLabelColor = NeonColor.neonCyan,
            unfocusedLabelColor = NeonColor.muted,
            focusedTextColor = NeonColor.ink,
            unfocusedTextColor = NeonColor.ink,
            cursorColor = NeonColor.neonCyan,
        ),
    )
}

private fun TransactionType.chipType(): MovementChipType = when (this) {
    TransactionType.CASH -> MovementChipType.CASH
    TransactionType.CARD -> MovementChipType.CARD
    TransactionType.SUBSCRIPTION -> MovementChipType.SUBSCRIPTION
    TransactionType.INCOME -> MovementChipType.INCOME
}

private fun parseAmount(text: String): Long? =
    runCatching { Money.parse(text) }.getOrNull()?.takeIf { it > 0 }

private fun categoryColor(hex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }
        .getOrDefault(NeonColor.neonCyan)

private fun sampleCategory(id: Long, name: String, colorHex: String, isIncome: Boolean) =
    CategoryEntity(
        id = id,
        name = name,
        colorHex = colorHex,
        icon = "category",
        isIncome = isIncome,
        isFixed = false,
        isDefault = false,
    )

@Preview(showBackground = true, backgroundColor = 0xFF0E1424)
@Composable
private fun ReviewContentPreview() {
    MiPatrimonioTheme {
        ReviewContent(
            initialType = TransactionType.CARD,
            cards = listOf(
                CardEntity(
                    id = 1L,
                    bank = "BBVA",
                    variant = "",
                    kind = CardKind.DEBIT,
                    alias = "Mi tarjeta",
                    theme = "cyan",
                    photoPath = null,
                    createdAt = 0L,
                ),
            ),
            categories = listOf(
                sampleCategory(1L, "Alimentación", "#FF6B9D", false),
                sampleCategory(2L, "Transporte", "#22D3EE", false),
                sampleCategory(3L, "Nómina", "#39FF88", true),
            ),
            saving = false,
            saveFailed = false,
            onSave = {},
            onDismiss = {},
        )
    }
}
