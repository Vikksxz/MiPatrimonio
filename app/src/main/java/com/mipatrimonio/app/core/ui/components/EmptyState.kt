package com.mipatrimonio.app.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mipatrimonio.app.core.ui.theme.MiPatrimonioTheme
import com.mipatrimonio.app.core.ui.theme.NeonColor

/**
 * Estado vacío reutilizable (T5.2): icono suave, mensaje cálido y un CTA principal.
 */
@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    icon: ImageVector = Icons.Filled.Info,
    iconContentDescription: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = iconContentDescription,
            tint = NeonColor.muted,
            modifier = Modifier.size(40.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (!title.isNullOrBlank()) {
            Text(
                text = title,
                color = NeonColor.ink,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        Text(
            text = message,
            color = NeonColor.muted,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(16.dp))
            NeonButton(onClick = onAction) {
                NeonButtonText(text = actionLabel)
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF070A12)
@Composable
private fun EmptyStatePreview() {
    MiPatrimonioTheme {
        EmptyState(
            title = "Aún no hay movimientos",
            message = "Cuando apuntes el primero, verás aquí en qué se va tu dinero.",
            actionLabel = "Añadir movimiento",
            onAction = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF070A12)
@Composable
private fun EmptyStateSimplePreview() {
    MiPatrimonioTheme {
        EmptyState(message = "No hay resultados con esos filtros.")
    }
}
