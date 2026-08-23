package com.app.traveldocs.domain.properties

import com.app.traveldocs.data.nlp.RegexNaturalLanguageParser
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/**
 * Property 19: NLP travel parameter extraction
 *
 * For any query containing "trip to {Destination}" pattern where Destination is a capitalized word,
 * the parser extracts that as the destination.
 *
 * Uses the real RegexNaturalLanguageParser. Generates random capitalized destination names.
 * 100 iterations. JUnit 5 + Kotest.
 *
 * **Validates: Requirements 8.1, 8.2, 8.3, 8.4, 8.5**
 */
@DisplayName("Property 19: NLP travel parameter extraction")
@Tag("Feature: travel-document-manager, Property 19: NLP travel parameter extraction")
class NlpTravelParameterExtractionPropertyTest {

    private lateinit var parser: RegexNaturalLanguageParser

    @BeforeEach
    fun setUp() {
        parser = RegexNaturalLanguageParser()
    }

    /**
     * Generator for random capitalized destination names.
     * Produces strings that start with an uppercase letter followed by lowercase letters,
     * with length between 4 and 12 characters. Avoids stop words by using minimum length of 4.
     */
    private val arbCapitalizedDestination: Arb<String> = arbitrary {
        val length = Arb.int(3..11).bind()
        val firstChar = ('A'..'Z').random()
        val restChars = (1..length).map { ('a'..'z').random() }.joinToString("")
        "$firstChar$restChars"
    }

    @Test
    @DisplayName("For any query with 'trip to {Destination}', parser extracts the capitalized destination")
    fun `trip to destination pattern extracts destination`() = runTest {
        checkAll(100, arbCapitalizedDestination) { destination ->
            val query = "trip to $destination"

            val result = parser.parse(query)

            assertNotNull(
                result.travelParams,
                "Parser should extract travel parameters from query: '$query'"
            )
            assertEquals(
                destination,
                result.travelParams!!.destination,
                "Parser should extract '$destination' as destination from query: '$query'"
            )
        }
    }

    @Test
    @DisplayName("For any query with 'traveling to {Destination}', parser extracts the capitalized destination")
    fun `traveling to destination pattern extracts destination`() = runTest {
        checkAll(100, arbCapitalizedDestination) { destination ->
            val query = "traveling to $destination"

            val result = parser.parse(query)

            assertNotNull(
                result.travelParams,
                "Parser should extract travel parameters from query: '$query'"
            )
            assertEquals(
                destination,
                result.travelParams!!.destination,
                "Parser should extract '$destination' as destination from query: '$query'"
            )
        }
    }

    @Test
    @DisplayName("For any query with 'going to {Destination}', parser extracts the capitalized destination")
    fun `going to destination pattern extracts destination`() = runTest {
        checkAll(100, arbCapitalizedDestination) { destination ->
            val query = "going to $destination"

            val result = parser.parse(query)

            assertNotNull(
                result.travelParams,
                "Parser should extract travel parameters from query: '$query'"
            )
            assertEquals(
                destination,
                result.travelParams!!.destination,
                "Parser should extract '$destination' as destination from query: '$query'"
            )
        }
    }

    @Test
    @DisplayName("For any query with 'trip to {Destination}' embedded in longer text, parser still extracts destination")
    fun `trip to destination in longer query extracts destination`() = runTest {
        checkAll(100, arbCapitalizedDestination) { destination ->
            val query = "What documents do I need for a trip to $destination for a week"

            val result = parser.parse(query)

            assertNotNull(
                result.travelParams,
                "Parser should extract travel parameters from query: '$query'"
            )
            assertEquals(
                destination,
                result.travelParams!!.destination,
                "Parser should extract '$destination' as destination from query: '$query'"
            )
        }
    }
}
