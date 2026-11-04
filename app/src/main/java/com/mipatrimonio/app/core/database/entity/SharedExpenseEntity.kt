package com.mipatrimonio.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shared_expenses")
data class SharedExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val totalCents: Long,
    val paidByPersonId: Long,
    val date: Long
)