package com.mipatrimonio.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mipatrimonio.app.core.database.entity.ExchangeRateEntity

@Dao
interface ExchangeRateDao {
    @Query("SELECT * FROM exchange_rates")
    suspend fun getAll(): List<ExchangeRateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ExchangeRateEntity>)

    @Query("DELETE FROM exchange_rates")
    suspend fun deleteAll()
}
