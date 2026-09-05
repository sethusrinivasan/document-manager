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
import kotlin.test.assertTrue

/**
 * Property 23: Missing document detection is set difference
 *
 * For any checklist with N required documents of type T, if fewer than N documents
 * of type T exist, T appears in the missing list. Uses the real BasicDocumentChecklistGenerator.
 *
 * **Validates: Requirements 9.1, 9.6**
 */
@DisplayName("Property Test")
// Removed JUnitTag - invalid syntax
class MissingDocDetectionPropertyTest {

    private lateinit var generator: BasicDocumentChecklistGenerator

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

    /**
     * Generator for a random checklist with 1..5 required document types,
     * each requiring 1..5 documents.
     */
    private val arbChecklist: Arb<TravelDocumentChecklist> = arbitrary {
        val count = Arb.int(1..5).bind()
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

    /**
     * Generator for a random set of existing documents with types drawn
     * from all DocumentType values (including UNKNOWN).
     */
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
    @DisplayName("For any checklist with N required of type T, if fewer than N of type T exist, T appears in missing list")
    fun `under-supplied document types appear in missing list`() = runTest {
        checkAll(100, arbChecklist, arbExistingDocuments) { checklist, existingDocs ->
            val missing = generator.detectMissing(checklist, existingDocs)
            val missingTypes = missing.map { it.required.type }.toSet()

            for (required in checklist.requiredDocuments) {
                val existingCount = existingDocs.count { it.type == required.type }
                if (existingCount < required.countNeeded) {
                    assertTrue(
                        missingTypes.contains(required.type),
                        "Type ${required.type} requires ${required.countNeeded} but only $existingCount exist. " +
                            "It should appear in the missing list but was not found."
                    )
                }
            }
        }
    }

    @Test
    @DisplayName("Sufficiently supplied document types never appear in the missing list")
    fun `sufficiently supplied document types are not in missing list`() = runTest {
        checkAll(100, arbChecklist, arbExistingDocuments) { checklist, existingDocs ->
            val missing = generator.detectMissing(checklist, existingDocs)
            val missingTypes = missing.map { it.required.type }.toSet()

            for (required in checklist.requiredDocuments) {
                val existingCount = existingDocs.count { it.type == required.type }
                if (existingCount >= required.countNeeded) {
                    assertTrue(
                        !missingTypes.contains(required.type),
                        "Type ${required.type} requires ${required.countNeeded} and $existingCount exist. " +
                            "It should NOT appear in the missing list but was found."
                    )
                }
            }
        }
    }

    @Test
    @DisplayName("Missing list is exactly the set of under-supplied types from the checklist")
    fun `missing list equals set difference of required minus satisfied`() = runTest {
        checkAll(100, arbChecklist, arbExistingDocuments) { checklist, existingDocs ->
            val missing = generator.detectMissing(checklist, existingDocs)

            val expectedMissingTypes = checklist.requiredDocuments
                .filter { required -> existingDocs.count { it.type == required.type } < required.countNeeded }
                .map { it.type }
                .toSet()

            val actualMissingTypes = missing.map { it.required.type }.toSet()

            assertTrue(
                expectedMissingTypes == actualMissingTypes,
                "Expected missing types: $expectedMissingTypes, actual: $actualMissingTypes. " +
                    "Checklist: ${checklist.requiredDocuments.map { "${it.type}:${it.countNeeded}" }}, " +
                    "Existing: ${existingDocs.groupingBy { it.type }.eachCount()}"
            )
        }
    }

    @Test
    @DisplayName("When no documents exist, all required types are missing")
    fun `empty document set means all checklist types are missing`() = runTest {
        checkAll(100, arbChecklist) { checklist ->
            val missing = generator.detectMissing(checklist, emptyList())
            val missingTypes = missing.map { it.required.type }.toSet()
            val requiredTypes = checklist.requiredDocuments.map { it.type }.toSet()

            assertTrue(
                missingTypes == requiredTypes,
                "With no existing documents, all required types should be missing. " +
                    "Expected: $requiredTypes, got: $missingTypes"
            )
        }
    }

    @Test
    @DisplayName("When all required documents are satisfied, missing list is empty")
    fun `fully satisfied checklist produces empty missing list`() = runTest {
        checkAll(100, arbChecklist) { checklist ->
            // Create enough documents of each required type
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
                "All requirements satisfied but missing list is not empty: " +
                    "${missing.map { "${it.required.type}:${it.required.countNeeded}" }}"
            )
        }
    }
}
