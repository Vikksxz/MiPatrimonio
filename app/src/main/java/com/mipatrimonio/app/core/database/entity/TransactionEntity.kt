package com.mipatrimonio.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mipatrimonio.app.core.model.IncomeKind
import com.mipatrimonio.app.core.model.TransactionType

@Entity(
    tableName = "transactions",
    indices = [
        Index("date"),
        Index("type"),
        Index("categoryId"),
        Index("cardId"),
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TransactionType,
    val incomeKind: IncomeKind? = null,
    val amountCents: Long,
    val currency: String = "EUR",
    val amountBaseCents: Long,
    val date: Long,
    val merchant: String,
    val note: String = "",
    val categoryId: Long? = null,
    val cardId: Long? = null,
    val receiptPath: String? = null,
    val recurringId: Long? = null,
    val sharedExpenseId: Long? = null,
    val createdAt: Long
)