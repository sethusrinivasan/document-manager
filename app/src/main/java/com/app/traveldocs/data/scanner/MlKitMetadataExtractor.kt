package com.app.traveldocs.data.scanner

import com.app.traveldocs.debug.DebugLogger
import android.graphics.BitmapFactory
import com.app.traveldocs.domain.model.DocumentType
import com.app.traveldocs.domain.model.ExtractionResult
import com.app.traveldocs.domain.model.ExtractedValue
import com.app.traveldocs.domain.model.MetadataField
import com.app.traveldocs.domain.repository.MetadataExtractor
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * ML Kit Text Recognition v2 implementation of [MetadataExtractor].
 *
 * Performs OCR on document images to extract text, classifies the document type
 * based on keyword patterns, and extracts type-specific metadata fields using
 * regex patterns. Confidence is calculated as the ratio of successfully extracted
 * fields to expected fields for the document type.
 */
@Singleton
class MlKitMetadataExtractor @Inject constructor() : MetadataExtractor {

    private val textRecognizer: TextRecognizer =
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun extract(imageData: ByteArray): Result<ExtractionResult> {
        DebugLogger.d("OCR", "extract: imageData=${imageData.size} bytes")
        return try {
            val text = recognizeText(imageData)
            val documentType = classifyFromText(text)
            val metadata = extractMetadata(text, documentType)
            val confidence = calculateConfidence(metadata, documentType)

            Result.success(
                ExtractionResult(
                    documentType = documentType,
                    metadata = metadata,
                    confidence = confidence,
                    requiresManualReview = confidence < 0.8f
                )
            )
        } catch (e: Exception) {
            Result.failure(
                ExtractionException("Failed to extract metadata: ${e.message}", e)
            )
        }
    }

    override suspend fun classifyDocumentType(imageData: ByteArray): Result<DocumentType> {
        DebugLogger.d("OCR", "classifyDocumentType: imageData=${imageData.size} bytes")
        return try {
            val text = recognizeText(imageData)
            Result.success(classifyFromText(text))
        } catch (e: Exception) {
            Result.failure(
                ExtractionException("Failed to classify document type: ${e.message}", e)
            )
        }
    }

    /**
     * Runs ML Kit Text Recognition on the given image bytes and returns the full text.
     */
    internal suspend fun recognizeText(imageData: ByteArray): String {
        val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
            ?: throw ExtractionException("Unable to decode image data")

        val inputImage = InputImage.fromBitmap(bitmap, 0)

        return suspendCancellableCoroutine { continuation ->
            textRecognizer.process(inputImage)
                .addOnSuccessListener { result ->
                    continuation.resume(result.text)
                }
                .addOnFailureListener { exception ->
                    continuation.resume("")
                }

            continuation.invokeOnCancellation {
                // TextRecognizer doesn't support cancellation directly
            }
        }
    }

    /**
     * Classifies the document type based on keyword patterns in the extracted text.
     *
     * Priority order prevents ambiguity when multiple keywords match:
     * PASSPORT > VISA > TICKET > HOTEL_BOOKING > HEALTH_INSURANCE > UNKNOWN
     */
    internal fun classifyFromText(text: String): DocumentType {
        val upperText = text.uppercase()

        return when {
            PASSPORT_KEYWORDS.any { it in upperText } -> DocumentType.PASSPORT
            VISA_KEYWORDS.any { it in upperText } -> DocumentType.VISA
            TICKET_KEYWORDS.any { it in upperText } -> DocumentType.TICKET
            HOTEL_KEYWORDS.any { it in upperText } -> DocumentType.HOTEL_BOOKING
            INSURANCE_KEYWORDS.any { it in upperText } -> DocumentType.HEALTH_INSURANCE
            else -> DocumentType.UNKNOWN
        }
    }

    /**
     * Extracts type-specific metadata fields from the recognized text using regex patterns.
     */
    internal fun extractMetadata(
        text: String,
        documentType: DocumentType
    ): Map<MetadataField, ExtractedValue> {
        return when (documentType) {
            DocumentType.PASSPORT -> extractPassportMetadata(text)
            DocumentType.VISA -> extractVisaMetadata(text)
            DocumentType.TICKET -> extractTicketMetadata(text)
            DocumentType.HOTEL_BOOKING -> extractHotelMetadata(text)
            DocumentType.HEALTH_INSURANCE -> extractInsuranceMetadata(text)
            DocumentType.UNKNOWN -> emptyMap()
        }
    }

    private fun extractPassportMetadata(text: String): Map<MetadataField, ExtractedValue> {
        val metadata = mutableMapOf<MetadataField, ExtractedValue>()

        extractPassportNumber(text)?.let {
            metadata[MetadataField.ID_NUMBER] = it
        }
        extractName(text)?.let {
            metadata[MetadataField.HOLDER_NAME] = it
        }
        extractDate(text, EXPIRY_PATTERNS)?.let {
            metadata[MetadataField.EXPIRY_DATE] = it
        }

        return metadata
    }

    private fun extractVisaMetadata(text: String): Map<MetadataField, ExtractedValue> {
        val metadata = mutableMapOf<MetadataField, ExtractedValue>()

        extractVisaNumber(text)?.let {
            metadata[MetadataField.VISA_NUMBER] = it
        }
        extractDate(text, ISSUE_DATE_PATTERNS)?.let {
            metadata[MetadataField.ISSUE_DATE] = it
        }
        extractDate(text, EXPIRY_PATTERNS)?.let {
            metadata[MetadataField.EXPIRY_DATE] = it
        }
        extractDestination(text)?.let {
            metadata[MetadataField.DESTINATION] = it
        }

        return metadata
    }

    private fun extractTicketMetadata(text: String): Map<MetadataField, ExtractedValue> {
        val metadata = mutableMapOf<MetadataField, ExtractedValue>()

        extractBookingReference(text)?.let {
            metadata[MetadataField.BOOKING_REFERENCE] = it
        }
        extractFlightDetails(text)?.let {
            metadata[MetadataField.FLIGHT_DETAILS] = it
        }
        extractDestination(text)?.let {
            metadata[MetadataField.DESTINATION] = it
        }

        return metadata
    }

    private fun extractHotelMetadata(text: String): Map<MetadataField, ExtractedValue> {
        val metadata = mutableMapOf<MetadataField, ExtractedValue>()

        extractBookingReference(text)?.let {
            metadata[MetadataField.BOOKING_REFERENCE] = it
        }
        extractHotelName(text)?.let {
            metadata[MetadataField.HOTEL_NAME] = it
        }
        extractDestination(text)?.let {
            metadata[MetadataField.DESTINATION] = it
        }

        return metadata
    }

    private fun extractInsuranceMetadata(text: String): Map<MetadataField, ExtractedValue> {
        val metadata = mutableMapOf<MetadataField, ExtractedValue>()

        extractPolicyNumber(text)?.let {
            metadata[MetadataField.POLICY_NUMBER] = it
        }
        extractCoveragePeriod(text)?.let {
            metadata[MetadataField.COVERAGE_PERIOD] = it
        }

        return metadata
    }

    // --- Field extraction methods ---

    /**
     * Extracts passport number (typically 8-9 alphanumeric characters).
     */
    internal fun extractPassportNumber(text: String): ExtractedValue? {
        val patterns = listOf(
            Regex("""(?:passport\s*(?:no|number|#)[.:]*\s*)([A-Z0-9]{6,9})""", RegexOption.IGNORE_CASE),
            Regex("""(?:document\s*(?:no|number|#)[.:]*\s*)([A-Z0-9]{6,9})""", RegexOption.IGNORE_CASE),
            Regex("""\b([A-Z]{1,2}\d{6,8})\b""")
        )

        for ((index, pattern) in patterns.withIndex()) {
            val match = pattern.find(text)
            if (match != null) {
                val confidence = if (index == 0) 0.95f else if (index == 1) 0.85f else 0.7f
                return ExtractedValue(
                    value = match.groupValues[1].trim(),
                    confidence = confidence
                )
            }
        }
        return null
    }

    /**
     * Extracts visa number from text.
     */
    internal fun extractVisaNumber(text: String): ExtractedValue? {
        val patterns = listOf(
            Regex("""(?:visa\s*(?:no|number|#)[.:]*\s*)([A-Z0-9]{6,12})""", RegexOption.IGNORE_CASE),
            Regex("""(?:visa\s*id[.:]*\s*)([A-Z0-9]{6,12})""", RegexOption.IGNORE_CASE)
        )

        for ((index, pattern) in patterns.withIndex()) {
            val match = pattern.find(text)
            if (match != null) {
                val confidence = if (index == 0) 0.9f else 0.8f
                return ExtractedValue(
                    value = match.groupValues[1].trim(),
                    confidence = confidence
                )
            }
        }
        return null
    }

    /**
     * Extracts holder name from text using surname/given name patterns.
     */
    internal fun extractName(text: String): ExtractedValue? {
        val patterns = listOf(
            Regex("""(?:surname|last\s*name)[/:\s]*([A-Z][A-Za-z\s-]+?)(?:\n|$)""", RegexOption.IGNORE_CASE),
            Regex("""(?:name|given\s*name|first\s*name)[/:\s]*([A-Z][A-Za-z\s-]+?)(?:\n|$)""", RegexOption.IGNORE_CASE),
            Regex("""(?:holder|passenger)[/:\s]*([A-Z][A-Za-z\s-]+?)(?:\n|$)""", RegexOption.IGNORE_CASE)
        )

        for ((index, pattern) in patterns.withIndex()) {
            val match = pattern.find(text)
            if (match != null) {
                val confidence = if (index <= 1) 0.9f else 0.75f
                return ExtractedValue(
                    value = match.groupValues[1].trim(),
                    confidence = confidence
                )
            }
        }
        return null
    }

    /**
     * Extracts a date from text using provided patterns.
     * Supports DD/MM/YYYY, MM/DD/YYYY, YYYY-MM-DD formats.
     */
    internal fun extractDate(text: String, labelPatterns: List<Regex>): ExtractedValue? {
        // Try labeled patterns first (e.g., "Expiry Date: 01/01/2025")
        for ((index, pattern) in labelPatterns.withIndex()) {
            val match = pattern.find(text)
            if (match != null) {
                val confidence = if (index == 0) 0.9f else 0.8f
                return ExtractedValue(
                    value = match.groupValues[1].trim(),
                    confidence = confidence
                )
            }
        }

        return null
    }

    /**
     * Extracts booking reference (typically 6-character alphanumeric code).
     */
    internal fun extractBookingReference(text: String): ExtractedValue? {
        val patterns = listOf(
            Regex("""(?:booking\s*(?:ref|reference|code|#)|confirmation\s*(?:code|#)|PNR)[.:]*\s*([A-Z0-9]{5,8})""", RegexOption.IGNORE_CASE),
            Regex("""\b([A-Z]{6})\b"""),
            Regex("""\b([A-Z0-9]{6})\b""")
        )

        for ((index, pattern) in patterns.withIndex()) {
            val match = pattern.find(text)
            if (match != null) {
                val confidence = if (index == 0) 0.9f else if (index == 1) 0.6f else 0.5f
                return ExtractedValue(
                    value = match.groupValues[1].trim(),
                    confidence = confidence
                )
            }
        }
        return null
    }

    /**
     * Extracts flight details (e.g., "AA 1234", "BA123").
     */
    internal fun extractFlightDetails(text: String): ExtractedValue? {
        val patterns = listOf(
            Regex("""(?:flight\s*(?:no|number|#)?[.:]*\s*)([A-Z]{2}\s*\d{1,4})""", RegexOption.IGNORE_CASE),
            Regex("""\b([A-Z]{2}\s*\d{3,4})\b""")
        )

        for ((index, pattern) in patterns.withIndex()) {
            val match = pattern.find(text)
            if (match != null) {
                val confidence = if (index == 0) 0.9f else 0.7f
                return ExtractedValue(
                    value = match.groupValues[1].trim(),
                    confidence = confidence
                )
            }
        }
        return null
    }

    /**
     * Extracts hotel name from text.
     */
    internal fun extractHotelName(text: String): ExtractedValue? {
        val patterns = listOf(
            Regex("""(?:hotel|resort|inn|lodge|suites?)[:\s]*([A-Za-z\s&'-]{3,40})""", RegexOption.IGNORE_CASE),
            Regex("""([A-Z][A-Za-z\s&'-]+?)\s*(?:Hotel|Resort|Inn|Lodge|Suites?)""", RegexOption.IGNORE_CASE)
        )

        for ((index, pattern) in patterns.withIndex()) {
            val match = pattern.find(text)
            if (match != null) {
                val confidence = if (index == 0) 0.85f else 0.8f
                return ExtractedValue(
                    value = match.groupValues[1].trim(),
                    confidence = confidence
                )
            }
        }
        return null
    }

    /**
     * Extracts policy number from insurance documents.
     */
    internal fun extractPolicyNumber(text: String): ExtractedValue? {
        val patterns = listOf(
            Regex("""(?:policy\s*(?:no|number|#)|member\s*(?:id|number))[.:]*\s*([A-Z0-9]{4,15})""", RegexOption.IGNORE_CASE),
            Regex("""(?:insurance\s*(?:id|number))[.:]*\s*([A-Z0-9]{4,15})""", RegexOption.IGNORE_CASE)
        )

        for ((index, pattern) in patterns.withIndex()) {
            val match = pattern.find(text)
            if (match != null) {
                val confidence = if (index == 0) 0.9f else 0.8f
                return ExtractedValue(
                    value = match.groupValues[1].trim(),
                    confidence = confidence
                )
            }
        }
        return null
    }

    /**
     * Extracts coverage period from insurance documents.
     */
    internal fun extractCoveragePeriod(text: String): ExtractedValue? {
        val patterns = listOf(
            Regex("""(?:coverage|valid(?:ity)?|period)[:\s]*(\d{1,2}[/-]\d{1,2}[/-]\d{2,4}\s*(?:to|-)\s*\d{1,2}[/-]\d{1,2}[/-]\d{2,4})""", RegexOption.IGNORE_CASE),
            Regex("""(?:from|start)[:\s]*(\d{1,2}[/-]\d{1,2}[/-]\d{2,4})\s*(?:to|-|until)\s*(\d{1,2}[/-]\d{1,2}[/-]\d{2,4})""", RegexOption.IGNORE_CASE)
        )

        for ((index, pattern) in patterns.withIndex()) {
            val match = pattern.find(text)
            if (match != null) {
                val confidence = if (index == 0) 0.85f else 0.8f
                val value = if (match.groupValues.size > 2 && match.groupValues[2].isNotEmpty()) {
                    "${match.groupValues[1]} to ${match.groupValues[2]}"
                } else {
                    match.groupValues[1]
                }
                return ExtractedValue(
                    value = value.trim(),
                    confidence = confidence
                )
            }
        }
        return null
    }

    /**
     * Extracts destination from text.
     */
    internal fun extractDestination(text: String): ExtractedValue? {
        val patterns = listOf(
            Regex("""(?:destination|to|arriving|arrival)[:\s]+([A-Z][A-Za-z\s]{2,30})""", RegexOption.IGNORE_CASE),
            Regex("""(?:→|->)\s*([A-Z][A-Za-z\s]{2,30})""")
        )

        for ((index, pattern) in patterns.withIndex()) {
            val match = pattern.find(text)
            if (match != null) {
                val confidence = if (index == 0) 0.85f else 0.75f
                return ExtractedValue(
                    value = match.groupValues[1].trim(),
                    confidence = confidence
                )
            }
        }
        return null
    }

    /**
     * Calculates overall extraction confidence as the ratio of extracted fields
     * to expected fields for the document type.
     */
    internal fun calculateConfidence(
        metadata: Map<MetadataField, ExtractedValue>,
        documentType: DocumentType
    ): Float {
        val expectedCount = getExpectedFieldCount(documentType)
        if (expectedCount == 0) return 0.0f

        val extractedCount = metadata.size
        val fieldRatio = extractedCount.toFloat() / expectedCount.toFloat()

        // Also factor in individual field confidences
        val avgFieldConfidence = if (metadata.isNotEmpty()) {
            metadata.values.map { it.confidence }.average().toFloat()
        } else {
            0.0f
        }

        // Weighted: 60% field coverage, 40% individual confidence
        return (0.6f * fieldRatio + 0.4f * avgFieldConfidence).coerceIn(0.0f, 1.0f)
    }

    /**
     * Returns the number of expected metadata fields for each document type.
     */
    internal fun getExpectedFieldCount(documentType: DocumentType): Int {
        return when (documentType) {
            DocumentType.PASSPORT -> 3     // ID_NUMBER, HOLDER_NAME, EXPIRY_DATE
            DocumentType.VISA -> 4         // VISA_NUMBER, ISSUE_DATE, EXPIRY_DATE, DESTINATION
            DocumentType.TICKET -> 3       // BOOKING_REFERENCE, FLIGHT_DETAILS, DESTINATION
            DocumentType.HOTEL_BOOKING -> 3 // BOOKING_REFERENCE, HOTEL_NAME, DESTINATION
            DocumentType.HEALTH_INSURANCE -> 2 // POLICY_NUMBER, COVERAGE_PERIOD
            DocumentType.UNKNOWN -> 0
        }
    }

    companion object {
        // Document type classification keywords
        private val PASSPORT_KEYWORDS = listOf("PASSPORT", "PASSEPORT")
        private val VISA_KEYWORDS = listOf("VISA")
        private val TICKET_KEYWORDS = listOf("BOOKING", "FLIGHT", "TICKET", "BOARDING", "ITINERARY")
        private val HOTEL_KEYWORDS = listOf("HOTEL", "RESERVATION", "CHECK-IN", "CHECK-OUT", "ACCOMMODATION")
        private val INSURANCE_KEYWORDS = listOf("INSURANCE", "POLICY", "COVERAGE", "INSURER")

        // Date extraction patterns for expiry dates
        internal val EXPIRY_PATTERNS = listOf(
            Regex("""(?:expir(?:y|ation)\s*date|date\s*of\s*expir(?:y|ation)|valid\s*until|expires?)[:\s]*(\d{1,2}[/-]\d{1,2}[/-]\d{2,4})""", RegexOption.IGNORE_CASE),
            Regex("""(?:expir(?:y|ation)\s*date|date\s*of\s*expir(?:y|ation)|valid\s*until|expires?)[:\s]*(\d{4}-\d{2}-\d{2})""", RegexOption.IGNORE_CASE)
        )

        // Date extraction patterns for issue dates
        internal val ISSUE_DATE_PATTERNS = listOf(
            Regex("""(?:issue\s*date|date\s*of\s*issue|issued)[:\s]*(\d{1,2}[/-]\d{1,2}[/-]\d{2,4})""", RegexOption.IGNORE_CASE),
            Regex("""(?:issue\s*date|date\s*of\s*issue|issued)[:\s]*(\d{4}-\d{2}-\d{2})""", RegexOption.IGNORE_CASE)
        )
    }
}

/**
 * Exception type for metadata extraction errors.
 */
class ExtractionException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
