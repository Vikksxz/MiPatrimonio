package com.mipatrimonio.app.feature.add

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mipatrimonio.app.R
import com.mipatrimonio.app.core.ui.components.NeonCard
import com.mipatrimonio.app.core.ui.components.ScreenTitle
import com.mipatrimonio.app.core.ui.theme.MiPatrimonioTheme
import com.mipatrimonio.app.core.ui.theme.NeonColor

/**
 * Hoja del botón + (T1.7, MiPatrimonio.md §8.3): tres opciones.
 * El escaneo de ticket llega en T2.5; hasta entonces se muestra deshabilitado.
 */
@Composable
fun AddMenuSheet(
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
            subtitle = stringResource(R.string.add_sheet_scan_soon),
            enabled = false,
            onClick = {},
        )
        AddMenuOption(
            title = stringResource(R.string.add_sheet_manual),
            subtitle = stringResource(R.string.add_sheet_manual_hint),
            onClick = onManualExpense,
        )
        AddMenuOption(
            title = stringResource(R.string.add_sheet_income),
            subtitle = stringResource(R.string.add_sheet_income_hint),
            onClick = onIncome,
        )
    }
}

@Composable
private fun AddMenuOption(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val alpha = if (enabled) 1f else 0.5f
    NeonCard(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
    ) {
        Text(
            text = title,
            color = NeonColor.ink,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = subtitle,
            color = NeonColor.muted,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0E1424)
@Composable
private fun AddMenuSheetPreview() {
    MiPatrimonioTheme {
        AddMenuSheet(onManualExpense = {}, onIncome = {})
    }
}
