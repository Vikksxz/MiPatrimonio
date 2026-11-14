package com.mipatrimonio.app.core.database.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
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

    @Query("SELECT * FROM plan_contributions WHERE planId = :planId ORDER BY date DESC, id DESC")
    fun observeContributions(planId: Long): Flow<List<PlanContributionEntity>>

    @Query("SELECT * FROM plan_contributions WHERE planId = :planId AND archived = 0 ORDER BY date DESC, id DESC")
    fun observeVisibleContributions(planId: Long): Flow<List<PlanContributionEntity>>

    @Query("SELECT * FROM plan_contributions WHERE sourceTransactionId = :sourceTransactionId ORDER BY date DESC, id DESC")
    fun observeContributionsBySource(sourceTransactionId: Long): Flow<List<PlanContributionEntity>>

    data class ContributionWithPlan(
        @Embedded val contribution: PlanContributionEntity,
        @androidx.room.ColumnInfo(name = "planName") val planName: String,
    )

    @Query(
        "SELECT pc.*, sp.name AS planName FROM plan_contributions pc " +
            "INNER JOIN savings_plans sp ON sp.id = pc.planId " +
            "WHERE pc.sourceTransactionId = :sourceTransactionId ORDER BY pc.date DESC, pc.id DESC"
    )
    fun observeContributionsWithPlanBySource(sourceTransactionId: Long): Flow<List<ContributionWithPlan>>

    @Query("SELECT COUNT(*) FROM plan_contributions WHERE sourceTransactionId = :sourceTransactionId")
    suspend fun countLinkedContributions(sourceTransactionId: Long): Int

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM plan_contributions WHERE sourceTransactionId = :sourceTransactionId")
    suspend fun sumLinkedContributionAmountCents(sourceTransactionId: Long): Long

    @Query("SELECT * FROM plan_contributions WHERE id = :id LIMIT 1")
    suspend fun getContributionById(id: Long): PlanContributionEntity?

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

    @Query("DELETE FROM plan_contributions WHERE id = :id")
    suspend fun deleteContributionById(id: Long)

    @Query("UPDATE plan_contributions SET archived = :archived WHERE id = :id")
    suspend fun setContributionArchived(id: Long, archived: Boolean)

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

    // Backup/restore (T4.2)
    @Query("SELECT * FROM savings_plans")
    suspend fun getAllPlans(): List<SavingsPlanEntity>

    @Query("SELECT * FROM plan_contributions")
    suspend fun getAllContributions(): List<PlanContributionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPlans(items: List<SavingsPlanEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllContributions(items: List<PlanContributionEntity>)

    @Query("DELETE FROM plan_contributions")
    suspend fun deleteAllContributions()

    @Query("DELETE FROM savings_plans")
    suspend fun deleteAllPlans()
}