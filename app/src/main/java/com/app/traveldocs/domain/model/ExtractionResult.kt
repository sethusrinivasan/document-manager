package com.app.traveldocs.domain.model

data class ExtractedValue(
    val value: String,
    val confidence: Float
)

data class ExtractionResult(
    val documentType: DocumentType,
    val metadata: Map<MetadataField, ExtractedValue>,
    val confidence: Float,
    val requiresManualReview: Boolean
)
