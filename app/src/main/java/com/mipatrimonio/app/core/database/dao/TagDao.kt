package com.mipatrimonio.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.mipatrimonio.app.core.database.entity.TagEntity
import com.mipatrimonio.app.core.database.entity.TransactionTagCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun observeAll(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun getById(id: Long): TagEntity?

    @Insert
    suspend fun insert(tag: TagEntity): Long

    @Delete
    suspend fun delete(tag: TagEntity)

    @Insert
    suspend fun addCrossRef(crossRef: TransactionTagCrossRef)

    @Delete
    suspend fun removeCrossRef(crossRef: TransactionTagCrossRef)

    @Query("SELECT t.* FROM tags t JOIN transaction_tags tt ON t.id = tt.tagId WHERE tt.transactionId = :txId")
    fun observeTagsForTransaction(txId: Long): Flow<List<TagEntity>>

    @Query("DELETE FROM transaction_tags WHERE transactionId = :txId AND tagId = :tagId")
    suspend fun removeTagFromTransaction(txId: Long, tagId: Long)
}