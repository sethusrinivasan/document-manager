package com.app.paperstow.domain.repository

import com.app.paperstow.domain.model.DocumentType
import com.app.paperstow.domain.model.ExtractedValue
import com.app.paperstow.domain.model.MetadataField

interface AutoTagGenerator {
    fun generateTags(documentType: DocumentType, metadata: Map<MetadataField, ExtractedValue>): List<String>
}
