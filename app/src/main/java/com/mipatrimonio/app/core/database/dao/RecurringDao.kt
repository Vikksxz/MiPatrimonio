package com.mipatrimonio.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.mipatrimonio.app.core.database.entity.RecurringEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringDao {

    @Query("SELECT * FROM recurring WHERE active = 1 ORDER BY nextDate ASC")
    fun observeActive(): Flow<List<RecurringEntity>>

    @Query("SELECT * FROM recurring WHERE active = 1 AND nextDate <= :untilEpochDay ORDER BY nextDate ASC")
    fun observeUpcoming(untilEpochDay: Long): Flow<List<RecurringEntity>>

    @Query("SELECT * FROM recurring WHERE active = 1 AND nextDate = :onEpochDay")
    suspend fun getDue(onEpochDay: Long): List<RecurringEntity>

    @Query("SELECT * FROM recurring WHERE id = :id")
    suspend fun getById(id: Long): RecurringEntity?

    @Insert
    suspend fun insert(recurring: RecurringEntity): Long

    @Update
    suspend fun update(recurring: RecurringEntity): Int

    @Delete
    suspend fun delete(recurring: RecurringEntity)

    @Query("UPDATE recurring SET active = :active WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean): Int

    @Query("UPDATE recurring SET nextDate = :nextDate WHERE id = :id")
    suspend fun updateNextDate(id: Long, nextDate: Long): Int
}