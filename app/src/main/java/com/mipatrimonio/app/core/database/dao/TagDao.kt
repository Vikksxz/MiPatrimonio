package com.mipatrimonio.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.mipatrimonio.app.core.database.entity.TagEntity
import com.mipatrimonio.app.core.database.entity.TransactionTagCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun observeAll(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun getById(id: Long): TagEntity?

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): TagEntity?

    @Insert
    suspend fun insert(tag: TagEntity): Long

    @Delete
    suspend fun delete(tag: TagEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addCrossRef(crossRef: TransactionTagCrossRef)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addCrossRefs(crossRefs: List<TransactionTagCrossRef>)

    @Delete
    suspend fun removeCrossRef(crossRef: TransactionTagCrossRef)

    @Query("SELECT t.* FROM tags t JOIN transaction_tags tt ON t.id = tt.tagId WHERE tt.transactionId = :txId")
    fun observeTagsForTransaction(txId: Long): Flow<List<TagEntity>>

    @Query("SELECT tagId FROM transaction_tags WHERE transactionId = :txId")
    suspend fun getTagIdsForTransaction(txId: Long): List<Long>

    @Query("SELECT * FROM transaction_tags")
    fun observeAllCrossRefs(): Flow<List<TransactionTagCrossRef>>

    @Query("DELETE FROM transaction_tags WHERE transactionId = :txId")
    suspend fun clearTagsForTransaction(txId: Long)

    @Query("DELETE FROM transaction_tags WHERE transactionId = :txId AND tagId = :tagId")
    suspend fun removeTagFromTransaction(txId: Long, tagId: Long)

    @Transaction
    suspend fun replaceTagsForTransaction(txId: Long, tagIds: List<Long>) {
        clearTagsForTransaction(txId)
        if (tagIds.isNotEmpty()) {
            addCrossRefs(tagIds.map { TransactionTagCrossRef(txId, it) })
        }
    }
}