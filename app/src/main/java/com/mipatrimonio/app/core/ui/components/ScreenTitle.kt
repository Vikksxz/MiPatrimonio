package com.mipatrimonio.app.core.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.mipatrimonio.app.core.ui.theme.NeonColor

/**
 * ScreenTitle (sección 5.2): título de pantalla en Orbitron, MAYÚSCULAS y letter-spacing ~0.04em.
 * Se usa [MaterialTheme.typography.headlineMedium] (Orbitron 22 sp) definido en Type.kt.
 */
@Composable
fun ScreenTitle(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = NeonColor.ink,
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        color = color,
        style = MaterialTheme.typography.headlineLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
