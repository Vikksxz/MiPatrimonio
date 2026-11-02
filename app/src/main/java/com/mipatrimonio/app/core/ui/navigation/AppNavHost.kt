package com.mipatrimonio.app.core.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.mipatrimonio.app.feature.home.HomeScreen
import com.mipatrimonio.app.feature.savings.SavingsScreen
import com.mipatrimonio.app.feature.transactions.TransactionsScreen
import com.mipatrimonio.app.feature.wallet.WalletScreen

/**
 * Grafo principal (T1.3): 4 destinos vacíos. El botón + es una hoja, no una ruta.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier.fillMaxSize(),
    ) {
        composable(Routes.HOME) { HomeScreen() }
        composable(Routes.TRANSACTIONS) { TransactionsScreen() }
        composable(Routes.SAVINGS) { SavingsScreen() }
        composable(Routes.WALLET) { WalletScreen() }
    }
}
