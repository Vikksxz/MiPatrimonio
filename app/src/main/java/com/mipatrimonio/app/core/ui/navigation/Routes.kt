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
    const val DOCUMENTS = "documents"
    const val DOCUMENT_VIEW = "documents/view/{documentId}"
    const val ANALYSIS = "analysis"
    const val SHARED = "shared"

    fun documentView(documentId: Long): String = "documents/view/$documentId"

    /** Destinos de la barra inferior, en el orden en que se resaltan. */
    val topLevel = listOf(HOME, TRANSACTIONS, SAVINGS, WALLET)
}
