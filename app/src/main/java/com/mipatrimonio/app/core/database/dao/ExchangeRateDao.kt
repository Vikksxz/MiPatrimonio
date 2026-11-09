package com.mipatrimonio.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mipatrimonio.app.core.database.entity.ExchangeRateEntity

@Dao
interface ExchangeRateDao {
    @Query("SELECT * FROM exchange_rates ORDER BY currency ASC")
    suspend fun getAll(): List<ExchangeRateEntity>

    @Query("SELECT * FROM exchange_rates ORDER BY currency ASC")
    fun observeAll(): kotlinx.coroutines.flow.Flow<List<ExchangeRateEntity>>

    @Query("SELECT * FROM exchange_rates WHERE currency = :currency LIMIT 1")
    suspend fun getByCurrency(currency: String): ExchangeRateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ExchangeRateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ExchangeRateEntity>)

    @Query("DELETE FROM exchange_rates WHERE currency = :currency")
    suspend fun delete(currency: String)

    @Query("DELETE FROM exchange_rates")
    suspend fun deleteAll()
}
