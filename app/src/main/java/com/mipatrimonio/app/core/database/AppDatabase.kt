package com.mipatrimonio.app.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mipatrimonio.app.core.database.dao.BudgetDao
import com.mipatrimonio.app.core.database.dao.CardDao
import com.mipatrimonio.app.core.database.dao.CategoryDao
import com.mipatrimonio.app.core.database.dao.DocumentDao
import com.mipatrimonio.app.core.database.dao.RecurringDao
import com.mipatrimonio.app.core.database.dao.SavingsDao
import com.mipatrimonio.app.core.database.dao.TagDao
import com.mipatrimonio.app.core.database.dao.TransactionDao
import com.mipatrimonio.app.core.database.entity.BudgetEntity
import com.mipatrimonio.app.core.database.entity.CardEntity
import com.mipatrimonio.app.core.database.entity.CategoryEntity
import com.mipatrimonio.app.core.database.entity.DocumentEntity
import com.mipatrimonio.app.core.database.entity.ExchangeRateEntity
import com.mipatrimonio.app.core.database.entity.PlanContributionEntity
import com.mipatrimonio.app.core.database.entity.PersonEntity
import com.mipatrimonio.app.core.database.entity.RecurringEntity
import com.mipatrimonio.app.core.database.entity.SavingsPlanEntity
import com.mipatrimonio.app.core.database.entity.SharedExpenseEntity
import com.mipatrimonio.app.core.database.entity.SharedShareEntity
import com.mipatrimonio.app.core.database.entity.TagEntity
import com.mipatrimonio.app.core.database.entity.TransactionEntity
import com.mipatrimonio.app.core.database.entity.TransactionTagCrossRef
import com.mipatrimonio.app.core.database.seed.SeedCallback

/**
 * Base de datos Room principal (MiPatrimonio.md §6, §18 T1.4).
 * exportSchema = true para generar schemas/ y migraciones explícitas.
 * NO usar fallbackToDestructiveMigration.
 */
@Database(
    entities = [
        CardEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        TagEntity::class,
        TransactionTagCrossRef::class,
        RecurringEntity::class,
        SavingsPlanEntity::class,
        PlanContributionEntity::class,
        BudgetEntity::class,
        DocumentEntity::class,
        ExchangeRateEntity::class,
        PersonEntity::class,
        SharedExpenseEntity::class,
        SharedShareEntity::class,
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun cardDao(): CardDao
    abstract fun categoryDao(): CategoryDao
    abstract fun recurringDao(): RecurringDao
    abstract fun savingsDao(): SavingsDao
    abstract fun budgetDao(): BudgetDao
    abstract fun documentDao(): DocumentDao
    abstract fun tagDao(): TagDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val seedCallback = SeedCallback(context)
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mipatrimonio.db"
                )
                    .addCallback(seedCallback)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}