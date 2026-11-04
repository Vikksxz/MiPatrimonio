package com.mipatrimonio.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mipatrimonio.app.core.model.DocumentKind

@Entity(
    tableName = "documents",
    indices = [Index("kind")]
)
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: DocumentKind,
    val title: String,
    val filePath: String,
    val mimeType: String,
    val date: Long,
    val expiresAt: Long? = null,
    val createdAt: Long
)