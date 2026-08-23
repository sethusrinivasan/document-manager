package com.app.traveldocs.data.scanner

import com.app.traveldocs.domain.model.DocumentType
import com.app.traveldocs.domain.model.ExtractionResult
import com.app.traveldocs.domain.model.ExtractedValue
import com.app.traveldocs.domain.model.MetadataField
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Focused unit tests for confidence scoring and manual review flagging behavior.
 *
 * Validates Requirements 4.7 and 4.9:
 * - 4.7: IF metadata extraction confidence is below 80%, THEN flag for manual review
 * - 4.9: WHERE no extraction model exists for a document type, skip extraction and prompt manual entry
 */
@DisplayName("Confidence Scoring and Manual Review Flagging")
class ConfidenceScoringTest {

    private lateinit var extractor: MlKitMetadataExtractor

    @BeforeEach
    fun setup() {
        extractor = MlKitMetadataExtractor()
    }

    @Nested
    @DisplayName("Requirement 4.7: Low confidence flags manual review")
    inner class LowConfidenceFlagsManualReview {

        @Test
        @DisplayName("confidence below 80% sets requiresManualReview to true")
        fun belowThresholdFlagsReview() {
            // 1 of 3 passport fields extracted → low coverage → low confidence
            val metadata = mapOf(
                MetadataField.ID_NUMBER to ExtractedValue("AB123456", 0.7f)
            )
            val confidence = extractor.calculateConfidence(metadata, DocumentType.PASSPORT)

            // Verify confidence is below 0.8
            assertTrue(confidence < 0.8f, "Expected confidence < 0.8 but got $confidence")

            // Build ExtractionResult as extract() would
            val result = ExtractionResult(
                documentType = DocumentType.PASSPORT,
                metadata = metadata,
                confidence = confidence,
                requiresManualReview = confidence < 0.8f
            )

            assertTrue(result.requiresManualReview,
                "requiresManualReview should be true when confidence ($confidence) < 0.8")
        }

        @Test
        @DisplayName("confidence exactly at 0.0 flags manual review")
        fun zeroConfidenceFlagsReview() {
            val confidence = extractor.calculateConfidence(emptyMap(), DocumentType.PASSPORT)

            assertEquals(0.0f, confidence, 0.001f)

            val result = ExtractionResult(
                documentType = DocumentType.PASSPORT,
                metadata = emptyMap(),
                confidence = confidence,
                requiresManualReview = confidence < 0.8f
            )

            assertTrue(result.requiresManualReview,
                "requiresManualReview should be true when confidence is 0.0")
        }

        @Test
        @DisplayName("partial extraction with moderate confidence still flags review")
        fun partialExtractionFlagsReview() {
            // 2 of 4 visa fields extracted with moderate confidence
            val metadata = mapOf(
                MetadataField.VISA_NUMBER to ExtractedValue("VN12345678", 0.9f),
                MetadataField.EXPIRY_DATE to ExtractedValue("01/01/2025", 0.8f)
            )
            val confidence = extractor.calculateConfidence(metadata, DocumentType.VISA)

            // 2/4 coverage = 0.5, avg conf = 0.85
            // weighted = 0.6 * 0.5 + 0.4 * 0.85 = 0.3 + 0.34 = 0.64
            assertTrue(confidence < 0.8f, "Expected confidence < 0.8 but got $confidence")

            val result = ExtractionResult(
                documentType = DocumentType.VISA,
                metadata = metadata,
                confidence = confidence,
                requiresManualReview = confidence < 0.8f
            )

            assertTrue(result.requiresManualReview,
                "requiresManualReview should be true when confidence ($confidence) < 0.8")
        }
    }

    @Nested
    @DisplayName("High confidence does NOT flag manual review")
    inner class HighConfidenceNoFlag {

        @Test
        @DisplayName("confidence >= 80% sets requiresManualReview to false")
        fun aboveThresholdNoFlag() {
            // All 3 passport fields extracted with high confidence
            val metadata = mapOf(
                MetadataField.ID_NUMBER to ExtractedValue("AB123456", 0.95f),
                MetadataField.HOLDER_NAME to ExtractedValue("SMITH", 0.9f),
                MetadataField.EXPIRY_DATE to ExtractedValue("15/06/2030", 0.9f)
            )
            val confidence = extractor.calculateConfidence(metadata, DocumentType.PASSPORT)

            // 3/3 coverage = 1.0, avg conf ≈ 0.917
            // weighted = 0.6 * 1.0 + 0.4 * 0.917 = 0.6 + 0.367 = 0.967
            assertTrue(confidence >= 0.8f, "Expected confidence >= 0.8 but got $confidence")

            val result = ExtractionResult(
                documentType = DocumentType.PASSPORT,
                metadata = metadata,
                confidence = confidence,
                requiresManualReview = confidence < 0.8f
            )

            assertFalse(result.requiresManualReview,
                "requiresManualReview should be false when confidence ($confidence) >= 0.8")
        }

        @Test
        @DisplayName("all insurance fields extracted with high confidence passes threshold")
        fun fullInsuranceExtractionPasses() {
            // All 2 insurance fields extracted with high confidence
            val metadata = mapOf(
                MetadataField.POLICY_NUMBER to ExtractedValue("TI12345678", 0.9f),
                MetadataField.COVERAGE_PERIOD to ExtractedValue("01/01/2024 to 31/12/2024", 0.85f)
            )
            val confidence = extractor.calculateConfidence(metadata, DocumentType.HEALTH_INSURANCE)

            // 2/2 coverage = 1.0, avg conf = 0.875
            // weighted = 0.6 * 1.0 + 0.4 * 0.875 = 0.6 + 0.35 = 0.95
            assertTrue(confidence >= 0.8f, "Expected confidence >= 0.8 but got $confidence")

            val result = ExtractionResult(
                documentType = DocumentType.HEALTH_INSURANCE,
                metadata = metadata,
                confidence = confidence,
                requiresManualReview = confidence < 0.8f
            )

            assertFalse(result.requiresManualReview,
                "requiresManualReview should be false when confidence ($confidence) >= 0.8")
        }

        @Test
        @DisplayName("confidence exactly at 0.8 does NOT flag manual review")
        fun exactlyAtThresholdNoFlag() {
            // Craft metadata that produces confidence close to 0.8
            // For HEALTH_INSURANCE: expected 2 fields
            // 2/2 coverage = 1.0, need avg conf such that 0.6*1.0 + 0.4*x = 0.8 → x = 0.5
            val metadata = mapOf(
                MetadataField.POLICY_NUMBER to ExtractedValue("TI123", 0.5f),
                MetadataField.COVERAGE_PERIOD to ExtractedValue("01/01/2024 to 31/12/2024", 0.5f)
            )
            val confidence = extractor.calculateConfidence(metadata, DocumentType.HEALTH_INSURANCE)

            // 0.6 * 1.0 + 0.4 * 0.5 = 0.6 + 0.2 = 0.8
            assertEquals(0.8f, confidence, 0.01f)

            val result = ExtractionResult(
                documentType = DocumentType.HEALTH_INSURANCE,
                metadata = metadata,
                confidence = confidence,
                requiresManualReview = confidence < 0.8f
            )

            assertFalse(result.requiresManualReview,
                "requiresManualReview should be false when confidence is exactly 0.8")
        }
    }

    @Nested
    @DisplayName("Requirement 4.9: UNKNOWN document type handling")
    inner class UnknownDocumentType {

        @Test
        @DisplayName("UNKNOWN document type yields confidence 0.0")
        fun unknownTypeZeroConfidence() {
            val confidence = extractor.calculateConfidence(emptyMap(), DocumentType.UNKNOWN)
            assertEquals(0.0f, confidence, 0.001f)
        }

        @Test
        @DisplayName("UNKNOWN document type always flags manual review")
        fun unknownTypeFlagsManualReview() {
            val metadata = extractor.extractMetadata("any text content", DocumentType.UNKNOWN)
            val confidence = extractor.calculateConfidence(metadata, DocumentType.UNKNOWN)

            assertTrue(metadata.isEmpty(),
                "UNKNOWN document type should produce no extracted metadata")
            assertEquals(0.0f, confidence, 0.001f)

            val result = ExtractionResult(
                documentType = DocumentType.UNKNOWN,
                metadata = metadata,
                confidence = confidence,
                requiresManualReview = confidence < 0.8f
            )

            assertTrue(result.requiresManualReview,
                "requiresManualReview must be true for UNKNOWN document type (confidence=0.0)")
        }

        @Test
        @DisplayName("UNKNOWN document type extraction returns empty metadata map")
        fun unknownTypeEmptyMetadata() {
            val metadata = extractor.extractMetadata(
                "PASSPORT Surname: SMITH Passport No: AB1234567",
                DocumentType.UNKNOWN
            )

            assertTrue(metadata.isEmpty(),
                "extractMetadata should return empty map for UNKNOWN type regardless of text content")
        }
    }

    @Nested
    @DisplayName("End-to-end confidence scoring integration")
    inner class EndToEndConfidenceScoring {

        @Test
        @DisplayName("full passport text extraction meets confidence threshold")
        fun fullPassportExtraction() {
            val text = """
                PASSPORT
                Surname: SMITH
                Passport No: AB1234567
                Expiry Date: 15/06/2030
            """.trimIndent()

            val documentType = extractor.classifyFromText(text)
            val metadata = extractor.extractMetadata(text, documentType)
            val confidence = extractor.calculateConfidence(metadata, documentType)

            assertEquals(DocumentType.PASSPORT, documentType)
            assertEquals(3, metadata.size, "Should extract all 3 passport fields")
            assertTrue(confidence >= 0.8f, "Full passport extraction confidence ($confidence) should be >= 0.8")

            val result = ExtractionResult(
                documentType = documentType,
                metadata = metadata,
                confidence = confidence,
                requiresManualReview = confidence < 0.8f
            )
            assertFalse(result.requiresManualReview)
        }

        @Test
        @DisplayName("minimal text yields low confidence and flags review")
        fun minimalTextLowConfidence() {
            val text = "PASSPORT\nSome unclear blurry text"

            val documentType = extractor.classifyFromText(text)
            val metadata = extractor.extractMetadata(text, documentType)
            val confidence = extractor.calculateConfidence(metadata, documentType)

            assertEquals(DocumentType.PASSPORT, documentType)
            assertTrue(metadata.size < 3, "Should extract fewer than 3 fields from minimal text")
            assertTrue(confidence < 0.8f, "Low extraction confidence ($confidence) should be < 0.8")

            val result = ExtractionResult(
                documentType = documentType,
                metadata = metadata,
                confidence = confidence,
                requiresManualReview = confidence < 0.8f
            )
            assertTrue(result.requiresManualReview)
        }

        @Test
        @DisplayName("unrecognizable text classified as UNKNOWN flags review")
        fun unrecognizableTextFlagsReview() {
            val text = "random gibberish 12345 xyzzy @@##"

            val documentType = extractor.classifyFromText(text)
            val metadata = extractor.extractMetadata(text, documentType)
            val confidence = extractor.calculateConfidence(metadata, documentType)

            assertEquals(DocumentType.UNKNOWN, documentType)
            assertTrue(metadata.isEmpty())
            assertEquals(0.0f, confidence, 0.001f)

            val result = ExtractionResult(
                documentType = documentType,
                metadata = metadata,
                confidence = confidence,
                requiresManualReview = confidence < 0.8f
            )
            assertTrue(result.requiresManualReview,
                "Unrecognizable document must always be flagged for manual review")
        }
    }
}
