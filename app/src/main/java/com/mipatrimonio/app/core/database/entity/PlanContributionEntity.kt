package com.mipatrimonio.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Origen de una aportación a un plan de ahorro (T6.1, §6). */
object ContributionOriginType {
    const val INCOME = "INCOME"
    const val CASH_SAVINGS = "CASH_SAVINGS"
    const val LEGACY = "LEGACY"
}

@Entity(
    tableName = "plan_contributions",
    indices = [Index("planId"), Index("sourceTransactionId")]
)
data class PlanContributionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val amountCents: Long,
    val date: Long,
    val sourceTransactionId: Long? = null,
    val originType: String = ContributionOriginType.LEGACY,
    val archived: Boolean = false,
    val createdAt: Long = 0L,
)