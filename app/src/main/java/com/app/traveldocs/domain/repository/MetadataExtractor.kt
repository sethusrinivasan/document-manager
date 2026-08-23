package com.app.traveldocs.domain.repository

import com.app.traveldocs.domain.model.DocumentType
import com.app.traveldocs.domain.model.ExtractionResult

interface MetadataExtractor {
    suspend fun extract(imageData: ByteArray): Result<ExtractionResult>
    suspend fun classifyDocumentType(imageData: ByteArray): Result<DocumentType>
}
