package com.mipatrimonio.app.core.ui.motion

import android.provider.Settings
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Utilidades de movimiento (T5.1). Respetan "reducir animaciones" del sistema.
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
}

/** Muelles suaves para desplazamientos/cambios de tamaño. */
fun <T> gentleSpring() = spring<T>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow,
)
