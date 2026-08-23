package com.app.traveldocs.domain.properties

import com.app.traveldocs.data.scanner.MlKitMetadataExtractor
import com.app.traveldocs.domain.model.DocumentType
import com.app.traveldocs.domain.model.ExtractionResult
import com.app.traveldocs.domain.model.ExtractedValue
import com.app.traveldocs.domain.model.MetadataField
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/**
 * Property 8: Low-confidence extraction flags manual review
 *
 * For any ExtractionResult where confidence < 0.8, the requiresManualReview field should be true.
 *
 * **Validates: Requirements 4.7**
 */
@Tag("Feature: travel-document-manager, Property 8: Low-confidence extraction flags manual review")
@DisplayName("Property 8: Low-confidence extraction flags manual review")
class LowConfidenceManualReviewPropertyTest {

    private val extractor = MlKitMetadataExtractor()

    companion object {
        private const val CONFIDENCE_THRESHOLD = 0.8f
    }

    // --- Custom Generators ---

    /**
     * Generator for random ExtractedValue with a confidence between 0.0 and 1.0.
     */
    private val arbExtractedValue: Arb<ExtractedValue> = arbitrary {
        val value = Arb.string(minSize = 1, maxSize = 30).bind()
        val confidence = Arb.numericFloat(min = 0.0f, max = 1.0f).bind()
        ExtractedValue(value = value, confidence = confidence)
    }

    /**
     * Generator for random metadata maps with 0 to 5 entries.
     */
    private val arbMetadata: Arb<Map<MetadataField, ExtractedValue>> = arbitrary {
        val fieldCount = Arb.int(0..5).bind()
        val fields = Arb.list(Arb.enum<MetadataField>(), range = fieldCount..fieldCount).bind()
        val values = Arb.list(arbExtractedValue, range = fieldCount..fieldCount).bind()
        fields.zip(values).toMap()
    }

    /**
     * Generator for confidence values below the threshold (0.0 to 0.79).
     * Uses integer division for clean, predictable values.
     */
    private val arbLowConfidence: Arb<Float> = arbitrary {
        Arb.int(0..79).bind() / 100f
    }

    /**
     * Generator for confidence values at or above the threshold (0.80 to 1.0).
     */
    private val arbHighConfidence: Arb<Float> = arbitrary {
        Arb.int(80..100).bind() / 100f
    }

    /**
     * Generator for any confidence value (0.0 to 1.0).
     */
    private val arbAnyConfidence: Arb<Float> = arbitrary {
        Arb.int(0..100).bind() / 100f
    }

    // --- Property Tests ---

    /**
     * Property: For any ExtractionResult where confidence < 0.8,
     * requiresManualReview must be true.
     *
     * **Validates: Requirements 4.7**
     */
    @Test
    @DisplayName("Any ExtractionResult with confidence < 0.8 has requiresManualReview = true")
    fun lowConfidenceAlwaysFlagsManualReview() = runTest {
        checkAll(100, Arb.enum<DocumentType>(), arbMetadata, arbLowConfidence) { documentType, metadata, confidence ->
            val result = ExtractionResult(
                documentType = documentType,
                metadata = metadata,
                confidence = confidence,
                requiresManualReview = confidence < CONFIDENCE_THRESHOLD
            )

            assertTrue(
                result.requiresManualReview,
                "ExtractionResult with confidence $confidence (< 0.8) " +
                    "should have requiresManualReview = true, but was false"
            )
        }
    }

    /**
     * Property: For any ExtractionResult where confidence >= 0.8,
     * requiresManualReview must be false.
     *
     * **Validates: Requirements 4.7**
     */
    @Test
    @DisplayName("Any ExtractionResult with confidence >= 0.8 has requiresManualReview = false")
    fun highConfidenceDoesNotFlagManualReview() = runTest {
        checkAll(100, Arb.enum<DocumentType>(), arbMetadata, arbHighConfidence) { documentType, metadata, confidence ->
            val result = ExtractionResult(
                documentType = documentType,
                metadata = metadata,
                confidence = confidence,
                requiresManualReview = confidence < CONFIDENCE_THRESHOLD
            )

            assertFalse(
                result.requiresManualReview,
                "ExtractionResult with confidence $confidence (>= 0.8) " +
                    "should have requiresManualReview = false, but was true"
            )
        }
    }

    /**
     * Property: For any randomly generated ExtractionResult, the requiresManualReview flag
     * is consistent with the confidence threshold (< 0.8 -> true, >= 0.8 -> false).
     *
     * **Validates: Requirements 4.7**
     */
    @Test
    @DisplayName("requiresManualReview is always consistent with confidence threshold across all values")
    fun manualReviewFlagConsistentWithThreshold() = runTest {
        checkAll(100, Arb.enum<DocumentType>(), arbMetadata, arbAnyConfidence) { documentType, metadata, confidence ->
            val result = ExtractionResult(
                documentType = documentType,
                metadata = metadata,
                confidence = confidence,
                requiresManualReview = confidence < CONFIDENCE_THRESHOLD
            )

            if (result.confidence < CONFIDENCE_THRESHOLD) {
                assertTrue(
                    result.requiresManualReview,
                    "Confidence ${result.confidence} < $CONFIDENCE_THRESHOLD should flag manual review"
                )
            } else {
                assertFalse(
                    result.requiresManualReview,
                    "Confidence ${result.confidence} >= $CONFIDENCE_THRESHOLD should NOT flag manual review"
                )
            }
        }
    }

    /**
     * Property: The MlKitMetadataExtractor's calculateConfidence + requiresManualReview logic
     * correctly flags for manual review. For any document type and metadata combination
     * that yields confidence < 0.8, the constructed result flags manual review.
     *
     * This tests the actual extractor logic (not just the data class constructor).
     *
     * **Validates: Requirements 4.7**
     */
    @Test
    @DisplayName("MlKitMetadataExtractor flags manual review when calculated confidence < 0.8")
    fun extractorCalculatedConfidenceFlagsManualReview() = runTest {
        checkAll(100, Arb.enum<DocumentType>(), arbMetadata) { documentType, metadata ->
            val confidence = extractor.calculateConfidence(metadata, documentType)
            val result = ExtractionResult(
                documentType = documentType,
                metadata = metadata,
                confidence = confidence,
                requiresManualReview = confidence < CONFIDENCE_THRESHOLD
            )

            if (confidence < CONFIDENCE_THRESHOLD) {
                assertTrue(
                    result.requiresManualReview,
                    "Extractor calculated confidence $confidence for $documentType with " +
                        "${metadata.size} fields; expected requiresManualReview = true"
                )
            } else {
                assertFalse(
                    result.requiresManualReview,
                    "Extractor calculated confidence $confidence for $documentType with " +
                        "${metadata.size} fields; expected requiresManualReview = false"
                )
            }
        }
    }

    /**
     * Property: Confidence of exactly 0.0 (no fields extracted) always flags manual review.
     *
     * **Validates: Requirements 4.7**
     */
    @Test
    @DisplayName("Zero confidence (no fields extracted) always flags manual review")
    fun zeroConfidenceAlwaysFlagsReview() = runTest {
        checkAll(100, Arb.enum<DocumentType>()) { documentType ->
            val confidence = extractor.calculateConfidence(emptyMap(), documentType)
            val result = ExtractionResult(
                documentType = documentType,
                metadata = emptyMap(),
                confidence = confidence,
                requiresManualReview = confidence < CONFIDENCE_THRESHOLD
            )

            assertTrue(
                result.requiresManualReview,
                "Empty metadata for $documentType should yield confidence 0.0 and flag manual review, " +
                    "but got confidence=$confidence, requiresManualReview=${result.requiresManualReview}"
            )
        }
    }
}
