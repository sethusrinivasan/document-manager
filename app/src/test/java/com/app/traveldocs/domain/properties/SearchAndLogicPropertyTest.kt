package com.app.traveldocs.domain.properties

import com.app.traveldocs.data.local.SearchEngineImpl
import com.app.traveldocs.domain.model.Document
import com.app.traveldocs.domain.model.DocumentFormat
import com.app.traveldocs.domain.model.DocumentType
import com.app.traveldocs.domain.model.SearchQuery
import com.app.traveldocs.domain.model.Tag
import com.app.traveldocs.domain.repository.DocumentChecklistGenerator
import com.app.traveldocs.domain.repository.DocumentRepository
import com.app.traveldocs.domain.repository.NaturalLanguageParser
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag as JUnitTag
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertTrue

/**
 * Property 17: Search results satisfy all criteria (AND logic)
 *
 * For any search query with tags, every document in the result set must satisfy
 * ALL specified criteria simultaneously: it must contain all queried tags.
 *
 * Multi-tag search returns only documents containing ALL specified tags.
 *
 * **Validates: Requirements 7.1, 7.2, 7.4**
 */
@DisplayName("Property 17: Search AND logic")
@JUnitTag("Feature: travel-document-manager, Property 17: Search AND logic")
class SearchAndLogicPropertyTest {

    private lateinit var documentRepository: DocumentRepository
    private lateinit var naturalLanguageParser: NaturalLanguageParser
    private lateinit var documentChecklistGenerator: DocumentChecklistGenerator
    private lateinit var searchEngine: SearchEngineImpl

    // In-memory document store
    private val documentStore = mutableListOf<Document>()

    private val memberId = "test-member"

    // Pool of possible tags used in generation
    private val tagPool = listOf(
        "passport", "visa", "ticket", "accommodation", "health",
        "travel", "family", "business", "europe", "asia",
        "urgent", "expired", "valid", "personal", "work"
    )

    @BeforeEach
    fun setUp() {
        documentStore.clear()

        documentRepository = mockk(relaxed = true)
        naturalLanguageParser = mockk(relaxed = true)
        documentChecklistGenerator = mockk(relaxed = true)

        // Mock search to implement in-memory AND logic filtering
        coEvery { documentRepository.search(any(), any()) } coAnswers {
            val queryMemberId = firstArg<String>()
            val query = secondArg<SearchQuery>()

            val memberDocs = documentStore.filter { it.memberId == queryMemberId }

            val filteredByTags = if (query.tags.isNotEmpty()) {
                memberDocs.filter { doc ->
                    val docTagNames = doc.tags.map { it.name }
                    query.tags.all { searchTag -> searchTag in docTagNames }
                }
            } else {
                memberDocs
            }

            val filteredByFreeText = if (query.freeText != null) {
                filteredByTags.filter { doc ->
                    val searchText = query.freeText.lowercase()
                    val matchesTags = doc.tags.any { it.name.lowercase().contains(searchText) }
                    val matchesMetadata = doc.metadata.values.any { it.lowercase().contains(searchText) }
                    val matchesFileName = doc.originalFileName?.lowercase()?.contains(searchText) == true
                    matchesTags || matchesMetadata || matchesFileName
                }
            } else {
                filteredByTags
            }

            filteredByFreeText
        }

        // Mock getAll for completeness
        coEvery { documentRepository.getAll(any()) } coAnswers {
            val queryMemberId = firstArg<String>()
            flowOf(documentStore.filter { it.memberId == queryMemberId })
        }

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

    // Generator for a random subset of tags from the pool
    private val arbTagSubset: Arb<List<String>> = arbitrary {
        val count = Arb.int(1..6).bind()
        val indices = (tagPool.indices).shuffled().take(count)
        indices.map { tagPool[it] }
    }

    // Generator for a document with random tags from the pool
    private val arbDocument: Arb<Document> = arbitrary {
        val docTags = arbTagSubset.bind()
        val docType = Arb.element(DocumentType.entries.toList()).bind()
        val docFormat = Arb.element(DocumentFormat.entries.toList()).bind()
        val docId = "doc-${Arb.string(minSize = 5, maxSize = 10).bind()}"

        Document(
            id = docId,
            memberId = memberId,
            type = docType,
            format = docFormat,
            originalFileName = "file-$docId.pdf",
            metadata = emptyMap(),
            tags = docTags.map { Tag(name = it, isAutoGenerated = false) },
            createdAt = Instant.ofEpochMilli(1700000000000L),
            updatedAt = Instant.ofEpochMilli(1700000000000L),
            extractionConfidence = 0.9f,
            requiresManualReview = false
        )
    }

    // Generator for a list of documents (3..10 documents)
    private val arbDocumentList: Arb<List<Document>> = Arb.list(arbDocument, 3..10)

    // Generator for search tags (subset of the tag pool, 1..4 tags)
    private val arbSearchTags: Arb<List<String>> = arbitrary {
        val count = Arb.int(1..4).bind()
        val indices = (tagPool.indices).shuffled().take(count)
        indices.map { tagPool[it] }
    }

    @Test
    @DisplayName("Multi-tag search returns only documents containing ALL specified tags")
    fun `multi-tag search returns only documents containing all specified tags`() = runTest {
        checkAll(100, arbDocumentList, arbSearchTags) { documents, searchTags ->
            // Reset store for each iteration
            documentStore.clear()
            documentStore.addAll(documents)

            // Perform tag-based search
            val results = searchEngine.searchByTags(memberId, searchTags)

            // Verify: every returned document must contain ALL searched tags
            for (resultDoc in results) {
                val resultTagNames = resultDoc.tags.map { it.name }
                for (searchTag in searchTags) {
                    assertTrue(
                        searchTag in resultTagNames,
                        "Document '${resultDoc.id}' in search results must contain tag '$searchTag' " +
                            "but only has tags: $resultTagNames. Searched for: $searchTags"
                    )
                }
            }
        }
    }

    @Test
    @DisplayName("Multi-tag search does not exclude any documents that contain all searched tags")
    fun `multi-tag search includes all documents that contain all searched tags`() = runTest {
        checkAll(100, arbDocumentList, arbSearchTags) { documents, searchTags ->
            // Reset store for each iteration
            documentStore.clear()
            documentStore.addAll(documents)

            // Perform tag-based search
            val results = searchEngine.searchByTags(memberId, searchTags)
            val resultIds = results.map { it.id }.toSet()

            // Verify: every document that contains all searched tags should be in results
            val expectedDocs = documents.filter { doc ->
                val docTagNames = doc.tags.map { it.name }
                searchTags.all { searchTag -> searchTag in docTagNames }
            }

            for (expectedDoc in expectedDocs) {
                assertTrue(
                    expectedDoc.id in resultIds,
                    "Document '${expectedDoc.id}' contains all searched tags $searchTags " +
                        "(has tags: ${expectedDoc.tags.map { it.name }}) but was not in results"
                )
            }
        }
    }

    @Test
    @DisplayName("Search result count equals exactly the number of documents matching all tags")
    fun `search result count equals documents matching all tags`() = runTest {
        checkAll(100, arbDocumentList, arbSearchTags) { documents, searchTags ->
            // Reset store for each iteration
            documentStore.clear()
            documentStore.addAll(documents)

            // Perform tag-based search
            val results = searchEngine.searchByTags(memberId, searchTags)

            // Count documents that should match (contain ALL searched tags)
            val expectedCount = documents.count { doc ->
                val docTagNames = doc.tags.map { it.name }
                searchTags.all { searchTag -> searchTag in docTagNames }
            }

            assertTrue(
                results.size == expectedCount,
                "Expected $expectedCount results for search tags $searchTags but got ${results.size}"
            )
        }
    }
}
