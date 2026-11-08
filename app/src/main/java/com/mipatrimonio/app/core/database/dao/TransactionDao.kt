package com.mipatrimonio.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
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

    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    suspend fun getAll(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE date >= :fromEpochDay AND date < :toEpochDayExclusive ORDER BY date DESC, id DESC")
    fun observeByMonth(fromEpochDay: Long, toEpochDayExclusive: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE date >= :fromEpochDay AND date < :toEpochDayExclusive ORDER BY date DESC, id DESC")
    suspend fun getByMonth(fromEpochDay: Long, toEpochDayExclusive: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TransactionEntity?

    /** Últimos movimientos para el resumen de Inicio (T1.9), sin filtrar por mes. */
    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<TransactionEntity>>

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

    @Query("SELECT COUNT(*) FROM transactions WHERE recurringId = :recurringId AND date = :epochDay")
    suspend fun countForRecurringOnDate(recurringId: Long, epochDay: Long): Int

    @Query("SELECT categoryId, SUM(amountBaseCents) as totalCents FROM transactions WHERE date >= :from AND date < :to AND type IN ('CASH','CARD','SUBSCRIPTION') GROUP BY categoryId")
    fun observeSumBaseCentsByCategory(from: Long, to: Long): Flow<List<CategorySum>>

    data class CategorySum(val categoryId: Long, val totalCents: Long)

    @Insert
    suspend fun insert(transaction: TransactionEntity): Long

    /** Reinserta un movimiento borrado (deshacer), conservando su id. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun restore(transaction: TransactionEntity)

    @Insert
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Update
    suspend fun update(transaction: TransactionEntity): Int

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Transaction
    @Query("DELETE FROM transactions WHERE cardId = :cardId")
    suspend fun deleteByCardId(cardId: Long)

    @Query("UPDATE transactions SET cardId = NULL WHERE cardId = :cardId")
    suspend fun nullifyCardId(cardId: Long)

    /** Al borrar una tarjeta sus movimientos se conservan, sin tarjeta asociada (§6). */
    @Transaction
    suspend fun deleteCardAndNullify(cardId: Long) {
        nullifyCardId(cardId)
    }

    @Transaction
    suspend fun reassignCategory(fromId: Long, toId: Long?) {
        updateCategory(fromId, toId)
    }

    @Query("UPDATE transactions SET categoryId = :toId WHERE categoryId = :fromId")
    suspend fun updateCategory(fromId: Long, toId: Long?)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}