package com.mipatrimonio.app.core.ui.motion

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

/**
 * Utilidades de movimiento (T5.1–T5.2). Respetan "reducir animaciones" del sistema.
 */
@Composable
fun rememberAnimationsEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            )
        }.getOrDefault(1f) != 0f
    }
}

/** Duraciones base coherentes (150–400 ms) para transiciones/efectos con sentido. */
object MotionDurations {
    const val Short = 150
    const val Medium = 240
    const val Default = 320
    const val Long = 400
    const val Progress = 800
    const val StaggerStep = 40
}

/** Muelles suaves para desplazamientos/cambios de tamaño. */
fun <T> gentleSpring() = spring<T>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow,
)

/** Retardo de entrada escalonada (stagger) para un índice dado. */
fun staggerDelayMillis(index: Int, step: Int = MotionDurations.StaggerStep, maxItems: Int = 8): Int =
    (index.coerceAtMost(maxItems)) * step

/**
 * Estado simple de entrada escalonada (fade+desplazamiento) para listas/bloques.
 * Devuelve (alpha, offsetY px) animados; con animaciones desactivadas aparece al instante.
 */
@Composable
fun rememberStaggeredEnter(
    index: Int,
    enabled: Boolean = rememberAnimationsEnabled(),
): Pair<Float, Int> {
    val alpha = remember(index, enabled) { Animatable(if (enabled) 0f else 1f) }
    val offset = remember(index, enabled) { Animatable(if (enabled) 12f else 0f) }
    LaunchedEffect(index, enabled) {
        if (!enabled) {
            alpha.snapTo(1f)
        } else {
            val d = staggerDelayMillis(index)
            if (d > 0) delay(d.toLong())
            alpha.animateTo(1f, animationSpec = tween(durationMillis = MotionDurations.Default))
        }
    }
    LaunchedEffect(index, enabled) {
        if (!enabled) {
            offset.snapTo(0f)
        } else {
            val d = staggerDelayMillis(index)
            if (d > 0) delay(d.toLong())
            offset.animateTo(0f, animationSpec = gentleSpring())
        }
    }
    return alpha.value to offset.value.roundToInt()
}
