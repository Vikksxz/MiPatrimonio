package com.mipatrimonio.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mipatrimonio.app.core.ui.theme.NeonColor
import com.mipatrimonio.app.core.ui.theme.NeonShape

/**
 * Tipo de movimiento de un chip. Los colores salen de la tabla obligatoria de la sección 5.1.
 */
enum class MovementChipType {
    CASH,
    CARD,
    SUBSCRIPTION,
    INCOME,
}

/**
 * NeonChip (sección 5.1): chip tintado por tipo de movimiento.
 * Sin seleccionar: fondo panelAlt, borde lineInput y texto chipText.
 * Seleccionado: fondo tintado del tipo, borde y texto del acento del tipo.
 */
@Composable
fun NeonChip(
    text: String,
    type: MovementChipType,
    selected: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) = NeonChip(
    text = text,
    accent = when (type) {
        MovementChipType.CASH -> NeonColor.MovementType.cashAccent
        MovementChipType.CARD -> NeonColor.MovementType.cardAccent
        MovementChipType.SUBSCRIPTION -> NeonColor.MovementType.subscriptionAccent
        MovementChipType.INCOME -> NeonColor.MovementType.incomeAccent
    },
    tint = when (type) {
        MovementChipType.CASH -> NeonColor.MovementType.cashBg
        MovementChipType.CARD -> NeonColor.MovementType.cardBg
        MovementChipType.SUBSCRIPTION -> NeonColor.MovementType.subscriptionBg
        MovementChipType.INCOME -> NeonColor.MovementType.incomeBg
    },
    labelColor = when (type) {
        MovementChipType.CASH -> NeonColor.MovementType.cashText
        MovementChipType.CARD -> NeonColor.MovementType.cardText
        MovementChipType.SUBSCRIPTION -> NeonColor.MovementType.subscriptionText
        MovementChipType.INCOME -> NeonColor.MovementType.incomeText
    },
    selected = selected,
    modifier = modifier,
    onClick = onClick,
)

/**
 * NeonChip genérico con colores personalizados.
 */
@Composable
fun NeonChip(
    text: String,
    accent: androidx.compose.ui.graphics.Color,
    tint: androidx.compose.ui.graphics.Color,
    labelColor: androidx.compose.ui.graphics.Color,
    selected: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(NeonShape.chipRadius)
    val interactiveModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(shape)
            .background(
                color = if (selected) tint else NeonColor.panelAlt,
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) accent else NeonColor.lineInput,
                shape = shape,
            )
            .then(interactiveModifier)
            .semantics { this.selected = selected }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            color = if (selected) labelColor else NeonColor.chipText,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
