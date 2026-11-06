package com.mipatrimonio.app.feature.wallet

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mipatrimonio.app.R
import com.mipatrimonio.app.core.database.entity.CardEntity
import com.mipatrimonio.app.core.model.CardKind
import com.mipatrimonio.app.core.ui.components.NeonButton
import com.mipatrimonio.app.core.ui.components.NeonButtonText
import com.mipatrimonio.app.core.ui.components.NeonCard
import com.mipatrimonio.app.core.ui.components.NeonChip
import com.mipatrimonio.app.core.ui.components.ScreenTitle
import com.mipatrimonio.app.core.ui.theme.MiPatrimonioTheme
import com.mipatrimonio.app.core.ui.theme.NeonColor
import com.mipatrimonio.app.core.ui.theme.NeonShape
import com.mipatrimonio.app.core.ui.theme.NeonType

/**
 * Cartera · Tarjetas (T1.6, MiPatrimonio.md §8.5).
 * Alta, edición, borrado con confirmación y foto propia en almacenamiento privado.
 */
@Composable
fun WalletScreen(
    viewModel: WalletViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val cards by viewModel.cards.collectAsStateWithLifecycle()
    var showForm by remember { mutableStateOf(false) }
    var editingCard by remember { mutableStateOf<CardEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<CardEntity?>(null) }

    MiPatrimonioTheme {
        WalletContent(
            cards = cards,
            onAdd = {
                editingCard = null
                showForm = true
            },
            onEdit = { card ->
                editingCard = card
                showForm = true
            },
            modifier = modifier,
        )

        if (showForm) {
            CardFormSheet(
                card = editingCard,
                onDismiss = {
                    showForm = false
                    editingCard = null
                },
                onSave = { card, obsoletePhotoPath ->
                    viewModel.saveCard(card, obsoletePhotoPath)
                    showForm = false
                    editingCard = null
                },
                onDelete = { card ->
                    deleteTarget = card
                },
            )
        }

        deleteTarget?.let { card ->
            AlertDialog(
                onDismissRequest = { deleteTarget = null },
                containerColor = NeonColor.panel,
                titleContentColor = NeonColor.ink,
                textContentColor = NeonColor.muted,
                title = { Text(stringResource(R.string.card_form_delete)) },
                text = { Text(stringResource(R.string.card_form_delete_confirm)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteCard(card)
                            deleteTarget = null
                            showForm = false
                            editingCard = null
                        },
                    ) {
                        Text(stringResource(R.string.card_form_delete), color = NeonColor.expenseRed)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deleteTarget = null }) {
                        Text(stringResource(R.string.common_cancel), color = NeonColor.neonCyan)
                    }
                },
            )
        }
    }
}

/** Contenido sin estado, para previsualizar sin ViewModel. */
@Composable
fun WalletContent(
    cards: List<CardEntity>,
    onAdd: () -> Unit,
    onEdit: (CardEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenTitle(text = stringResource(R.string.wallet_title))

        Text(
            text = stringResource(R.string.wallet_empty_text),
            color = NeonColor.muted,
            fontSize = NeonType.secondary,
        )

        if (cards.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                NeonCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.wallet_empty_title),
                        color = NeonColor.muted,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items = cards, key = { it.id }) { card ->
                    CardListItem(card = card, onClick = { onEdit(card) })
                }
            }
        }

        DashedAddCardButton(
            text = stringResource(R.string.wallet_add_card),
            onClick = onAdd,
            modifier = Modifier.padding(bottom = 16.dp),
        )
    }
}

@Composable
private fun CardListItem(card: CardEntity, onClick: () -> Unit) {
    val actionLabel = stringResource(R.string.card_item_action, card.alias)
    CardPreview(
        card = card,
        design = CardDesign.getById(card.theme),
        height = 180,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = actionLabel, role = Role.Button, onClick = onClick),
    )
}

/** Botón "Añadir tarjeta" con borde discontinuo (MiPatrimonio.md §8.5). */
@Composable
private fun DashedAddCardButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(NeonShape.buttonRadius)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .drawBehind {
                drawRoundRect(
                    color = NeonColor.neonCyan,
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)),
                    ),
                    cornerRadius = CornerRadius(NeonShape.buttonRadius.toPx()),
                )
            }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = NeonColor.neonCyan,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

/** Estado del formulario de tarjeta. */
@Stable
private class CardFormState(initial: CardEntity?, defaultBank: String) {
    var bank by mutableStateOf(initial?.bank?.ifBlank { defaultBank } ?: defaultBank)
    var variant by mutableStateOf(initial?.variant.orEmpty())
    var kind by mutableStateOf(initial?.kind ?: CardKind.DEBIT)
    var alias by mutableStateOf(initial?.alias.orEmpty())
    var designId by mutableStateOf(initial?.theme ?: CardDesign.DESIGNS.first().id)
    var photoPath by mutableStateOf(initial?.photoPath)
    var copiedPhotoPath by mutableStateOf<String?>(null)
    var showNameError by mutableStateOf(false)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardFormSheet(
    card: CardEntity?,
    onDismiss: () -> Unit,
    onSave: (CardEntity, String?) -> Unit,
    onDelete: (CardEntity) -> Unit,
) {
    val context = LocalContext.current
    val defaultBank = stringResource(R.string.bank_caixabank)
    val state = remember(card, defaultBank) { CardFormState(card, defaultBank) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var saved by remember { mutableStateOf(false) }

    val pickPhoto = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) {
            val copied = CardPhotoStore.copyToPrivate(context, uri)
            if (copied != null) {
                val previousCopy = state.copiedPhotoPath
                if (previousCopy != null && previousCopy != card?.photoPath) {
                    CardPhotoStore.delete(context, previousCopy)
                }
                state.copiedPhotoPath = copied
                state.photoPath = copied
            }
        }
    }

    fun discardPendingCopy() {
        state.copiedPhotoPath
            ?.takeIf { it != card?.photoPath }
            ?.let { CardPhotoStore.delete(context, it) }
        state.copiedPhotoPath = null
    }

    fun buildCard(): CardEntity = CardEntity(
        id = card?.id ?: 0L,
        bank = state.bank,
        variant = state.variant.trim(),
        kind = state.kind,
        alias = state.alias.trim(),
        theme = state.designId,
        photoPath = state.photoPath,
        createdAt = card?.createdAt ?: System.currentTimeMillis(),
    )

    ModalBottomSheet(
        onDismissRequest = {
            if (!saved) discardPendingCopy()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = NeonColor.panel,
        contentColor = NeonColor.ink,
        scrimColor = NeonColor.bg.copy(alpha = 0.72f),
    ) {
        CardFormBody(
            state = state,
            isEditing = card != null,
            onBankChange = { state.bank = it },
            onVariantChange = {
                state.variant = it
                state.showNameError = false
            },
            onKindChange = { state.kind = it },
            onAliasChange = {
                state.alias = it
                state.showNameError = false
            },
            onDesignChange = { state.designId = it },
            onPickPhoto = {
                pickPhoto.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            onRemovePhoto = {
                val pendingCopy = state.copiedPhotoPath
                if (pendingCopy != null && pendingCopy != card?.photoPath) {
                    CardPhotoStore.delete(context, pendingCopy)
                }
                state.copiedPhotoPath = null
                state.photoPath = null
            },
            onSave = {
                if (state.alias.isBlank()) {
                    state.showNameError = true
                } else {
                    saved = true
                    onSave(buildCard(), card?.photoPath)
                }
            },
            onDelete = {
                saved = true
                discardPendingCopy()
                card?.let(onDelete)
            },
            onDismiss = {
                if (!saved) discardPendingCopy()
                onDismiss()
            },
        )
    }
}

@Composable
private fun CardFormBody(
    state: CardFormState,
    isEditing: Boolean,
    onBankChange: (String) -> Unit,
    onVariantChange: (String) -> Unit,
    onKindChange: (CardKind) -> Unit,
    onAliasChange: (String) -> Unit,
    onDesignChange: (String) -> Unit,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val previewCard = CardEntity(
        id = 0L,
        bank = state.bank,
        variant = state.variant,
        kind = state.kind,
        alias = state.alias,
        theme = state.designId,
        photoPath = state.photoPath,
        createdAt = 0L,
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenTitle(
            text = stringResource(
                if (isEditing) R.string.card_form_title_edit else R.string.card_form_title_new,
            ),
        )

        CardPreview(
            card = previewCard,
            design = CardDesign.getById(state.designId),
            height = 120,
        )

        FormSection(title = stringResource(R.string.card_form_bank)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BankOption.BANKS.forEach { bank ->
                    val name = stringResource(bank.nameRes)
                    NeonChip(
                        text = name,
                        accent = NeonColor.MovementType.cardAccent,
                        tint = NeonColor.MovementType.cardBg,
                        labelColor = NeonColor.MovementType.cardText,
                        selected = state.bank == name,
                        onClick = { onBankChange(name) },
                    )
                }
            }
        }

        NeonTextField(
            value = state.variant,
            onValueChange = onVariantChange,
            label = stringResource(R.string.card_form_variant),
        )

        FormSection(title = stringResource(R.string.card_form_kind)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    CardKind.DEBIT to R.string.card_kind_debit,
                    CardKind.CREDIT to R.string.card_kind_credit,
                    CardKind.PREPAID to R.string.card_kind_prepaid,
                ).forEach { (kind, labelRes) ->
                    NeonChip(
                        text = stringResource(labelRes),
                        accent = NeonColor.MovementType.cardAccent,
                        tint = NeonColor.MovementType.cardBg,
                        labelColor = NeonColor.MovementType.cardText,
                        selected = state.kind == kind,
                        onClick = { onKindChange(kind) },
                    )
                }
            }
        }

        NeonTextField(
            value = state.alias,
            onValueChange = onAliasChange,
            label = stringResource(R.string.card_form_alias),
            isError = state.showNameError,
        )

        if (state.showNameError) {
            Text(
                text = stringResource(R.string.card_form_error_name),
                color = NeonColor.expenseRed,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        FormSection(title = stringResource(R.string.card_form_design)) {
            DesignSelector(selectedId = state.designId, onSelect = onDesignChange)
        }

        FormSection(title = stringResource(R.string.card_form_photo)) {
            NeonButton(
                onClick = onPickPhoto,
                secondary = true,
                modifier = Modifier.fillMaxWidth(),
            ) {
                NeonButtonText(
                    text = stringResource(R.string.card_form_photo),
                    secondary = true,
                )
            }
            if (state.photoPath != null) {
                NeonButton(
                    onClick = onRemovePhoto,
                    secondary = true,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    NeonButtonText(
                        text = stringResource(R.string.card_form_remove_photo),
                        secondary = true,
                    )
                }
            }
        }

        NeonButton(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
            NeonButtonText(text = stringResource(R.string.card_form_save))
        }

        if (isEditing) {
            NeonButton(
                onClick = onDelete,
                secondary = true,
                accent = NeonColor.expenseRed,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.card_form_delete),
                    color = NeonColor.expenseRed,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }

        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.common_cancel),
                color = NeonColor.muted,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
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
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label) },
        singleLine = true,
        isError = isError,
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

/** Seis muestras con el nombre del diseño (MiPatrimonio.md §8.5). */
@Composable
private fun DesignSelector(selectedId: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CardDesign.DESIGNS.chunked(3).forEach { rowDesigns ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowDesigns.forEach { design ->
                    DesignSwatch(
                        design = design,
                        selected = design.id == selectedId,
                        onClick = { onSelect(design.id) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DesignSwatch(
    design: CardDesign,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    val accent = Color(design.accentColor)
    val name = stringResource(design.nameRes)
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(design.startColor), Color(design.endColor)),
                ),
                shape = shape,
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) accent else NeonColor.line,
                shape = shape,
            )
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name,
            color = accent,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

private fun sampleCard(
    bank: String,
    variant: String,
    kind: CardKind,
    alias: String,
    theme: String,
): CardEntity = CardEntity(
    id = 1L,
    bank = bank,
    variant = variant,
    kind = kind,
    alias = alias,
    theme = theme,
    photoPath = null,
    createdAt = 0L,
)

@Preview(showBackground = true, backgroundColor = 0xFF070A12)
@Composable
private fun WalletContentPreview() {
    MiPatrimonioTheme {
        WalletContent(
            cards = listOf(
                sampleCard("CaixaBank", "Imagin", CardKind.DEBIT, "Mi tarjeta", "cyan"),
                sampleCard("BBVA", "", CardKind.CREDIT, "Tarjeta principal", "magenta"),
            ),
            onAdd = {},
            onEdit = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF070A12)
@Composable
private fun CardFormBodyPreview() {
    MiPatrimonioTheme {
        Column(modifier = Modifier.background(NeonColor.panel)) {
            CardFormBody(
                state = CardFormState(
                    CardEntity(
                        id = 0L,
                        bank = "BBVA",
                        variant = "Aqua",
                        kind = CardKind.CREDIT,
                        alias = "Tarjeta principal",
                        theme = "violet",
                        photoPath = null,
                        createdAt = 0L,
                    ),
                    "CaixaBank",
                ),
                isEditing = true,
                onBankChange = {},
                onVariantChange = {},
                onKindChange = {},
                onAliasChange = {},
                onDesignChange = {},
                onPickPhoto = {},
                onRemovePhoto = {},
                onSave = {},
                onDelete = {},
                onDismiss = {},
            )
        }
    }
}
