package com.app.traveldocs.domain.properties

import androidx.room.withTransaction
import com.app.traveldocs.data.local.DocumentRepositoryImpl
import com.app.traveldocs.data.local.TravelDocsDatabase
import com.app.traveldocs.data.local.dao.DocumentDao
import com.app.traveldocs.data.local.dao.DocumentMetadataDao
import com.app.traveldocs.data.local.dao.DocumentTagDao
import com.app.traveldocs.data.local.entity.DocumentEntity
import com.app.traveldocs.data.local.entity.DocumentMetadataEntity
import com.app.traveldocs.data.local.entity.DocumentTagEntity
import com.app.traveldocs.domain.model.Document
import com.app.traveldocs.domain.model.DocumentFormat
import com.app.traveldocs.domain.model.DocumentType
import com.app.traveldocs.domain.model.MetadataField
import com.app.traveldocs.domain.model.SearchQuery
import com.app.traveldocs.domain.model.Tag
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.uuid
import io.kotest.property.checkAll
import io.mockk.captureLambda
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag as JUnitTag
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Property 25: Member document isolation
 *
 * For any two distinct family members A and B, documents stored by member A should never
 * appear in query results for member B, and vice versa.
 *
 * **Validates: Requirements 10.7**
 */
@DisplayName("Property 25: Member document isolation")
@JUnitTag("Feature: travel-document-manager, Property 25: Member document isolation")
class MemberDocumentIsolationPropertyTest {

    private lateinit var database: TravelDocsDatabase
    private lateinit var documentDao: DocumentDao
    private lateinit var documentMetadataDao: DocumentMetadataDao
    private lateinit var documentTagDao: DocumentTagDao
    private lateinit var repository: DocumentRepositoryImpl

    // In-memory store simulating the database
    private val documentStore = mutableMapOf<String, DocumentEntity>()
    private val metadataStore = mutableMapOf<String, MutableList<DocumentMetadataEntity>>()
    private val tagStore = mutableMapOf<String, MutableList<DocumentTagEntity>>()

    @BeforeEach
    fun setUp() {
        documentStore.clear()
        metadataStore.clear()
        tagStore.clear()

        database = mockk(relaxed = true)
        documentDao = mockk(relaxed = true)
        documentMetadataDao = mockk(relaxed = true)
        documentTagDao = mockk(relaxed = true)

        // Mock Room's withTransaction extension function
        mockkStatic("androidx.room.RoomDatabaseKt")
        coEvery { database.withTransaction<Any>(captureLambda()) } coAnswers {
            lambda<suspend () -> Any>().invoke()
        }

        // Mock DocumentDao to use in-memory store
        coEvery { documentDao.insert(any()) } coAnswers {
            val entity = firstArg<DocumentEntity>()
            documentStore[entity.id] = entity
        }

        coEvery { documentDao.delete(any()) } coAnswers {
            val id = firstArg<String>()
            documentStore.remove(id)
        }

        coEvery { documentDao.getById(any()) } coAnswers {
            val id = firstArg<String>()
            documentStore[id]
        }

        coEvery { documentDao.getAllByMemberId(any()) } coAnswers {
            val memberId = firstArg<String>()
            flowOf(documentStore.values.filter { it.memberId == memberId })
        }

        coEvery { documentDao.getCount(any()) } coAnswers {
            val memberId = firstArg<String>()
            documentStore.values.count { it.memberId == memberId }
        }

        coEvery { documentDao.searchByTags(any(), any(), any()) } coAnswers {
            val memberId = firstArg<String>()
            val tags = secondArg<List<String>>()
            documentStore.values.filter { entity ->
                entity.memberId == memberId &&
                    tagStore[entity.id]?.map { it.tag }?.let { docTags ->
                        tags.all { tag -> tag in docTags }
                    } == true
            }
        }

        coEvery { documentDao.searchByFreeText(any(), any()) } coAnswers {
            val memberId = firstArg<String>()
            val freeText = secondArg<String>()
            documentStore.values.filter { entity ->
                entity.memberId == memberId && (
                    tagStore[entity.id]?.any { it.tag.contains(freeText, ignoreCase = true) } == true ||
                        metadataStore[entity.id]?.any { it.value.contains(freeText, ignoreCase = true) } == true
                    )
            }
        }

        coEvery { documentDao.searchByTagsAndFreeText(any(), any(), any(), any()) } coAnswers {
            val memberId = arg<String>(0)
            val tags = arg<List<String>>(1)
            val freeText = arg<String>(3)
            documentStore.values.filter { entity ->
                entity.memberId == memberId &&
                    tagStore[entity.id]?.map { it.tag }?.let { docTags ->
                        tags.all { tag -> tag in docTags }
                    } == true &&
                    (tagStore[entity.id]?.any { it.tag.contains(freeText, ignoreCase = true) } == true ||
                        metadataStore[entity.id]?.any { it.value.contains(freeText, ignoreCase = true) } == true)
            }
        }

        // Mock DocumentMetadataDao
        coEvery { documentMetadataDao.insertAll(any()) } coAnswers {
            val entities = firstArg<List<DocumentMetadataEntity>>()
            entities.forEach { meta ->
                metadataStore.getOrPut(meta.documentId) { mutableListOf() }.add(meta)
            }
        }

        coEvery { documentMetadataDao.getByDocumentId(any()) } coAnswers {
            val docId = firstArg<String>()
            metadataStore[docId] ?: emptyList()
        }

        coEvery { documentMetadataDao.deleteByDocumentId(any()) } coAnswers {
            val docId = firstArg<String>()
            metadataStore.remove(docId)
        }

        // Mock DocumentTagDao
        coEvery { documentTagDao.insertAll(any()) } coAnswers {
            val entities = firstArg<List<DocumentTagEntity>>()
            entities.forEach { tag ->
                tagStore.getOrPut(tag.documentId) { mutableListOf() }.add(tag)
            }
        }

        coEvery { documentTagDao.getByDocumentId(any()) } coAnswers {
            val docId = firstArg<String>()
            tagStore[docId] ?: emptyList()
        }

        coEvery { documentTagDao.deleteAllForDocument(any()) } coAnswers {
            val docId = firstArg<String>()
            tagStore.remove(docId)
        }

        repository = DocumentRepositoryImpl(
            database = database,
            documentDao = documentDao,
            documentMetadataDao = documentMetadataDao,
            documentTagDao = documentTagDao
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    // -- Custom Arb generators --

    private val arbDocumentType = Arb.element(DocumentType.entries.toList())
    private val arbDocumentFormat = Arb.element(DocumentFormat.entries.toList())
    private val arbMetadataField = Arb.element(MetadataField.entries.toList())

    private val arbTagName: Arb<String> = Arb.string(minSize = 1, maxSize = 20)
        .map { s -> s.replace("\u0000", "a").ifBlank { "tag" } }

    private val arbMetadataValue: Arb<String> = Arb.string(minSize = 1, maxSize = 30)
        .map { s -> s.replace("\u0000", "a").ifBlank { "value" } }

    private val arbTag: Arb<Tag> = arbitrary {
        Tag(
            name = arbTagName.bind(),
            isAutoGenerated = Arb.boolean().bind()
        )
    }

    /**
     * Generates a pair of distinct member IDs.
     */
    private val arbMemberPair: Arb<Pair<String, String>> = arbitrary {
        val idA = Arb.int(1..100).bind()
        var idB = Arb.int(1..100).bind()
        while (idB == idA) {
            idB = Arb.int(1..100).bind()
        }
        "member-$idA" to "member-$idB"
    }

    /**
     * Generates a document assigned to a specific member.
     */
    private fun arbDocumentForMember(memberId: String): Arb<Document> = arbitrary {
        val id = Arb.uuid().bind().toString()
        val type = arbDocumentType.bind()
        val format = arbDocumentFormat.bind()
        val tags = Arb.list(arbTag, 1..5).bind()
            .distinctBy { it.name }
        val metadataKeys = Arb.list(arbMetadataField, 0..3).bind().distinct()
        val metadata = metadataKeys.associateWith { arbMetadataValue.bind() }
        val confidence = Arb.int(0..100).bind() / 100.0f
        val now = Instant.now()

        Document(
            id = id,
            memberId = memberId,
            type = type,
            format = format,
            originalFileName = "file_${id.take(8)}.${format.name.lowercase()}",
            metadata = metadata,
            tags = tags,
            createdAt = now,
            updatedAt = now,
            extractionConfidence = confidence,
            requiresManualReview = confidence < 0.8f
        )
    }

    /**
     * Generates a test scenario with two distinct members, each with a list of documents.
     */
    private val arbIsolationScenario: Arb<Triple<String, String, Pair<List<Document>, List<Document>>>> = arbitrary {
        val (memberA, memberB) = arbMemberPair.bind()
        val docsA = Arb.list(arbDocumentForMember(memberA), 1..10).bind()
        val docsB = Arb.list(arbDocumentForMember(memberB), 1..10).bind()
        Triple(memberA, memberB, docsA to docsB)
    }

    /**
     * Property 25a: Documents stored by member A never appear in getAll() for member B.
     *
     * **Validates: Requirements 10.7**
     */
    @Test
    @DisplayName("Documents stored by member A never appear in getAll() for member B")
    fun `documents stored by member A never appear in getAll for member B`() = runTest {
        checkAll(100, arbIsolationScenario) { (memberA, memberB, docsPair) ->
            val (docsA, docsB) = docsPair

            // Reset store for each iteration
            documentStore.clear()
            metadataStore.clear()
            tagStore.clear()

            // Insert all documents for member A
            docsA.forEach { doc ->
                repository.insert(doc)
            }

            // Insert all documents for member B
            docsB.forEach { doc ->
                repository.insert(doc)
            }

            // getAll for member A should only contain member A's documents
            val allForA = repository.getAll(memberA).first()
            allForA.all { it.memberId == memberA } shouldBe true
            allForA.none { it.memberId == memberB } shouldBe true

            // getAll for member B should only contain member B's documents
            val allForB = repository.getAll(memberB).first()
            allForB.all { it.memberId == memberB } shouldBe true
            allForB.none { it.memberId == memberA } shouldBe true

            // Cross-check: no document ID from A appears in B's results
            val idsA = docsA.map { it.id }.toSet()
            val idsB = docsB.map { it.id }.toSet()
            allForA.none { it.id in idsB } shouldBe true
            allForB.none { it.id in idsA } shouldBe true
        }
    }

    /**
     * Property 25b: Documents stored by member A never appear in search() results for member B.
     *
     * **Validates: Requirements 10.7**
     */
    @Test
    @DisplayName("Documents stored by member A never appear in search() results for member B")
    fun `documents stored by member A never appear in search results for member B`() = runTest {
        checkAll(100, arbIsolationScenario) { (memberA, memberB, docsPair) ->
            val (docsA, docsB) = docsPair

            // Reset store for each iteration
            documentStore.clear()
            metadataStore.clear()
            tagStore.clear()

            // Insert all documents for both members
            docsA.forEach { doc -> repository.insert(doc) }
            docsB.forEach { doc -> repository.insert(doc) }

            // Search using member A's tags against member B's collection
            val tagsFromA = docsA.flatMap { it.tags }.map { it.name }.distinct().take(3)
            if (tagsFromA.isNotEmpty()) {
                val tagSearchResults = repository.search(memberB, SearchQuery(tags = tagsFromA))
                tagSearchResults.none { it.memberId == memberA } shouldBe true

                // Also search member A's collection - should only return A's docs
                val tagSearchForA = repository.search(memberA, SearchQuery(tags = tagsFromA))
                tagSearchForA.all { it.memberId == memberA } shouldBe true
            }

            // Search using member A's metadata values against member B's collection
            val metadataValueFromA = docsA.flatMap { it.metadata.values }.firstOrNull()
            if (metadataValueFromA != null) {
                val freeTextResults = repository.search(memberB, SearchQuery(freeText = metadataValueFromA))
                freeTextResults.none { it.memberId == memberA } shouldBe true
            }

            // Search using member B's tags against member A's collection
            val tagsFromB = docsB.flatMap { it.tags }.map { it.name }.distinct().take(3)
            if (tagsFromB.isNotEmpty()) {
                val tagSearchResults = repository.search(memberA, SearchQuery(tags = tagsFromB))
                tagSearchResults.none { it.memberId == memberB } shouldBe true
            }
        }
    }

    /**
     * Property 25c: getById() for member A's document returns it, but member B's getAll() doesn't include it.
     *
     * **Validates: Requirements 10.7**
     */
    @Test
    @DisplayName("getById returns member A's document but it never appears in member B's getAll")
    fun `getById returns member A document but it does not appear in member B getAll`() = runTest {
        checkAll(100, arbIsolationScenario) { (memberA, memberB, docsPair) ->
            val (docsA, docsB) = docsPair

            // Reset store for each iteration
            documentStore.clear()
            metadataStore.clear()
            tagStore.clear()

            // Insert all documents for both members
            docsA.forEach { doc -> repository.insert(doc) }
            docsB.forEach { doc -> repository.insert(doc) }

            // For each document of member A, verify it can be retrieved by ID
            // but does NOT appear in member B's getAll results
            val allForB = repository.getAll(memberB).first()
            val bDocIds = allForB.map { it.id }.toSet()

            docsA.forEach { docA ->
                val retrieved = repository.getById(docA.id)
                retrieved?.id shouldBe docA.id
                retrieved?.memberId shouldBe memberA

                // Member A's document must not be in member B's listing
                (docA.id in bDocIds) shouldBe false
            }

            // Symmetrically, member B's documents should not appear in member A's getAll
            val allForA = repository.getAll(memberA).first()
            val aDocIds = allForA.map { it.id }.toSet()

            docsB.forEach { docB ->
                val retrieved = repository.getById(docB.id)
                retrieved?.id shouldBe docB.id
                retrieved?.memberId shouldBe memberB

                // Member B's document must not be in member A's listing
                (docB.id in aDocIds) shouldBe false
            }
        }
    }
}
