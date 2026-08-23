package com.app.traveldocs.domain.model

data class RequiredDocument(
    val type: DocumentType,
    val countNeeded: Int,
    val description: String
)

data class TravelDocumentChecklist(
    val requiredDocuments: List<RequiredDocument>,
    val totalCount: Int
)

data class MissingDocument(
    val required: RequiredDocument,
    val suggestion: String
)
