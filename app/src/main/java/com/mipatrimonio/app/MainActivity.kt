package com.mipatrimonio.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MiPatrimonioTheme {
                // T1.1: pantalla vacía oscura. La navegación llega en T1.3.
                EmptyScreen()
            }
        }
    }
}

/**
 * Tema mínimo para la tarea T1.1. Los colores oficiales (sección 5.1 de
 * MiPatrimonio.md) y los componentes se definirán en T1.2.
 */
@Composable
private fun MiPatrimonioTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color(0xFF070A12), // bg
            surface = Color(0xFF0E1424),     // panel
            onBackground = Color(0xFFE6ECFF), // ink
            onSurface = Color(0xFFE6ECFF),
        ),
        content = content,
    )
}

@Composable
private fun EmptyScreen() {
    Box(modifier = Modifier.fillMaxSize())
}

@Preview(showBackground = true, backgroundColor = 0xFF070A12)
@Composable
private fun EmptyScreenPreview() {
    MiPatrimonioTheme {
        EmptyScreen()
    }
}
