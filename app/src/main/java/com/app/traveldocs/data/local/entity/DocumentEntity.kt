package com.app.traveldocs.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val id: String,
    val memberId: String,
    val type: String,
    val fileId: String,
    val format: String,
    val originalFileName: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val extractionConfidence: Float?,
    val requiresManualReview: Boolean = false
)
