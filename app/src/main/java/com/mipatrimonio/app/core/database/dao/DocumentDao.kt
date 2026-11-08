package com.mipatrimonio.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.mipatrimonio.app.core.database.entity.DocumentEntity
import com.mipatrimonio.app.core.model.DocumentKind
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    @Query("SELECT * FROM documents ORDER BY date DESC")
    fun observeAll(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE kind = :kind ORDER BY date DESC")
    fun observeByKind(kind: DocumentKind): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE kind = :kind ORDER BY date DESC")
    suspend fun getAllByKind(kind: DocumentKind): List<DocumentEntity>

    @Query("SELECT * FROM documents WHERE expiresAt IS NOT NULL AND expiresAt <= :beforeEpochDay ORDER BY expiresAt ASC")
    fun observeExpiringBefore(beforeEpochDay: Long): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getById(id: Long): DocumentEntity?

    @Insert
    suspend fun insert(document: DocumentEntity): Long

    @Update
    suspend fun update(document: DocumentEntity): Int

    @Delete
    suspend fun delete(document: DocumentEntity)
}