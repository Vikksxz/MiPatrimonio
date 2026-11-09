package com.mipatrimonio.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mipatrimonio.app.core.database.entity.PersonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {
    @Query("SELECT * FROM people ORDER BY isMe DESC, name ASC")
    fun observeAll(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM people ORDER BY isMe DESC, name ASC")
    suspend fun getAll(): List<PersonEntity>

    @Query("SELECT * FROM people WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): PersonEntity?

    @Query("SELECT * FROM people WHERE isMe = 1 LIMIT 1")
    suspend fun getMe(): PersonEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(person: PersonEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PersonEntity>): List<Long>

    @Delete
    suspend fun delete(person: PersonEntity)

    @Query("DELETE FROM people")
    suspend fun deleteAll()
}
