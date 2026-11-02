package com.mipatrimonio.app.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.mipatrimonio.app.R
import com.mipatrimonio.app.core.ui.components.PlaceholderScreen
import com.mipatrimonio.app.core.ui.theme.MiPatrimonioTheme

/** Inicio (T1.9 la completa). De momento pantalla vacía con su título. */
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(titleRes = R.string.home_title, modifier = modifier)
}

@Preview(showBackground = true, backgroundColor = 0xFF070A12)
@Composable
private fun HomeScreenPreview() {
    MiPatrimonioTheme {
        HomeScreen()
    }
}
