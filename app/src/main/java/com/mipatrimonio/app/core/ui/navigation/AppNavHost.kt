package com.mipatrimonio.app.core.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mipatrimonio.app.feature.documents.DocumentViewerScreen
import com.mipatrimonio.app.feature.documents.DocumentsScreen
import com.mipatrimonio.app.feature.home.HomeScreen
import com.mipatrimonio.app.feature.analysis.AnalysisScreen
import com.mipatrimonio.app.feature.savings.SavingsScreen
import com.mipatrimonio.app.feature.shared.SharedExpensesScreen
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
        composable(Routes.HOME) {
            HomeScreen(
                onOpenSavings = {
                    navController.navigate(Routes.SAVINGS) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }
        composable(Routes.TRANSACTIONS) {
            TransactionsScreen(onOpenAnalysis = { navController.navigate(Routes.ANALYSIS) })
        }
        composable(Routes.SAVINGS) { SavingsScreen() }
        composable(Routes.WALLET) {
            WalletScreen(
                onOpenDocuments = { navController.navigate(Routes.DOCUMENTS) },
                onOpenShared = { navController.navigate(Routes.SHARED) },
            )
        }
        composable(Routes.SHARED) {
            SharedExpensesScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.DOCUMENTS) {
            DocumentsScreen(
                onBack = { navController.popBackStack() },
                onOpen = { id -> navController.navigate(Routes.documentView(id)) },
            )
        }
        composable(
            route = Routes.DOCUMENT_VIEW,
            arguments = listOf(navArgument("documentId") { type = NavType.LongType }),
        ) {
            DocumentViewerScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.ANALYSIS) {
            AnalysisScreen(onBack = { navController.popBackStack() })
        }
    }
}
