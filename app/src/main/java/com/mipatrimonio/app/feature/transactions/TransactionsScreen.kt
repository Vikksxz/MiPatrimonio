package com.mipatrimonio.app.feature.transactions

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.mipatrimonio.app.R
import com.mipatrimonio.app.core.ui.components.PlaceholderScreen
import com.mipatrimonio.app.core.ui.theme.MiPatrimonioTheme

/** Movimientos (T1.8 la completa). De momento pantalla vacía con su título. */
@Composable
fun TransactionsScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(titleRes = R.string.tab_transactions, modifier = modifier)
}

@Preview(showBackground = true, backgroundColor = 0xFF070A12)
@Composable
private fun TransactionsScreenPreview() {
    MiPatrimonioTheme {
        TransactionsScreen()
    }
}
