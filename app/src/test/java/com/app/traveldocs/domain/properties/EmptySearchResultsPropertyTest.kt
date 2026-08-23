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
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Property 18: Empty search returns empty list without error
 *
 * For any search query where no documents match, the result set is empty
 * and no exception is thrown.
 *
 * **Validates: Requirements 7.5**
 */
@DisplayName("Property 18: Empty search results")
@JUnitTag("Feature: travel-document-manager, Property 18: Empty search results")
class EmptySearchResultsPropertyTest {

    private lateinit var documentRepository: DocumentRepository
    private lateinit var naturalLanguageParser: NaturalLanguageParser
    private lateinit var documentChecklistGenerator: DocumentChecklistGenerator
    private lateinit var searchEngine: SearchEngineImpl

    // In-memory document store
    private val documentStore = mutableListOf<Document>()

    private val memberId = "test-member"

    // Tags that documents in the store may have
    private val storeTagPool = listOf(
        "passport", "visa", "ticket", "accommodation", "health"
    )

    // Tags that are guaranteed NOT to be in the store tag pool
    private val nonMatchingTagPool = listOf(
        "zzzz-nonexistent-tag", "xyz-unknown-category", "qqq-random-label",
        "nope-not-here", "missing-tag-alpha", "absent-beta", "ghost-gamma",
        "phantom-delta", "void-epsilon", "null-zeta", "fake-eta",
        "bogus-theta", "invalid-iota", "empty-kappa", "zilch-lambda"
    )

    @BeforeEach
    fun setUp() {
        documentStore.clear()

        documentRepository = mockk(relaxed = true)
        naturalLanguageParser = mockk(relaxed = true)
        documentChecklistGenerator = mockk(relaxed = true)

        // Mock search to implement in-memory filtering (AND logic)
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

    // Generator for documents that only use tags from the store tag pool
    private val arbDocument: Arb<Document> = arbitrary {
        val tagCount = Arb.int(1..3).bind()
        val docTags = storeTagPool.shuffled().take(tagCount)
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
            tags = docTags.map { Tag(name = it, isAutoGenerated = true) },
            createdAt = Instant.ofEpochMilli(1700000000000L),
            updatedAt = Instant.ofEpochMilli(1700000000000L),
            extractionConfidence = 0.9f,
            requiresManualReview = false
        )
    }

    // Generator for a list of documents (0..8)
    private val arbDocumentList: Arb<List<Document>> = Arb.list(arbDocument, 0..8)

    // Generator for non-matching search tags (tags guaranteed to not exist in the store)
    private val arbNonMatchingTags: Arb<List<String>> = arbitrary {
        val count = Arb.int(1..4).bind()
        nonMatchingTagPool.shuffled().take(count)
    }

    // Generator for non-matching free-text queries
    private val arbNonMatchingFreeText: Arb<String> = arbitrary {
        val prefixes = listOf("zznonexist", "xqmissing", "qqabsent", "wwphantom", "uurandom")
        val prefix = Arb.element(prefixes).bind()
        val suffix = Arb.string(minSize = 3, maxSize = 8).bind()
        "$prefix-$suffix"
    }

    @Test
    @DisplayName("Tag search with non-matching tags returns empty list without exception")
    fun `tag search with non-matching tags returns empty list without exception`() = runTest {
        checkAll(100, arbDocumentList, arbNonMatchingTags) { documents, searchTags ->
            documentStore.clear()
            documentStore.addAll(documents)

            // Search with tags that don't exist in any document
            val results = searchEngine.searchByTags(memberId, searchTags)

            // The result must be an empty list — no exceptions thrown
            assertTrue(
                results.isEmpty(),
                "Expected empty results for non-matching tags $searchTags " +
                    "but got ${results.size} results. " +
                    "Store has tags: ${documents.flatMap { d -> d.tags.map { it.name } }.distinct()}"
            )
        }
    }

    @Test
    @DisplayName("Free-form search with non-matching text returns empty list without exception")
    fun `free-form search with non-matching text returns empty list without exception`() = runTest {
        checkAll(100, arbDocumentList, arbNonMatchingFreeText) { documents, searchText ->
            documentStore.clear()
            documentStore.addAll(documents)

            // Search with free text that won't match any document
            val results = searchEngine.searchFreeForm(memberId, searchText)

            // The result must be an empty list — no exceptions thrown
            assertTrue(
                results.isEmpty(),
                "Expected empty results for non-matching text '$searchText' " +
                    "but got ${results.size} results"
            )
        }
    }

    @Test
    @DisplayName("Search on empty document store returns empty list without exception")
    fun `search on empty document store returns empty list without exception`() = runTest {
        val arbRandomTags: Arb<List<String>> = arbitrary {
            val count = Arb.int(1..5).bind()
            val allTags = storeTagPool + nonMatchingTagPool
            allTags.shuffled().take(count)
        }

        checkAll(100, arbRandomTags) { searchTags ->
            // Ensure document store is empty
            documentStore.clear()

            // Search by tags on an empty store
            val tagResults = searchEngine.searchByTags(memberId, searchTags)

            assertEquals(
                emptyList(),
                tagResults,
                "Search on empty store must return empty list but got ${tagResults.size} results"
            )
        }
    }

    @Test
    @DisplayName("Combined tag + free-text search with no matches returns empty list")
    fun `combined tag and free-text search with no matches returns empty list`() = runTest {
        checkAll(100, arbDocumentList, arbNonMatchingTags, arbNonMatchingFreeText) { documents, searchTags, searchText ->
            documentStore.clear()
            documentStore.addAll(documents)

            // Perform a combined search using both non-matching tags and non-matching free text
            val query = SearchQuery(tags = searchTags, freeText = searchText)
            val results = documentRepository.search(memberId, query)

            assertTrue(
                results.isEmpty(),
                "Expected empty results for combined non-matching search " +
                    "(tags=$searchTags, text='$searchText') but got ${results.size} results"
            )
        }
    }
}
