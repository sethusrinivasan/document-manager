package com.app.traveldocs.data.local

import com.app.traveldocs.domain.model.Document
import com.app.traveldocs.domain.model.DocumentFormat
import com.app.traveldocs.domain.model.DocumentType
import com.app.traveldocs.domain.model.MissingDocument
import com.app.traveldocs.domain.model.ParseResult
import com.app.traveldocs.domain.model.QueryIntent
import com.app.traveldocs.domain.model.RequiredDocument
import com.app.traveldocs.domain.model.SearchQuery
import com.app.traveldocs.domain.model.SearchResult
import com.app.traveldocs.domain.model.Tag
import com.app.traveldocs.domain.model.TravelDocumentChecklist
import com.app.traveldocs.domain.model.TravelParameters
import com.app.traveldocs.domain.repository.DocumentChecklistGenerator
import com.app.traveldocs.domain.repository.DocumentRepository
import com.app.traveldocs.domain.repository.NaturalLanguageParser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class SearchEngineImplTest {

    private lateinit var documentRepository: DocumentRepository
    private lateinit var naturalLanguageParser: NaturalLanguageParser
    private lateinit var documentChecklistGenerator: DocumentChecklistGenerator
    private lateinit var searchEngine: SearchEngineImpl

    private val memberId = "member-1"

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

    private fun createTestDocument(
        id: String = "doc-1",
        memberId: String = "member-1",
        type: DocumentType = DocumentType.PASSPORT,
        tags: List<Tag> = listOf(Tag("passport", true))
    ): Document {
        return Document(
            id = id,
            memberId = memberId,
            type = type,
            format = DocumentFormat.PDF,
            originalFileName = "test.pdf",
            metadata = emptyMap(),
            tags = tags,
            createdAt = Instant.ofEpochMilli(1700000000000L),
            updatedAt = Instant.ofEpochMilli(1700000000000L),
            extractionConfidence = 0.95f,
            requiresManualReview = false
        )
    }

    // --- searchByTags tests ---

    @Test
    fun `searchByTags delegates to repository with tag-based SearchQuery`() = runTest {
        val tags = listOf("passport", "visa")
        val expectedDocs = listOf(createTestDocument())
        coEvery {
            documentRepository.search(memberId, SearchQuery(tags = tags))
        } returns expectedDocs

        val result = searchEngine.searchByTags(memberId, tags)

        assertEquals(expectedDocs, result)
        coVerify { documentRepository.search(memberId, SearchQuery(tags = tags)) }
    }

    @Test
    fun `searchByTags with empty tags returns empty list`() = runTest {
        coEvery {
            documentRepository.search(memberId, SearchQuery(tags = emptyList()))
        } returns emptyList()

        val result = searchEngine.searchByTags(memberId, emptyList())

        assertTrue(result.isEmpty())
    }

    @Test
    fun `searchByTags returns multiple matching documents`() = runTest {
        val tags = listOf("travel")
        val docs = listOf(
            createTestDocument(id = "doc-1"),
            createTestDocument(id = "doc-2")
        )
        coEvery {
            documentRepository.search(memberId, SearchQuery(tags = tags))
        } returns docs

        val result = searchEngine.searchByTags(memberId, tags)

        assertEquals(2, result.size)
    }

    // --- searchFreeForm tests ---

    @Test
    fun `searchFreeForm delegates to repository with freeText SearchQuery`() = runTest {
        val query = "john passport"
        val expectedDocs = listOf(createTestDocument())
        coEvery {
            documentRepository.search(memberId, SearchQuery(freeText = query))
        } returns expectedDocs

        val result = searchEngine.searchFreeForm(memberId, query)

        assertEquals(expectedDocs, result)
        coVerify { documentRepository.search(memberId, SearchQuery(freeText = query)) }
    }

    @Test
    fun `searchFreeForm with no matches returns empty list`() = runTest {
        coEvery {
            documentRepository.search(memberId, SearchQuery(freeText = "nonexistent"))
        } returns emptyList()

        val result = searchEngine.searchFreeForm(memberId, "nonexistent")

        assertTrue(result.isEmpty())
    }

    // --- searchNaturalLanguage tests ---

    @Test
    fun `searchNaturalLanguage with DOCUMENT_SEARCH intent and search terms uses tags and freeText`() = runTest {
        val query = "find my passport"
        val parseResult = ParseResult(
            intent = QueryIntent.DOCUMENT_SEARCH,
            travelParams = null,
            searchTerms = listOf("passport")
        )
        val expectedDocs = listOf(createTestDocument())
        every { naturalLanguageParser.parse(query) } returns parseResult
        coEvery {
            documentRepository.search(memberId, SearchQuery(tags = listOf("passport"), freeText = query))
        } returns expectedDocs

        val result = searchEngine.searchNaturalLanguage(memberId, query)

        assertTrue(result is SearchResult.DocumentResults)
        assertEquals(expectedDocs, (result as SearchResult.DocumentResults).documents)
    }

    @Test
    fun `searchNaturalLanguage with DOCUMENT_SEARCH intent and empty search terms uses freeForm`() = runTest {
        val query = "some documents"
        val parseResult = ParseResult(
            intent = QueryIntent.DOCUMENT_SEARCH,
            travelParams = null,
            searchTerms = emptyList()
        )
        val expectedDocs = listOf(createTestDocument())
        every { naturalLanguageParser.parse(query) } returns parseResult
        coEvery {
            documentRepository.search(memberId, SearchQuery(freeText = query))
        } returns expectedDocs

        val result = searchEngine.searchNaturalLanguage(memberId, query)

        assertTrue(result is SearchResult.DocumentResults)
        assertEquals(expectedDocs, (result as SearchResult.DocumentResults).documents)
    }

    @Test
    fun `searchNaturalLanguage with TRAVEL_CHECKLIST intent generates checklist`() = runTest {
        val query = "What documents do I need for Singapore?"
        val travelParams = TravelParameters(
            familySize = 4,
            origin = "US",
            destination = "Singapore",
            durationDays = 7,
            rawQuery = query
        )
        val parseResult = ParseResult(
            intent = QueryIntent.TRAVEL_CHECKLIST,
            travelParams = travelParams,
            searchTerms = emptyList()
        )
        val checklist = TravelDocumentChecklist(
            requiredDocuments = listOf(
                RequiredDocument(DocumentType.PASSPORT, 4, "Valid passport")
            ),
            totalCount = 4
        )
        every { naturalLanguageParser.parse(query) } returns parseResult
        every { documentChecklistGenerator.generateChecklist(travelParams) } returns checklist

        val result = searchEngine.searchNaturalLanguage(memberId, query)

        assertTrue(result is SearchResult.TravelChecklist)
        assertEquals(checklist, (result as SearchResult.TravelChecklist).checklist)
        verify { documentChecklistGenerator.generateChecklist(travelParams) }
    }

    @Test
    fun `searchNaturalLanguage with TRAVEL_CHECKLIST intent and no travel params returns NeedMoreInfo`() = runTest {
        val query = "what documents?"
        val parseResult = ParseResult(
            intent = QueryIntent.TRAVEL_CHECKLIST,
            travelParams = null,
            searchTerms = emptyList()
        )
        every { naturalLanguageParser.parse(query) } returns parseResult

        val result = searchEngine.searchNaturalLanguage(memberId, query)

        assertTrue(result is SearchResult.NeedMoreInfo)
        val needMoreInfo = result as SearchResult.NeedMoreInfo
        assertTrue(needMoreInfo.missingParams.contains("origin"))
        assertTrue(needMoreInfo.missingParams.contains("destination"))
    }

    @Test
    fun `searchNaturalLanguage with MISSING_DOCUMENTS intent detects missing documents`() = runTest {
        val query = "What am I missing for my trip to Singapore?"
        val travelParams = TravelParameters(
            familySize = 2,
            origin = "US",
            destination = "Singapore",
            durationDays = 7,
            rawQuery = query
        )
        val parseResult = ParseResult(
            intent = QueryIntent.MISSING_DOCUMENTS,
            travelParams = travelParams,
            searchTerms = emptyList()
        )
        val checklist = TravelDocumentChecklist(
            requiredDocuments = listOf(
                RequiredDocument(DocumentType.PASSPORT, 2, "Valid passport"),
                RequiredDocument(DocumentType.VISA, 2, "Tourist visa")
            ),
            totalCount = 4
        )
        val existingDocs = listOf(createTestDocument(id = "doc-1", type = DocumentType.PASSPORT))
        val missingDocs = listOf(
            MissingDocument(
                required = RequiredDocument(DocumentType.VISA, 2, "Tourist visa"),
                suggestion = "Apply for visa at embassy"
            )
        )

        every { naturalLanguageParser.parse(query) } returns parseResult
        every { documentChecklistGenerator.generateChecklist(travelParams) } returns checklist
        coEvery { documentRepository.getAll(memberId) } returns flowOf(existingDocs)
        every { documentChecklistGenerator.detectMissing(checklist, existingDocs) } returns missingDocs

        val result = searchEngine.searchNaturalLanguage(memberId, query)

        assertTrue(result is SearchResult.DocumentResults)
        val docResults = (result as SearchResult.DocumentResults).documents
        assertEquals(1, docResults.size)
        assertEquals(DocumentType.VISA, docResults[0].type)
    }

    @Test
    fun `searchNaturalLanguage with MISSING_DOCUMENTS intent and no travel params returns NeedMoreInfo`() = runTest {
        val query = "what am I missing?"
        val parseResult = ParseResult(
            intent = QueryIntent.MISSING_DOCUMENTS,
            travelParams = null,
            searchTerms = emptyList()
        )
        every { naturalLanguageParser.parse(query) } returns parseResult

        val result = searchEngine.searchNaturalLanguage(memberId, query)

        assertTrue(result is SearchResult.NeedMoreInfo)
    }

    @Test
    fun `searchNaturalLanguage with MISSING_DOCUMENTS and no missing docs returns empty DocumentResults`() = runTest {
        val query = "Am I missing anything for my Singapore trip?"
        val travelParams = TravelParameters(
            familySize = 1,
            origin = "US",
            destination = "Singapore",
            durationDays = 7,
            rawQuery = query
        )
        val parseResult = ParseResult(
            intent = QueryIntent.MISSING_DOCUMENTS,
            travelParams = travelParams,
            searchTerms = emptyList()
        )
        val checklist = TravelDocumentChecklist(
            requiredDocuments = listOf(
                RequiredDocument(DocumentType.PASSPORT, 1, "Valid passport")
            ),
            totalCount = 1
        )
        val existingDocs = listOf(createTestDocument(id = "doc-1", type = DocumentType.PASSPORT))

        every { naturalLanguageParser.parse(query) } returns parseResult
        every { documentChecklistGenerator.generateChecklist(travelParams) } returns checklist
        coEvery { documentRepository.getAll(memberId) } returns flowOf(existingDocs)
        every { documentChecklistGenerator.detectMissing(checklist, existingDocs) } returns emptyList()

        val result = searchEngine.searchNaturalLanguage(memberId, query)

        assertTrue(result is SearchResult.DocumentResults)
        assertTrue((result as SearchResult.DocumentResults).documents.isEmpty())
    }
}
