package com.mipatrimonio.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "savings_plans")
data class SavingsPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetCents: Long,
    val monthlyQuotaCents: Long,
    val priority: Int,
    val isActive: Boolean,
    val initialEstimatedMonths: Int,
    val lastNotifiedState: String = "NONE",
    val lastNotifiedMonths: Int = -1,
    val createdAt: Long
)