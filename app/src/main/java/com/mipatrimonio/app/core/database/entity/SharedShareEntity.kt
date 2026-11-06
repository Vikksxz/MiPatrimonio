package com.mipatrimonio.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "shared_shares",
    primaryKeys = ["sharedExpenseId", "personId"]
)
data class SharedShareEntity(
    val sharedExpenseId: Long,
    val personId: Long,
    val shareCents: Long,
    val settled: Boolean = false
)