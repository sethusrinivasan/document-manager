package com.app.traveldocs.domain.properties

import com.app.traveldocs.domain.model.Document
import com.app.traveldocs.domain.model.DocumentFormat
import com.app.traveldocs.domain.model.DocumentType
import com.app.traveldocs.domain.model.ExtractionResult
import com.app.traveldocs.domain.model.ImportedDocument
import com.app.traveldocs.domain.repository.AutoTagGenerator
import com.app.traveldocs.domain.repository.DocumentFileStorage
import com.app.traveldocs.domain.repository.DocumentRepository
import com.app.traveldocs.domain.repository.MetadataExtractor
import com.app.traveldocs.domain.usecase.DocumentImportUseCase
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.byteArray
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag as JUnitTag
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

/**
 * Property 7: Import triggers extraction
 *
 * For any successfully imported document, the system should produce an ExtractionResult
 * (which may have low confidence but must exist). This is verified by checking that
 * metadataExtractor.extract() is always called after file storage succeeds.
 *
 * **Validates: Requirements 3.7**
 */
@DisplayName("Property 7: Import triggers extraction")
@JUnitTag("Feature: travel-document-manager, Property 7: Import triggers extraction")
class ImportTriggersExtractionPropertyTest {

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
     * Generator for ImportedDocument with valid content across all supported formats.
     */
    private val arbImportedDocument: Arb<ImportedDocument> = arbitrary {
        val format = Arb.enum<DocumentFormat>().bind()
        val contentSize = Arb.int(10..512).bind()
        val rawBytes = Arb.byteArray(Arb.int(contentSize..contentSize), Arb.byte()).bind()
        val fileName = Arb.string(5..20).bind() + when (format) {
            DocumentFormat.PDF -> ".pdf"
            DocumentFormat.JPG -> ".jpg"
            DocumentFormat.PNG -> ".png"
        }
        ImportedDocument(rawBytes = rawBytes, format = format, originalFileName = fileName)
    }

    /**
     * Generator for member IDs.
     */
    private val arbMemberId: Arb<String> = Arb.string(8..16)

    /**
     * Generator for file IDs returned by file storage.
     */
    private val arbFileId: Arb<String> = Arb.string(16..32)

    /**
     * Generator for ExtractionResult with varying confidence levels.
     */
    private val arbExtractionResult: Arb<ExtractionResult> = arbitrary {
        val docType = Arb.enum<DocumentType>().bind()
        val confidence = Arb.int(1..100).bind() / 100f
        ExtractionResult(
            documentType = docType,
            metadata = emptyMap(),
            confidence = confidence,
            requiresManualReview = confidence < 0.8f
        )
    }

    @Test
    @DisplayName("For any successfully imported document, metadataExtractor.extract() is called")
    fun `extraction is always triggered after successful file storage`() = runTest {
        checkAll(100, arbImportedDocument, arbMemberId, arbFileId, arbExtractionResult) { importedDoc, memberId, fileId, extractionResult ->
            // Arrange: file storage succeeds
            coEvery { fileStorage.store(memberId, importedDoc.rawBytes, importedDoc.format) } returns Result.success(fileId)

            // Arrange: extraction succeeds with the generated result
            coEvery { metadataExtractor.extract(importedDoc.rawBytes) } returns Result.success(extractionResult)

            // Arrange: tag generation returns tags based on type
            every { autoTagGenerator.generateTags(any(), any()) } returns listOf("auto-tag")

            // Arrange: repository insert succeeds
            coEvery { documentRepository.insert(any()) } returns Result.success("doc-id")

            // Act
            val result = useCase.importAndProcess(importedDoc, memberId)

            // Assert: import succeeded
            assertTrue(
                result.isSuccess,
                "Import should succeed when file storage and extraction both succeed"
            )

            // Assert: metadataExtractor.extract() was called with the document's raw bytes
            coVerify(exactly = 1) { metadataExtractor.extract(importedDoc.rawBytes) }
        }
    }

    @Test
    @DisplayName("Extraction is triggered even when it returns low confidence results")
    fun `extraction is triggered regardless of confidence level`() = runTest {
        checkAll(100, arbImportedDocument, arbMemberId, arbFileId) { importedDoc, memberId, fileId ->
            // Arrange: file storage succeeds
            coEvery { fileStorage.store(memberId, importedDoc.rawBytes, importedDoc.format) } returns Result.success(fileId)

            // Arrange: extraction returns low-confidence result
            val lowConfidenceResult = ExtractionResult(
                documentType = DocumentType.UNKNOWN,
                metadata = emptyMap(),
                confidence = 0.1f,
                requiresManualReview = true
            )
            coEvery { metadataExtractor.extract(importedDoc.rawBytes) } returns Result.success(lowConfidenceResult)

            // Arrange: tag generation returns empty (low confidence, unknown type)
            every { autoTagGenerator.generateTags(any(), any()) } returns emptyList()

            // Arrange: repository insert succeeds
            coEvery { documentRepository.insert(any()) } returns Result.success("doc-id")

            // Act
            val result = useCase.importAndProcess(importedDoc, memberId)

            // Assert: import succeeded
            assertTrue(result.isSuccess, "Import should succeed even with low confidence extraction")

            // Assert: extraction was still invoked
            coVerify(exactly = 1) { metadataExtractor.extract(importedDoc.rawBytes) }
        }
    }

    @Test
    @DisplayName("Extraction is triggered even when it fails - graceful degradation")
    fun `extraction is attempted even when it fails gracefully`() = runTest {
        checkAll(100, arbImportedDocument, arbMemberId, arbFileId) { importedDoc, memberId, fileId ->
            // Arrange: file storage succeeds
            coEvery { fileStorage.store(memberId, importedDoc.rawBytes, importedDoc.format) } returns Result.success(fileId)

            // Arrange: extraction fails
            coEvery { metadataExtractor.extract(importedDoc.rawBytes) } returns Result.failure(RuntimeException("OCR failed"))

            // Arrange: repository insert succeeds (document stored without metadata)
            coEvery { documentRepository.insert(any()) } returns Result.success("doc-id")

            // Act
            val result = useCase.importAndProcess(importedDoc, memberId)

            // Assert: import still succeeds (graceful degradation)
            assertTrue(
                result.isSuccess,
                "Import should succeed even when extraction fails (graceful degradation)"
            )

            // Assert: extraction WAS attempted
            coVerify(exactly = 1) { metadataExtractor.extract(importedDoc.rawBytes) }
        }
    }

    @Test
    @DisplayName("Extraction is NOT called when file storage fails")
    fun `extraction is not triggered when file storage fails`() = runTest {
        checkAll(100, arbImportedDocument, arbMemberId) { importedDoc, memberId ->
            // Arrange: file storage fails
            coEvery { fileStorage.store(memberId, importedDoc.rawBytes, importedDoc.format) } returns Result.failure(RuntimeException("Disk full"))

            // Act
            val result = useCase.importAndProcess(importedDoc, memberId)

            // Assert: import failed
            assertTrue(result.isFailure, "Import should fail when file storage fails")

            // Assert: extraction was NEVER called (it only runs after successful storage)
            coVerify(exactly = 0) { metadataExtractor.extract(any()) }
        }
    }

    @Test
    @DisplayName("Extraction result is reflected in the stored document")
    fun `extraction result is used in the final stored document`() = runTest {
        checkAll(100, arbImportedDocument, arbMemberId, arbFileId, arbExtractionResult) { importedDoc, memberId, fileId, extractionResult ->
            // Arrange: file storage succeeds
            coEvery { fileStorage.store(memberId, importedDoc.rawBytes, importedDoc.format) } returns Result.success(fileId)

            // Arrange: extraction succeeds
            coEvery { metadataExtractor.extract(importedDoc.rawBytes) } returns Result.success(extractionResult)

            // Arrange: tag generation
            every { autoTagGenerator.generateTags(any(), any()) } returns emptyList()

            // Capture the document inserted into the repository
            var insertedDocument: Document? = null
            coEvery { documentRepository.insert(any()) } answers {
                insertedDocument = firstArg()
                Result.success(firstArg<Document>().id)
            }

            // Act
            val result = useCase.importAndProcess(importedDoc, memberId)

            // Assert: import succeeded
            assertTrue(result.isSuccess, "Import should succeed")

            // Assert: the stored document reflects the extraction result
            val doc = insertedDocument!!
            assertTrue(
                doc.type == extractionResult.documentType,
                "Document type should come from extraction result: expected ${extractionResult.documentType}, got ${doc.type}"
            )
            assertTrue(
                doc.extractionConfidence == extractionResult.confidence,
                "Extraction confidence should be stored: expected ${extractionResult.confidence}, got ${doc.extractionConfidence}"
            )
            assertTrue(
                doc.requiresManualReview == extractionResult.requiresManualReview,
                "Manual review flag should come from extraction: expected ${extractionResult.requiresManualReview}, got ${doc.requiresManualReview}"
            )
        }
    }
}
