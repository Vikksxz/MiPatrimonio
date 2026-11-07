package com.mipatrimonio.app.core.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mipatrimonio.app.core.ui.components.NeonTabBar
import com.mipatrimonio.app.core.model.TransactionType
import com.mipatrimonio.app.core.ui.theme.NeonColor
import com.mipatrimonio.app.feature.add.AddMenuSheet
import com.mipatrimonio.app.feature.add.ReviewSheet

/**
 * Raíz de la app (T1.3): Scaffold con barra inferior neón, NavHost de 4 pestañas y
 * hoja vacía del botón +.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val selectedRoute = Routes.topLevel.firstOrNull { route ->
        currentDestination?.hierarchy?.any { it.route == route } == true
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showAddSheet by rememberSaveable { mutableStateOf(false) }
    var addMode by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = NeonColor.bg,
        bottomBar = {
            NeonTabBar(
                selectedRoute = selectedRoute,
                onTabSelected = { route ->
                    if (route != selectedRoute) {
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                onAddClick = {
                    addMode = null
                    showAddSheet = true
                },
            )
        },
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
        )
    }

    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showAddSheet = false
                addMode = null
            },
            sheetState = sheetState,
            containerColor = NeonColor.panel,
            contentColor = NeonColor.ink,
            scrimColor = NeonColor.bg.copy(alpha = 0.72f),
        ) {
            val mode = addMode
            if (mode == null) {
                AddMenuSheet(
                    onManualExpense = { addMode = TransactionType.CASH.name },
                    onIncome = { addMode = TransactionType.INCOME.name },
                )
            } else {
                ReviewSheet(
                    initialType = TransactionType.valueOf(mode),
                    onSaved = {
                        // Si el usuario cerró la hoja mientras se guardaba, no navegar.
                        if (showAddSheet) {
                            showAddSheet = false
                            addMode = null
                            navController.navigate(Routes.TRANSACTIONS) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    onDismiss = {
                        showAddSheet = false
                        addMode = null
                    },
                )
            }
        }
    }
}
