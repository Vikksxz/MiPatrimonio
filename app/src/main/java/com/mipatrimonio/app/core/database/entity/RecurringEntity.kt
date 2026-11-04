package com.mipatrimonio.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mipatrimonio.app.core.model.RecurrencePeriod
import com.mipatrimonio.app.core.model.TransactionType

@Entity(tableName = "recurring")
data class RecurringEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: TransactionType,
    val amountCents: Long,
    val period: RecurrencePeriod,
    val nextDate: Long,
    val cardId: Long? = null,
    val categoryId: Long? = null,
    val isSubscription: Boolean,
    val remindDaysBefore: Int = 2,
    val active: Boolean = true
)