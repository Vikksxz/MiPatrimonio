package com.mipatrimonio.app.feature.add

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mipatrimonio.app.R
import com.mipatrimonio.app.core.ui.components.NeonCard
import com.mipatrimonio.app.core.ui.components.ScreenTitle
import com.mipatrimonio.app.core.ui.theme.MiPatrimonioTheme
import com.mipatrimonio.app.core.ui.theme.NeonColor
import com.mipatrimonio.app.core.ui.theme.NeonShape

/**
 * Hoja del botón + (T1.7, §8.3; T2.5 escaneo OCR; T5.7 textos/iconos cercanos).
 */
@Composable
fun AddMenuSheet(
    onScan: () -> Unit,
    onManualExpense: () -> Unit,
    onIncome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenTitle(text = stringResource(R.string.add_sheet_title))

        AddMenuOption(
            title = stringResource(R.string.add_sheet_scan),
            subtitle = stringResource(R.string.add_sheet_scan_hint),
            icon = Icons.Filled.Search,
            accent = NeonColor.neonViolet,
            onClick = onScan,
        )
        AddMenuOption(
            title = stringResource(R.string.add_sheet_manual),
            subtitle = stringResource(R.string.add_sheet_manual_hint),
            icon = Icons.Filled.Edit,
            accent = NeonColor.neonAmber,
            onClick = onManualExpense,
        )
        AddMenuOption(
            title = stringResource(R.string.add_sheet_income),
            subtitle = stringResource(R.string.add_sheet_income_hint),
            icon = Icons.Filled.Add,
            accent = NeonColor.neonGreen,
            onClick = onIncome,
        )
    }
}

@Composable
private fun AddMenuOption(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val alpha = if (enabled) 1f else 0.5f
    NeonCard(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .clip(RoundedCornerShape(NeonShape.cardRadius))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = NeonColor.ink,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = subtitle,
                    color = NeonColor.muted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0E1424)
@Composable
private fun AddMenuSheetPreview() {
    MiPatrimonioTheme {
        AddMenuSheet(onScan = {}, onManualExpense = {}, onIncome = {})
    }
}
