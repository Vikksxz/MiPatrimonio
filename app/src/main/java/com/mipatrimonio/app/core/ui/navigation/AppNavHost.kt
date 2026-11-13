package com.mipatrimonio.app.core.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mipatrimonio.app.core.ui.motion.MotionDurations
import com.mipatrimonio.app.core.ui.motion.rememberAnimationsEnabled
import com.mipatrimonio.app.feature.documents.DocumentViewerScreen
import com.mipatrimonio.app.feature.documents.DocumentsScreen
import com.mipatrimonio.app.feature.home.HomeScreen
import com.mipatrimonio.app.feature.analysis.AnalysisScreen
import com.mipatrimonio.app.feature.savings.SavingsScreen
import com.mipatrimonio.app.feature.home.HomeSettingsScreen
import com.mipatrimonio.app.feature.settings.SettingsScreen
import com.mipatrimonio.app.feature.shared.SharedExpensesScreen
import com.mipatrimonio.app.feature.transactions.TransactionsScreen
import com.mipatrimonio.app.feature.wallet.WalletScreen

/**
 * Grafo principal (T1.3): 4 destinos vacíos. El botón + es una hoja, no una ruta.
 * T5.3: transiciones suaves y desactivables respetando "reducir animaciones".
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onAdd: () -> Unit = {},
) {
    val animationsEnabled = rememberAnimationsEnabled()
    val duration = MotionDurations.Default
    val enter: EnterTransition = if (animationsEnabled) {
        fadeIn(animationSpec = tween(duration)) + slideInHorizontally(
            animationSpec = tween(duration),
            initialOffsetX = { (it * 0.06f).toInt() },
        )
    } else EnterTransition.None
    val exit: ExitTransition = if (animationsEnabled) {
        fadeOut(animationSpec = tween(duration)) + slideOutHorizontally(
            animationSpec = tween(duration),
            targetOffsetX = { -(it * 0.04f).toInt() },
        )
    } else ExitTransition.None
    val popEnter: EnterTransition = if (animationsEnabled) {
        fadeIn(animationSpec = tween(duration)) + slideInHorizontally(
            animationSpec = tween(duration),
            initialOffsetX = { -(it * 0.04f).toInt() },
        )
    } else EnterTransition.None
    val popExit: ExitTransition = if (animationsEnabled) {
        fadeOut(animationSpec = tween(duration)) + slideOutHorizontally(
            animationSpec = tween(duration),
            targetOffsetX = { (it * 0.06f).toInt() },
        )
    } else ExitTransition.None

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier.fillMaxSize(),
        enterTransition = { enter },
        exitTransition = { exit },
        popEnterTransition = { popEnter },
        popExitTransition = { popExit },
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
            TransactionsScreen(
                onOpenAnalysis = { navController.navigate(Routes.ANALYSIS) },
                onAdd = onAdd,
            )
        }
        composable(Routes.SAVINGS) { SavingsScreen() }
        composable(Routes.WALLET) {
            WalletScreen(
                onOpenDocuments = { navController.navigate(Routes.DOCUMENTS) },
                onOpenShared = { navController.navigate(Routes.SHARED) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.SHARED) {
            SharedExpensesScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.HOME_SETTINGS) {
            HomeSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenHomeSettings = { navController.navigate(Routes.HOME_SETTINGS) },
            )
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
