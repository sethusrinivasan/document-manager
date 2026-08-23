package com.app.traveldocs.domain.properties

import com.app.traveldocs.data.local.SearchEngineImpl
import com.app.traveldocs.domain.model.ParseResult
import com.app.traveldocs.domain.model.QueryIntent
import com.app.traveldocs.domain.model.SearchResult
import com.app.traveldocs.domain.repository.DocumentChecklistGenerator
import com.app.traveldocs.domain.repository.DocumentRepository
import com.app.traveldocs.domain.repository.NaturalLanguageParser
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

/**
 * Property 22: Insufficient query parameters request more info
 *
 * For any query that the NLP parser cannot extract travel parameters from
 * (returns null travelParams) with TRAVEL_CHECKLIST intent, SearchEngine
 * returns NeedMoreInfo.
 *
 * **Validates: Requirements 8.8**
 */
@DisplayName("Property 22: Insufficient parameters")
@Tag("Feature: travel-document-manager, Property 22: Insufficient parameters")
class InsufficientParametersPropertyTest {

    private lateinit var documentRepository: DocumentRepository
    private lateinit var naturalLanguageParser: NaturalLanguageParser
    private lateinit var documentChecklistGenerator: DocumentChecklistGenerator
    private lateinit var searchEngine: SearchEngineImpl

    private val memberId = "test-member"

    @BeforeEach
    fun setUp() {
        documentRepository = mockk(relaxed = true)
        naturalLanguageParser = mockk(relaxed = true)
        documentChecklistGenerator = mockk(relaxed = true)

        searchEngine = SearchEngineImpl(
            documentRepository = documentRepository,
            naturalLanguageParser = naturalLanguageParser,
            documentChecklistGenerator = documentChecklistGenerator
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    // Generator for random query strings that represent insufficient queries
    private val arbRandomQuery: Arb<String> = Arb.string(minSize = 1, maxSize = 100)

    @Test
    @DisplayName("TRAVEL_CHECKLIST intent with null travelParams returns NeedMoreInfo")
    fun `travel checklist intent with null travelParams returns NeedMoreInfo`() = runTest {
        checkAll(100, arbRandomQuery) { query ->
            // The parser returns null travelParams, simulating insufficient parameters
            val parseResult = ParseResult(
                intent = QueryIntent.TRAVEL_CHECKLIST,
                travelParams = null,
                searchTerms = emptyList()
            )

            every { naturalLanguageParser.parse(query) } returns parseResult

            val result = searchEngine.searchNaturalLanguage(memberId, query)

            assertTrue(
                result is SearchResult.NeedMoreInfo,
                "Expected NeedMoreInfo for TRAVEL_CHECKLIST intent with null travelParams " +
                    "(insufficient parameters), but got ${result::class.simpleName}. Query: '$query'"
            )

            val needMoreInfo = result as SearchResult.NeedMoreInfo
            assertTrue(
                needMoreInfo.missingParams.isNotEmpty(),
                "NeedMoreInfo should contain a non-empty list of missing parameters. Query: '$query'"
            )
        }
    }

    @Test
    @DisplayName("MISSING_DOCUMENTS intent with null travelParams returns NeedMoreInfo")
    fun `missing documents intent with null travelParams returns NeedMoreInfo`() = runTest {
        checkAll(100, arbRandomQuery) { query ->
            // The parser returns null travelParams for MISSING_DOCUMENTS intent
            val parseResult = ParseResult(
                intent = QueryIntent.MISSING_DOCUMENTS,
                travelParams = null,
                searchTerms = emptyList()
            )

            every { naturalLanguageParser.parse(query) } returns parseResult

            val result = searchEngine.searchNaturalLanguage(memberId, query)

            assertTrue(
                result is SearchResult.NeedMoreInfo,
                "Expected NeedMoreInfo for MISSING_DOCUMENTS intent with null travelParams " +
                    "(insufficient parameters), but got ${result::class.simpleName}. Query: '$query'"
            )

            val needMoreInfo = result as SearchResult.NeedMoreInfo
            assertTrue(
                needMoreInfo.missingParams.isNotEmpty(),
                "NeedMoreInfo should contain a non-empty list of missing parameters. Query: '$query'"
            )
        }
    }

    @Test
    @DisplayName("Insufficient parameters with various random query strings always returns NeedMoreInfo")
    fun `varied random queries with insufficient params always return NeedMoreInfo`() = runTest {
        // Use varied string sizes to simulate diverse query inputs
        val arbVariedQuery: Arb<String> = Arb.string(minSize = 1, maxSize = 200)

        checkAll(100, arbVariedQuery) { query ->
            val parseResult = ParseResult(
                intent = QueryIntent.TRAVEL_CHECKLIST,
                travelParams = null,
                searchTerms = emptyList()
            )

            every { naturalLanguageParser.parse(query) } returns parseResult

            val result = searchEngine.searchNaturalLanguage(memberId, query)

            assertTrue(
                result is SearchResult.NeedMoreInfo,
                "Expected NeedMoreInfo for any query where parser cannot extract travel parameters, " +
                    "but got ${result::class.simpleName}. Query: '$query'"
            )
        }
    }
}
