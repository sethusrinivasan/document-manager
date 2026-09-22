package com.app.paperstow.domain.repository

import com.app.paperstow.domain.model.Document
import com.app.paperstow.domain.model.MissingDocument
import com.app.paperstow.domain.model.TravelDocumentChecklist
import com.app.paperstow.domain.model.TravelParameters

interface DocumentChecklistGenerator {
    fun generateChecklist(params: TravelParameters): TravelDocumentChecklist
    fun detectMissing(checklist: TravelDocumentChecklist, existingDocuments: List<Document>): List<MissingDocument>
}
