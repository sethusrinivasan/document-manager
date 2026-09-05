package com.app.traveldocs.domain.properties

import com.app.traveldocs.data.nlp.BasicDocumentChecklistGenerator
import com.app.traveldocs.domain.model.Document
import com.app.traveldocs.domain.model.DocumentFormat
import com.app.traveldocs.domain.model.DocumentType
import com.app.traveldocs.domain.model.RequiredDocument
import com.app.traveldocs.domain.model.Tag
import com.app.traveldocs.domain.model.TravelDocumentChecklist
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Property 21: Missing document detection is set difference
 *
 * For any checklist and any set of existing documents, detectMissing returns exactly
 * those types where existing count < required count.
 *
 * **Validates: Requirements 9.5, 9.6, 9.7**
 */
@DisplayName("Property Test")
// Removed JUnitTag - invalid syntax
class MissingDocumentDetectionPropertyTest {

    private lateinit var generator: BasicDocumentChecklistGenerator

    // Document types that can appear in checklists (exclude UNKNOWN as it's not used in checklists)
    private val checklistDocTypes = listOf(
        DocumentType.PASSPORT,
        DocumentType.VISA,
        DocumentType.TICKET,
        DocumentType.HOTEL_BOOKING,
        DocumentType.HEALTH_INSURANCE
    )

    @BeforeEach
    fun setUp() {
        generator = BasicDocumentChecklistGenerator()
    }

    // Generator for a RequiredDocument with a valid type and reasonable count
    private val arbRequiredDocument: Arb<RequiredDocument> = arbitrary {
        val type = Arb.element(checklistDocTypes).bind()
        val countNeeded = Arb.int(1..5).bind()
        RequiredDocument(
            type = type,
            countNeeded = countNeeded,
            description = "Required ${type.name.lowercase()}"
        )
    }

    // Generator for a checklist with 1..5 distinct required document types
    private val arbChecklist: Arb<TravelDocumentChecklist> = arbitrary {
        val count = Arb.int(1..5).bind()
        // Generate distinct types by shuffling and taking N
        val types = checklistDocTypes.shuffled().take(count)
        val requiredDocs = types.map { type ->
            val countNeeded = Arb.int(1..5).bind()
            RequiredDocument(
                type = type,
                countNeeded = countNeeded,
                description = "Required ${type.name.lowercase()}"
            )
        }
        val totalCount = requiredDocs.sumOf { it.countNeeded }
        TravelDocumentChecklist(
            requiredDocuments = requiredDocs,
            totalCount = totalCount
        )
    }

    // Generator for a list of existing documents with random types
    private val arbExistingDocuments: Arb<List<Document>> = arbitrary {
        val count = Arb.int(0..15).bind()
        (0 until count).map { i ->
            val type = Arb.element(DocumentType.entries.toList()).bind()
            val format = Arb.element(DocumentFormat.entries.toList()).bind()
            Document(
                id = "doc-$i",
                memberId = "member-1",
                type = type,
                format = format,
                originalFileName = "file-$i.pdf",
                metadata = emptyMap(),
                tags = listOf(Tag("test", false)),
                createdAt = Instant.ofEpochMilli(1700000000000L),
                updatedAt = Instant.ofEpochMilli(1700000000000L),
                extractionConfidence = 0.9f,
                requiresManualReview = false
            )
        }
    }

    @Test
    @DisplayName("detectMissing returns exactly those types where existing count < required count")
    fun `detectMissing identifies missing documents where count is less than needed`() = runTest {
        checkAll(100, arbChecklist, arbExistingDocuments) { checklist, existingDocs ->
            val missing = generator.detectMissing(checklist, existingDocs)

            // Compute expected missing: those required types where existing count < countNeeded
            val expectedMissingTypes = checklist.requiredDocuments.filter { required ->
                val matchingCount = existingDocs.count { it.type == required.type }
                matchingCount < required.countNeeded
            }.map { it.type }.toSet()

            val actualMissingTypes = missing.map { it.required.type }.toSet()

            assertEquals(
                expectedMissingTypes,
                actualMissingTypes,
                "Missing types mismatch. Expected: $expectedMissingTypes, Got: $actualMissingTypes. " +
                    "Checklist: ${checklist.requiredDocuments.map { "${it.type}:${it.countNeeded}" }}, " +
                    "Existing: ${existingDocs.map { it.type }}"
            )
        }
    }

    @Test
    @DisplayName("detectMissing returns empty when all required documents are satisfied")
    fun `detectMissing returns empty when all documents are present in sufficient quantity`() = runTest {
        checkAll(100, arbChecklist) { checklist ->
            // Create existing documents that satisfy all requirements
            val existingDocs = checklist.requiredDocuments.flatMapIndexed { reqIdx, required ->
                (0 until required.countNeeded).map { i ->
                    Document(
                        id = "doc-$reqIdx-$i",
                        memberId = "member-1",
                        type = required.type,
                        format = DocumentFormat.PDF,
                        originalFileName = "file.pdf",
                        metadata = emptyMap(),
                        tags = listOf(Tag("test", false)),
                        createdAt = Instant.ofEpochMilli(1700000000000L),
                        updatedAt = Instant.ofEpochMilli(1700000000000L),
                        extractionConfidence = 0.9f,
                        requiresManualReview = false
                    )
                }
            }

            val missing = generator.detectMissing(checklist, existingDocs)

            assertTrue(
                missing.isEmpty(),
                "Expected no missing documents when all requirements are met, but got: " +
                    "${missing.map { "${it.required.type}:${it.required.countNeeded}" }}. " +
                    "Checklist: ${checklist.requiredDocuments.map { "${it.type}:${it.countNeeded}" }}, " +
                    "Existing: ${existingDocs.map { it.type }}"
            )
        }
    }

    @Test
    @DisplayName("detectMissing returns all required types when no existing documents are present")
    fun `detectMissing flags all types as missing when no documents exist`() = runTest {
        checkAll(100, arbChecklist) { checklist ->
            val existingDocs = emptyList<Document>()

            val missing = generator.detectMissing(checklist, existingDocs)

            val expectedMissingTypes = checklist.requiredDocuments.map { it.type }.toSet()
            val actualMissingTypes = missing.map { it.required.type }.toSet()

            assertEquals(
                expectedMissingTypes,
                actualMissingTypes,
                "When no documents exist, all required types should be missing. " +
                    "Expected: $expectedMissingTypes, Got: $actualMissingTypes"
            )
        }
    }

    @Test
    @DisplayName("each missing document has a non-empty suggestion")
    fun `each missing document includes a non-empty actionable suggestion`() = runTest {
        checkAll(100, arbChecklist, arbExistingDocuments) { checklist, existingDocs ->
            val missing = generator.detectMissing(checklist, existingDocs)

            for (missingDoc in missing) {
                assertTrue(
                    missingDoc.suggestion.isNotBlank(),
                    "Missing document of type ${missingDoc.required.type} should have a non-empty suggestion"
                )
            }
        }
    }

    @Test
    @DisplayName("missing count matches number of required types with insufficient documents")
    fun `missing count equals number of under-supplied required types`() = runTest {
        checkAll(100, arbChecklist, arbExistingDocuments) { checklist, existingDocs ->
            val missing = generator.detectMissing(checklist, existingDocs)

            val expectedCount = checklist.requiredDocuments.count { required ->
                val matchingCount = existingDocs.count { it.type == required.type }
                matchingCount < required.countNeeded
            }

            assertEquals(
                expectedCount,
                missing.size,
                "Expected $expectedCount missing entries but got ${missing.size}. " +
                    "Checklist: ${checklist.requiredDocuments.map { "${it.type}:${it.countNeeded}" }}, " +
                    "Existing types: ${existingDocs.groupBy { it.type }.mapValues { it.value.size }}"
            )
        }
    }
}
