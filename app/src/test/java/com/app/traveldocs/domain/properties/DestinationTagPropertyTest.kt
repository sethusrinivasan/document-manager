package com.app.traveldocs.domain.properties

import com.app.traveldocs.data.tags.AutoTagGeneratorImpl
import com.app.traveldocs.domain.model.DocumentType
import com.app.traveldocs.domain.model.ExtractedValue
import com.app.traveldocs.domain.model.MetadataField
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/**
 * Property 10: Destination metadata generates destination tag
 *
 * For any ExtractionResult containing a non-empty DESTINATION metadata field,
 * the auto-generated tags should include a tag matching or containing the
 * destination value (trimmed and lowercased).
 *
 * Uses the real AutoTagGeneratorImpl class.
 *
 * **Validates: Requirements 5.7**
 */
@DisplayName("Property 10: Destination tag generation")
@Tag("Feature: travel-document-manager, Property 10: Destination tag generation")
class DestinationTagPropertyTest {

    private lateinit var generator: AutoTagGeneratorImpl

    @BeforeEach
    fun setUp() {
        generator = AutoTagGeneratorImpl()
    }

    /**
     * Generator for non-blank destination strings.
     * Produces strings with at least one non-whitespace character.
     */
    private val arbNonBlankDestination: Arb<String> = arbitrary {
        val base = Arb.string(1..30).bind()
        // Ensure the string has at least one non-whitespace character
        if (base.isBlank()) "Tokyo" else base
    }

    /**
     * Generator for confidence values between 0.0 and 1.0.
     */
    private val arbConfidence: Arb<Float> = arbitrary {
        Arb.int(0..100).bind() / 100f
    }

    @Test
    @DisplayName("For any non-blank DESTINATION metadata, a lowercase destination tag is generated")
    fun `non-blank destination metadata generates lowercase destination tag`() = runTest {
        checkAll(100, arbNonBlankDestination, Arb.enum<DocumentType>(), arbConfidence) { destination, docType, confidence ->
            val metadata = mapOf(
                MetadataField.DESTINATION to ExtractedValue(destination, confidence)
            )

            val tags = generator.generateTags(docType, metadata)

            val expectedTag = destination.trim().lowercase()
            assertTrue(
                tags.contains(expectedTag),
                "Tags should contain '$expectedTag' for destination '$destination' " +
                    "(docType=$docType), but got: $tags"
            )
        }
    }

    @Test
    @DisplayName("Destination tag is always lowercase regardless of input casing")
    fun `destination tag is always lowercase`() = runTest {
        checkAll(100, arbNonBlankDestination, arbConfidence) { destination, confidence ->
            val metadata = mapOf(
                MetadataField.DESTINATION to ExtractedValue(destination, confidence)
            )

            val tags = generator.generateTags(DocumentType.UNKNOWN, metadata)

            val expectedTag = destination.trim().lowercase()
            assertTrue(
                tags.any { it == expectedTag },
                "Tags should contain lowercase version '$expectedTag' of destination '$destination', but got: $tags"
            )
            // Verify the tag itself is fully lowercase
            val destinationTag = tags.find { it == expectedTag }
            assertTrue(
                destinationTag == destinationTag?.lowercase(),
                "Destination tag should be entirely lowercase, but got: '$destinationTag'"
            )
        }
    }

    @Test
    @DisplayName("Destination tag is trimmed of leading/trailing whitespace")
    fun `destination tag is trimmed`() = runTest {
        checkAll(100, arbNonBlankDestination, arbConfidence) { destination, confidence ->
            // Add whitespace padding to the destination
            val paddedDestination = "  $destination  "
            val metadata = mapOf(
                MetadataField.DESTINATION to ExtractedValue(paddedDestination, confidence)
            )

            val tags = generator.generateTags(DocumentType.UNKNOWN, metadata)

            val expectedTag = destination.trim().lowercase()
            assertTrue(
                tags.contains(expectedTag),
                "Tags should contain trimmed tag '$expectedTag' for padded destination " +
                    "'$paddedDestination', but got: $tags"
            )
        }
    }
}
