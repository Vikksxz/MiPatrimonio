package com.mipatrimonio.app.feature.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mipatrimonio.app.R
import com.mipatrimonio.app.core.backup.BackupFormat
import com.mipatrimonio.app.core.database.entity.ExchangeRateEntity
import com.mipatrimonio.app.core.export.ExportFileSharer
import com.mipatrimonio.app.core.money.Money
import com.mipatrimonio.app.core.notifications.BudgetNotifier
import com.mipatrimonio.app.core.notifications.CashReminderNotifier
import com.mipatrimonio.app.core.notifications.NotificationPermission
import com.mipatrimonio.app.core.notifications.SavingsNotifier
import com.mipatrimonio.app.core.ui.components.NeonButton
import com.mipatrimonio.app.core.ui.components.NeonButtonText
import com.mipatrimonio.app.core.ui.components.NeonCard
import com.mipatrimonio.app.core.ui.components.NeonChip
import com.mipatrimonio.app.core.ui.theme.NeonColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenHomeSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val cashReminderEnabled by viewModel.cashReminderEnabled.collectAsStateWithLifecycle()
    val exchangeRates by viewModel.exchangeRates.collectAsStateWithLifecycle()
    val backupBusy by viewModel.backupBusy.collectAsStateWithLifecycle()

    var exportError by remember { mutableStateOf(false) }
    var reportError by remember { mutableStateOf(false) }
    var exchangeRateError by remember { mutableStateOf(false) }
    var backupMessage by remember { mutableStateOf<String?>(null) }
    var showCreatePassword by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var showRestorePassword by remember { mutableStateOf(false) }
    var pendingCreatePassword by remember { mutableStateOf<String?>(null) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }

    val createBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BackupFormat.MIME_TYPE),
    ) { uri: Uri? ->
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
    ) { uri: Uri? -> if (uri != null) { pendingRestoreUri = uri; showRestorePassword = true } }

    Scaffold(
        modifier = modifier,
        containerColor = NeonColor.bg,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), color = NeonColor.ink) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = NeonColor.neonCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NeonColor.bg),
                windowInsets = WindowInsets(0.dp),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                NeonCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.wallet_customize_home), color = NeonColor.ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.wallet_customize_home_hint), color = NeonColor.muted, style = MaterialTheme.typography.bodySmall)
                        NeonButton(onClick = onOpenHomeSettings, modifier = Modifier.fillMaxWidth()) {
                            NeonButtonText(stringResource(R.string.wallet_customize_home))
                        }
                    }
                }
            }
            item {
                NotificationsSection(
                    cashReminderEnabled = cashReminderEnabled,
                    onCashReminderEnabledChange = viewModel::setCashReminderEnabled,
                )
            }
            item {
                ExchangeRatesSection(
                    rates = exchangeRates,
                    onSave = { c, r -> viewModel.saveExchangeRate(c, r, onError = { exchangeRateError = true }) },
                    onDelete = viewModel::deleteExchangeRate,
                )
            }
            item {
                ExportSection(
                    onExportCsv = {
                        viewModel.exportCsv(
                            onReady = { f, m -> ExportFileSharer.share(context, f, m, context.getString(R.string.export_share_csv)) },
                            onError = { exportError = true },
                        )
                    },
                    onExportPdf = {
                        viewModel.exportPdf(
                            onReady = { f, m -> ExportFileSharer.share(context, f, m, context.getString(R.string.export_share_pdf)) },
                            onError = { exportError = true },
                        )
                    },
                    onMonthlyReport = {
                        viewModel.generateMonthlyReport(
                            onReady = { f, m -> ExportFileSharer.share(context, f, m, context.getString(R.string.monthly_report_share)) },
                            onError = { reportError = true },
                        )
                    },
                )
            }
            item {
                BackupSection(
                    busy = backupBusy,
                    onCreate = { showCreatePassword = true },
                    onRestore = { showRestoreConfirm = true },
                )
            }
        }
    }

    if (exportError) SimpleAlert(R.string.export_section_title, R.string.export_error) { exportError = false }
    if (reportError) SimpleAlert(R.string.monthly_report_section_title, R.string.monthly_report_error) { reportError = false }
    if (exchangeRateError) SimpleAlert(R.string.exchange_rates_title, R.string.exchange_rate_invalid) { exchangeRateError = false }
    if (backupMessage != null) SimpleAlert(R.string.backup_section_title, text = backupMessage!!) { backupMessage = null }

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
                TextButton(onClick = { showRestoreConfirm = false; restoreBackupLauncher.launch(arrayOf("*/*")) }) {
                    Text(stringResource(R.string.backup_restore_confirm_action), color = NeonColor.expenseRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) { Text(stringResource(R.string.common_cancel), color = NeonColor.neonCyan) }
            },
        )
    }
    if (showRestorePassword && pendingRestoreUri != null) {
        val uri = pendingRestoreUri!!
        BackupRestorePasswordDialog(
            onDismiss = { showRestorePassword = false; pendingRestoreUri = null },
            onConfirm = { pwd ->
                showRestorePassword = false
                viewModel.restoreBackup(uri, pwd.toCharArray()) { ok ->
                    backupMessage = context.getString(if (ok) R.string.backup_restored else R.string.backup_error)
                }
                pendingRestoreUri = null
            },
        )
    }
}

@Composable
private fun SimpleAlert(titleRes: Int, textRes: Int? = null, text: String? = null, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NeonColor.panel,
        titleContentColor = NeonColor.ink,
        textContentColor = NeonColor.muted,
        title = { Text(stringResource(titleRes)) },
        text = { Text(text ?: stringResource(textRes!!)) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_accept), color = NeonColor.neonCyan) } },
    )
}

@Composable
private fun NotificationsSection(cashReminderEnabled: Boolean, onCashReminderEnabledChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(NotificationPermission.isGranted(context)) }
    var showRationale by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted -> granted = isGranted }

    NeonCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.notifications_section_title), color = NeonColor.ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.notifications_permission_explanation), color = NeonColor.muted, style = MaterialTheme.typography.bodySmall)
            if (NotificationPermission.required() && !granted) {
                NeonButton(onClick = {
                    if (showRationale && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else showRationale = true
                }, modifier = Modifier.fillMaxWidth()) { NeonButtonText(stringResource(R.string.notifications_enable)) }
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
                Text(stringResource(R.string.cash_reminder_toggle), color = NeonColor.muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f).padding(end = 8.dp))
                Switch(checked = cashReminderEnabled, onCheckedChange = onCashReminderEnabledChange, colors = SwitchDefaults.colors(checkedThumbColor = NeonColor.neonCyan, checkedTrackColor = NeonColor.neonCyan.copy(alpha = 0.4f)))
            }
            if (granted) {
                TextButton(onClick = { CashReminderNotifier.notify(context) }) { Text(stringResource(R.string.cash_reminder_test), color = NeonColor.neonCyan) }
            }
            HorizontalDivider(color = NeonColor.line)
            Text(stringResource(R.string.notifications_reliability), color = NeonColor.ink, style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.notifications_reliability_hint), color = NeonColor.muted, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }) {
                Text(stringResource(R.string.notifications_open_battery_settings), color = NeonColor.neonCyan)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExchangeRatesSection(rates: List<ExchangeRateEntity>, onSave: (String, String) -> Unit, onDelete: (String) -> Unit) {
    var selectedCurrency by remember { mutableStateOf(Money.SUPPORTED_CURRENCIES.firstOrNull { it != Money.BASE_CURRENCY } ?: "USD") }
    var rateText by remember { mutableStateOf("") }
    NeonCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.exchange_rates_title), color = NeonColor.ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.exchange_rates_hint), color = NeonColor.muted, style = MaterialTheme.typography.bodySmall)
            if (rates.isEmpty()) {
                Text("EUR (base)", color = NeonColor.neonCyan, style = MaterialTheme.typography.bodySmall)
            } else {
                rates.forEach { rate ->
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("1 ${rate.currency} = ${formatRateDisplay(rate.rateToBase)} EUR", color = NeonColor.ink, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        TextButton(onClick = { onDelete(rate.currency) }) { Text(stringResource(R.string.exchange_rate_delete), color = NeonColor.expenseRed) }
                    }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Money.SUPPORTED_CURRENCIES.filter { it != Money.BASE_CURRENCY }.forEach { code ->
                    val selected = selectedCurrency == code
                    NeonChip(text = code, accent = if (selected) NeonColor.neonCyan else NeonColor.muted, tint = if (selected) NeonColor.neonCyan.copy(alpha = 0.18f) else NeonColor.panelAlt, labelColor = if (selected) NeonColor.neonCyan else NeonColor.chipText, selected = selected, onClick = {
                        selectedCurrency = code
                        rateText = rates.firstOrNull { it.currency == code }?.let { formatRateDisplay(it.rateToBase) } ?: ""
                    })
                }
            }
            OutlinedTextField(value = rateText, onValueChange = { rateText = it }, label = { Text(stringResource(R.string.exchange_rate_value)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), colors = fieldColors())
            NeonButton(onClick = { onSave(selectedCurrency, rateText); rateText = "" }, secondary = true, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.exchange_rate_save), color = NeonColor.neonCyan, style = MaterialTheme.typography.labelLarge) }
        }
    }
}

@Composable
private fun ExportSection(onExportCsv: () -> Unit, onExportPdf: () -> Unit, onMonthlyReport: () -> Unit) {
    NeonCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.export_section_title), color = NeonColor.ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.export_section_hint), color = NeonColor.muted, style = MaterialTheme.typography.bodySmall)
            NeonButton(onClick = onExportCsv, modifier = Modifier.fillMaxWidth()) { NeonButtonText(stringResource(R.string.export_csv)) }
            NeonButton(onClick = onExportPdf, secondary = true, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.export_pdf), color = NeonColor.neonCyan, style = MaterialTheme.typography.labelLarge) }
            Text(stringResource(R.string.monthly_report_section_hint), color = NeonColor.muted, style = MaterialTheme.typography.bodySmall)
            NeonButton(onClick = onMonthlyReport, secondary = true, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.monthly_report_generate), color = NeonColor.neonCyan, style = MaterialTheme.typography.labelLarge) }
        }
    }
}

@Composable
private fun BackupSection(busy: Boolean, onCreate: () -> Unit, onRestore: () -> Unit) {
    NeonCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.backup_section_title), color = NeonColor.ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.backup_section_hint), color = NeonColor.muted, style = MaterialTheme.typography.bodySmall)
            NeonButton(onClick = { if (!busy) onCreate() }, modifier = Modifier.fillMaxWidth()) { NeonButtonText(stringResource(R.string.backup_create)) }
            NeonButton(onClick = { if (!busy) onRestore() }, secondary = true, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.backup_restore), color = NeonColor.expenseRed, style = MaterialTheme.typography.labelLarge) }
        }
    }
}

@Composable
private fun BackupCreatePasswordDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var pwd by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val shortMsg = stringResource(R.string.backup_password_short)
    val mismatchMsg = stringResource(R.string.backup_password_mismatch)
    AlertDialog(onDismissRequest = onDismiss, containerColor = NeonColor.panel, titleContentColor = NeonColor.ink, textContentColor = NeonColor.muted, title = { Text(stringResource(R.string.backup_create)) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.backup_password_hint), style = MaterialTheme.typography.bodySmall, color = NeonColor.muted)
            OutlinedTextField(value = pwd, onValueChange = { pwd = it; error = null }, label = { Text(stringResource(R.string.backup_password)) }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), colors = fieldColors())
            OutlinedTextField(value = confirm, onValueChange = { confirm = it; error = null }, label = { Text(stringResource(R.string.backup_password_confirm)) }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), colors = fieldColors())
            if (error != null) Text(error!!, color = NeonColor.expenseRed, style = MaterialTheme.typography.bodySmall)
        }
    }, confirmButton = {
        TextButton(onClick = {
            when { pwd.length < 8 -> error = shortMsg; pwd != confirm -> error = mismatchMsg; else -> onConfirm(pwd) }
        }) { Text(stringResource(R.string.common_accept), color = NeonColor.neonCyan) }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel), color = NeonColor.neonCyan) } })
}

@Composable
private fun BackupRestorePasswordDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var pwd by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, containerColor = NeonColor.panel, titleContentColor = NeonColor.ink, textContentColor = NeonColor.muted, title = { Text(stringResource(R.string.backup_restore)) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.backup_password_restore_hint), style = MaterialTheme.typography.bodySmall, color = NeonColor.muted)
            OutlinedTextField(value = pwd, onValueChange = { pwd = it }, label = { Text(stringResource(R.string.backup_password)) }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), colors = fieldColors())
        }
    }, confirmButton = { TextButton(onClick = { if (pwd.isNotEmpty()) onConfirm(pwd) }) { Text(stringResource(R.string.backup_restore_confirm_action), color = NeonColor.neonCyan) } }, dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel), color = NeonColor.neonCyan) } })
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = NeonColor.neonCyan, unfocusedBorderColor = NeonColor.lineInput,
    focusedLabelColor = NeonColor.neonCyan, unfocusedLabelColor = NeonColor.muted,
    focusedTextColor = NeonColor.ink, unfocusedTextColor = NeonColor.ink, cursorColor = NeonColor.neonCyan,
)

private fun formatRateDisplay(rate: Double): String {
    if (!rate.isFinite() || rate <= 0.0) return "0"
    val s = String.format(java.util.Locale.ROOT, "%.6f", rate).trimEnd('0').trimEnd('.')
    return s.replace('.', ',')
}
