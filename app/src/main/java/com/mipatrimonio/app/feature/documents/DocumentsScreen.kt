package com.mipatrimonio.app.feature.documents

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mipatrimonio.app.R
import com.mipatrimonio.app.core.database.entity.DocumentEntity
import com.mipatrimonio.app.core.model.DocumentKind
import com.mipatrimonio.app.core.model.TransactionType
import com.mipatrimonio.app.core.ui.components.NeonButton
import com.mipatrimonio.app.core.ui.components.NeonButtonText
import com.mipatrimonio.app.core.ui.components.NeonCard
import com.mipatrimonio.app.core.ui.theme.MiPatrimonioTheme
import com.mipatrimonio.app.core.ui.theme.NeonColor
import com.mipatrimonio.app.feature.add.ReviewSheet
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(
    onBack: () -> Unit,
    onOpen: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DocumentsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingDelete by remember { mutableStateOf<DocumentEntity?>(null) }
    val errorText = stringResource(R.string.documents_action_failed)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) viewModel.onFilePicked(uri)
    }

    LaunchedEffect(state.error) {
        if (state.error) {
            snackbarHostState.showSnackbar(errorText)
            viewModel.clearError()
        }
    }

    MiPatrimonioTheme {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(NeonColor.bg),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                            tint = NeonColor.ink,
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.documents_title),
                        color = NeonColor.ink,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.weight(1f),
                    )
                }

                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    placeholder = {
                        Text(
                            text = stringResource(R.string.documents_search_hint),
                            color = NeonColor.muted,
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonColor.neonCyan,
                        unfocusedBorderColor = NeonColor.lineInput,
                        focusedTextColor = NeonColor.ink,
                        unfocusedTextColor = NeonColor.ink,
                        cursorColor = NeonColor.neonCyan,
                    ),
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (state.saving) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = NeonColor.neonCyan,
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.documents_add),
                            color = NeonColor.muted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (state.documents.isEmpty() && !state.saving) {
                    NeonCard(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.documents_empty),
                            color = NeonColor.ink,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.documents_empty_hint),
                            color = NeonColor.muted,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(state.documents, key = { it.id }) { doc ->
                            DocumentRow(
                                document = doc,
                                onClick = { onOpen(doc.id) },
                                onDelete = { pendingDelete = doc },
                            )
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }
                }

                NeonButton(
                    onClick = { if (!state.saving) picker.launch(arrayOf("application/pdf")) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = NeonColor.bg,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    NeonButtonText(text = stringResource(R.string.documents_add))
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = NeonColor.panelAlt,
                    contentColor = NeonColor.ink,
                )
            }
        }

        val toDelete = pendingDelete
        if (toDelete != null) {
            AlertDialog(
                onDismissRequest = { pendingDelete = null },
                containerColor = NeonColor.panel,
                title = { Text(text = stringResource(R.string.documents_delete), color = NeonColor.ink) },
                text = { Text(text = stringResource(R.string.documents_delete_confirm), color = NeonColor.muted) },
                confirmButton = {
                    TextButton(onClick = { viewModel.delete(toDelete); pendingDelete = null }) {
                        Text(stringResource(R.string.documents_delete), color = NeonColor.expenseRed)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDelete = null }) {
                        Text(stringResource(R.string.common_cancel), color = NeonColor.muted)
                    }
                },
            )
        }

        if (state.pendingDuplicateWarning && state.pendingPayrollDraft != null) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissPendingPayroll() },
                containerColor = NeonColor.panel,
                title = { Text(stringResource(R.string.duplicate_payroll_title), color = NeonColor.ink) },
                text = { Text(stringResource(R.string.duplicate_payroll_message), color = NeonColor.muted) },
                confirmButton = {
                    TextButton(onClick = { viewModel.confirmDuplicateAndShowDraft() }) {
                        Text(stringResource(R.string.duplicate_continue), color = NeonColor.neonCyan)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissPendingPayroll() }) {
                        Text(stringResource(R.string.common_cancel), color = NeonColor.muted)
                    }
                },
            )
        }

        val draft = state.pendingPayrollDraft
        if (draft != null && !state.pendingDuplicateWarning) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.dismissPendingPayroll() },
                sheetState = sheetState,
                containerColor = NeonColor.panel,
                contentColor = NeonColor.ink,
            ) {
                ReviewSheet(
                    initialType = TransactionType.INCOME,
                    payrollDraft = draft,
                    onSaved = { viewModel.onPayrollSaved() },
                    onDismiss = { viewModel.dismissPendingPayroll() },
                )
            }
        }
    }
}

@Composable
private fun DocumentRow(
    document: DocumentEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val dateText = remember(document.date) {
        LocalDate.ofEpochDay(document.date)
            .format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es-ES")))
    }
    NeonCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = document.title,
                    color = NeonColor.ink,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${document.kind.label()} · $dateText",
                    color = NeonColor.neonGreen,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.documents_delete),
                    tint = NeonColor.muted,
                )
            }
        }
    }
}

@Composable
private fun DocumentKind.label(): String = stringResource(
    when (this) {
        DocumentKind.PAYSLIP -> R.string.document_kind_payslip
        DocumentKind.INVOICE -> R.string.document_kind_invoice
        DocumentKind.WARRANTY -> R.string.document_kind_warranty
        DocumentKind.INSURANCE -> R.string.document_kind_insurance
        DocumentKind.CONTRACT -> R.string.document_kind_contract
        DocumentKind.OTHER -> R.string.document_kind_other
    }
)