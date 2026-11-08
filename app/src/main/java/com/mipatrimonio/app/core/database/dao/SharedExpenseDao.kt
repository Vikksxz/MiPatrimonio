package com.mipatrimonio.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mipatrimonio.app.core.database.entity.SharedExpenseEntity

@Dao
interface SharedExpenseDao {
    @Query("SELECT * FROM shared_expenses")
    suspend fun getAll(): List<SharedExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SharedExpenseEntity>): List<Long>

    @Query("DELETE FROM shared_expenses")
    suspend fun deleteAll()
}
