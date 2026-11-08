package com.mipatrimonio.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.mipatrimonio.app.core.database.entity.CardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {

    @Query("SELECT * FROM cards ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards ORDER BY createdAt DESC")
    suspend fun getAll(): List<CardEntity>

    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun getById(id: Long): CardEntity?

    @Query("SELECT COUNT(*) FROM cards")
    fun count(): Flow<Int>

    @Insert
    suspend fun insert(card: CardEntity): Long

    @Insert
    suspend fun insertAll(cards: List<CardEntity>)

    @Update
    suspend fun update(card: CardEntity): Int

    @Delete
    suspend fun delete(card: CardEntity)

    @Query("DELETE FROM cards")
    suspend fun deleteAll()
}