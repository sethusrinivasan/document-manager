package com.app.traveldocs.domain.properties

import com.app.traveldocs.data.tags.AutoTagGeneratorImpl
import com.app.traveldocs.domain.model.DocumentType
import com.app.traveldocs.domain.model.ExtractedValue
import com.app.traveldocs.domain.model.MetadataField
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.float
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/**
 * Property 11: Date-range tag generation
 *
 * For any metadata containing EXPIRY_DATE with a 4-digit year, AutoTagGeneratorImpl generates
 * an "expires-{year}" tag. Similarly for ISSUE_DATE → "issued-{year}".
 *
 * **Validates: Requirements 5.8**
 */
@DisplayName("Property Test")
class DateRangeTagPropertyTest {

    private lateinit var generator: AutoTagGeneratorImpl

    @BeforeEach
    fun setUp() {
        generator = AutoTagGeneratorImpl()
    }

    /**
     * Generator for 4-digit years in valid range.
     */
    private val arbYear: Arb<Int> = Arb.int(1900..2099)

    /**
     * Generator for confidence values.
     */
    private val arbConfidence: Arb<Float> = Arb.float(0.1f..1.0f)

    /**
     * Generator for random date strings in DD/MM/YYYY format.
     */
    private fun arbDateDMY(yearArb: Arb<Int>): Arb<String> = arbitrary {
        val day = Arb.int(1..28).bind()
        val month = Arb.int(1..12).bind()
        val year = yearArb.bind()
        "%02d/%02d/%04d".format(day, month, year)
    }

    /**
     * Generator for random date strings in YYYY-MM-DD format.
     */
    private fun arbDateYMD(yearArb: Arb<Int>): Arb<String> = arbitrary {
        val year = yearArb.bind()
        val month = Arb.int(1..12).bind()
        val day = Arb.int(1..28).bind()
        "%04d-%02d-%02d".format(year, month, day)
    }

    /**
     * Generator that produces random date strings in various formats, all containing a 4-digit year.
     */
    private val arbDateStringWithYear: Arb<Pair<String, Int>> = arbitrary {
        val year = arbYear.bind()
        val month = Arb.int(1..12).bind()
        val day = Arb.int(1..28).bind()
        val format = Arb.int(0..4).bind()
        val dateString = when (format) {
            0 -> "%02d/%02d/%04d".format(day, month, year)         // DD/MM/YYYY
            1 -> "%04d-%02d-%02d".format(year, month, day)         // YYYY-MM-DD
            2 -> "%02d-%02d-%04d".format(month, day, year)         // MM-DD-YYYY
            3 -> "%02d.%02d.%04d".format(day, month, year)         // DD.MM.YYYY
            else -> "%04d/%02d/%02d".format(year, month, day)      // YYYY/MM/DD
        }
        Pair(dateString, year)
    }

    @Test
    @DisplayName("EXPIRY_DATE with 4-digit year generates 'expires-{year}' tag")
    fun `expiry date with 4-digit year generates expires-year tag`() = runTest {
        checkAll(100, arbDateStringWithYear, Arb.enum<DocumentType>(), arbConfidence) { (dateString, year), docType, confidence ->
            val metadata = mapOf(
                MetadataField.EXPIRY_DATE to ExtractedValue(dateString, confidence)
            )

            val tags = generator.generateTags(docType, metadata)

            assertTrue(
                tags.contains("expires-$year"),
                "For EXPIRY_DATE='$dateString' (year=$year), expected tag 'expires-$year' but got: $tags"
            )
        }
    }

    @Test
    @DisplayName("ISSUE_DATE with 4-digit year generates 'issued-{year}' tag")
    fun `issue date with 4-digit year generates issued-year tag`() = runTest {
        checkAll(100, arbDateStringWithYear, Arb.enum<DocumentType>(), arbConfidence) { (dateString, year), docType, confidence ->
            val metadata = mapOf(
                MetadataField.ISSUE_DATE to ExtractedValue(dateString, confidence)
            )

            val tags = generator.generateTags(docType, metadata)

            assertTrue(
                tags.contains("issued-$year"),
                "For ISSUE_DATE='$dateString' (year=$year), expected tag 'issued-$year' but got: $tags"
            )
        }
    }

    @Test
    @DisplayName("Both EXPIRY_DATE and ISSUE_DATE generate both date-range tags")
    fun `both date fields generate both date-range tags`() = runTest {
        checkAll(100, arbDateStringWithYear, arbDateStringWithYear, Arb.enum<DocumentType>()) { (expiryDate, expiryYear), (issueDate, issueYear), docType ->
            val metadata = mapOf(
                MetadataField.EXPIRY_DATE to ExtractedValue(expiryDate, 0.9f),
                MetadataField.ISSUE_DATE to ExtractedValue(issueDate, 0.9f)
            )

            val tags = generator.generateTags(docType, metadata)

            assertTrue(
                tags.contains("expires-$expiryYear"),
                "For EXPIRY_DATE='$expiryDate', expected tag 'expires-$expiryYear' but got: $tags"
            )
            assertTrue(
                tags.contains("issued-$issueYear"),
                "For ISSUE_DATE='$issueDate', expected tag 'issued-$issueYear' but got: $tags"
            )
        }
    }

    @Test
    @DisplayName("Date metadata always produces at least one date-range tag")
    fun `any metadata with date fields produces at least one date-range tag`() = runTest {
        checkAll(100, arbDateStringWithYear, Arb.enum<DocumentType>(), arbConfidence) { (dateString, year), docType, confidence ->
            // Alternate between EXPIRY_DATE and ISSUE_DATE based on the year
            val field = if (year % 2 == 0) MetadataField.EXPIRY_DATE else MetadataField.ISSUE_DATE
            val metadata = mapOf(
                field to ExtractedValue(dateString, confidence)
            )

            val tags = generator.generateTags(docType, metadata)

            val hasDateTag = tags.any { it.startsWith("expires-") || it.startsWith("issued-") }
            assertTrue(
                hasDateTag,
                "For $field='$dateString', expected at least one date-range tag but got: $tags"
            )
        }
    }

    @Test
    @DisplayName("DD/MM/YYYY format dates correctly extract year for tag generation")
    fun `DD-MM-YYYY format dates correctly extract year`() = runTest {
        checkAll(100, arbYear, arbConfidence) { year, confidence ->
            val day = (year % 28) + 1
            val month = (year % 12) + 1
            val dateString = "%02d/%02d/%04d".format(day, month, year)

            val metadata = mapOf(
                MetadataField.EXPIRY_DATE to ExtractedValue(dateString, confidence)
            )

            val tags = generator.generateTags(DocumentType.UNKNOWN, metadata)

            assertTrue(
                tags.contains("expires-$year"),
                "For DD/MM/YYYY date '$dateString', expected 'expires-$year' but got: $tags"
            )
        }
    }

    @Test
    @DisplayName("YYYY-MM-DD format dates correctly extract year for tag generation")
    fun `YYYY-MM-DD format dates correctly extract year`() = runTest {
        checkAll(100, arbYear, arbConfidence) { year, confidence ->
            val month = (year % 12) + 1
            val day = (year % 28) + 1
            val dateString = "%04d-%02d-%02d".format(year, month, day)

            val metadata = mapOf(
                MetadataField.ISSUE_DATE to ExtractedValue(dateString, confidence)
            )

            val tags = generator.generateTags(DocumentType.UNKNOWN, metadata)

            assertTrue(
                tags.contains("issued-$year"),
                "For YYYY-MM-DD date '$dateString', expected 'issued-$year' but got: $tags"
            )
        }
    }
}
