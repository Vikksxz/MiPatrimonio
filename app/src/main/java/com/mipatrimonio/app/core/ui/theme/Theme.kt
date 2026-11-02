package com.mipatrimonio.app.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Tema oscuro neón de MiPatrimonio (sección 5).
 * Siempre oscuro. Los colores salen de NeonColor, nunca de valores sueltos.
 */
@Composable
fun MiPatrimonioTheme(
    content: @Composable () -> Unit,
) {
    val colorScheme = darkColorScheme(
        primary = NeonColor.neonCyan,
        onPrimary = NeonColor.bg,
        primaryContainer = NeonColor.MovementType.cardBg,
        onPrimaryContainer = NeonColor.MovementType.cardText,
        secondary = NeonColor.neonViolet,
        onSecondary = NeonColor.bg,
        secondaryContainer = NeonColor.MovementType.subscriptionBg,
        onSecondaryContainer = NeonColor.MovementType.subscriptionText,
        tertiary = NeonColor.neonMagenta,
        onTertiary = NeonColor.bg,
        tertiaryContainer = NeonColor.alertErrorBg,
        onTertiaryContainer = NeonColor.alertErrorText,
        background = NeonColor.bg,
        onBackground = NeonColor.ink,
        surface = NeonColor.panel,
        onSurface = NeonColor.ink,
        surfaceVariant = NeonColor.panelAlt,
        onSurfaceVariant = NeonColor.chipText,
        surfaceTint = NeonColor.neonCyan,
        inverseSurface = NeonColor.ink,
        inverseOnSurface = NeonColor.bg,
        inversePrimary = NeonColor.neonViolet,
        outline = NeonColor.line,
        outlineVariant = NeonColor.lineInput,
        error = NeonColor.expenseRed,
        onError = NeonColor.bg,
        errorContainer = NeonColor.alertErrorBg,
        onErrorContainer = NeonColor.alertErrorText,
        scrim = NeonColor.bg,
    )
    MaterialTheme(
        colorScheme = colorScheme,
        typography = NeonTypography,
        shapes = NeonShapes,
        content = content,
    )
}
