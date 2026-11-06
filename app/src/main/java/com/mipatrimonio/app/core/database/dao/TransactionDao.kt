package com.mipatrimonio.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.mipatrimonio.app.core.database.entity.TransactionEntity
import com.mipatrimonio.app.core.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE date >= :fromEpochDay AND date < :toEpochDayExclusive ORDER BY date DESC, id DESC")
    fun observeByMonth(fromEpochDay: Long, toEpochDayExclusive: Long): Flow<List<TransactionEntity>>

    @Query("SELECT type, SUM(amountBaseCents) as totalCents FROM transactions WHERE date >= :from AND date < :to GROUP BY type")
    fun observeTotalsByType(from: Long, to: Long): Flow<List<TypeTotal>>

    data class TypeTotal(val type: TransactionType, val totalCents: Long)

    @Query("SELECT SUM(amountBaseCents) FROM transactions WHERE date >= :from AND date < :to AND type IN (:types)")
    fun observeSumBaseCents(from: Long, to: Long, types: List<TransactionType>): Flow<Long?>

    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId AND date >= :from AND date < :to ORDER BY date DESC")
    fun observeByCategory(categoryId: Long, from: Long, to: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE cardId = :cardId AND date >= :from AND date < :to ORDER BY date DESC")
    fun observeByCard(cardId: Long, from: Long, to: Long): Flow<List<TransactionEntity>>

    @Query("SELECT COUNT(*) FROM transactions WHERE recurringId = :recurringId AND date = :epochDay")
    fun existsForRecurring(recurringId: Long, epochDay: Long): Flow<Int>

    @Query("SELECT categoryId, SUM(amountBaseCents) as totalCents FROM transactions WHERE date >= :from AND date < :to AND type IN ('CASH','CARD','SUBSCRIPTION') GROUP BY categoryId")
    fun observeSumBaseCentsByCategory(from: Long, to: Long): Flow<List<CategorySum>>

    data class CategorySum(val categoryId: Long, val totalCents: Long)

    @Insert
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Update
    suspend fun update(transaction: TransactionEntity): Int

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Transaction
    @Query("DELETE FROM transactions WHERE cardId = :cardId")
    suspend fun deleteByCardId(cardId: Long)

    @Transaction
    suspend fun deleteCardAndNullify(cardId: Long) {
        // No borrar los movimientos: solo nullificar cardId
        deleteByCardId(cardId)
    }

    @Transaction
    suspend fun reassignCategory(fromId: Long, toId: Long?) {
        updateCategory(fromId, toId)
    }

    @Query("UPDATE transactions SET categoryId = :toId WHERE categoryId = :fromId")
    suspend fun updateCategory(fromId: Long, toId: Long?)
}