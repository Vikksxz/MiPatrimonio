package com.mipatrimonio.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.mipatrimonio.app.core.ui.theme.NeonColor
import com.mipatrimonio.app.core.ui.theme.NeonShape

/**
 * NeonButton (sección 5.3).
 * Primario: degradado horizontal neonCyan → neonViolet, texto #070A12, negrita,
 * alto mínimo 48 dp y radio 14 dp.
 * Secundario: fondo transparente, borde y texto del acento indicado.
 */
@Composable
fun NeonButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    secondary: Boolean = false,
    accent: Color = NeonColor.neonCyan,
    content: @Composable RowScope.() -> Unit,
) {
    val shape = RoundedCornerShape(NeonShape.buttonRadius)
    val backgroundModifier = if (secondary) {
        Modifier.background(color = Color.Transparent)
    } else {
        Modifier.background(
            brush = Brush.horizontalGradient(
                listOf(NeonColor.neonCyan, NeonColor.neonViolet),
            ),
        )
    }
    val borderModifier = if (secondary) {
        Modifier.border(width = 1.dp, color = accent)
    } else {
        Modifier
    }

    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(shape)
            .then(backgroundModifier)
            .then(borderModifier)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/**
 * Texto de botón. Usar dentro de [NeonButton] para que el color coincida con la variante.
 */
@Composable
fun NeonButtonText(
    text: String,
    secondary: Boolean = false,
    modifier: Modifier = Modifier,
    accent: Color = NeonColor.neonCyan,
) {
    Text(
        text = text,
        modifier = modifier,
        color = if (secondary) accent else NeonColor.bg,
        style = MaterialTheme.typography.labelLarge,
    )
}
