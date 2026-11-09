package com.mipatrimonio.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mipatrimonio.app.R
import com.mipatrimonio.app.core.settings.HomeBlock
import com.mipatrimonio.app.core.settings.HomeBlockConfig
import com.mipatrimonio.app.core.ui.components.NeonCard
import com.mipatrimonio.app.core.ui.theme.NeonColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeSettingsViewModel = hiltViewModel(),
) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    Scaffold(
        modifier = modifier,
        containerColor = NeonColor.bg,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_settings_title), color = NeonColor.ink) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = NeonColor.neonCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NeonColor.bg),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.home_settings_hint),
                    color = NeonColor.muted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            itemsIndexed(config, key = { _, item -> item.block.key }) { index, item ->
                HomeBlockRow(
                    config = item,
                    isFirst = index == 0,
                    isLast = index == config.lastIndex,
                    onToggle = { viewModel.setVisible(item.block.key, it) },
                    onMoveUp = { viewModel.move(item.block.key, -1) },
                    onMoveDown = { viewModel.move(item.block.key, +1) },
                )
            }
        }
    }
}

@Composable
private fun HomeBlockRow(
    config: HomeBlockConfig,
    isFirst: Boolean,
    isLast: Boolean,
    onToggle: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    NeonCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = blockLabel(config.block),
                    color = NeonColor.ink,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            IconButton(onClick = onMoveUp, enabled = !isFirst) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = stringResource(R.string.home_settings_move_up), tint = if (isFirst) NeonColor.muted else NeonColor.neonCyan)
            }
            IconButton(onClick = onMoveDown, enabled = !isLast) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = stringResource(R.string.home_settings_move_down), tint = if (isLast) NeonColor.muted else NeonColor.neonCyan)
            }
            Switch(
                checked = config.visible,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = NeonColor.bg,
                    checkedTrackColor = NeonColor.neonCyan,
                    uncheckedThumbColor = NeonColor.muted,
                    uncheckedTrackColor = NeonColor.panelAlt,
                ),
            )
        }
    }
}

@Composable
private fun blockLabel(block: HomeBlock): String = stringResource(
    when (block) {
        HomeBlock.BALANCE -> R.string.home_block_balance
        HomeBlock.SAVINGS_PLAN -> R.string.home_block_savings_plan
        HomeBlock.BREAKDOWN -> R.string.home_block_breakdown
        HomeBlock.ALERTS -> R.string.home_block_alerts
        HomeBlock.PROJECTION -> R.string.home_block_projection
        HomeBlock.RECENT -> R.string.home_block_recent
    },
)
