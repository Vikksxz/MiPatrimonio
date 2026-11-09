package com.mipatrimonio.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mipatrimonio.app.core.database.entity.SharedExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SharedExpenseDao {
    @Query("SELECT * FROM shared_expenses ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<SharedExpenseEntity>>

    @Query("SELECT * FROM shared_expenses ORDER BY date DESC, id DESC")
    suspend fun getAll(): List<SharedExpenseEntity>

    @Query("SELECT * FROM shared_expenses WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SharedExpenseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: SharedExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SharedExpenseEntity>): List<Long>

    @Update
    suspend fun update(expense: SharedExpenseEntity)

    @Delete
    suspend fun delete(expense: SharedExpenseEntity)

    @Query("DELETE FROM shared_expenses")
    suspend fun deleteAll()
}
