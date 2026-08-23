package com.app.traveldocs.domain.model

import java.time.Instant

data class Document(
    val id: String,
    val memberId: String,
    val type: DocumentType,
    val format: DocumentFormat,
    val originalFileName: String?,
    val metadata: Map<MetadataField, String>,
    val tags: List<Tag>,
    val createdAt: Instant,
    val updatedAt: Instant,
    val extractionConfidence: Float?,
    val requiresManualReview: Boolean
)
