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
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag as JUnitTag
import org.junit.jupiter.api.Test

/**
 * Property 12: Silent tag generation failure
 *
 * Tag generation must never throw an exception — it always returns a List or empty list.
 * For any adversarial/corrupt metadata values, generateTags() never throws.
 *
 * **Validates: Requirements 5.9**
 */
@DisplayName("Property 12: Silent tag generation failure")
@JUnitTag("Feature: travel-document-manager, Property 12: Silent tag generation failure")
class SilentTagGenerationFailurePropertyTest {

    private lateinit var generator: AutoTagGeneratorImpl

    @BeforeEach
    fun setUp() {
        generator = AutoTagGeneratorImpl()
    }

    /**
     * Generator for adversarial/garbage ExtractedValue instances with random strings,
     * special characters, extreme floats, and edge-case values.
     */
    private val arbAdversarialExtractedValue: Arb<ExtractedValue> = arbitrary {
        val value = Arb.element(
            // Garbage strings
            Arb.string(0..500).bind(),
            // Null-like strings
            Arb.element("null", "NULL", "undefined", "NaN", "").bind(),
            // Special characters
            Arb.element(
                "\u0000", "\t\n\r", "%%%", "{{{}}}",
                "<script>alert('xss')</script>",
                "'; DROP TABLE documents; --",
                "\uFFFD", // Replacement character
                "\uFFFF",
                "a".repeat(10000),
                "🇺🇸🇬🇧🇯🇵", // Emoji flags
                "   \t\n   ",
                "2025-13-45", // Invalid date
                "9999-99-99",
                "-1",
                "0000",
                "99999999999999999999",
                "/../../etc/passwd",
                "COM1", // Reserved Windows names
                "\r\n\r\n",
                "true",
                "false",
                "[]",
                "{}"
            ).bind()
        ).bind()

        val confidence = Arb.element(
            Arb.float(range = -Float.MAX_VALUE..Float.MAX_VALUE).bind(),
            Float.NaN,
            Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY,
            0.0f,
            -1.0f,
            999.99f
        ).bind()

        ExtractedValue(value = value, confidence = confidence)
    }

    /**
     * Generator for adversarial metadata maps with random subsets of MetadataField
     * mapped to garbage values.
     */
    private val arbAdversarialMetadata: Arb<Map<MetadataField, ExtractedValue>> = arbitrary {
        val fieldCount = Arb.int(0..MetadataField.entries.size).bind()
        val fields = Arb.list(Arb.enum<MetadataField>(), 0..fieldCount).bind().distinct()
        fields.associateWith { arbAdversarialExtractedValue.bind() }
    }

    @Test
    @DisplayName("generateTags never throws with any document type and adversarial metadata")
    fun `generateTags never throws with adversarial metadata`() = runTest {
        checkAll(100, Arb.enum<DocumentType>(), arbAdversarialMetadata) { docType, metadata ->
            val result = assertDoesNotThrow {
                generator.generateTags(docType, metadata)
            }
            // Result must always be a list (possibly empty)
            assertNotNull(result, "generateTags must return a List<String>")
        }
    }

    @Test
    @DisplayName("generateTags returns a list (not null) for completely empty inputs")
    fun `generateTags returns list for empty inputs`() = runTest {
        checkAll(100, Arb.enum<DocumentType>()) { docType ->
            val result = assertDoesNotThrow {
                generator.generateTags(docType, emptyMap())
            }
            assertNotNull(result, "generateTags must return a List<String> even with empty metadata")
        }
    }

    @Test
    @DisplayName("generateTags never throws with extremely long metadata values")
    fun `generateTags never throws with extremely long strings`() = runTest {
        checkAll(100, Arb.enum<DocumentType>(), Arb.enum<MetadataField>(), Arb.string(1000..5000)) { docType, field, longValue ->
            val metadata = mapOf(field to ExtractedValue(value = longValue, confidence = 0.5f))

            val result = assertDoesNotThrow {
                generator.generateTags(docType, metadata)
            }
            assertNotNull(result, "generateTags must return a List<String> even with very long values")
        }
    }

    @Test
    @DisplayName("generateTags never throws with special unicode and control characters")
    fun `generateTags never throws with special characters in metadata`() = runTest {
        val specialStrings = listOf(
            "\u0000\u0001\u0002\u0003", // Control characters
            "\uD83D\uDE00", // Emoji
            "\u200B\u200C\u200D", // Zero-width characters
            "café résumé naïve", // Accented characters
            "日本語テスト", // Japanese
            "مرحبا", // Arabic (RTL)
            "𐍈", // Gothic script (supplementary plane)
            "\t\t\t\n\n\n\r\r\r", // Whitespace variants
            "\u202E\u202Dreverse", // Bidi override characters
            "line1\nline2\nline3", // Multi-line
            " ".repeat(10000), // Long whitespace
            "\u0000".repeat(100) // Null bytes
        )

        checkAll(100, Arb.enum<DocumentType>(), Arb.element(specialStrings)) { docType, specialStr ->
            val metadata = MetadataField.entries.associateWith {
                ExtractedValue(value = specialStr, confidence = Float.NaN)
            }

            val result = assertDoesNotThrow {
                generator.generateTags(docType, metadata)
            }
            assertNotNull(result, "generateTags must return a non-null list with special characters")
        }
    }

    @Test
    @DisplayName("generateTags never throws when all metadata fields have corrupt date-like values")
    fun `generateTags never throws with corrupt date values`() = runTest {
        val corruptDates = listOf(
            "not-a-date",
            "2025-13-45", // Invalid month/day
            "0000-00-00",
            "9999-99-99",
            "-2025-01-01",
            "2025/13/45",
            "32/13/2025",
            "February 30, 2025",
            "99999999",
            "",
            "   ",
            "2025",
            "20250101",
            "2025-01-01T00:00:00Z",
            "∞",
            "NaN"
        )

        checkAll(100, Arb.enum<DocumentType>(), Arb.element(corruptDates), Arb.element(corruptDates)) { docType, expiryStr, issueStr ->
            val metadata = mapOf(
                MetadataField.EXPIRY_DATE to ExtractedValue(value = expiryStr, confidence = -1.0f),
                MetadataField.ISSUE_DATE to ExtractedValue(value = issueStr, confidence = Float.MAX_VALUE),
                MetadataField.DESTINATION to ExtractedValue(value = expiryStr, confidence = Float.NaN)
            )

            val result = assertDoesNotThrow {
                generator.generateTags(docType, metadata)
            }
            assertNotNull(result, "generateTags must return a non-null list with corrupt dates")
        }
    }
}
