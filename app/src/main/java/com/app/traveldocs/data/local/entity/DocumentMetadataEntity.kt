package com.app.traveldocs.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "document_metadata")
data class DocumentMetadataEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: String,
    val field: String,
    val value: String,
    val confidence: Float
)
