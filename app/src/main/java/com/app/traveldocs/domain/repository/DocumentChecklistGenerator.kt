package com.app.traveldocs.domain.repository

import com.app.traveldocs.domain.model.Document
import com.app.traveldocs.domain.model.MissingDocument
import com.app.traveldocs.domain.model.TravelDocumentChecklist
import com.app.traveldocs.domain.model.TravelParameters

interface DocumentChecklistGenerator {
    fun generateChecklist(params: TravelParameters): TravelDocumentChecklist
    fun detectMissing(checklist: TravelDocumentChecklist, existingDocuments: List<Document>): List<MissingDocument>
}
