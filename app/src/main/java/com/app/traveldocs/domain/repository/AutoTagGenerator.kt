package com.app.traveldocs.domain.repository

import com.app.traveldocs.domain.model.DocumentType
import com.app.traveldocs.domain.model.ExtractedValue
import com.app.traveldocs.domain.model.MetadataField

interface AutoTagGenerator {
    fun generateTags(documentType: DocumentType, metadata: Map<MetadataField, ExtractedValue>): List<String>
}
