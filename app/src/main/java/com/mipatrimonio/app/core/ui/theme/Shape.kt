package com.mipatrimonio.app.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Formas de MiPatrimonio.md sección 5.3.
 * Paneles: radio 16–20 dp. Botones: radio 14 dp.
 */
object NeonShape {
    val panelRadius = 18.dp
    val cardRadius = 16.dp
    val buttonRadius = 14.dp
    val chipRadius = 12.dp
    val inputRadius = 12.dp
}

val NeonShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(NeonShape.chipRadius),
    medium = RoundedCornerShape(NeonShape.buttonRadius),
    large = RoundedCornerShape(NeonShape.panelRadius),
    extraLarge = RoundedCornerShape(24.dp),
)
