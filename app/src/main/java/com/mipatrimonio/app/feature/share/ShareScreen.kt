package com.mipatrimonio.app.feature.share

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mipatrimonio.app.R
import com.mipatrimonio.app.core.model.DocumentKind
import com.mipatrimonio.app.core.model.TransactionType
import com.mipatrimonio.app.core.money.Money
import com.mipatrimonio.app.core.ocr.SuggestedDocumentKind
import com.mipatrimonio.app.core.security.LockScreen
import com.mipatrimonio.app.core.security.LockViewModel
import com.mipatrimonio.app.core.ui.components.NeonButton
import com.mipatrimonio.app.core.ui.components.NeonButtonText
import com.mipatrimonio.app.core.ui.components.NeonCard
import com.mipatrimonio.app.core.ui.components.NeonChip
import com.mipatrimonio.app.core.ui.components.ScreenTitle
import com.mipatrimonio.app.core.ui.theme.NeonColor
import com.mipatrimonio.app.feature.add.ReceiptScanDraft
import com.mipatrimonio.app.feature.add.ReviewSheet
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ShareGate(
    incomingUri: Uri?,
    sourceLabel: String,
    onDone: () -> Unit,
    lockViewModel: LockViewModel = hiltViewModel(),
) {
    val lockState by lockViewModel.state.collectAsStateWithLifecycle()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NeonColor.bg.copy(alpha = 0.96f)),
        contentAlignment = Alignment.BottomCenter,
    ) {
        when {
            !lockState.ready -> Box(Modifier.fillMaxSize())
            lockState.locked -> LockScreen(
                onAuthStarted = lockViewModel::onAuthStarted,
                onAuthFinished = lockViewModel::onAuthFinished,
                onUnlocked = lockViewModel::unlock,
            )
            incomingUri == null -> ErrorSheet(stringResource(R.string.share_error), onDone)
            else -> ShareSheet(incomingUri, sourceLabel, onDone)
        }
    }
}

private enum class ShareChoice { TICKET, PAYSLIP, INVOICE, WARRANTY, OTHER }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShareSheet(
    uri: Uri,
    sourceLabel: String,
    onDone: () -> Unit,
    viewModel: ShareViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var ticketDraft by remember { mutableStateOf<ReceiptScanDraft?>(null) }
    var buildingDraft by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uri) { viewModel.handleIncomingUri(uri, sourceLabel) }

    NeonCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
    ) {
        when (val s = state) {
            ShareUiState.Loading -> {
                Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(color = NeonColor.neonCyan)
                        Text(stringResource(R.string.scan_processing), color = NeonColor.muted)
                    }
                }
            }
            is ShareUiState.Error -> ErrorContent(stringResource(R.string.share_error), onDone)
            is ShareUiState.Ready -> {
                if (ticketDraft != null) {
                    ReviewSheet(
                        initialType = TransactionType.CASH,
                        scanDraft = ticketDraft,
                        onSaved = { onDone() },
                        onDismiss = {
                            ticketDraft?.thumbnail?.takeUnless { it.isRecycled }?.recycle()
                            ticketDraft?.sourceBitmap?.takeUnless { it.isRecycled }?.recycle()
                            ticketDraft = null
                        },
                    )
                } else {
                    val suggested = when (s.classification.suggested) {
                        SuggestedDocumentKind.PAYROLL -> ShareChoice.PAYSLIP
                        SuggestedDocumentKind.TICKET -> ShareChoice.TICKET
                        SuggestedDocumentKind.UNKNOWN -> null
                    }
                    var choice by remember(s.file.inboxPath) { mutableStateOf(suggested) }

                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(4.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        ScreenTitle(text = stringResource(R.string.share_title, sourceLabel))
                        Text(s.file.displayName, color = NeonColor.ink, style = MaterialTheme.typography.bodyMedium)
                        Text(stringResource(R.string.share_what_is), color = NeonColor.muted, style = MaterialTheme.typography.labelMedium)

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ChoiceChip(ShareChoice.TICKET, stringResource(R.string.share_ticket), choice) { choice = it }
                            ChoiceChip(ShareChoice.PAYSLIP, stringResource(R.string.share_payslip), choice) { choice = it }
                            ChoiceChip(ShareChoice.INVOICE, stringResource(R.string.share_invoice), choice) { choice = it }
                            ChoiceChip(ShareChoice.WARRANTY, stringResource(R.string.share_warranty), choice) { choice = it }
                            ChoiceChip(ShareChoice.OTHER, stringResource(R.string.share_other_doc), choice) { choice = it }
                        }

                        if (actionError) {
                            Text(stringResource(R.string.share_error), color = NeonColor.expenseRed, style = MaterialTheme.typography.bodySmall)
                        }

                        val busy = buildingDraft || saving
                        NeonButton(
                            onClick = {
                                if (busy) return@NeonButton
                                actionError = false
                                when (choice) {
                                    ShareChoice.TICKET -> {
                                        buildingDraft = true
                                        scope.launch {
                                            val draft = runCatching { viewModel.buildTicketDraft(s.file, s.lines) }.getOrNull()
                                            buildingDraft = false
                                            if (draft != null) ticketDraft = draft else actionError = true
                                        }
                                    }
                                    ShareChoice.PAYSLIP -> {
                                        saving = true
                                        val month = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("es-ES")))
                                        val title = "Nómina ${month.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.forLanguageTag("es-ES")) else it.toString() }}"
                                        viewModel.saveAsPayrollWithIncome(
                                            file = s.file,
                                            title = title,
                                            date = LocalDate.now(),
                                            netCents = s.classification.suggestedNetCents,
                                            onSaved = { saving = false; onDone() },
                                            onError = { saving = false; actionError = true },
                                        )
                                    }
                                    ShareChoice.INVOICE, ShareChoice.WARRANTY, ShareChoice.OTHER -> {
                                        val kind = when (choice) {
                                            ShareChoice.INVOICE -> DocumentKind.INVOICE
                                            ShareChoice.WARRANTY -> DocumentKind.WARRANTY
                                            else -> DocumentKind.OTHER
                                        }
                                        saving = true
                                        viewModel.saveAsDocument(
                                            file = s.file,
                                            kind = kind,
                                            title = s.file.displayName.removeSuffix(".pdf").ifBlank { s.file.displayName },
                                            date = LocalDate.now(),
                                            onSaved = { saving = false; onDone() },
                                            onError = { saving = false; actionError = true },
                                        )
                                    }
                                    null -> actionError = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            NeonButtonText(if (busy) stringResource(R.string.share_saving) else stringResource(R.string.review_save))
                        }

                        NeonButton(onClick = onDone, secondary = true, modifier = Modifier.fillMaxWidth()) {
                            NeonButtonText(stringResource(R.string.common_cancel), secondary = true)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChoiceChip(value: ShareChoice, label: String, selected: ShareChoice?, onSelect: (ShareChoice) -> Unit) {
    val isSel = selected == value
    NeonChip(
        text = label,
        accent = if (isSel) NeonColor.neonCyan else NeonColor.muted,
        tint = if (isSel) NeonColor.neonCyan.copy(alpha = 0.18f) else NeonColor.panelAlt,
        labelColor = if (isSel) NeonColor.neonCyan else NeonColor.chipText,
        selected = isSel,
        onClick = { onSelect(value) },
    )
}

@Composable
private fun ErrorSheet(message: String, onDone: () -> Unit) {
    NeonCard(Modifier.fillMaxWidth().padding(12.dp)) {
        ErrorContent(message, onDone)
    }
}

@Composable
private fun ErrorContent(message: String, onDone: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(message, color = NeonColor.expenseRed)
        NeonButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) { NeonButtonText(stringResource(R.string.common_accept)) }
    }
}
