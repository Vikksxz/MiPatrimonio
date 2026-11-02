package com.mipatrimonio.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mipatrimonio.app.core.ui.theme.NeonColor
import com.mipatrimonio.app.core.ui.theme.NeonShape

/**
 * NeonCard (sección 5.3): panel con fondo panel, borde 1 dp line, radio 18 dp y
 * brillo de acento opcional (sombra con alfa ~0.25, radio 24 dp).
 *
 * @param glowColor color del brillo; null para panel sin brillo.
 */
@Composable
fun NeonCard(
    modifier: Modifier = Modifier,
    glowColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(NeonShape.panelRadius)
    val glowModifier = if (glowColor != null) {
        Modifier.shadow(
            elevation = 24.dp,
            shape = shape,
            clip = false,
            ambientColor = glowColor.copy(alpha = 0.25f),
            spotColor = glowColor.copy(alpha = 0.25f),
        )
    } else {
        Modifier
    }

    Column(
        modifier = modifier
            .then(glowModifier)
            .background(color = NeonColor.panel, shape = shape)
            .border(width = 1.dp, color = NeonColor.line, shape = shape)
            .padding(16.dp),
        content = content,
    )
}
