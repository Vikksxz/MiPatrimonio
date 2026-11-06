package com.mipatrimonio.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "transaction_tags",
    primaryKeys = ["transactionId", "tagId"]
)
data class TransactionTagCrossRef(
    val transactionId: Long,
    val tagId: Long
)