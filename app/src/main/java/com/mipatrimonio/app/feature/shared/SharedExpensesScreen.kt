package com.mipatrimonio.app.feature.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mipatrimonio.app.R
import com.mipatrimonio.app.core.database.entity.PersonEntity
import com.mipatrimonio.app.core.database.entity.SharedExpenseEntity
import com.mipatrimonio.app.core.database.entity.SharedShareEntity
import com.mipatrimonio.app.core.money.Money
import com.mipatrimonio.app.core.ui.components.NeonButton
import com.mipatrimonio.app.core.ui.components.NeonCard
import com.mipatrimonio.app.core.ui.theme.MiPatrimonioTheme
import com.mipatrimonio.app.core.ui.theme.NeonColor
import com.mipatrimonio.app.domain.usecase.DebtTransfer
import com.mipatrimonio.app.domain.usecase.SharedSplit
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedExpensesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SharedExpensesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SharedExpensesContent(
        state = state,
        onBack = onBack,
        onAddPerson = viewModel::addPerson,
        onAddExpense = viewModel::addExpense,
        onUpdateExpense = viewModel::updateExpense,
        onDeleteExpense = viewModel::deleteExpense,
        onSettleExpense = viewModel::settleExpense,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedExpensesContent(
    state: SharedExpensesState,
    onBack: () -> Unit,
    onAddPerson: (String, (Long) -> Unit, () -> Unit) -> Unit,
    onAddExpense: (String, Long, Long, Long, Map<Long, Long>, () -> Unit, (String) -> Unit) -> Unit,
    onUpdateExpense: (Long, String, Long, Long, Long, Map<Long, Long>, () -> Unit, (String) -> Unit) -> Unit,
    onDeleteExpense: (Long) -> Unit,
    onSettleExpense: (Long, Long?, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showForm by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<SharedExpenseUi?>(null) }
    var showAddPerson by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = NeonColor.bg,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.shared_title), color = NeonColor.ink) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = NeonColor.neonCyan)
                    }
                },
                actions = {
                    IconButton(onClick = { showAddPerson = true }) {
                        Icon(Icons.Default.Person, contentDescription = stringResource(R.string.shared_add_person), tint = NeonColor.neonGreen)
                    }
                    IconButton(onClick = { editing = null; showForm = true }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.shared_add_expense), tint = NeonColor.neonCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NeonColor.bg),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { PeopleRow(people = state.people) }
            if (state.transfers.isNotEmpty() || state.balances.isNotEmpty()) {
                item { BalancesCard(people = state.people, balances = state.balances, transfers = state.transfers) }
            }
            if (state.expenses.isEmpty()) {
                item {
                    Text(stringResource(R.string.shared_empty), color = NeonColor.muted, style = MaterialTheme.typography.bodyMedium)
                }
            }
            items(state.expenses, key = { it.expense.id }) { item ->
                SharedExpenseRow(
                    item = item,
                    people = state.people,
                    onEdit = { editing = item; showForm = true },
                    onDelete = { onDeleteExpense(item.expense.id) },
                    onSettleShare = { personId, settled -> onSettleExpense(item.expense.id, personId, settled) },
                )
            }
        }
    }

    if (showAddPerson) {
        AddPersonDialog(
            onDismiss = { showAddPerson = false },
            onAdd = { name -> onAddPerson(name, { _ -> showAddPerson = false }, { showAddPerson = false }) },
        )
    }

    if (showForm) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showForm = false },
            sheetState = sheetState,
            containerColor = NeonColor.bg,
        ) {
            SharedExpenseForm(
                people = state.people,
                editing = editing,
                onDismiss = { showForm = false },
                onSave = { title, total, payer, date, shares, onDone, onError ->
                    val done = { showForm = false; onDone() }
                    val err = { _: String -> onError() }
                    if (editing == null) {
                        onAddExpense(title, total, payer, date, shares, done, err)
                    } else {
                        onUpdateExpense(editing!!.expense.id, title, total, payer, date, shares, done, err)
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PeopleRow(people: List<PersonEntity>) {
    NeonCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.shared_people), color = NeonColor.ink, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            if (people.isEmpty()) {
                Text(stringResource(R.string.shared_people_empty), color = NeonColor.muted, style = MaterialTheme.typography.bodySmall)
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    people.forEach { p ->
                        val label = if (p.isMe) "${p.name} (${stringResource(R.string.shared_me)})" else p.name
                        Text(
                            text = label,
                            color = if (p.isMe) NeonColor.neonCyan else NeonColor.chipText,
                            modifier = Modifier.background(NeonColor.panelAlt, RoundedCornerShape(999.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BalancesCard(people: List<PersonEntity>, balances: Map<Long, Long>, transfers: List<DebtTransfer>) {
    val names = remember(people) { people.associate { it.id to it.name } }
    NeonCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.shared_balances), color = NeonColor.ink, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            balances.forEach { (id, net) ->
                val name = names[id] ?: id.toString()
                val color = if (net >= 0L) NeonColor.neonGreen else NeonColor.expenseRed
                Text("$name: ${Money.formatSigned(net, isExpense = net < 0L)}", color = color, style = MaterialTheme.typography.bodyMedium)
            }
            if (transfers.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.shared_who_owes), color = NeonColor.neonViolet, style = MaterialTheme.typography.labelLarge)
                transfers.forEach { t ->
                    val from = names[t.fromPersonId] ?: t.fromPersonId.toString()
                    val to = names[t.toPersonId] ?: t.toPersonId.toString()
                    Text(stringResource(R.string.shared_owes_format, from, to, Money.format(t.amountCents)), color = NeonColor.ink, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun SharedExpenseRow(
    item: SharedExpenseUi,
    people: List<PersonEntity>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSettleShare: (personId: Long, settled: Boolean) -> Unit,
) {
    val names = remember(people) { people.associate { it.id to it.name } }
    val meId = remember(people) { people.firstOrNull { it.isMe }?.id }
    val payer = names[item.expense.paidByPersonId] ?: "?"
    val allSettled = item.shares.isNotEmpty() && item.shares.all { it.settled }
    val date = LocalDate.ofEpochDay(item.expense.date).format(RowDateFmt)
    var confirmDelete by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    NeonCard(modifier = Modifier.fillMaxWidth().clickable(onClick = { expanded = !expanded })) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(item.expense.title, color = NeonColor.ink, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(R.string.shared_paid_by, payer, Money.format(item.expense.totalCents), date),
                        color = NeonColor.muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (allSettled) {
                        Text(stringResource(R.string.shared_settled), color = NeonColor.neonGreen, style = MaterialTheme.typography.labelSmall)
                    }
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.common_edit), tint = NeonColor.neonCyan)
                }
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.common_delete), tint = NeonColor.expenseRed)
                }
            }
            if (expanded) {
                Text(stringResource(R.string.shared_settle_hint), color = NeonColor.muted, style = MaterialTheme.typography.labelSmall)
                item.shares.forEach { share ->
                    val label = names[share.personId] ?: share.personId.toString()
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "$label: ${Money.format(share.shareCents)}" + if (share.settled) " ✓" else "",
                            color = if (share.settled) NeonColor.muted else NeonColor.ink,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { onSettleShare(share.personId, !share.settled) }) {
                            Text(
                                stringResource(if (share.settled) R.string.shared_unsettle else R.string.shared_settle),
                                color = if (share.settled) NeonColor.muted else NeonColor.neonViolet,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = NeonColor.panel,
            titleContentColor = NeonColor.ink,
            textContentColor = NeonColor.muted,
            title = { Text(stringResource(R.string.shared_delete_title)) },
            text = { Text(stringResource(R.string.shared_delete_confirm)) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) {
                    Text(stringResource(R.string.common_delete), color = NeonColor.expenseRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.common_cancel), color = NeonColor.neonCyan)
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SharedExpenseForm(
    people: List<PersonEntity>,
    editing: SharedExpenseUi?,
    onDismiss: () -> Unit,
    onSave: (title: String, totalCents: Long, payerId: Long, dateEpochDay: Long, shares: Map<Long, Long>, onDone: () -> Unit, onError: () -> Unit) -> Unit,
) {
    var title by remember { mutableStateOf(editing?.expense?.title.orEmpty()) }
    var amountText by remember { mutableStateOf(editing?.expense?.totalCents?.let { Money.formatNumber(it) }.orEmpty()) }
    var payerId by remember { mutableLongStateOf(editing?.expense?.paidByPersonId ?: people.firstOrNull { it.isMe }?.id ?: people.firstOrNull()?.id ?: 0L) }
    var date by remember { mutableStateOf(editing?.expense?.date?.let { LocalDate.ofEpochDay(it) } ?: LocalDate.now()) }
    var equalSplit by remember { mutableStateOf(true) }
    var selectedIds by remember { mutableStateOf(editing?.shares?.map { it.personId }?.toSet() ?: people.map { it.id }.toSet()) }
    var customText by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    var showDate by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(editing) {
        if (editing != null) {
            val participantIds = editing.shares.map { it.personId }
            selectedIds = participantIds.toSet()
            val meId = people.firstOrNull { it.isMe }?.id
            val expectedEqual = runCatching { SharedSplit.splitEvenly(editing.expense.totalCents, participantIds, meId) }.getOrNull()
            equalSplit = expectedEqual == editing.shares.associate { it.personId to it.shareCents }
            if (!equalSplit) customText = editing.shares.associate { it.personId to Money.formatNumber(it.shareCents) }
        } else if (selectedIds.isEmpty() && people.isNotEmpty()) {
            selectedIds = people.map { it.id }.toSet()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(if (editing == null) R.string.shared_add_expense else R.string.shared_edit_expense),
            color = NeonColor.ink,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        if (people.size < 2) {
            Text(stringResource(R.string.shared_need_people), color = NeonColor.expenseRed, style = MaterialTheme.typography.bodySmall)
        }
        OutlinedTextField(
            value = title, onValueChange = { title = it },
            label = { Text(stringResource(R.string.shared_concept)) }, singleLine = true,
            modifier = Modifier.fillMaxWidth(), colors = fieldColors(),
        )
        OutlinedTextField(
            value = amountText, onValueChange = { amountText = it.filter { c -> c.isDigit() || c == ',' || c == '.' } },
            label = { Text(stringResource(R.string.shared_amount)) }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(), colors = fieldColors(),
        )
        Text(stringResource(R.string.shared_paid_by_label), color = NeonColor.muted, style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            people.forEach { p ->
                FilterChip(
                    selected = payerId == p.id,
                    onClick = { payerId = p.id },
                    label = { Text(p.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonColor.neonCyan.copy(alpha = 0.2f),
                        selectedLabelColor = NeonColor.neonCyan,
                        labelColor = NeonColor.chipText,
                    ),
                )
            }
        }
        NeonButton(onClick = { showDate = true }, secondary = true, modifier = Modifier.fillMaxWidth()) {
            Text(date.format(RowDateFmt), color = NeonColor.neonCyan)
        }
        Text(stringResource(R.string.shared_split), color = NeonColor.muted, style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = equalSplit, onClick = { equalSplit = true }, label = { Text(stringResource(R.string.shared_split_equal)) }, colors = chipColors())
            FilterChip(selected = !equalSplit, onClick = { equalSplit = false }, label = { Text(stringResource(R.string.shared_split_custom)) }, colors = chipColors())
        }
        Text(stringResource(R.string.shared_participants), color = NeonColor.muted, style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            people.forEach { p ->
                val sel = p.id in selectedIds
                FilterChip(
                    selected = sel,
                    onClick = { selectedIds = if (sel) selectedIds - p.id else selectedIds + p.id },
                    label = { Text(p.name) },
                    colors = chipColors(),
                )
            }
        }
        if (!equalSplit) {
            selectedIds.mapNotNull { id -> people.find { it.id == id } }.forEach { p ->
                OutlinedTextField(
                    value = customText[p.id].orEmpty(),
                    onValueChange = { customText = customText + (p.id to it.filter { c -> c.isDigit() || c == ',' || c == '.' }) },
                    label = { Text(stringResource(R.string.shared_share_for, p.name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(), colors = fieldColors(),
                )
            }
        }
        if (error) Text(stringResource(R.string.shared_error), color = NeonColor.expenseRed, style = MaterialTheme.typography.bodySmall)
        NeonButton(
            onClick = {
                if (saving) return@NeonButton
                val total = runCatching { Money.parse(amountText) }.getOrNull()
                val participants = selectedIds.toList()
                val shares = if (equalSplit && total != null && total > 0L && participants.isNotEmpty()) {
                    SharedSplit.splitEvenly(total, participants, preferPersonId = people.firstOrNull { it.isMe }?.id)
                } else {
                    participants.mapNotNull { id ->
                        runCatching { Money.parse(customText[id].orEmpty()) }.getOrNull()?.let { id to it }
                    }.toMap()
                }
                if (title.trim().isEmpty() || total == null || total <= 0L || payerId <= 0L || shares.size != participants.size) {
                    error = true
                } else {
                    try {
                        SharedSplit.validateCustomShares(total, shares)
                        error = false
                        saving = true
                        onSave(title.trim(), total, payerId, date.toEpochDay(), shares, { saving = false }, { saving = false; error = true })
                    } catch (_: Exception) { error = true }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(if (saving) R.string.share_saving else R.string.common_save), color = NeonColor.bg, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(16.dp))
    }

    if (showDate) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = { TextButton(onClick = {
                pickerState.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                showDate = false
            }) { Text(stringResource(R.string.common_accept), color = NeonColor.neonCyan) } },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text(stringResource(R.string.common_cancel), color = NeonColor.muted) } },
            colors = DatePickerDefaults.colors(containerColor = NeonColor.panel),
        ) { DatePicker(state = pickerState) }
    }
}

@Composable
private fun AddPersonDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NeonColor.panel,
        titleContentColor = NeonColor.ink,
        textContentColor = NeonColor.muted,
        title = { Text(stringResource(R.string.shared_add_person)) },
        text = {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text(stringResource(R.string.shared_person_name)) }, singleLine = true,
                colors = fieldColors(), modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(onClick = { if (name.trim().isNotEmpty()) onAdd(name.trim()) }) { Text(stringResource(R.string.common_save), color = NeonColor.neonCyan) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel), color = NeonColor.muted) } },
    )
}

@Composable private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = NeonColor.neonCyan, unfocusedBorderColor = NeonColor.lineInput,
    focusedLabelColor = NeonColor.neonCyan, unfocusedLabelColor = NeonColor.muted,
    focusedTextColor = NeonColor.ink, unfocusedTextColor = NeonColor.ink, cursorColor = NeonColor.neonCyan,
)

@Composable private fun chipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = NeonColor.neonViolet.copy(alpha = 0.2f),
    selectedLabelColor = NeonColor.neonViolet, labelColor = NeonColor.chipText,
)

private val RowDateFmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es-ES"))

@Preview(showBackground = true, backgroundColor = 0xFF070A12)
@Composable
private fun SharedExpensesPreview() {
    val people = listOf(PersonEntity(1, "Yo", true), PersonEntity(2, "Ana", false))
    val exp = SharedExpenseEntity(1, "Cena", 3000, 1, LocalDate.now().toEpochDay())
    val shares = listOf(SharedShareEntity(1, 1, 1500), SharedShareEntity(1, 2, 1500))
    MiPatrimonioTheme {
        SharedExpensesContent(
            state = SharedExpensesState(
                people = people,
                expenses = listOf(SharedExpenseUi(exp, shares)),
                balances = mapOf(1L to 1500L, 2L to -1500L),
                transfers = listOf(DebtTransfer(2, 1, 1500)),
            ),
            onBack = {}, onAddPerson = { _, _, _ -> }, onAddExpense = { _, _, _, _, _, _, _ -> },
            onUpdateExpense = { _, _, _, _, _, _, _, _ -> }, onDeleteExpense = {}, onSettleExpense = { _, _, _ -> },
        )
    }
}
