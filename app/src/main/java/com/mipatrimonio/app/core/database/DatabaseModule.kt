package com.mipatrimonio.app.core.database

import android.content.Context
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
import com.mipatrimonio.app.core.database.seed.SeedCallback
import com.mipatrimonio.app.core.security.CryptoManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@dagger.hilt.android.qualifiers.ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideSeedCallback(@dagger.hilt.android.qualifiers.ApplicationContext context: Context): SeedCallback {
        return SeedCallback(context)
    }

    @Provides
    @Singleton
    fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()

    @Provides
    @Singleton
    fun provideCardDao(db: AppDatabase): CardDao = db.cardDao()

    @Provides
    @Singleton
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    @Singleton
    fun provideRecurringDao(db: AppDatabase): RecurringDao = db.recurringDao()

    @Provides
    @Singleton
    fun provideSavingsDao(db: AppDatabase): SavingsDao = db.savingsDao()

    @Provides
    @Singleton
    fun provideBudgetDao(db: AppDatabase): BudgetDao = db.budgetDao()

    @Provides
    @Singleton
    fun provideDocumentDao(db: AppDatabase): DocumentDao = db.documentDao()

    @Provides
    @Singleton
    fun provideTagDao(db: AppDatabase): TagDao = db.tagDao()

    @Provides
    @Singleton
    fun provideExchangeRateDao(db: AppDatabase): ExchangeRateDao = db.exchangeRateDao()

    @Provides
    @Singleton
    fun providePersonDao(db: AppDatabase): PersonDao = db.personDao()

    @Provides
    @Singleton
    fun provideSharedExpenseDao(db: AppDatabase): SharedExpenseDao = db.sharedExpenseDao()

    @Provides
    @Singleton
    fun provideSharedShareDao(db: AppDatabase): SharedShareDao = db.sharedShareDao()

    @Provides
    @Singleton
    fun provideCryptoManager(@dagger.hilt.android.qualifiers.ApplicationContext context: Context): CryptoManager {
        return CryptoManager(context)
    }
}