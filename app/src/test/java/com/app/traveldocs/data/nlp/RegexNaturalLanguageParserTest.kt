package com.app.traveldocs.data.nlp

import com.app.traveldocs.domain.model.QueryIntent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class RegexNaturalLanguageParserTest {

    private lateinit var parser: RegexNaturalLanguageParser

    @BeforeEach
    fun setUp() {
        parser = RegexNaturalLanguageParser()
    }

    @Nested
    inner class IntentDetection {

        @Test
        fun `detects TRAVEL_CHECKLIST for what documents do I need`() {
            val result = parser.parse("what documents do I need for a trip to Singapore?")
            assertEquals(QueryIntent.TRAVEL_CHECKLIST, result.intent)
        }

        @Test
        fun `detects TRAVEL_CHECKLIST for checklist keyword`() {
            val result = parser.parse("travel checklist for Japan")
            assertEquals(QueryIntent.TRAVEL_CHECKLIST, result.intent)
        }

        @Test
        fun `detects TRAVEL_CHECKLIST for requirements for`() {
            val result = parser.parse("requirements for traveling to France")
            assertEquals(QueryIntent.TRAVEL_CHECKLIST, result.intent)
        }

        @Test
        fun `detects MISSING_DOCUMENTS for what am I missing`() {
            val result = parser.parse("what am I missing for my trip?")
            assertEquals(QueryIntent.MISSING_DOCUMENTS, result.intent)
        }

        @Test
        fun `detects MISSING_DOCUMENTS for missing documents`() {
            val result = parser.parse("show me missing documents")
            assertEquals(QueryIntent.MISSING_DOCUMENTS, result.intent)
        }

        @Test
        fun `detects MISSING_DOCUMENTS for what's missing`() {
            val result = parser.parse("what's missing for my trip to Japan?")
            assertEquals(QueryIntent.MISSING_DOCUMENTS, result.intent)
        }

        @Test
        fun `detects DOCUMENT_SEARCH for find keyword`() {
            val result = parser.parse("find my passport")
            assertEquals(QueryIntent.DOCUMENT_SEARCH, result.intent)
        }

        @Test
        fun `detects DOCUMENT_SEARCH for show keyword`() {
            val result = parser.parse("show all visas")
            assertEquals(QueryIntent.DOCUMENT_SEARCH, result.intent)
        }

        @Test
        fun `detects DOCUMENT_SEARCH for search keyword`() {
            val result = parser.parse("search for tickets")
            assertEquals(QueryIntent.DOCUMENT_SEARCH, result.intent)
        }

        @Test
        fun `defaults to DOCUMENT_SEARCH for unrecognized queries`() {
            val result = parser.parse("passport expiring soon")
            assertEquals(QueryIntent.DOCUMENT_SEARCH, result.intent)
        }
    }

    @Nested
    inner class FamilySizeExtraction {

        @Test
        fun `extracts family size from family of N`() {
            val result = parser.parse("what documents do I need for a family of 4 trip to Singapore?")
            assertNotNull(result.travelParams)
            assertEquals(4, result.travelParams!!.familySize)
        }

        @Test
        fun `extracts family size from N people`() {
            val result = parser.parse("traveling with 5 people to Japan")
            assertNotNull(result.travelParams)
            assertEquals(5, result.travelParams!!.familySize)
        }

        @Test
        fun `extracts family size from N travelers`() {
            val result = parser.parse("3 travelers going to France")
            assertNotNull(result.travelParams)
            assertEquals(3, result.travelParams!!.familySize)
        }

        @Test
        fun `defaults to 4 for generic family keyword`() {
            val result = parser.parse("what documents do I need for a family trip to Singapore?")
            assertNotNull(result.travelParams)
            assertEquals(4, result.travelParams!!.familySize)
        }

        @Test
        fun `explicit family of N overrides generic family`() {
            val result = parser.parse("family of 6 trip to Singapore")
            assertNotNull(result.travelParams)
            assertEquals(6, result.travelParams!!.familySize)
        }
    }

    @Nested
    inner class DestinationExtraction {

        @Test
        fun `extracts destination from trip to X`() {
            val result = parser.parse("what documents do I need for a trip to Singapore?")
            assertNotNull(result.travelParams)
            assertEquals("Singapore", result.travelParams!!.destination)
        }

        @Test
        fun `extracts destination from traveling to X`() {
            val result = parser.parse("traveling to Japan next month")
            assertNotNull(result.travelParams)
            assertEquals("Japan", result.travelParams!!.destination)
        }

        @Test
        fun `extracts destination from going to X`() {
            val result = parser.parse("going to France for vacation")
            assertNotNull(result.travelParams)
            assertEquals("France", result.travelParams!!.destination)
        }

        @Test
        fun `extracts multi-word destination`() {
            val result = parser.parse("trip to New Zealand")
            assertNotNull(result.travelParams)
            assertEquals("New Zealand", result.travelParams!!.destination)
        }
    }

    @Nested
    inner class OriginExtraction {

        @Test
        fun `extracts origin from living in X`() {
            val result = parser.parse("family of 4 living in US going to Singapore")
            assertNotNull(result.travelParams)
            assertEquals("US", result.travelParams!!.origin)
        }

        @Test
        fun `extracts origin from from X`() {
            val result = parser.parse("traveling from Canada to Japan")
            assertNotNull(result.travelParams)
            assertEquals("Canada", result.travelParams!!.origin)
        }

        @Test
        fun `extracts origin from based in X`() {
            val result = parser.parse("based in Germany, trip to Italy")
            assertNotNull(result.travelParams)
            assertEquals("Germany", result.travelParams!!.origin)
        }
    }

    @Nested
    inner class DurationExtraction {

        @Test
        fun `extracts duration from N days`() {
            val result = parser.parse("trip to Japan for 10 days")
            assertNotNull(result.travelParams)
            assertEquals(10, result.travelParams!!.durationDays)
        }

        @Test
        fun `extracts duration from N weeks`() {
            val result = parser.parse("2 weeks in Singapore")
            assertNotNull(result.travelParams)
            assertEquals(14, result.travelParams!!.durationDays)
        }

        @Test
        fun `extracts duration from a week keyword`() {
            val result = parser.parse("going to Japan for a week")
            assertNotNull(result.travelParams)
            assertEquals(7, result.travelParams!!.durationDays)
        }

        @Test
        fun `extracts duration from two weeks keyword`() {
            val result = parser.parse("two weeks trip to France")
            assertNotNull(result.travelParams)
            assertEquals(14, result.travelParams!!.durationDays)
        }

        @Test
        fun `extracts duration from N nights`() {
            val result = parser.parse("5 nights in Tokyo")
            assertNotNull(result.travelParams)
            assertEquals(5, result.travelParams!!.durationDays)
        }

        @Test
        fun `extracts duration from weekend keyword`() {
            val result = parser.parse("weekend trip to Singapore")
            assertNotNull(result.travelParams)
            assertEquals(3, result.travelParams!!.durationDays)
        }

        @Test
        fun `extracts duration from week keyword`() {
            val result = parser.parse("what do I need for a week in Singapore?")
            assertNotNull(result.travelParams)
            assertEquals(7, result.travelParams!!.durationDays)
        }
    }

    @Nested
    inner class FullQueryParsing {

        @Test
        fun `parses full travel query with all parameters`() {
            val result = parser.parse(
                "what documents do I need for a family of 4 living in US going to Singapore for a week?"
            )
            assertEquals(QueryIntent.TRAVEL_CHECKLIST, result.intent)
            assertNotNull(result.travelParams)
            assertEquals(4, result.travelParams!!.familySize)
            assertEquals("US", result.travelParams!!.origin)
            assertEquals("Singapore", result.travelParams!!.destination)
            assertEquals(7, result.travelParams!!.durationDays)
        }

        @Test
        fun `preserves raw query in travel parameters`() {
            val query = "trip to Japan for 5 days"
            val result = parser.parse(query)
            assertNotNull(result.travelParams)
            assertEquals(query, result.travelParams!!.rawQuery)
        }
    }

    @Nested
    inner class SearchTermExtraction {

        @Test
        fun `extracts significant search terms`() {
            val result = parser.parse("find passport for Singapore")
            assertTrue(result.searchTerms.contains("passport"))
            assertTrue(result.searchTerms.contains("singapore"))
        }

        @Test
        fun `excludes common stop words`() {
            val result = parser.parse("find the passport for a trip")
            assertTrue(!result.searchTerms.contains("the"))
            assertTrue(!result.searchTerms.contains("for"))
        }

        @Test
        fun `excludes short words under 3 characters`() {
            val result = parser.parse("go to it")
            assertTrue(result.searchTerms.isEmpty())
        }

        @Test
        fun `returns distinct terms`() {
            val result = parser.parse("passport passport visa")
            val passportCount = result.searchTerms.count { it == "passport" }
            assertEquals(1, passportCount)
        }
    }

    @Nested
    inner class EdgeCases {

        @Test
        fun `handles empty query`() {
            val result = parser.parse("")
            assertEquals(QueryIntent.DOCUMENT_SEARCH, result.intent)
            assertNull(result.travelParams)
            assertTrue(result.searchTerms.isEmpty())
        }

        @Test
        fun `handles query with no travel parameters`() {
            val result = parser.parse("find my visa")
            assertEquals(QueryIntent.DOCUMENT_SEARCH, result.intent)
            assertNull(result.travelParams)
        }

        @Test
        fun `handles query with only one travel parameter`() {
            val result = parser.parse("trip to Singapore")
            assertNotNull(result.travelParams)
            assertEquals("Singapore", result.travelParams!!.destination)
            assertNull(result.travelParams!!.familySize)
            assertNull(result.travelParams!!.origin)
            assertNull(result.travelParams!!.durationDays)
        }

        @Test
        fun `case insensitive intent detection`() {
            val result = parser.parse("WHAT DOCUMENTS DO I NEED for Japan?")
            assertEquals(QueryIntent.TRAVEL_CHECKLIST, result.intent)
        }

        @Test
        fun `handles special characters in query`() {
            val result = parser.parse("what's needed for a trip to Singapore?!")
            assertNotNull(result.travelParams)
            assertEquals("Singapore", result.travelParams!!.destination)
        }
    }
}
