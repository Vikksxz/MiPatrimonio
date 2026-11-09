package com.mipatrimonio.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mipatrimonio.app.core.database.entity.SharedShareEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SharedShareDao {
    @Query("SELECT * FROM shared_shares")
    fun observeAll(): Flow<List<SharedShareEntity>>

    @Query("SELECT * FROM shared_shares")
    suspend fun getAll(): List<SharedShareEntity>

    @Query("SELECT * FROM shared_shares WHERE sharedExpenseId = :expenseId")
    fun observeByExpense(expenseId: Long): Flow<List<SharedShareEntity>>

    @Query("SELECT * FROM shared_shares WHERE sharedExpenseId = :expenseId")
    suspend fun getByExpense(expenseId: Long): List<SharedShareEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(share: SharedShareEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SharedShareEntity>)

    @Query("UPDATE shared_shares SET settled = :settled WHERE sharedExpenseId = :expenseId")
    suspend fun setSettled(expenseId: Long, settled: Boolean)

    @Query("UPDATE shared_shares SET settled = :settled WHERE sharedExpenseId = :expenseId AND personId = :personId")
    suspend fun setSettled(expenseId: Long, personId: Long, settled: Boolean)

    @Query("DELETE FROM shared_shares WHERE sharedExpenseId = :expenseId")
    suspend fun deleteByExpense(expenseId: Long)

    @Query("DELETE FROM shared_shares")
    suspend fun deleteAll()
}
