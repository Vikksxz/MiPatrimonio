package com.mipatrimonio.app.feature.savings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mipatrimonio.app.R
import com.mipatrimonio.app.core.database.entity.CategoryEntity
import com.mipatrimonio.app.core.money.Money
import com.mipatrimonio.app.core.ui.components.NeonButton
import com.mipatrimonio.app.core.ui.components.NeonButtonText
import com.mipatrimonio.app.core.ui.components.NeonCard
import com.mipatrimonio.app.core.ui.components.ProgressNeonBar
import com.mipatrimonio.app.core.ui.components.ScreenTitle
import com.mipatrimonio.app.core.ui.theme.MiPatrimonioTheme
import com.mipatrimonio.app.core.ui.theme.NeonColor
import com.mipatrimonio.app.core.ui.theme.NeonShape
import com.mipatrimonio.app.domain.usecase.BudgetLevel
import com.mipatrimonio.app.domain.usecase.EstimateSavings
import com.mipatrimonio.app.domain.usecase.SavingsStatus
import java.time.YearMonth
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SavingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showPlanSheet by remember { mutableStateOf(false) }
    var editingPlanId by remember { mutableStateOf<Long?>(null) }
    var showContribute by remember { mutableStateOf(false) }
    var contributePlanId by remember { mutableStateOf<Long?>(null) }
    var showBudgetSheet by remember { mutableStateOf(false) }
    var editingBudgetId by remember { mutableStateOf<Long?>(null) }

    SavingsContent(
        state = state,
        modifier = modifier,
        onCreatePlan = { editingPlanId = null; showPlanSheet = true },
        onEditPlan = { editingPlanId = it; showPlanSheet = true },
        onChangeQuota = viewModel::changeQuota,
        onSetActive = viewModel::setActive,
        onContribute = { contributePlanId = it; showContribute = true },
        onMovePlan = viewModel::movePlan,
        onAddBudget = { editingBudgetId = null; showBudgetSheet = true },
        onEditBudget = { editingBudgetId = it; showBudgetSheet = true },
    )

    if (showPlanSheet) {
        val editing = editingPlanId?.let { id -> state.plans.firstOrNull { it.plan.id == id } }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showPlanSheet = false },
            sheetState = sheetState,
            containerColor = NeonColor.panel,
            contentColor = NeonColor.ink,
        ) {
            PlanEditSheet(
                initialName = editing?.plan?.name.orEmpty(),
                initialTargetCents = editing?.plan?.targetCents,
                isEdit = editing != null,
                onSave = { name, target ->
                    if (editing != null) viewModel.updatePlan(editing.plan.id, name, target)
                    else viewModel.createPlan(name, target)
                    showPlanSheet = false
                },
                onDelete = if (editing != null) {
                    {
                        viewModel.deletePlan(editing.plan.id)
                        showPlanSheet = false
                    }
                } else null,
                onDismiss = { showPlanSheet = false },
            )
        }
    }

    if (showContribute && contributePlanId != null) {
        ContributeDialog(
            onConfirm = { amount ->
                viewModel.addContribution(contributePlanId!!, amount)
                showContribute = false
            },
            onDismiss = { showContribute = false },
        )
    }

    if (showBudgetSheet) {
        val editing = editingBudgetId?.let { id -> state.budgets.firstOrNull { it.budget.id == id } }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showBudgetSheet = false },
            sheetState = sheetState,
            containerColor = NeonColor.panel,
            contentColor = NeonColor.ink,
        ) {
            BudgetEditSheet(
                categories = state.expenseCategories,
                initialCategoryId = editing?.budget?.categoryId,
                initialLimitCents = editing?.budget?.limitCents,
                existingId = editing?.budget?.id,
                onSave = { catId, limit, existingId ->
                    viewModel.saveBudget(catId, limit, existingId)
                    showBudgetSheet = false
                },
                onDelete = if (editing != null) {
                    { viewModel.deleteBudget(editing.budget.id); showBudgetSheet = false }
                } else null,
                onDismiss = { showBudgetSheet = false },
            )
        }
    }
}

@Composable
internal fun SavingsContent(
    state: SavingsUiState,
    modifier: Modifier = Modifier,
    onCreatePlan: () -> Unit = {},
    onEditPlan: (Long) -> Unit = {},
    onChangeQuota: (Long, Long) -> Unit = { _, _ -> },
    onSetActive: (Long) -> Unit = {},
    onContribute: (Long) -> Unit = {},
    onMovePlan: (Long, Boolean) -> Unit = { _, _ -> },
    onAddBudget: () -> Unit = {},
    onEditBudget: (Long) -> Unit = {},
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenTitle(text = stringResource(R.string.tab_savings)) }

        if (!state.loaded) return@LazyColumn

        val active = state.activePlan
        if (active != null) {
            item(key = "active-${active.plan.id}") {
                ActivePlanCard(
                    item = active,
                    onEdit = { onEditPlan(active.plan.id) },
                    onQuotaDelta = { delta -> onChangeQuota(active.plan.id, delta) },
                    onContribute = { onContribute(active.plan.id) },
                )
            }
        } else {
            item(key = "no-active") {
                NeonCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.savings_no_active_plan),
                        style = MaterialTheme.typography.bodyMedium,
                        color = NeonColor.muted,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.savings_plans_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = NeonColor.ink,
                )
            }
        }

        if (state.plans.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.savings_no_plans),
                    style = MaterialTheme.typography.bodyMedium,
                    color = NeonColor.muted,
                )
            }
        } else {
            items(items = state.plans, key = { it.plan.id }) { item ->
                val index = state.plans.indexOfFirst { it.plan.id == item.plan.id }
                PlanRow(
                    item = item,
                    isActive = item.plan.id == active?.plan?.id,
                    canMoveUp = index > 0,
                    canMoveDown = index >= 0 && index < state.plans.lastIndex,
                    onSetActive = { onSetActive(item.plan.id) },
                    onEdit = { onEditPlan(item.plan.id) },
                    onMoveUp = { onMovePlan(item.plan.id, true) },
                    onMoveDown = { onMovePlan(item.plan.id, false) },
                )
            }
        }

        item {
            NeonButton(onClick = onCreatePlan, modifier = Modifier.fillMaxWidth()) {
                NeonButtonText(text = stringResource(R.string.savings_create_plan))
            }
        }

        // --- Presupuestos (T3.4, §7.4/§8.4) ---
        item {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.budgets_title),
                style = MaterialTheme.typography.titleMedium,
                color = NeonColor.ink,
            )
        }
        if (state.budgets.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.budgets_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = NeonColor.muted,
                )
            }
        } else {
            items(items = state.budgets, key = { "budget-${it.budget.id}" }) { item ->
                BudgetRow(item = item, onEdit = { onEditBudget(item.budget.id) })
            }
        }
        item {
            NeonButton(onClick = onAddBudget, secondary = true, modifier = Modifier.fillMaxWidth()) {
                NeonButtonText(text = stringResource(R.string.budgets_add), secondary = true)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ActivePlanCard(
    item: PlanItemUi,
    onEdit: () -> Unit,
    onQuotaDelta: (Long) -> Unit,
    onContribute: () -> Unit,
) {
    val plan = item.plan
    val estimate = item.estimate
    val progress = if (plan.targetCents > 0L) {
        (item.contributedCents.toDouble() / plan.targetCents.toDouble()).toFloat().coerceIn(0f, 1f)
    } else 0f

    NeonCard(glowColor = NeonColor.neonGreen, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = plan.name, style = MaterialTheme.typography.titleMedium, color = NeonColor.ink, fontWeight = FontWeight.Bold)

            ProgressNeonBar(progress = progress, modifier = Modifier.fillMaxWidth())

            Text(
                text = stringResource(R.string.savings_saved_of_target, Money.format(item.contributedCents), Money.format(plan.targetCents)),
                style = MaterialTheme.typography.bodyMedium,
                color = NeonColor.ink,
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(text = stringResource(R.string.savings_target_readonly), style = MaterialTheme.typography.labelSmall, color = NeonColor.muted)
                    Text(text = Money.format(plan.targetCents), style = MaterialTheme.typography.bodyMedium, color = NeonColor.ink)
                }
                TextButton(onClick = onEdit) {
                    Text(stringResource(R.string.savings_edit_plan), color = NeonColor.neonCyan)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(R.string.savings_monthly_quota), style = MaterialTheme.typography.bodyMedium, color = NeonColor.ink)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val decDesc = stringResource(R.string.savings_decrease_quota)
                    IconButton(
                        onClick = { onQuotaDelta(-EstimateSavings.QUOTA_STEP_CENTS) },
                        modifier = Modifier.size(44.dp).semantics { contentDescription = decDesc },
                    ) { Text(text = "−", color = NeonColor.neonCyan, style = MaterialTheme.typography.titleLarge) }
                    Text(text = Money.format(plan.monthlyQuotaCents), style = MaterialTheme.typography.bodyMedium, color = NeonColor.ink, modifier = Modifier.padding(horizontal = 4.dp))
                    val incDesc = stringResource(R.string.savings_increase_quota)
                    IconButton(
                        onClick = { onQuotaDelta(EstimateSavings.QUOTA_STEP_CENTS) },
                        modifier = Modifier.size(44.dp).semantics { contentDescription = incDesc },
                    ) { Icon(Icons.Filled.Add, contentDescription = null, tint = NeonColor.neonCyan) }
                }
            }

            val reachesText = if (estimate.goalReached) {
                stringResource(R.string.savings_reaches_now)
            } else {
                val monthLabel = if (estimate.monthsEstimated == 1) stringResource(R.string.savings_months_one) else stringResource(R.string.savings_months_other)
                stringResource(R.string.savings_reaches_in, estimate.monthsEstimated, monthLabel, estimate.targetYearMonth.toMonthYearLabel())
            }
            Text(text = reachesText, style = MaterialTheme.typography.bodySmall, color = NeonColor.muted)

            val (statusText, statusColor, statusBg) = when (estimate.status) {
                SavingsStatus.BEHIND -> Triple(stringResource(R.string.savings_status_behind), NeonColor.expenseRed, NeonColor.expenseRed.copy(alpha = 0.15f))
                SavingsStatus.AHEAD -> Triple(stringResource(R.string.savings_status_ahead), NeonColor.neonGreen, NeonColor.neonGreen.copy(alpha = 0.15f))
                SavingsStatus.ON_TRACK -> Triple(stringResource(R.string.savings_status_on_track), NeonColor.neonGreen, NeonColor.neonGreen.copy(alpha = 0.15f))
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(NeonShape.chipRadius))
                    .background(statusBg)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(text = statusText, style = MaterialTheme.typography.bodySmall, color = statusColor)
            }

            NeonButton(onClick = onContribute, secondary = true, modifier = Modifier.fillMaxWidth()) {
                NeonButtonText(text = stringResource(R.string.savings_contribute), secondary = true)
            }
        }
    }
}



@Composable
private fun PlanRow(
    item: PlanItemUi,
    isActive: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onSetActive: () -> Unit,
    onEdit: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    NeonCard(modifier = Modifier.fillMaxWidth().clickable { onEdit() }) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val activeDesc = stringResource(R.string.savings_set_active)
            RadioButton(
                selected = isActive,
                onClick = onSetActive,
                modifier = Modifier.semantics { contentDescription = activeDesc },
                colors = RadioButtonDefaults.colors(selectedColor = NeonColor.neonCyan, unselectedColor = NeonColor.muted),
            )
            Spacer(Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.plan.name, style = MaterialTheme.typography.bodyLarge, color = NeonColor.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = stringResource(R.string.savings_saved_of_target, Money.format(item.contributedCents), Money.format(item.plan.targetCents)),
                    style = MaterialTheme.typography.bodySmall,
                    color = NeonColor.muted,
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val upDesc = stringResource(R.string.savings_move_up)
                IconButton(
                    onClick = onMoveUp,
                    enabled = canMoveUp,
                    modifier = Modifier.size(44.dp).semantics { contentDescription = upDesc },
                ) {
                    Text("▲", color = if (canMoveUp) NeonColor.neonCyan else NeonColor.lineInput, style = MaterialTheme.typography.labelSmall)
                }
                val downDesc = stringResource(R.string.savings_move_down)
                IconButton(
                    onClick = onMoveDown,
                    enabled = canMoveDown,
                    modifier = Modifier.size(44.dp).semantics { contentDescription = downDesc },
                ) {
                    Text("▼", color = if (canMoveDown) NeonColor.neonCyan else NeonColor.lineInput, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun PlanEditSheet(
    initialName: String,
    initialTargetCents: Long?,
    isEdit: Boolean,
    onSave: (String, Long) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var targetText by remember { mutableStateOf(initialTargetCents?.let { Money.formatNumber(it) } ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val invalidName = stringResource(R.string.savings_invalid_name)
    val invalidAmount = stringResource(R.string.savings_invalid_amount)

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(if (isEdit) R.string.savings_sheet_edit else R.string.savings_sheet_create),
            style = MaterialTheme.typography.titleLarge,
            color = NeonColor.ink,
        )
        SavingsTextField(value = name, onValueChange = { name = it }, label = stringResource(R.string.savings_plan_name))
        SavingsTextField(
            value = targetText,
            onValueChange = { targetText = it },
            label = stringResource(R.string.savings_plan_target),
            keyboardType = KeyboardType.Decimal,
        )
        Text(text = stringResource(R.string.savings_plan_help), style = MaterialTheme.typography.bodySmall, color = NeonColor.muted)
        if (error != null) Text(text = error!!, color = NeonColor.expenseRed, style = MaterialTheme.typography.bodySmall)

        NeonButton(
            onClick = {
                if (name.trim().isEmpty()) { error = invalidName; return@NeonButton }
                val cents = try { Money.parse(targetText) } catch (_: Exception) { null }
                if (cents == null || cents <= 0L) { error = invalidAmount; return@NeonButton }
                onSave(name, cents)
            },
            modifier = Modifier.fillMaxWidth(),
        ) { NeonButtonText(text = stringResource(R.string.savings_save_plan)) }

        if (isEdit && onDelete != null) {
            NeonButton(onClick = { showDeleteConfirm = true }, secondary = true, accent = NeonColor.expenseRed, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.savings_delete_plan), color = NeonColor.expenseRed, style = MaterialTheme.typography.labelLarge)
            }
        }
    }

    if (showDeleteConfirm && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = NeonColor.panel,
            title = { Text(stringResource(R.string.savings_delete_plan), color = NeonColor.ink) },
            text = { Text(stringResource(R.string.savings_delete_plan_confirm), color = NeonColor.muted) },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) {
                    Text(stringResource(R.string.savings_delete_plan), color = NeonColor.expenseRed)
                }
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
private fun ContributeDialog(onConfirm: (Long) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val invalidAmount = stringResource(R.string.savings_invalid_amount)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NeonColor.panel,
        title = { Text(stringResource(R.string.savings_contribute), color = NeonColor.ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SavingsTextField(
                    value = text,
                    onValueChange = { text = it; error = false },
                    label = stringResource(R.string.savings_contribution_amount),
                    keyboardType = KeyboardType.Decimal,
                )
                if (error) Text(invalidAmount, color = NeonColor.expenseRed, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cents = try { Money.parse(text) } catch (_: Exception) { null }
                if (cents == null || cents <= 0L) { error = true } else onConfirm(cents)
            }) { Text(stringResource(R.string.savings_contribution_confirm), color = NeonColor.neonCyan) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel), color = NeonColor.muted) }
        },
    )
}

@Composable
private fun SavingsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
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

// --- Presupuestos (T3.4) ---

@Composable
private fun BudgetRow(item: BudgetItemUi, onEdit: () -> Unit) {
    val catName = item.category?.name ?: stringResource(R.string.budgets_category)
    val barColor = when (item.status.level) {
        BudgetLevel.OK -> NeonColor.neonCyan
        BudgetLevel.WARNING -> NeonColor.neonAmber
        BudgetLevel.OVER -> NeonColor.expenseRed
    }
    val progress = if (item.budget.limitCents > 0L) {
        (item.status.spentCents.toDouble() / item.budget.limitCents.toDouble()).toFloat().coerceIn(0f, 1f)
    } else 0f
    NeonCard(modifier = Modifier.fillMaxWidth().clickable { onEdit() }) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = catName, style = MaterialTheme.typography.bodyLarge, color = NeonColor.ink, modifier = Modifier.weight(1f))
                Text(text = stringResource(R.string.budgets_percent, item.status.percent), style = MaterialTheme.typography.labelMedium, color = barColor)
            }
            BudgetBar(progress = progress, color = barColor)
            Text(
                text = stringResource(R.string.budgets_spent_of_limit, Money.format(item.status.spentCents), Money.format(item.budget.limitCents)),
                style = MaterialTheme.typography.bodySmall,
                color = NeonColor.muted,
            )
        }
    }
}

@Composable
private fun BudgetBar(progress: Float, color: Color) {
    val shape = RoundedCornerShape(8.dp)
    Box(modifier = Modifier.fillMaxWidth().height(10.dp).clip(shape).background(NeonColor.track)) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(10.dp)
                .clip(shape)
                .background(color),
        )
    }
}

@Composable
private fun BudgetEditSheet(
    categories: List<CategoryEntity>,
    initialCategoryId: Long?,
    initialLimitCents: Long?,
    existingId: Long?,
    onSave: (Long, Long, Long?) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var selectedCategoryId by remember { mutableStateOf(initialCategoryId ?: categories.firstOrNull()?.id) }
    var limitText by remember { mutableStateOf(initialLimitCents?.let { Money.formatNumber(it) } ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val invalidAmount = stringResource(R.string.savings_invalid_amount)
    val noCategories = stringResource(R.string.budgets_no_categories)

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(if (existingId != null) R.string.budgets_edit else R.string.budgets_add),
            style = MaterialTheme.typography.titleLarge,
            color = NeonColor.ink,
        )
        if (categories.isEmpty()) {
            Text(stringResource(R.string.budgets_no_categories), color = NeonColor.muted)
        } else {
            Text(stringResource(R.string.budgets_category), style = MaterialTheme.typography.labelLarge, color = NeonColor.muted)
            Column(
                modifier = Modifier.heightIn(max = 180.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                categories.forEach { cat ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(NeonShape.inputRadius)).clickable { selectedCategoryId = cat.id }.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedCategoryId == cat.id,
                            onClick = { selectedCategoryId = cat.id },
                            colors = RadioButtonDefaults.colors(selectedColor = NeonColor.neonCyan, unselectedColor = NeonColor.muted),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(cat.name, color = NeonColor.ink)
                    }
                }
            }
        }
        SavingsTextField(
            value = limitText,
            onValueChange = { limitText = it },
            label = stringResource(R.string.budgets_limit),
            keyboardType = KeyboardType.Decimal,
        )
        if (error != null) Text(error!!, color = NeonColor.expenseRed, style = MaterialTheme.typography.bodySmall)

        NeonButton(
            onClick = {
                val catId = selectedCategoryId
                val cents = try { Money.parse(limitText) } catch (_: Exception) { null }
                if (catId == null) { error = noCategories; return@NeonButton }
                if (cents == null || cents <= 0L) { error = invalidAmount; return@NeonButton }
                onSave(catId, cents, existingId)
            },
            modifier = Modifier.fillMaxWidth(),
        ) { NeonButtonText(text = stringResource(R.string.budgets_save)) }

        if (existingId != null && onDelete != null) {
            NeonButton(onClick = { showDeleteConfirm = true }, secondary = true, accent = NeonColor.expenseRed, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.budgets_delete), color = NeonColor.expenseRed, style = MaterialTheme.typography.labelLarge)
            }
        }
    }

    if (showDeleteConfirm && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = NeonColor.panel,
            title = { Text(stringResource(R.string.budgets_delete), color = NeonColor.ink) },
            text = { Text(stringResource(R.string.budgets_delete_confirm), color = NeonColor.muted) },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) {
                    Text(stringResource(R.string.budgets_delete), color = NeonColor.expenseRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.common_cancel), color = NeonColor.neonCyan)
                }
            },
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF070A12)
@Composable
private fun SavingsScreenPreview() {
    MiPatrimonioTheme {
        SavingsContent(state = SavingsUiState(loaded = true))
    }
}
