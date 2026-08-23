package com.app.traveldocs.data.tags

import com.app.traveldocs.domain.model.DocumentType
import com.app.traveldocs.domain.model.ExtractedValue
import com.app.traveldocs.domain.model.MetadataField
import com.app.traveldocs.domain.repository.AutoTagGenerator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutoTagGeneratorImpl @Inject constructor() : AutoTagGenerator {

    override fun generateTags(
        documentType: DocumentType,
        metadata: Map<MetadataField, ExtractedValue>
    ): List<String> {
        return try {
            val tags = mutableListOf<String>()

            addTypeTag(documentType, tags)
            addDestinationTag(metadata, tags)
            addDateTags(metadata, tags)

            tags
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun addTypeTag(documentType: DocumentType, tags: MutableList<String>) {
        try {
            val typeTag = when (documentType) {
                DocumentType.PASSPORT -> "passport"
                DocumentType.VISA -> "visa"
                DocumentType.TICKET -> "ticket"
                DocumentType.HOTEL_BOOKING -> "accommodation"
                DocumentType.HEALTH_INSURANCE -> "health"
                DocumentType.UNKNOWN -> null
            }
            typeTag?.let { tags.add(it) }
        } catch (_: Exception) {
            // Fail silently per requirement 5.9
        }
    }

    private fun addDestinationTag(metadata: Map<MetadataField, ExtractedValue>, tags: MutableList<String>) {
        try {
            val destination = metadata[MetadataField.DESTINATION]
            if (destination != null && destination.value.isNotBlank()) {
                tags.add(destination.value.trim().lowercase())
            }
        } catch (_: Exception) {
            // Fail silently per requirement 5.9
        }
    }

    private fun addDateTags(metadata: Map<MetadataField, ExtractedValue>, tags: MutableList<String>) {
        try {
            val expiryDate = metadata[MetadataField.EXPIRY_DATE]
            if (expiryDate != null && expiryDate.value.isNotBlank()) {
                extractYear(expiryDate.value)?.let { year ->
                    tags.add("expires-$year")
                }
            }
        } catch (_: Exception) {
            // Fail silently per requirement 5.9
        }

        try {
            val issueDate = metadata[MetadataField.ISSUE_DATE]
            if (issueDate != null && issueDate.value.isNotBlank()) {
                extractYear(issueDate.value)?.let { year ->
                    tags.add("issued-$year")
                }
            }
        } catch (_: Exception) {
            // Fail silently per requirement 5.9
        }
    }

    private fun extractYear(dateString: String): String? {
        // Match a 4-digit year in the date string
        val yearPattern = Regex("\\b(\\d{4})\\b")
        return yearPattern.find(dateString)?.groupValues?.get(1)
    }
}
