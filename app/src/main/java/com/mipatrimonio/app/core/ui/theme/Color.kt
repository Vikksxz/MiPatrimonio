package com.mipatrimonio.app.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Tokens de color de MiPatrimonio.md sección 5.1.
 * Tema siempre oscuro. No usar colores sueltos en las pantallas: siempre vía este objeto.
 */
object NeonColor {
    // Tokens base
    val bg = Color(0xFF070A12)        // #070A12 Fondo de pantalla
    val panel = Color(0xFF0E1424)     // #0E1424 Tarjetas y paneles
    val panelAlt = Color(0xFF0A1226)  // #0A1226 Subpaneles, botones circulares, chips sin seleccionar
    val line = Color(0xFF1F2A4A)      // #1F2A4A Bordes de paneles
    val lineInput = Color(0xFF26325A) // #26325A Bordes de inputs y chips
    val track = Color(0xFF161F3A)     // #161F3A Fondo de barras de progreso
    val ink = Color(0xFFE6ECFF)       // #E6ECFF Texto principal
    val muted = Color(0xFF8FA0C4)     // #8FA0C4 Texto secundario
    val chipText = Color(0xFFC7D2F0)  // #C7D2F0 Texto de chips sin seleccionar
    val navBar = Color(0xFF0B1020)    // #0B1020 Fondo de la barra inferior (sección 8)

    // Acentos neón
    val neonCyan = Color(0xFF22D3EE)    // #22D3EE Acento principal, tarjeta, pestaña activa
    val neonViolet = Color(0xFFB26BFF)  // #B26BFF Acento secundario
    val neonMagenta = Color(0xFFFF4FD8) // #FF4FD8 Acento terciario (degradados)
    val neonGreen = Color(0xFF39FF88)   // #39FF88 Ingresos, estado correcto
    val neonAmber = Color(0xFFFFB020)   // #FFB020 Metálico, avisos
    val expenseRed = Color(0xFFFF7A93)  // #FF7A93 Texto de gastos y errores

    // Fondos de alerta (sección 5.1)
    val alertAmberBg = Color(0xFF2A1F08)
    val alertAmberBorder = Color(0xFF6B4A0E)
    val alertAmberText = Color(0xFFFFD27A)
    val alertCorrectBg = Color(0xFF0B2A19)
    val alertCorrectText = Color(0xFF7CFFB2)
    val alertErrorBg = Color(0xFF330A14)
    val alertErrorText = Color(0xFFFF8FA3)

    // Colores por tipo de movimiento (sección 5.1, obligatorio y consistente en toda la app)
    object MovementType {
        val cashAccent = Color(0xFFFFB020)       // #FFB020
        val cashBg = Color(0xFF2A1F08)           // #2A1F08
        val cashText = Color(0xFFFFB020)         // #FFB020

        val cardAccent = Color(0xFF22D3EE)       // #22D3EE
        val cardBg = Color(0xFF07252B)           // #07252B
        val cardText = Color(0xFF22D3EE)         // #22D3EE

        val subscriptionAccent = Color(0xFFB26BFF) // #B26BFF
        val subscriptionBg = Color(0xFF241236)     // #241236
        val subscriptionText = Color(0xFFC79BFF)   // #C79BFF

        val incomeAccent = Color(0xFF39FF88)     // #39FF88
        val incomeBg = Color(0xFF0B2A19)         // #0B2A19
        val incomeText = Color(0xFF39FF88)       // #39FF88
    }
}
