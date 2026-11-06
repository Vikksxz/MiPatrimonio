package com.mipatrimonio.app.core.database.seed

import com.mipatrimonio.app.R
import com.mipatrimonio.app.core.database.seed.DefaultCategories
import org.junit.Assert.*
import org.junit.Test

class DefaultCategoriesTest {

    @Test
    fun categoriesCountIs12() {
        assertEquals(12, DefaultCategories.ALL.size)
    }

    @Test
    fun exactlyTwoIncomes() {
        val incomes = DefaultCategories.ALL.count { it.isIncome }
        assertEquals(2, incomes)
    }

    @Test
    fun exactlyTenExpenses() {
        val expenses = DefaultCategories.ALL.count { !it.isIncome }
        assertEquals(10, expenses)
    }

    @Test
    fun hogarAndSubscriptionsAreFixed() {
        val hogar = DefaultCategories.ALL.find { it.nameRes == R.string.cat_home }
        val subs = DefaultCategories.ALL.find { it.nameRes == R.string.cat_subscriptions }
        assertTrue(hogar?.isFixed == true)
        assertTrue(subs?.isFixed == true)
    }

    @Test
    fun otherExpenseIsDefault() {
        val other = DefaultCategories.ALL.find { it.nameRes == R.string.cat_other_expense }
        assertTrue(other?.isDefault == true)
    }

    @Test
    fun namesAreUnique() {
        val names = DefaultCategories.ALL.map { it.nameRes }
        assertEquals(names.size, names.distinct().size)
    }

    @Test
    fun colorHexFormat() {
        DefaultCategories.ALL.forEach {
            assertTrue(it.colorHex.matches("#[0-9A-Fa-f]{6}".toRegex()))
        }
    }

    @Test
    fun iconNotEmpty() {
        DefaultCategories.ALL.forEach {
            assertFalse(it.icon.isBlank())
        }
    }
}