package com.mipatrimonio.app.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mipatrimonio.app.R

/**
 * Tipografía de MiPatrimonio.md sección 5.2.
 * Orbitron: títulos de pantalla en MAYÚSCULAS y cifras grandes.
 * Space Grotesk: resto del texto.
 * Fuentes variables OFL en res/font; se fija el eje de peso con [FontVariation].
 */
private fun orbitron(weight: FontWeight) = Font(
    resId = R.font.orbitron_variable,
    weight = weight,
    style = FontStyle.Normal,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

private fun spaceGrotesk(weight: FontWeight) = Font(
    resId = R.font.space_grotesk_variable,
    weight = weight,
    style = FontStyle.Normal,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val Orbitron = FontFamily(
    orbitron(FontWeight.Normal),
    orbitron(FontWeight.SemiBold),
    orbitron(FontWeight.Bold),
)

val SpaceGrotesk = FontFamily(
    spaceGrotesk(FontWeight.Normal),
    spaceGrotesk(FontWeight.Medium),
    spaceGrotesk(FontWeight.SemiBold),
    spaceGrotesk(FontWeight.Bold),
)

/** Tamaños de referencia de la sección 5.2. El mínimo es 12 sp. */
object NeonType {
    val displayAmount = 30.sp
    val screenTitle = 22.sp
    val sectionTitle = 18.sp
    val body = 14.sp
    val secondary = 12.sp
    val min = 12.sp

    /** Letter-spacing de Orbitron, ~0.04em sobre 22 sp. */
    val orbitronLetterSpacing = 0.9.sp

    /** Cifras tabulares para importes, saldos y porcentajes (evita "baile" al cambiar). */
    fun tabular(style: TextStyle): TextStyle = style.copy(fontFeatureSettings = "tnum")
}

val NeonTypography = Typography(
    // Cifras grandes (saldo, importes destacados) en Orbitron
    displayLarge = TextStyle(
        fontFamily = Orbitron,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 46.sp,
        letterSpacing = 0.8.sp,
        fontFeatureSettings = "tnum",
    ),
    displayMedium = TextStyle(
        fontFamily = Orbitron,
        fontWeight = FontWeight.Bold,
        fontSize = NeonType.displayAmount,
        lineHeight = 36.sp,
        letterSpacing = NeonType.orbitronLetterSpacing,
        fontFeatureSettings = "tnum",
    ),
    displaySmall = TextStyle(
        fontFamily = Orbitron,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.8.sp,
        fontFeatureSettings = "tnum",
    ),
    // Títulos de pantalla en Orbitron
    headlineLarge = TextStyle(
        fontFamily = Orbitron,
        fontWeight = FontWeight.Bold,
        fontSize = NeonType.screenTitle,
        lineHeight = 28.sp,
        letterSpacing = NeonType.orbitronLetterSpacing,
    ),
    headlineMedium = TextStyle(
        fontFamily = Orbitron,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.8.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = Orbitron,
        fontWeight = FontWeight.SemiBold,
        fontSize = NeonType.sectionTitle,
        lineHeight = 24.sp,
        letterSpacing = 0.7.sp,
    ),
    // Texto general en Space Grotesk
    titleLarge = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = NeonType.sectionTitle,
        lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = NeonType.body,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = NeonType.secondary,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        letterSpacing = 0.2.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = NeonType.secondary,
        letterSpacing = 0.2.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Medium,
        fontSize = NeonType.min,
        letterSpacing = 0.2.sp,
    ),
)
