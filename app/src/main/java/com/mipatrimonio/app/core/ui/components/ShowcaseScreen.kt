package com.mipatrimonio.app.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mipatrimonio.app.R
import com.mipatrimonio.app.core.ui.theme.MiPatrimonioTheme
import com.mipatrimonio.app.core.ui.theme.NeonColor

/**
 * ShowcaseScreen (T1.2): pantalla de muestra con todos los componentes del tema neón.
 * Criterio de aceptación: NeonCard, NeonButton (primario y secundario), NeonChip (4 tipos)
 * y ScreenTitle con los colores de la sección 5.1.
 */
@Composable
fun ShowcaseScreen(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = NeonColor.bg,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ScreenTitle(text = stringResource(R.string.showcase_title))

            NeonCard(
                modifier = Modifier.fillMaxWidth(),
                glowColor = NeonColor.neonCyan,
            ) {
                Text(
                    text = stringResource(R.string.showcase_amount),
                    color = NeonColor.neonCyan,
                    style = MaterialTheme.typography.displayMedium,
                )
                Text(
                    text = stringResource(R.string.showcase_card_title),
                    color = NeonColor.ink,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.showcase_card_body),
                    color = NeonColor.muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            NeonButton(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
            ) {
                NeonButtonText(text = stringResource(R.string.showcase_button_primary))
            }

            NeonButton(
                onClick = {},
                secondary = true,
                modifier = Modifier.fillMaxWidth(),
            ) {
                NeonButtonText(
                    text = stringResource(R.string.showcase_button_secondary),
                    secondary = true,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NeonChip(
                    text = stringResource(R.string.showcase_chip_cash),
                    type = MovementChipType.CASH,
                )
                NeonChip(
                    text = stringResource(R.string.showcase_chip_card),
                    type = MovementChipType.CARD,
                    selected = true,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NeonChip(
                    text = stringResource(R.string.showcase_chip_subscription),
                    type = MovementChipType.SUBSCRIPTION,
                    selected = true,
                )
                NeonChip(
                    text = stringResource(R.string.showcase_chip_income),
                    type = MovementChipType.INCOME,
                    selected = true,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF070A12)
@Composable
private fun ShowcaseScreenPreview() {
    MiPatrimonioTheme {
        ShowcaseScreen()
    }
}
