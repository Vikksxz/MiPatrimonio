package com.mipatrimonio.app.feature.wallet

import com.mipatrimonio.app.R

/**
 * Modelo de diseño de tarjeta (6 diseños por defecto según MiPatrimonio.md §5.5).
 */
data class CardDesign(
    val id: String,
    val nameRes: Int,
    val startColor: Long,
    val endColor: Long,
    val accentColor: Long,
) {
    companion object {
        val DESIGNS = listOf(
            CardDesign("cyan", R.string.card_design_cyan, 0xFF0B1B3A, 0xFF1D0B3A, 0xFF22D3EE),
            CardDesign("magenta", R.string.card_design_magenta, 0xFF2A0B2E, 0xFF0B1B3A, 0xFFFF4FD8),
            CardDesign("green", R.string.card_design_green, 0xFF06241A, 0xFF0B1B3A, 0xFF39FF88),
            CardDesign("amber", R.string.card_design_amber, 0xFF2A1A06, 0xFF1A0B2E, 0xFFFFB020),
            CardDesign("violet", R.string.card_design_violet, 0xFF1D0B3A, 0xFF0B0B24, 0xFFB26BFF),
            CardDesign("graphite", R.string.card_design_graphite, 0xFF1A2030, 0xFF0A0D14, 0xFF8FA0C4),
        )

        fun getById(id: String): CardDesign = DESIGNS.find { it.id == id } ?: DESIGNS[0]
    }
}

/**
 * Bancos disponibles (chips en el formulario).
 */
data class BankOption(
    val id: String,
    val nameRes: Int,
) {
    companion object {
        val BANKS = listOf(
            BankOption("caixabank", R.string.bank_caixabank),
            BankOption("bbva", R.string.bank_bbva),
            BankOption("santander", R.string.bank_santander),
            BankOption("ing", R.string.bank_ing),
            BankOption("revolut", R.string.bank_revolut),
            BankOption("other", R.string.bank_other),
        )

        fun getById(id: String): BankOption = BANKS.find { it.id == id } ?: BANKS[0]
    }
}