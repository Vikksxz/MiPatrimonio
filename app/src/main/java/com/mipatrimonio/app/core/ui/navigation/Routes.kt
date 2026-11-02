package com.mipatrimonio.app.core.ui.navigation

/**
 * Rutas del grafo de navegación (sección 4 de MiPatrimonio.md).
 * Cuatro pestañas principales; el botón + abre una hoja, no una ruta.
 */
object Routes {
    const val HOME = "home"
    const val TRANSACTIONS = "transactions"
    const val SAVINGS = "savings"
    const val WALLET = "wallet"

    /** Destinos de la barra inferior, en el orden en que se resaltan. */
    val topLevel = listOf(HOME, TRANSACTIONS, SAVINGS, WALLET)
}
