package com.app.traveldocs.domain.properties

import com.app.traveldocs.data.tags.AutoTagGeneratorImpl
import com.app.traveldocs.domain.model.DocumentType
import com.app.traveldocs.domain.model.ExtractedValue
import com.app.traveldocs.domain.model.MetadataField
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.float
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/**
 * Property 9: Document type to tag mapping
 *
 * For any recognized DocumentType in {PASSPORT, VISA, TICKET, HOTEL_BOOKING, HEALTH_INSURANCE},
 * AutoTagGeneratorImpl.generateTags() produces the expected type tag:
 * PASSPORT→"passport", VISA→"visa", TICKET→"ticket", HOTEL_BOOKING→"accommodation",
 * HEALTH_INSURANCE→"health".
 *
 * Uses the real AutoTagGeneratorImpl class (no mocks).
 *
 * **Validates: Requirements 5.2, 5.3, 5.4, 5.5, 5.6**
 */
@DisplayName("Property 9: Document type to tag mapping")
@Tag("Feature: travel-document-manager, Property 9: Document type to tag mapping")
class DocumentTypeToTagMappingPropertyTest {

    private lateinit var autoTagGenerator: AutoTagGeneratorImpl

    @BeforeEach
    fun setUp() {
        autoTagGenerator = AutoTagGeneratorImpl()
    }

    /**
     * Generator for recognized document types only (excluding UNKNOWN).
     */
    private val arbRecognizedDocumentType: Arb<DocumentType> = Arb.element(
        DocumentType.PASSPORT,
        DocumentType.VISA,
        DocumentType.TICKET,
        DocumentType.HOTEL_BOOKING,
        DocumentType.HEALTH_INSURANCE
    )

    /**
     * Generator for arbitrary metadata maps with random fields and values.
     */
    private val arbMetadata: Arb<Map<MetadataField, ExtractedValue>> = arbitrary {
        val fields = Arb.list(Arb.enum<MetadataField>(), 0..5).bind()
        fields.associateWith {
            ExtractedValue(
                value = Arb.string(1..20).bind(),
                confidence = Arb.float(0.0f..1.0f).bind()
            )
        }
    }

    /**
     * Maps DocumentType to its expected tag string per requirements 5.2-5.6.
     */
    private fun expectedTagForType(documentType: DocumentType): String = when (documentType) {
        DocumentType.PASSPORT -> "passport"
        DocumentType.VISA -> "visa"
        DocumentType.TICKET -> "ticket"
        DocumentType.HOTEL_BOOKING -> "accommodation"
        DocumentType.HEALTH_INSURANCE -> "health"
        DocumentType.UNKNOWN -> throw IllegalArgumentException("UNKNOWN has no expected tag")
    }

    @Test
    @DisplayName("For any recognized DocumentType, generateTags() includes the corresponding type tag")
    fun `generateTags produces expected type tag for all recognized document types`() = runTest {
        checkAll(100, arbRecognizedDocumentType, arbMetadata) { docType, metadata ->
            val tags = autoTagGenerator.generateTags(docType, metadata)

            val expectedTag = expectedTagForType(docType)
            assertTrue(
                tags.contains(expectedTag),
                "For DocumentType.$docType, expected tag '$expectedTag' to be present in generated tags: $tags"
            )
        }
    }

    @Test
    @DisplayName("For any recognized DocumentType with empty metadata, generateTags() includes the type tag")
    fun `generateTags produces type tag even with empty metadata`() = runTest {
        checkAll(100, arbRecognizedDocumentType) { docType ->
            val tags = autoTagGenerator.generateTags(docType, emptyMap())

            val expectedTag = expectedTagForType(docType)
            assertTrue(
                tags.contains(expectedTag),
                "For DocumentType.$docType with empty metadata, expected tag '$expectedTag' in: $tags"
            )
        }
    }

    @Test
    @DisplayName("The type tag is always the first tag in the generated list")
    fun `type tag appears first in the generated tag list`() = runTest {
        checkAll(100, arbRecognizedDocumentType, arbMetadata) { docType, metadata ->
            val tags = autoTagGenerator.generateTags(docType, metadata)

            val expectedTag = expectedTagForType(docType)
            assertTrue(
                tags.isNotEmpty() && tags.first() == expectedTag,
                "For DocumentType.$docType, expected '$expectedTag' as first tag, but got: $tags"
            )
        }
    }
}
