package com.app.traveldocs.data.nlp

import com.app.traveldocs.domain.model.Document
import com.app.traveldocs.domain.model.DocumentType
import com.app.traveldocs.domain.model.MissingDocument
import com.app.traveldocs.domain.model.RequiredDocument
import com.app.traveldocs.domain.model.TravelDocumentChecklist
import com.app.traveldocs.domain.model.TravelParameters
import com.app.traveldocs.domain.repository.DocumentChecklistGenerator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BasicDocumentChecklistGenerator @Inject constructor() : DocumentChecklistGenerator {

    override fun generateChecklist(params: TravelParameters): TravelDocumentChecklist {
        val familySize = params.familySize ?: 1
        val requiredDocuments = mutableListOf<RequiredDocument>()

        // Always require passports for each family member
        requiredDocuments.add(
            RequiredDocument(
                type = DocumentType.PASSPORT,
                countNeeded = familySize,
                description = "Valid passport for each traveler"
            )
        )

        // For international travel (destination differs from origin), add visas
        if (isInternationalTravel(params)) {
            requiredDocuments.add(
                RequiredDocument(
                    type = DocumentType.VISA,
                    countNeeded = familySize,
                    description = "Visa for ${params.destination ?: "destination"}"
                )
            )
        }

        // For trips longer than 3 days, add hotel booking
        val durationDays = params.durationDays ?: 1
        if (durationDays > 3) {
            requiredDocuments.add(
                RequiredDocument(
                    type = DocumentType.HOTEL_BOOKING,
                    countNeeded = 1,
                    description = "Hotel booking confirmation"
                )
            )
        }

        // Always add health insurance
        requiredDocuments.add(
            RequiredDocument(
                type = DocumentType.HEALTH_INSURANCE,
                countNeeded = 1,
                description = "Travel health insurance policy"
            )
        )

        // For air travel (default assumption), add tickets for each family member
        requiredDocuments.add(
            RequiredDocument(
                type = DocumentType.TICKET,
                countNeeded = familySize,
                description = "Flight ticket for each traveler"
            )
        )

        val totalCount = requiredDocuments.sumOf { it.countNeeded }
        return TravelDocumentChecklist(
            requiredDocuments = requiredDocuments,
            totalCount = totalCount
        )
    }

    override fun detectMissing(
        checklist: TravelDocumentChecklist,
        existingDocuments: List<Document>
    ): List<MissingDocument> {
        val missingDocuments = mutableListOf<MissingDocument>()

        for (required in checklist.requiredDocuments) {
            val matchingCount = existingDocuments.count { it.type == required.type }
            if (matchingCount < required.countNeeded) {
                val suggestion = generateSuggestion(required.type)
                missingDocuments.add(
                    MissingDocument(
                        required = required,
                        suggestion = suggestion
                    )
                )
            }
        }

        return missingDocuments
    }

    private fun isInternationalTravel(params: TravelParameters): Boolean {
        val origin = params.origin
        val destination = params.destination
        // If we have both origin and destination and they differ, it's international
        if (origin != null && destination != null) {
            return !origin.equals(destination, ignoreCase = true)
        }
        // If we have a destination but no origin, assume international
        return destination != null
    }

    private fun generateSuggestion(type: DocumentType): String {
        return when (type) {
            DocumentType.PASSPORT -> "Ensure passport validity is 6+ months"
            DocumentType.VISA -> "Apply for visa at embassy"
            DocumentType.TICKET -> "Book flight tickets for all travelers"
            DocumentType.HOTEL_BOOKING -> "Reserve accommodation for your stay"
            DocumentType.HEALTH_INSURANCE -> "Check vaccination requirements"
            DocumentType.UNKNOWN -> "Prepare required documentation"
        }
    }
}
