package com.app.traveldocs.domain.properties

import com.app.traveldocs.domain.model.Document
import com.app.traveldocs.domain.model.DocumentFormat
import com.app.traveldocs.domain.model.DocumentType
import com.app.traveldocs.domain.model.ExtractionResult
import com.app.traveldocs.domain.model.ExtractedValue
import com.app.traveldocs.domain.model.ImportedDocument
import com.app.traveldocs.domain.model.MetadataField
import com.app.traveldocs.domain.repository.AutoTagGenerator
import com.app.traveldocs.domain.repository.DocumentFileStorage
import com.app.traveldocs.domain.repository.DocumentRepository
import com.app.traveldocs.domain.repository.MetadataExtractor
import com.app.traveldocs.domain.usecase.DocumentImportUseCase
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.byteArray
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag as JUnitTag
import org.junit.jupiter.api.Test

/**
 * Property 9: Auto-tagging on import
 *
 * For any imported document with a recognized DocumentType in {PASSPORT, VISA, TICKET,
 * HOTEL_BOOKING, HEALTH_INSURANCE}, at least one auto-tag is generated and included
 * in the stored document. The auto-tag must correspond to the document type mapping.
 *
 * **Validates: Requirements 5.1, 5.2, 5.3, 5.4, 5.5, 5.6**
 */
@DisplayName("Property 9: Auto-tagging on import")
@JUnitTag("Feature: travel-document-manager, Property 9: Auto-tagging on import")
class AutoTagGenerationPropertyTest {

    private lateinit var fileStorage: DocumentFileStorage
    private lateinit var metadataExtractor: MetadataExtractor
    private lateinit var documentRepository: DocumentRepository
    private lateinit var autoTagGenerator: AutoTagGenerator
    private lateinit var useCase: DocumentImportUseCase

    @BeforeEach
    fun setUp() {
        fileStorage = mockk()
        metadataExtractor = mockk()
        documentRepository = mockk()
        autoTagGenerator = mockk()
        useCase = DocumentImportUseCase(fileStorage, metadataExtractor, documentRepository, autoTagGenerator)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    /**
     * Only recognized (non-UNKNOWN) document types.
     */
    private val arbRecognizedDocumentType: Arb<DocumentType> = Arb.element(
        DocumentType.PASSPORT,
        DocumentType.VISA,
        DocumentType.TICKET,
        DocumentType.HOTEL_BOOKING,
        DocumentType.HEALTH_INSURANCE
    )

    /**
     * Generator for ImportedDocument with valid content across all supported formats.
     */
    private val arbImportedDocument: Arb<ImportedDocument> = arbitrary {
        val format = Arb.enum<DocumentFormat>().bind()
        val contentSize = Arb.int(10..256).bind()
        val rawBytes = Arb.byteArray(Arb.int(contentSize..contentSize), Arb.byte()).bind()
        val fileName = Arb.string(5..15).bind() + when (format) {
            DocumentFormat.PDF -> ".pdf"
            DocumentFormat.JPG -> ".jpg"
            DocumentFormat.PNG -> ".png"
        }
        ImportedDocument(rawBytes = rawBytes, format = format, originalFileName = fileName)
    }

    private val arbMemberId: Arb<String> = Arb.string(8..16)

    /**
     * Maps a DocumentType to its expected tag string per requirements 5.2-5.6.
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
    @DisplayName("For any imported document with a recognized type, at least one auto-tag is generated in the stored document")
    fun `auto-tags are generated and included in stored document for recognized types`() = runTest {
        checkAll(100, arbImportedDocument, arbMemberId, arbRecognizedDocumentType) { importedDoc, memberId, docType ->
            // Arrange: file storage succeeds
            coEvery { fileStorage.store(memberId, importedDoc.rawBytes, importedDoc.format) } returns Result.success("file-id")

            // Arrange: extraction returns a result with the given recognized document type
            val extractionResult = ExtractionResult(
                documentType = docType,
                metadata = emptyMap(),
                confidence = 0.9f,
                requiresManualReview = false
            )
            coEvery { metadataExtractor.extract(importedDoc.rawBytes) } returns Result.success(extractionResult)

            // Arrange: use a real-behavior mock that returns the expected type tag
            val expectedTag = expectedTagForType(docType)
            every { autoTagGenerator.generateTags(docType, emptyMap()) } returns listOf(expectedTag)

            // Capture the document stored in the repository
            val documentSlot = slot<Document>()
            coEvery { documentRepository.insert(capture(documentSlot)) } returns Result.success("doc-id")

            // Act
            val result = useCase.importAndProcess(importedDoc, memberId)

            // Assert: import succeeded
            assertTrue(result.isSuccess, "Import should succeed for recognized document type $docType")

            // Assert: the stored document has at least one auto-generated tag
            val storedDoc = documentSlot.captured
            assertTrue(
                storedDoc.tags.isNotEmpty(),
                "Document with type $docType should have at least one auto-generated tag, but tags were empty"
            )

            // Assert: the stored document tags include the expected type-based tag
            val tagNames = storedDoc.tags.map { it.name }
            assertTrue(
                tagNames.contains(expectedTag),
                "Document with type $docType should have tag '$expectedTag', but tags were: $tagNames"
            )

            // Assert: the tags are marked as auto-generated
            val autoTag = storedDoc.tags.first { it.name == expectedTag }
            assertTrue(
                autoTag.isAutoGenerated,
                "Tag '$expectedTag' should be marked as auto-generated"
            )
        }
    }

    @Test
    @DisplayName("Tag generation is invoked with extraction result's document type and metadata")
    fun `tag generation receives correct document type and metadata from extraction`() = runTest {
        checkAll(100, arbImportedDocument, arbMemberId, arbRecognizedDocumentType) { importedDoc, memberId, docType ->
            // Arrange: file storage succeeds
            coEvery { fileStorage.store(memberId, importedDoc.rawBytes, importedDoc.format) } returns Result.success("file-id")

            // Arrange: extraction returns result with metadata
            val metadata = mapOf(
                MetadataField.DESTINATION to ExtractedValue("Tokyo", 0.9f)
            )
            val extractionResult = ExtractionResult(
                documentType = docType,
                metadata = metadata,
                confidence = 0.85f,
                requiresManualReview = false
            )
            coEvery { metadataExtractor.extract(importedDoc.rawBytes) } returns Result.success(extractionResult)

            // Arrange: tag generator returns type tag + destination tag
            val expectedTypeTag = expectedTagForType(docType)
            every { autoTagGenerator.generateTags(docType, metadata) } returns listOf(expectedTypeTag, "tokyo")

            // Capture stored document
            val documentSlot = slot<Document>()
            coEvery { documentRepository.insert(capture(documentSlot)) } returns Result.success("doc-id")

            // Act
            val result = useCase.importAndProcess(importedDoc, memberId)

            // Assert: import succeeded
            assertTrue(result.isSuccess, "Import should succeed")

            // Assert: both tags are present in stored document
            val tagNames = documentSlot.captured.tags.map { it.name }
            assertTrue(
                tagNames.contains(expectedTypeTag),
                "Should contain type tag '$expectedTypeTag', got: $tagNames"
            )
            assertTrue(
                tagNames.contains("tokyo"),
                "Should contain destination tag 'tokyo', got: $tagNames"
            )
        }
    }

    @Test
    @DisplayName("Tag generation failure does not block document storage")
    fun `document is stored successfully even when tag generation throws`() = runTest {
        checkAll(100, arbImportedDocument, arbMemberId, arbRecognizedDocumentType) { importedDoc, memberId, docType ->
            // Arrange: file storage succeeds
            coEvery { fileStorage.store(memberId, importedDoc.rawBytes, importedDoc.format) } returns Result.success("file-id")

            // Arrange: extraction succeeds
            val extractionResult = ExtractionResult(
                documentType = docType,
                metadata = emptyMap(),
                confidence = 0.9f,
                requiresManualReview = false
            )
            coEvery { metadataExtractor.extract(importedDoc.rawBytes) } returns Result.success(extractionResult)

            // Arrange: tag generation THROWS an exception
            every { autoTagGenerator.generateTags(any(), any()) } throws RuntimeException("Tag generation failed")

            // Capture stored document
            val documentSlot = slot<Document>()
            coEvery { documentRepository.insert(capture(documentSlot)) } returns Result.success("doc-id")

            // Act
            val result = useCase.importAndProcess(importedDoc, memberId)

            // Assert: import still succeeds (graceful degradation - Requirement 5.9)
            assertTrue(
                result.isSuccess,
                "Document import should succeed even when tag generation fails for type $docType"
            )

            // Assert: document is stored with empty tags (graceful degradation)
            val storedDoc = documentSlot.captured
            assertTrue(
                storedDoc.tags.isEmpty(),
                "Tags should be empty when tag generation fails, but got: ${storedDoc.tags}"
            )

            // Assert: document type is still correctly set from extraction
            assertTrue(
                storedDoc.type == docType,
                "Document type should still be $docType even when tagging fails"
            )
        }
    }
}
