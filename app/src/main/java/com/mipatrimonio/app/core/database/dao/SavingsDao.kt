package com.mipatrimonio.app.core.database.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.mipatrimonio.app.core.database.entity.PlanContributionEntity
import com.mipatrimonio.app.core.database.entity.SavingsPlanEntity
import kotlinx.coroutines.flow.Flow

/** Plan con sus aportaciones (relación 1:N). */
data class PlanWithContributions(
    @Embedded val plan: SavingsPlanEntity,
    @Relation(parentColumn = "id", entityColumn = "planId")
    val contributions: List<PlanContributionEntity>,
)

@Dao
interface SavingsDao {

    @Query("SELECT * FROM savings_plans ORDER BY priority ASC, createdAt ASC")
    fun observePlans(): Flow<List<SavingsPlanEntity>>

    @Transaction
    @Query("SELECT * FROM savings_plans ORDER BY priority ASC, createdAt ASC")
    fun observePlansWithContributions(): Flow<List<PlanWithContributions>>

    @Query("SELECT * FROM savings_plans WHERE isActive = 1 LIMIT 1")
    fun observeActivePlan(): Flow<SavingsPlanEntity?>

    @Query("SELECT * FROM savings_plans WHERE id = :id")
    suspend fun getPlanById(id: Long): SavingsPlanEntity?

    @Query("SELECT * FROM plan_contributions WHERE planId = :planId ORDER BY date DESC")
    fun observeContributions(planId: Long): Flow<List<PlanContributionEntity>>

    @Query("SELECT SUM(amountCents) FROM plan_contributions WHERE planId = :planId")
    fun sumContributed(planId: Long): Flow<Long?>

    @Query("SELECT SUM(amountCents) FROM plan_contributions WHERE planId = :planId")
    suspend fun getContributed(planId: Long): Long?

    @Query("SELECT SUM(amountCents) FROM plan_contributions WHERE date >= :from AND date < :to")
    fun observeMonthlyContributed(from: Long, to: Long): Flow<Long?>

    @Query("SELECT SUM(amountCents) FROM plan_contributions WHERE date >= :from AND date < :to")
    suspend fun getMonthlyContributed(from: Long, to: Long): Long?

    @Insert
    suspend fun insertPlan(plan: SavingsPlanEntity): Long

    @Insert
    suspend fun insertContribution(contribution: PlanContributionEntity): Long

    @Update
    suspend fun updatePlan(plan: SavingsPlanEntity): Int

    @Query("UPDATE savings_plans SET isActive = 0")
    suspend fun clearActive()

    @Transaction
    suspend fun setActivePlan(planId: Long) {
        clearActive()
        activatePlan(planId)
    }

    @Query("UPDATE savings_plans SET isActive = 1 WHERE id = :planId")
    suspend fun activatePlan(planId: Long)

    @Query("SELECT * FROM savings_plans WHERE isActive = 1 ORDER BY priority ASC, createdAt ASC")
    suspend fun getActivePlans(): List<SavingsPlanEntity>

    @Query("UPDATE savings_plans SET lastNotifiedState = :state, lastNotifiedMonths = :months WHERE id = :id")
    suspend fun updateNotifiedState(id: Long, state: String, months: Int): Int

    @Transaction
    suspend fun deletePlanWithContributions(planId: Long) {
        deleteContributionsForPlan(planId)
        deletePlan(planId)
    }

    @Query("DELETE FROM plan_contributions WHERE planId = :planId")
    suspend fun deleteContributionsForPlan(planId: Long)

    @Query("DELETE FROM savings_plans WHERE id = :id")
    suspend fun deletePlan(id: Long)
}