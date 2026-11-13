package com.mipatrimonio.app.core.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Brillo neón radial barato (T5.2 / guía mipatrimonio-design). Usar solo en momentos definidos.
 */
fun Modifier.neonGlow(
    color: Color,
    radius: Dp = 24.dp,
    alpha: Float = 0.35f,
): Modifier = drawBehind {
    val r = radius.toPx() + size.maxDimension / 2f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = alpha), Color.Transparent),
            center = Offset(size.width / 2f, size.height / 2f),
            radius = r,
        ),
        radius = r,
        center = Offset(size.width / 2f, size.height / 2f),
    )
}
