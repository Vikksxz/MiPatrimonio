package com.mipatrimonio.app.core.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mipatrimonio.app.R
import com.mipatrimonio.app.core.ui.navigation.Routes
import com.mipatrimonio.app.core.ui.theme.MiPatrimonioTheme
import com.mipatrimonio.app.core.ui.theme.NeonColor

/** Pestaña de la barra inferior. */
data class NeonTab(
    val route: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
)

/** Orden visual: Inicio, Movimientos, +, Ahorro, Cartera (sección 8). */
val neonTabs = listOf(
    NeonTab(Routes.HOME, R.string.tab_home, Icons.Filled.Home),
    NeonTab(Routes.TRANSACTIONS, R.string.tab_transactions, Icons.AutoMirrored.Filled.List),
)

val neonTabsRight = listOf(
    NeonTab(Routes.SAVINGS, R.string.tab_savings, Icons.Filled.Star),
    NeonTab(Routes.WALLET, R.string.tab_wallet, Icons.Filled.AccountBox),
)

/**
 * Barra inferior con 4 pestañas y botón central + (sección 8 de MiPatrimonio.md).
 * Fondo #0B1020, borde superior line, pestaña activa en neonCyan con brillo de texto.
 * La pestaña activa añade un indicador superior y más peso tipográfico: la selección
 * no se comunica solo por color (sección 5.1).
 */
@Composable
fun NeonTabBar(
    selectedRoute: String?,
    onTabSelected: (String) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(NeonColor.navBar)
            .navigationBarsPadding(),
    ) {
        HorizontalDivider(thickness = 1.dp, color = NeonColor.line)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            neonTabs.forEach { tab ->
                TabItem(
                    tab = tab,
                    selected = selectedRoute == tab.route,
                    onClick = { onTabSelected(tab.route) },
                )
            }
            AddButton(onClick = onAddClick)
            neonTabsRight.forEach { tab ->
                TabItem(
                    tab = tab,
                    selected = selectedRoute == tab.route,
                    onClick = { onTabSelected(tab.route) },
                )
            }
        }
    }
}

@Composable
private fun RowScope.TabItem(
    tab: NeonTab,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val color = if (selected) NeonColor.neonCyan else NeonColor.muted
    Column(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 64.dp)
            .selectable(
                selected = selected,
                role = Role.Tab,
                onClick = onClick,
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Indicador de selección, independiente del color.
        Box(
            modifier = Modifier
                .size(width = 16.dp, height = 2.dp)
                .background(if (selected) color else androidx.compose.ui.graphics.Color.Transparent),
        )
        Icon(
            imageVector = tab.icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .padding(top = 4.dp)
                .size(24.dp),
        )
        Text(
            text = stringResource(tab.labelRes),
            color = color,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                shadow = if (selected) {
                    Shadow(color = NeonColor.neonCyan, blurRadius = 8f)
                } else {
                    null
                },
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Botón central +: círculo de 56 dp con degradado cian → violeta y brillo cian.
 */
@Composable
private fun RowScope.AddButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 64.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    clip = false,
                    ambientColor = NeonColor.neonCyan.copy(alpha = 0.35f),
                    spotColor = NeonColor.neonCyan.copy(alpha = 0.35f),
                )
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(NeonColor.neonCyan, NeonColor.neonViolet)))
                .clickable(role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(R.string.add_sheet_title),
                tint = NeonColor.bg,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF070A12)
@Composable
private fun NeonTabBarPreview() {
    MiPatrimonioTheme {
        NeonTabBar(
            selectedRoute = Routes.HOME,
            onTabSelected = {},
            onAddClick = {},
        )
    }
}
