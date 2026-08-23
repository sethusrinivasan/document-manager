package com.app.traveldocs.domain.properties

import com.app.traveldocs.data.local.SearchEngineImpl
import com.app.traveldocs.domain.model.DocumentType
import com.app.traveldocs.domain.model.ParseResult
import com.app.traveldocs.domain.model.QueryIntent
import com.app.traveldocs.domain.model.RequiredDocument
import com.app.traveldocs.domain.model.SearchResult
import com.app.traveldocs.domain.model.TravelDocumentChecklist
import com.app.traveldocs.domain.model.TravelParameters
import com.app.traveldocs.domain.repository.DocumentChecklistGenerator
import com.app.traveldocs.domain.repository.DocumentRepository
import com.app.traveldocs.domain.repository.NaturalLanguageParser
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

/**
 * Property 19: Natural language query routing
 *
 * For any ParseResult with TRAVEL_CHECKLIST intent + valid travelParams,
 * searchNaturalLanguage returns TravelChecklist result.
 *
 * For any ParseResult with MISSING_DOCUMENTS intent + null travelParams,
 * searchNaturalLanguage returns NeedMoreInfo.
 *
 * **Validates: Requirements 8.1**
 */
@DisplayName("Property 19: Natural language query routing")
@Tag("Feature: travel-document-manager, Property 19: Natural language query routing")
class NlpIntentRoutingPropertyTest {

    private lateinit var documentRepository: DocumentRepository
    private lateinit var naturalLanguageParser: NaturalLanguageParser
    private lateinit var documentChecklistGenerator: DocumentChecklistGenerator
    private lateinit var searchEngine: SearchEngineImpl

    private val memberId = "test-member"

    private val countries = listOf(
        "US", "UK", "Singapore", "Japan", "France", "Germany",
        "Australia", "Canada", "India", "Brazil", "Mexico", "Italy"
    )

    @BeforeEach
    fun setUp() {
        documentRepository = mockk(relaxed = true)
        naturalLanguageParser = mockk(relaxed = true)
        documentChecklistGenerator = mockk(relaxed = true)

        coEvery { documentRepository.getAll(any()) } returns flowOf(emptyList())

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

    // Generator for valid TravelParameters (non-null)
    private val arbTravelParameters: Arb<TravelParameters> = arbitrary {
        val familySize = Arb.int(1..10).bind()
        val origin = Arb.element(countries).bind()
        val destination = Arb.element(countries.filter { it != origin }).bind()
        val duration = Arb.int(1..90).bind()
        val query = Arb.string(minSize = 5, maxSize = 30).bind()

        TravelParameters(
            familySize = familySize,
            origin = origin,
            destination = destination,
            durationDays = duration,
            rawQuery = query
        )
    }

    // Generator for a natural language query string
    private val arbQuery: Arb<String> = Arb.string(minSize = 5, maxSize = 50)

    // Generator for TravelDocumentChecklist
    private val arbChecklist: Arb<TravelDocumentChecklist> = arbitrary {
        val docTypes = listOf(
            DocumentType.PASSPORT, DocumentType.VISA,
            DocumentType.TICKET, DocumentType.HEALTH_INSURANCE
        )
        val numDocs = Arb.int(1..4).bind()
        val selectedTypes = docTypes.shuffled().take(numDocs)
        val familySize = Arb.int(1..10).bind()

        val requiredDocs = selectedTypes.map { type ->
            RequiredDocument(
                type = type,
                countNeeded = familySize,
                description = "Required ${type.name.lowercase()}"
            )
        }

        TravelDocumentChecklist(
            requiredDocuments = requiredDocs,
            totalCount = requiredDocs.sumOf { it.countNeeded }
        )
    }

    @Test
    @DisplayName("TRAVEL_CHECKLIST intent with valid travelParams returns TravelChecklist result")
    fun `travel checklist intent with valid params returns TravelChecklist`() = runTest {
        checkAll(100, arbQuery, arbTravelParameters, arbChecklist) { query, travelParams, checklist ->
            val parseResult = ParseResult(
                intent = QueryIntent.TRAVEL_CHECKLIST,
                travelParams = travelParams,
                searchTerms = emptyList()
            )

            every { naturalLanguageParser.parse(query) } returns parseResult
            every { documentChecklistGenerator.generateChecklist(travelParams) } returns checklist

            val result = searchEngine.searchNaturalLanguage(memberId, query)

            assertTrue(
                result is SearchResult.TravelChecklist,
                "Expected TravelChecklist result for TRAVEL_CHECKLIST intent with valid travelParams, " +
                    "but got ${result::class.simpleName}. Query: '$query', Params: $travelParams"
            )

            val checklistResult = result as SearchResult.TravelChecklist
            assertTrue(
                checklistResult.checklist == checklist,
                "Returned checklist should match the one generated by DocumentChecklistGenerator"
            )
        }
    }

    @Test
    @DisplayName("MISSING_DOCUMENTS intent with null travelParams returns NeedMoreInfo")
    fun `missing documents intent with null params returns NeedMoreInfo`() = runTest {
        checkAll(100, arbQuery) { query ->
            val parseResult = ParseResult(
                intent = QueryIntent.MISSING_DOCUMENTS,
                travelParams = null,
                searchTerms = emptyList()
            )

            every { naturalLanguageParser.parse(query) } returns parseResult

            val result = searchEngine.searchNaturalLanguage(memberId, query)

            assertTrue(
                result is SearchResult.NeedMoreInfo,
                "Expected NeedMoreInfo result for MISSING_DOCUMENTS intent with null travelParams, " +
                    "but got ${result::class.simpleName}. Query: '$query'"
            )
        }
    }

    @Test
    @DisplayName("TRAVEL_CHECKLIST intent with null travelParams returns NeedMoreInfo")
    fun `travel checklist intent with null params returns NeedMoreInfo`() = runTest {
        checkAll(100, arbQuery) { query ->
            val parseResult = ParseResult(
                intent = QueryIntent.TRAVEL_CHECKLIST,
                travelParams = null,
                searchTerms = emptyList()
            )

            every { naturalLanguageParser.parse(query) } returns parseResult

            val result = searchEngine.searchNaturalLanguage(memberId, query)

            assertTrue(
                result is SearchResult.NeedMoreInfo,
                "Expected NeedMoreInfo for TRAVEL_CHECKLIST intent with null travelParams, " +
                    "but got ${result::class.simpleName}. Query: '$query'"
            )
        }
    }
}
