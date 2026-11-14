package com.mipatrimonio.app.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mipatrimonio.app.core.database.dao.BudgetDao
import com.mipatrimonio.app.core.database.dao.CardDao
import com.mipatrimonio.app.core.database.dao.CategoryDao
import com.mipatrimonio.app.core.database.dao.DocumentDao
import com.mipatrimonio.app.core.database.dao.ExchangeRateDao
import com.mipatrimonio.app.core.database.dao.PersonDao
import com.mipatrimonio.app.core.database.dao.RecurringDao
import com.mipatrimonio.app.core.database.dao.SavingsDao
import com.mipatrimonio.app.core.database.dao.SharedExpenseDao
import com.mipatrimonio.app.core.database.dao.SharedShareDao
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
    version = 2,
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
    abstract fun exchangeRateDao(): ExchangeRateDao
    abstract fun personDao(): PersonDao
    abstract fun sharedExpenseDao(): SharedExpenseDao
    abstract fun sharedShareDao(): SharedShareDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        /** T6.1: trazabilidad de aportaciones (origen, archivado, fecha de registro). */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `plan_contributions` ADD COLUMN `sourceTransactionId` INTEGER DEFAULT NULL"
                )
                db.execSQL(
                    "ALTER TABLE `plan_contributions` ADD COLUMN `originType` TEXT NOT NULL DEFAULT 'LEGACY'"
                )
                db.execSQL(
                    "ALTER TABLE `plan_contributions` ADD COLUMN `archived` INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE `plan_contributions` ADD COLUMN `createdAt` INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_plan_contributions_sourceTransactionId` ON `plan_contributions` (`sourceTransactionId`)"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val seedCallback = SeedCallback(context)
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mipatrimonio.db"
                )
                    .addCallback(seedCallback)
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}