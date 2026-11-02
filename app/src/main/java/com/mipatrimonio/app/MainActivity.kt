package com.mipatrimonio.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.mipatrimonio.app.core.ui.components.ShowcaseScreen
import com.mipatrimonio.app.core.ui.theme.MiPatrimonioTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MiPatrimonioTheme {
                // T1.2: pantalla de muestra del tema neón. T1.3 la sustituye por el NavHost.
                ShowcaseScreen(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
