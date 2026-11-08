package com.mipatrimonio.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.mipatrimonio.app.core.database.entity.BudgetEntity
import com.mipatrimonio.app.core.database.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Query("SELECT * FROM budgets ORDER BY categoryId")
    fun observeAll(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets ORDER BY categoryId")
    suspend fun getAll(): List<BudgetEntity>

    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId")
    suspend fun getByCategory(categoryId: Long): BudgetEntity?

    @Transaction
    @Query("SELECT * FROM budgets")
    fun observeWithCategory(): Flow<List<BudgetWithCategory>>

    @Transaction
    @Query("SELECT * FROM budgets")
    suspend fun getAllWithCategory(): List<BudgetWithCategory>

    data class BudgetWithCategory(
        @Embedded val budget: BudgetEntity,
        @Relation(parentColumn = "categoryId", entityColumn = "id") val category: CategoryEntity?
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: BudgetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<BudgetEntity>)

    @Delete
    suspend fun delete(budget: BudgetEntity)

    @Query("UPDATE budgets SET notified80Month = :m80, notified100Month = :m100 WHERE id = :id")
    suspend fun updateNotifiedMonths(id: Long, m80: Int, m100: Int): Int

    @Query("DELETE FROM budgets")
    suspend fun deleteAll()
}