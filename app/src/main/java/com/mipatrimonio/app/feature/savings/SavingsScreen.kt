package com.mipatrimonio.app.feature.savings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.mipatrimonio.app.R
import com.mipatrimonio.app.core.ui.components.PlaceholderScreen
import com.mipatrimonio.app.core.ui.theme.MiPatrimonioTheme

/** Ahorro (T3.1 la completa). De momento pantalla vacía con su título. */
@Composable
fun SavingsScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(titleRes = R.string.tab_savings, modifier = modifier)
}

@Preview(showBackground = true, backgroundColor = 0xFF070A12)
@Composable
private fun SavingsScreenPreview() {
    MiPatrimonioTheme {
        SavingsScreen()
    }
}
