package com.mipatrimonio.app.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.mipatrimonio.app.R
import com.mipatrimonio.app.core.model.TransactionType
import com.mipatrimonio.app.core.ui.theme.NeonColor

/**
 * Nombre visible del tipo de movimiento (sección 5.1/6). Compartido por las
 * pantallas de Movimientos e Inicio para que la etiqueta sea idéntica.
 */
@Composable
fun TransactionType.label(): String = stringResource(
    when (this) {
        TransactionType.CASH -> R.string.type_cash
        TransactionType.CARD -> R.string.type_card
        TransactionType.SUBSCRIPTION -> R.string.type_subscription
        TransactionType.INCOME -> R.string.type_income
    },
)

/** Color neón del tipo, obligatorio y consistente en toda la app (sección 5.1). */
fun TransactionType.accentColor(): Color = when (this) {
    TransactionType.CASH -> NeonColor.MovementType.cashAccent
    TransactionType.CARD -> NeonColor.MovementType.cardAccent
    TransactionType.SUBSCRIPTION -> NeonColor.MovementType.subscriptionAccent
    TransactionType.INCOME -> NeonColor.MovementType.incomeAccent
}
