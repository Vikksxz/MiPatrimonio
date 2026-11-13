package com.mipatrimonio.app.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import com.mipatrimonio.app.core.money.Money
import com.mipatrimonio.app.core.ui.motion.MotionDurations
import com.mipatrimonio.app.core.ui.motion.rememberAnimationsEnabled
import com.mipatrimonio.app.core.ui.theme.NeonType
import kotlin.math.roundToLong

/**
 * Importe animado en céntimos (Long). Respeta reducir animaciones y usa cifras tabulares.
 */
@Composable
fun AnimatedAmount(
    cents: Long,
    modifier: Modifier = Modifier,
    currency: String = Money.BASE_CURRENCY,
    signed: Boolean = false,
    isExpense: Boolean = false,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Ellipsis,
) {
    val animationsEnabled = rememberAnimationsEnabled()
    val anim = remember { Animatable(cents.toFloat()) }
    LaunchedEffect(cents, animationsEnabled) {
        val target = cents.toFloat()
        if (!animationsEnabled) {
            anim.snapTo(target)
        } else {
            anim.animateTo(target, animationSpec = tween(durationMillis = MotionDurations.Long))
        }
    }
    val current = anim.value.roundToLong()
    val text = if (signed) Money.formatSigned(current, isExpense, currency) else Money.format(current, currency)
    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = NeonType.tabular(style),
        maxLines = maxLines,
        overflow = overflow,
    )
}
