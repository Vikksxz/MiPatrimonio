package com.mipatrimonio.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.mipatrimonio.app.core.ui.navigation.AppRoot
import com.mipatrimonio.app.core.ui.theme.MiPatrimonioTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MiPatrimonioTheme {
                AppRoot()
            }
        }
    }
}
