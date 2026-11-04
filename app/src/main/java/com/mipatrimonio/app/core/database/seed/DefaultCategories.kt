package com.mipatrimonio.app.core.database.seed

import androidx.annotation.StringRes
import com.mipatrimonio.app.R

/**
 * Categorías por defecto (MiPatrimonio.md §6: 10 gastos + 2 ingresos).
 * colorHex e icon son de Material Icons (core).
 * Nombres en strings.xml para traducir en el futuro.
 */
object DefaultCategories {

    data class CategorySeed(
        @StringRes val nameRes: Int,
        val colorHex: String,
        val icon: String,
        val isIncome: Boolean,
        val isFixed: Boolean,
        val isDefault: Boolean
    )

    // 10 gastos + 2 ingresos = 12 categorías
    val ALL = listOf(
        // Gastos (isIncome = false)
        CategorySeed(R.string.cat_food, "#FF6B9D", "restaurant", false, false, false),
        CategorySeed(R.string.cat_dining, "#FF8E72", "local_dining", false, false, false),
        CategorySeed(R.string.cat_transport, "#22D3EE", "directions_car", false, false, false),
        CategorySeed(R.string.cat_home, "#FFB020", "home", false, true, true),
        CategorySeed(R.string.cat_leisure, "#B26BFF", "sports_esports", false, false, false),
        CategorySeed(R.string.cat_health, "#39FF88", "local_hospital", false, false, false),
        CategorySeed(R.string.cat_shopping, "#FF4FD8", "shopping_bag", false, false, false),
        CategorySeed(R.string.cat_subscriptions, "#8FA0C4", "subscriptions", false, true, false),
        CategorySeed(R.string.cat_travel, "#FFFF8A", "flight", false, false, false),
        CategorySeed(R.string.cat_other_expense, "#7C7C7C", "category", false, false, true),
        // Ingresos (isIncome = true)
        CategorySeed(R.string.cat_payroll, "#39FF88", "payments", true, false, false),
        CategorySeed(R.string.cat_other_income, "#22D3EE", "attach_money", true, false, false),
    )
}