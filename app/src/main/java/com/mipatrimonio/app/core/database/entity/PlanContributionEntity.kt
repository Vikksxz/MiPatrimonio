package com.mipatrimonio.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "plan_contributions",
    indices = [Index("planId")]
)
data class PlanContributionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val amountCents: Long,
    val date: Long
)