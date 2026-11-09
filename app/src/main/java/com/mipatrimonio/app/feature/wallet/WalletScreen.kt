package com.mipatrimonio.app.feature.wallet

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontWeight
import java.time.Instant
import java.time.ZoneId
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
import com.mipatrimonio.app.core.database.entity.CategoryEntity
import com.mipatrimonio.app.core.database.entity.RecurringEntity
import com.mipatrimonio.app.core.backup.BackupFormat
import com.mipatrimonio.app.core.export.ExportFileSharer
import com.mipatrimonio.app.core.model.CardKind
import com.mipatrimonio.app.core.model.RecurrencePeriod
import com.mipatrimonio.app.core.model.TransactionType
import com.mipatrimonio.app.core.money.Money
import com.mipatrimonio.app.core.notifications.BudgetNotifier
import com.mipatrimonio.app.core.notifications.CashReminderNotifier
import com.mipatrimonio.app.core.notifications.NotificationPermission
import com.mipatrimonio.app.core.notifications.SavingsNotifier
import com.mipatrimonio.app.domain.usecase.RecurringSchedule
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
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
    onOpenDocuments: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val cards by viewModel.cards.collectAsStateWithLifecycle()
    val recurrings by viewModel.recurrings.collectAsStateWithLifecycle()
    val expenseCategories by viewModel.expenseCategories.collectAsStateWithLifecycle()
    val cashReminderEnabled by viewModel.cashReminderEnabled.collectAsStateWithLifecycle()
    var showForm by remember { mutableStateOf(false) }
    var editingCard by remember { mutableStateOf<CardEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<CardEntity?>(null) }
    var showRecurringForm by remember { mutableStateOf(false) }
    var editingRecurring by remember { mutableStateOf<RecurringEntity?>(null) }
    var exportError by remember { mutableStateOf(false) }
    var reportError by remember { mutableStateOf(false) }

    val backupBusy by viewModel.backupBusy.collectAsStateWithLifecycle()
    var backupMessage by remember { mutableStateOf<String?>(null) }
    var showCreatePassword by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var showRestorePassword by remember { mutableStateOf(false) }
    var pendingCreatePassword by remember { mutableStateOf<String?>(null) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }

    val createBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BackupFormat.MIME_TYPE),
    ) { uri ->
        val pwd = pendingCreatePassword
        pendingCreatePassword = null
        if (uri != null && pwd != null) {
            viewModel.createBackup(uri, pwd.toCharArray()) { ok ->
                backupMessage = context.getString(if (ok) R.string.backup_created else R.string.backup_error)
            }
        }
    }
    val restoreBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            pendingRestoreUri = uri
            showRestorePassword = true
        }
    }

    MiPatrimonioTheme {
        WalletContent(
            cards = cards,
            recurrings = recurrings,
            onAdd = {
                editingCard = null
                showForm = true
            },
            onEdit = { card ->
                editingCard = card
                showForm = true
            },
            onOpenDocuments = onOpenDocuments,
            onAddRecurring = { editingRecurring = null; showRecurringForm = true },
            onEditRecurring = { editingRecurring = it; showRecurringForm = true },
            cashReminderEnabled = cashReminderEnabled,
            onCashReminderEnabledChange = viewModel::setCashReminderEnabled,
            onExportCsv = {
                viewModel.exportCsv(
                    onReady = { file, mime ->
                        ExportFileSharer.share(context, file, mime, context.getString(R.string.export_share_csv))
                    },
                    onError = { exportError = true },
                )
            },
            onExportPdf = {
                viewModel.exportPdf(
                    onReady = { file, mime ->
                        ExportFileSharer.share(context, file, mime, context.getString(R.string.export_share_pdf))
                    },
                    onError = { exportError = true },
                )
            },
            onMonthlyReport = {
                viewModel.generateMonthlyReport(
                    onReady = { file, mime ->
                        ExportFileSharer.share(context, file, mime, context.getString(R.string.monthly_report_share))
                    },
                    onError = { reportError = true },
                )
            },
            backupBusy = backupBusy,
            onCreateBackup = { showCreatePassword = true },
            onRestoreBackup = { showRestoreConfirm = true },
            modifier = modifier,
        )

        if (exportError) {
            AlertDialog(
                onDismissRequest = { exportError = false },
                containerColor = NeonColor.panel,
                titleContentColor = NeonColor.ink,
                textContentColor = NeonColor.muted,
                title = { Text(stringResource(R.string.export_section_title)) },
                text = { Text(stringResource(R.string.export_error)) },
                confirmButton = {
                    TextButton(onClick = { exportError = false }) {
                        Text(stringResource(R.string.common_accept), color = NeonColor.neonCyan)
                    }
                },
            )
        }

        if (reportError) {
            AlertDialog(
                onDismissRequest = { reportError = false },
                containerColor = NeonColor.panel,
                titleContentColor = NeonColor.ink,
                textContentColor = NeonColor.muted,
                title = { Text(stringResource(R.string.monthly_report_section_title)) },
                text = { Text(stringResource(R.string.monthly_report_error)) },
                confirmButton = {
                    TextButton(onClick = { reportError = false }) {
                        Text(stringResource(R.string.common_accept), color = NeonColor.neonCyan)
                    }
                },
            )
        }

        if (showCreatePassword) {
            BackupCreatePasswordDialog(
                onDismiss = { showCreatePassword = false },
                onConfirm = { pwd ->
                    showCreatePassword = false
                    pendingCreatePassword = pwd
                    createBackupLauncher.launch(viewModel.defaultBackupFileName())
                },
            )
        }

        if (showRestoreConfirm) {
            AlertDialog(
                onDismissRequest = { showRestoreConfirm = false },
                containerColor = NeonColor.panel,
                titleContentColor = NeonColor.ink,
                textContentColor = NeonColor.muted,
                title = { Text(stringResource(R.string.backup_restore_confirm_title)) },
                text = { Text(stringResource(R.string.backup_restore_confirm_text)) },
                confirmButton = {
                    TextButton(onClick = {
                        showRestoreConfirm = false
                        restoreBackupLauncher.launch(arrayOf("*/*"))
                    }) { Text(stringResource(R.string.backup_restore_confirm_action), color = NeonColor.expenseRed) }
                },
                dismissButton = {
                    TextButton(onClick = { showRestoreConfirm = false }) {
                        Text(stringResource(R.string.common_cancel), color = NeonColor.neonCyan)
                    }
                },
            )
        }

        if (showRestorePassword && pendingRestoreUri != null) {
            BackupRestorePasswordDialog(
                onDismiss = { showRestorePassword = false; pendingRestoreUri = null },
                onConfirm = { pwd ->
                    val uri = pendingRestoreUri
                    showRestorePassword = false
                    pendingRestoreUri = null
                    if (uri != null) {
                        viewModel.restoreBackup(uri, pwd.toCharArray()) { ok ->
                            backupMessage = context.getString(if (ok) R.string.backup_restored else R.string.backup_error)
                        }
                    }
                },
            )
        }

        if (backupMessage != null) {
            AlertDialog(
                onDismissRequest = { backupMessage = null },
                containerColor = NeonColor.panel,
                titleContentColor = NeonColor.ink,
                textContentColor = NeonColor.muted,
                title = { Text(stringResource(R.string.backup_section_title)) },
                text = { Text(backupMessage!!) },
                confirmButton = {
                    TextButton(onClick = { backupMessage = null }) {
                        Text(stringResource(R.string.common_accept), color = NeonColor.neonCyan)
                    }
                },
            )
        }

        if (backupBusy) {
            AlertDialog(
                onDismissRequest = {},
                containerColor = NeonColor.panel,
                titleContentColor = NeonColor.ink,
                textContentColor = NeonColor.muted,
                title = { Text(stringResource(R.string.backup_section_title)) },
                text = { Text(stringResource(R.string.backup_in_progress)) },
                confirmButton = {},
            )
        }

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

        if (showRecurringForm) {
            RecurringFormSheet(
                existing = editingRecurring,
                cards = cards,
                categories = expenseCategories,
                onDismiss = { showRecurringForm = false; editingRecurring = null },
                onSave = { entity ->
                    viewModel.saveRecurring(entity)
                    showRecurringForm = false
                    editingRecurring = null
                },
                onDelete = { entity ->
                    viewModel.deleteRecurring(entity)
                    showRecurringForm = false
                    editingRecurring = null
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
    onOpenDocuments: () -> Unit = {},
    modifier: Modifier = Modifier,
    recurrings: List<RecurringEntity> = emptyList(),
    onAddRecurring: () -> Unit = {},
    onEditRecurring: (RecurringEntity) -> Unit = {},
    cashReminderEnabled: Boolean = false,
    onCashReminderEnabledChange: (Boolean) -> Unit = {},
    onExportCsv: () -> Unit = {},
    onExportPdf: () -> Unit = {},
    onMonthlyReport: () -> Unit = {},
    backupBusy: Boolean = false,
    onCreateBackup: () -> Unit = {},
    onRestoreBackup: () -> Unit = {},
) {
    val subscriptions = recurrings.filter { it.isSubscription }
    val fixed = recurrings.filter { !it.isSubscription }
    val dateFmt = remember { DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es-ES")) }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenTitle(text = stringResource(R.string.wallet_title)) }
        item {
            Text(text = stringResource(R.string.wallet_empty_text), color = NeonColor.muted, fontSize = NeonType.secondary)
        }
        item {
            NeonCard(modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onOpenDocuments)) {
                Text(text = stringResource(R.string.wallet_documents), color = NeonColor.ink, style = MaterialTheme.typography.titleMedium)
                Text(text = stringResource(R.string.wallet_documents_hint), color = NeonColor.neonGreen, style = MaterialTheme.typography.bodySmall)
            }
        }

        // Tarjetas
        if (cards.isEmpty()) {
            item {
                NeonCard(modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(R.string.wallet_empty_title), color = NeonColor.muted, style = MaterialTheme.typography.titleMedium)
                }
            }
        } else {
            items(items = cards, key = { "card-${it.id}" }) { card ->
                CardListItem(card = card, onClick = { onEdit(card) })
            }
        }
        item {
            DashedAddCardButton(text = stringResource(R.string.wallet_add_card), onClick = onAdd)
        }

        // Suscripciones (T3.5)
        item {
            SectionHeader(title = stringResource(R.string.wallet_subscriptions), onAdd = onAddRecurring)
        }
        if (subscriptions.isEmpty()) {
            item { Text(stringResource(R.string.wallet_recurring_empty_subs), color = NeonColor.muted, style = MaterialTheme.typography.bodySmall) }
        } else {
            val annualTotal = subscriptions.sumOf { RecurringSchedule.annualCostCents(it.amountCents, it.period) }
            item {
                Text(
                    text = stringResource(R.string.recurring_annual_total, Money.format(annualTotal)),
                    color = NeonColor.neonViolet,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            items(items = subscriptions, key = { "sub-${it.id}" }) { r ->
                RecurringRow(r, dateFmt, onClick = { onEditRecurring(r) })
            }
        }

        // Gastos fijos recurrentes (T3.5)
        item {
            SectionHeader(title = stringResource(R.string.wallet_recurring), onAdd = onAddRecurring)
        }
        if (fixed.isEmpty()) {
            item { Text(stringResource(R.string.wallet_recurring_empty_fixed), color = NeonColor.muted, style = MaterialTheme.typography.bodySmall) }
        } else {
            items(items = fixed, key = { "rec-${it.id}" }) { r ->
                RecurringRow(r, dateFmt, onClick = { onEditRecurring(r) })
            }
        }
        item {
            NotificationsSection(
                cashReminderEnabled = cashReminderEnabled,
                onCashReminderEnabledChange = onCashReminderEnabledChange,
            )
        }
        item {
            ExportSection(
                onExportCsv = onExportCsv,
                onExportPdf = onExportPdf,
                onMonthlyReport = onMonthlyReport,
            )
        }
        item {
            BackupSection(
                busy = backupBusy,
                onCreate = onCreateBackup,
                onRestore = onRestoreBackup,
            )
        }
        item { Box(Modifier.padding(bottom = 16.dp)) }
    }
}

@Composable
private fun SectionHeader(title: String, onAdd: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = NeonColor.ink, fontWeight = FontWeight.Bold)
        TextButton(onClick = onAdd) { Text(stringResource(R.string.wallet_recurring_add), color = NeonColor.neonCyan) }
    }
}

@Composable
private fun RecurringRow(r: RecurringEntity, dateFmt: DateTimeFormatter, onClick: () -> Unit) {
    val next = LocalDate.ofEpochDay(r.nextDate)
    val annual = RecurringSchedule.annualCostCents(r.amountCents, r.period)
    NeonCard(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(r.name, color = NeonColor.ink, style = MaterialTheme.typography.bodyLarge)
                Text("${Money.format(r.amountCents)} · ${periodLabel(r.period)} · ${next.format(dateFmt)}", color = NeonColor.muted, style = MaterialTheme.typography.bodySmall)
                if (r.isSubscription) {
                    Text(stringResource(R.string.recurring_annual_cost, Money.format(annual)), color = NeonColor.neonViolet, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun periodLabel(p: RecurrencePeriod): String = when (p) {
    RecurrencePeriod.WEEKLY -> stringResource(R.string.period_weekly)
    RecurrencePeriod.MONTHLY -> stringResource(R.string.period_monthly)
    RecurrencePeriod.QUARTERLY -> stringResource(R.string.period_quarterly)
    RecurrencePeriod.YEARLY -> stringResource(R.string.period_yearly)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun RecurringFormSheet(
    existing: RecurringEntity?,
    cards: List<CardEntity>,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (RecurringEntity) -> Unit,
    onDelete: (RecurringEntity) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var amountText by remember { mutableStateOf(existing?.let { Money.formatNumber(it.amountCents) } ?: "") }
    var period by remember { mutableStateOf(existing?.period ?: RecurrencePeriod.MONTHLY) }
    var isSub by remember { mutableStateOf(existing?.isSubscription ?: true) }
    var fixedType by remember {
        mutableStateOf(
            when (existing?.type) {
                TransactionType.CASH -> TransactionType.CASH
                else -> TransactionType.CARD
            }
        )
    }
    var cardId by remember { mutableStateOf(existing?.cardId) }
    var categoryId by remember { mutableStateOf(existing?.categoryId) }
    var nextDate by remember { mutableStateOf(existing?.let { LocalDate.ofEpochDay(it.nextDate) } ?: LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val invalidAmount = stringResource(R.string.savings_invalid_amount)
    val invalidName = stringResource(R.string.savings_invalid_name)
    val dateFmt = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("es-ES")) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = NeonColor.panel, contentColor = NeonColor.ink) {
        Column(
            Modifier.padding(16.dp).padding(bottom = 24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(if (existing == null) R.string.wallet_recurring_add else R.string.wallet_recurring_edit), style = MaterialTheme.typography.titleLarge, color = NeonColor.ink)
            RecurringTextField(name, { name = it }, stringResource(R.string.recurring_name))
            RecurringTextField(amountText, { amountText = it }, stringResource(R.string.recurring_amount), KeyboardType.Decimal)

            Text(stringResource(R.string.recurring_period), color = NeonColor.muted, style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RecurrencePeriod.entries.forEach { p ->
                    val selected = period == p
                    NeonChip(
                        text = periodLabel(p),
                        accent = NeonColor.neonCyan,
                        tint = if (selected) NeonColor.neonCyan.copy(alpha = 0.18f) else NeonColor.panelAlt,
                        labelColor = if (selected) NeonColor.neonCyan else NeonColor.chipText,
                        selected = selected,
                        onClick = { period = p },
                    )
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.recurring_is_subscription), color = NeonColor.ink)
                Switch(
                    checked = isSub,
                    onCheckedChange = { isSub = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = NeonColor.neonViolet, checkedTrackColor = NeonColor.neonViolet.copy(alpha = 0.4f)),
                )
            }

            if (!isSub) {
                Text(stringResource(R.string.recurring_fixed_type), color = NeonColor.muted, style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val cashSel = fixedType == TransactionType.CASH
                    NeonChip(text = stringResource(R.string.type_cash), accent = NeonColor.neonAmber, tint = if (cashSel) NeonColor.neonAmber.copy(alpha = 0.18f) else NeonColor.panelAlt, labelColor = if (cashSel) NeonColor.neonAmber else NeonColor.chipText, selected = cashSel, onClick = { fixedType = TransactionType.CASH; cardId = null })
                    val cardSel = fixedType == TransactionType.CARD
                    NeonChip(text = stringResource(R.string.type_card), accent = NeonColor.neonCyan, tint = if (cardSel) NeonColor.neonCyan.copy(alpha = 0.18f) else NeonColor.panelAlt, labelColor = if (cardSel) NeonColor.neonCyan else NeonColor.chipText, selected = cardSel, onClick = { fixedType = TransactionType.CARD })
                }
            }

            if (isSub || fixedType == TransactionType.CARD) {
                Text(stringResource(R.string.recurring_card), color = NeonColor.muted, style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val noneSel = cardId == null
                    NeonChip(text = stringResource(R.string.review_card_none), accent = NeonColor.neonCyan, tint = if (noneSel) NeonColor.neonCyan.copy(alpha = 0.18f) else NeonColor.panelAlt, labelColor = if (noneSel) NeonColor.neonCyan else NeonColor.chipText, selected = noneSel, onClick = { cardId = null })
                    cards.forEach { c ->
                        val sel = cardId == c.id
                        NeonChip(text = c.alias, accent = NeonColor.neonCyan, tint = if (sel) NeonColor.neonCyan.copy(alpha = 0.18f) else NeonColor.panelAlt, labelColor = if (sel) NeonColor.neonCyan else NeonColor.chipText, selected = sel, onClick = { cardId = c.id })
                    }
                }
            }

            Text(stringResource(R.string.recurring_category), color = NeonColor.muted, style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val noneCat = categoryId == null
                NeonChip(text = stringResource(R.string.recurring_no_category), accent = NeonColor.neonMagenta, tint = if (noneCat) NeonColor.neonMagenta.copy(alpha = 0.18f) else NeonColor.panelAlt, labelColor = if (noneCat) NeonColor.neonMagenta else NeonColor.chipText, selected = noneCat, onClick = { categoryId = null })
                categories.forEach { cat ->
                    val sel = categoryId == cat.id
                    NeonChip(text = cat.name, accent = NeonColor.neonMagenta, tint = if (sel) NeonColor.neonMagenta.copy(alpha = 0.18f) else NeonColor.panelAlt, labelColor = if (sel) NeonColor.neonMagenta else NeonColor.chipText, selected = sel, onClick = { categoryId = cat.id })
                }
            }

            TextButton(onClick = { showDatePicker = true }) {
                Text("${stringResource(R.string.recurring_next_date)}: ${nextDate.format(dateFmt)}", color = NeonColor.neonCyan)
            }

            val annual = try { Money.parse(amountText) } catch (_: Exception) { 0L }
            if (isSub && annual > 0) {
                Text(stringResource(R.string.recurring_annual_cost, Money.format(RecurringSchedule.annualCostCents(annual, period))), color = NeonColor.neonViolet, style = MaterialTheme.typography.bodySmall)
            }

            if (error != null) Text(error!!, color = NeonColor.expenseRed, style = MaterialTheme.typography.bodySmall)

            NeonButton(onClick = {
                if (name.trim().isEmpty()) { error = invalidName; return@NeonButton }
                val cents = try { Money.parse(amountText) } catch (_: Exception) { null }
                if (cents == null || cents <= 0L) { error = invalidAmount; return@NeonButton }
                val type = if (isSub) TransactionType.SUBSCRIPTION else fixedType
                val effectiveCardId = if (type == TransactionType.CARD) cardId else null
                onSave(
                    (existing ?: RecurringEntity(name = "", type = type, amountCents = 0, period = period, nextDate = nextDate.toEpochDay(), isSubscription = isSub)).copy(
                        name = name.trim(),
                        type = type,
                        amountCents = cents,
                        period = period,
                        nextDate = nextDate.toEpochDay(),
                        cardId = effectiveCardId,
                        categoryId = categoryId,
                        isSubscription = isSub,
                        remindDaysBefore = existing?.remindDaysBefore ?: 2,
                        active = true,
                    )
                )
            }, modifier = Modifier.fillMaxWidth()) { NeonButtonText(stringResource(R.string.recurring_save)) }

            if (existing != null) {
                NeonButton(onClick = { showDeleteConfirm = true }, secondary = true, accent = NeonColor.expenseRed, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.recurring_delete), color = NeonColor.expenseRed, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = nextDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { state.selectedDateMillis?.let { nextDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }; showDatePicker = false }) { Text("OK", color = NeonColor.neonCyan) } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.common_cancel), color = NeonColor.neonCyan) } },
            colors = DatePickerDefaults.colors(containerColor = NeonColor.panel),
        ) { DatePicker(state = state) }
    }

    if (showDeleteConfirm && existing != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = NeonColor.panel,
            title = { Text(stringResource(R.string.recurring_delete), color = NeonColor.ink) },
            text = { Text(stringResource(R.string.recurring_delete_confirm), color = NeonColor.muted) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete(existing)
                }) { Text(stringResource(R.string.recurring_delete), color = NeonColor.expenseRed) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.common_cancel), color = NeonColor.neonCyan)
                }
            },
        )
    }
}

@Composable
private fun ExportSection(
    onExportCsv: () -> Unit,
    onExportPdf: () -> Unit,
    onMonthlyReport: () -> Unit,
) {
    NeonCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.export_section_title),
                color = NeonColor.ink,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.export_section_hint),
                color = NeonColor.muted,
                style = MaterialTheme.typography.bodySmall,
            )
            NeonButton(onClick = onExportCsv, modifier = Modifier.fillMaxWidth()) {
                NeonButtonText(stringResource(R.string.export_csv))
            }
            NeonButton(onClick = onExportPdf, secondary = true, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.export_pdf),
                    color = NeonColor.neonCyan,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Text(
                text = stringResource(R.string.monthly_report_section_hint),
                color = NeonColor.muted,
                style = MaterialTheme.typography.bodySmall,
            )
            NeonButton(onClick = onMonthlyReport, secondary = true, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.monthly_report_generate),
                    color = NeonColor.neonCyan,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun BackupSection(
    busy: Boolean,
    onCreate: () -> Unit,
    onRestore: () -> Unit,
) {
    NeonCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.backup_section_title),
                color = NeonColor.ink,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.backup_section_hint),
                color = NeonColor.muted,
                style = MaterialTheme.typography.bodySmall,
            )
            NeonButton(onClick = { if (!busy) onCreate() }, modifier = Modifier.fillMaxWidth()) {
                NeonButtonText(stringResource(R.string.backup_create))
            }
            NeonButton(onClick = { if (!busy) onRestore() }, secondary = true, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.backup_restore),
                    color = NeonColor.expenseRed,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun BackupCreatePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var pwd by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val shortMsg = stringResource(R.string.backup_password_short)
    val mismatchMsg = stringResource(R.string.backup_password_mismatch)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = NeonColor.neonCyan, unfocusedBorderColor = NeonColor.lineInput,
        focusedLabelColor = NeonColor.neonCyan, unfocusedLabelColor = NeonColor.muted,
        focusedTextColor = NeonColor.ink, unfocusedTextColor = NeonColor.ink, cursorColor = NeonColor.neonCyan,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NeonColor.panel,
        titleContentColor = NeonColor.ink,
        textContentColor = NeonColor.muted,
        title = { Text(stringResource(R.string.backup_create)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.backup_password_hint), style = MaterialTheme.typography.bodySmall, color = NeonColor.muted)
                OutlinedTextField(
                    value = pwd, onValueChange = { pwd = it; error = null },
                    label = { Text(stringResource(R.string.backup_password)) },
                    singleLine = true, visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(), colors = fieldColors,
                )
                OutlinedTextField(
                    value = confirm, onValueChange = { confirm = it; error = null },
                    label = { Text(stringResource(R.string.backup_password_confirm)) },
                    singleLine = true, visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(), colors = fieldColors,
                )
                if (error != null) Text(error!!, color = NeonColor.expenseRed, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    pwd.length < 8 -> error = shortMsg
                    pwd != confirm -> error = mismatchMsg
                    else -> onConfirm(pwd)
                }
            }) { Text(stringResource(R.string.common_accept), color = NeonColor.neonCyan) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel), color = NeonColor.neonCyan) }
        },
    )
}

@Composable
private fun BackupRestorePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var pwd by remember { mutableStateOf("") }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = NeonColor.neonCyan, unfocusedBorderColor = NeonColor.lineInput,
        focusedLabelColor = NeonColor.neonCyan, unfocusedLabelColor = NeonColor.muted,
        focusedTextColor = NeonColor.ink, unfocusedTextColor = NeonColor.ink, cursorColor = NeonColor.neonCyan,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NeonColor.panel,
        titleContentColor = NeonColor.ink,
        textContentColor = NeonColor.muted,
        title = { Text(stringResource(R.string.backup_restore)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.backup_password_restore_hint), style = MaterialTheme.typography.bodySmall, color = NeonColor.muted)
                OutlinedTextField(
                    value = pwd, onValueChange = { pwd = it },
                    label = { Text(stringResource(R.string.backup_password)) },
                    singleLine = true, visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(), colors = fieldColors,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (pwd.isNotEmpty()) onConfirm(pwd) }) {
                Text(stringResource(R.string.backup_restore_confirm_action), color = NeonColor.neonCyan)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel), color = NeonColor.neonCyan) }
        },
    )
}

@Composable
private fun NotificationsSection(
    cashReminderEnabled: Boolean = false,
    onCashReminderEnabledChange: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(NotificationPermission.isGranted(context)) }
    var showRationale by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted -> granted = isGranted }

    NeonCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.notifications_section_title),
                color = NeonColor.ink,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.notifications_permission_explanation),
                color = NeonColor.muted,
                style = MaterialTheme.typography.bodySmall,
            )
            if (NotificationPermission.required() && !granted) {
                if (!showRationale) {
                    NeonButton(onClick = { showRationale = true }, modifier = Modifier.fillMaxWidth()) {
                        NeonButtonText(stringResource(R.string.notifications_enable))
                    }
                } else {
                    NeonButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { NeonButtonText(stringResource(R.string.notifications_enable)) }
                }
            } else {
                Text(stringResource(R.string.notifications_enabled), color = NeonColor.neonGreen, style = MaterialTheme.typography.bodySmall)
            }

            if (granted) {
                NeonButton(onClick = { SavingsNotifier.notifyTest(context); BudgetNotifier.notifyTest(context) }, secondary = true, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.notifications_test), color = NeonColor.neonCyan, style = MaterialTheme.typography.labelLarge)
                }
            }

            HorizontalDivider(color = NeonColor.line)
            Text(stringResource(R.string.cash_reminder_title), color = NeonColor.ink, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.cash_reminder_toggle),
                    color = NeonColor.muted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )
                Switch(
                    checked = cashReminderEnabled,
                    onCheckedChange = onCashReminderEnabledChange,
                    colors = SwitchDefaults.colors(checkedThumbColor = NeonColor.neonCyan, checkedTrackColor = NeonColor.neonCyan.copy(alpha = 0.4f)),
                )
            }
            if (granted) {
                TextButton(onClick = { CashReminderNotifier.notify(context) }) {
                    Text(stringResource(R.string.cash_reminder_test), color = NeonColor.neonCyan)
                }
            }

            HorizontalDivider(color = NeonColor.line)
            Text(stringResource(R.string.notifications_reliability), color = NeonColor.ink, style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.notifications_reliability_hint), color = NeonColor.muted, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                context.startActivity(intent)
            }) {
                Text(stringResource(R.string.notifications_open_battery_settings), color = NeonColor.neonCyan)
            }
        }
    }
}

@Composable
private fun RecurringTextField(value: String, onValueChange: (String) -> Unit, label: String, keyboardType: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(label) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType), modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonColor.neonCyan, unfocusedBorderColor = NeonColor.lineInput, focusedLabelColor = NeonColor.neonCyan, unfocusedLabelColor = NeonColor.muted, focusedTextColor = NeonColor.ink, unfocusedTextColor = NeonColor.ink, cursorColor = NeonColor.neonCyan),
    )
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

@OptIn(ExperimentalLayoutApi::class)
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
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
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
                width = 1.dp,
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
