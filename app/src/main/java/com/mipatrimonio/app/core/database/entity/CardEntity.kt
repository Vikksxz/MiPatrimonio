package com.mipatrimonio.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mipatrimonio.app.core.model.CardKind

@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bank: String,
    val variant: String = "",
    val kind: CardKind,
    val alias: String,
    val theme: String,
    val photoPath: String? = null,
    val createdAt: Long
)