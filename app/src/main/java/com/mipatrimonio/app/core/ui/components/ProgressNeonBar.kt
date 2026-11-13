package com.mipatrimonio.app.core.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mipatrimonio.app.core.ui.motion.MotionDurations
import com.mipatrimonio.app.core.ui.motion.rememberAnimationsEnabled
import com.mipatrimonio.app.core.ui.theme.MiPatrimonioTheme
import com.mipatrimonio.app.core.ui.theme.NeonColor
import kotlin.math.roundToInt

/**
 * Barra de progreso neón animada (§5.4).
 * - Pista: [trackHeight] (14 dp), radio 8 dp, fondo [NeonColor.track].
 * - Relleno: degradado verde→cian→magenta con brillo exterior cian suave.
 * - Destello: franja blanca translúcida (alfa 0.3, ~30% de la pista) cada 2,4 s.
 * - Cabeza pulsante: punto blanco 10 dp al final del relleno, halo cian/magenta (1,6 s).
 * - Cambio de progreso: 800 ms. Sin animaciones si el sistema las desactiva.
 */
@Composable
fun ProgressNeonBar(
    progress: Float,
    modifier: Modifier = Modifier,
    trackHeight: Dp = 14.dp,
    contentDescriptionOverride: String? = null,
) {
    val animationsEnabled = rememberAnimationsEnabled()
    val clamped = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clamped,
        animationSpec = if (animationsEnabled) tween(durationMillis = MotionDurations.Progress) else snap(),
        label = "progressWidth",
    )
    val percent = (clamped * 100f).roundToInt()
    val description = contentDescriptionOverride ?: "Progreso $percent por ciento"
    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(trackHeight)
            .semantics { contentDescription = description }
            .clip(shape)
            .background(NeonColor.track),
    ) {
        // Relleno con brillo exterior cian suave
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .drawBehind {
                    val glowPaint = Paint().asFrameworkPaint().apply {
                        isAntiAlias = true
                        color = NeonColor.neonCyan.copy(alpha = 0.35f).toArgb()
                        setShadowLayer(12.dp.toPx(), 0f, 0f, NeonColor.neonCyan.copy(alpha = 0.45f).toArgb())
                    }
                    drawIntoCanvas {
                        it.nativeCanvas.drawRoundRect(
                            0f, 0f, size.width, size.height,
                            8.dp.toPx(), 8.dp.toPx(),
                            glowPaint,
                        )
                    }
                }
                .background(
                    Brush.horizontalGradient(
                        listOf(NeonColor.neonGreen, NeonColor.neonCyan, NeonColor.neonMagenta)
                    )
                ),
        )

        // Destello y cabeza pulsante (solo con animaciones activas)
        if (animationsEnabled) {
            ShimmerAndHead(animatedProgress = animatedProgress, trackHeight = trackHeight)
        } else if (animatedProgress > 0f) {
            // Cabeza estática al final sin animación
            StaticHead(animatedProgress = animatedProgress, trackHeight = trackHeight)
        }
    }
}

@Composable
private fun ShimmerAndHead(animatedProgress: Float, trackHeight: Dp) {
    val transition = rememberInfiniteTransition(label = "neonBar")
    val shimmerX by transition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer",
    )
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    Canvas(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
        val w = size.width
        val h = size.height
        val radius = 8.dp.toPx()

        // Destello: franja blanca translúcida (~30% del ancho de la pista)
        val stripeWidth = w * 0.3f
        val stripeLeft = (shimmerX * w) - stripeWidth
        val stripeBrush = Brush.horizontalGradient(
            colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.3f), Color.Transparent),
            startX = stripeLeft,
            endX = stripeLeft + stripeWidth,
        )
        drawRoundRect(
            brush = stripeBrush,
            topLeft = Offset(stripeLeft.coerceAtLeast(0f), 0f),
            size = Size(stripeWidth.coerceAtMost(w), h),
            cornerRadius = CornerRadius(radius, radius),
        )

        // Cabeza pulsante al final del relleno
        if (animatedProgress > 0f) {
            val headX = (w * animatedProgress).coerceIn(0f, w)
            val headY = h / 2f
            val headRadius = 5.dp.toPx() // 10 dp diámetro
            val haloColor = androidx.compose.ui.graphics.lerp(NeonColor.neonCyan, NeonColor.neonMagenta, pulse)
            // Halo
            drawCircle(color = haloColor.copy(alpha = 0.45f), radius = headRadius * 2.2f, center = Offset(headX, headY))
            drawCircle(color = haloColor.copy(alpha = 0.25f), radius = headRadius * 3.2f, center = Offset(headX, headY))
            // Punto blanco
            drawCircle(color = Color.White, radius = headRadius, center = Offset(headX, headY))
        }
    }
}

@Composable
private fun StaticHead(animatedProgress: Float, trackHeight: Dp) {
    Canvas(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
        if (animatedProgress <= 0f) return@Canvas
        val headX = (size.width * animatedProgress).coerceIn(0f, size.width)
        val headY = size.height / 2f
        val headRadius = 5.dp.toPx()
        drawCircle(color = NeonColor.neonCyan.copy(alpha = 0.3f), radius = headRadius * 2f, center = Offset(headX, headY))
        drawCircle(color = Color.White, radius = headRadius, center = Offset(headX, headY))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF070A12)
@Composable
private fun ProgressNeonBarPreview() {
    MiPatrimonioTheme {
        Box(modifier = Modifier.fillMaxWidth().height(40.dp).background(NeonColor.bg)) {
            ProgressNeonBar(progress = 0.62f, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF070A12)
@Composable
private fun ProgressNeonBarEmptyPreview() {
    MiPatrimonioTheme {
        ProgressNeonBar(progress = 0f)
    }
}
