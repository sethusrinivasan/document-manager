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
import io.kotest.matchers.nulls.shouldBeNull
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
 * Property 2: Deleted documents are non-retrievable
 *
 * For any document that has been successfully deleted, subsequent queries
 * (by ID, by search, by listing) should never return that document.
 *
 * **Validates: Requirements 1.5**
 */
@DisplayName("Property 2: Deleted documents are non-retrievable")
@JUnitTag("Feature: travel-document-manager, Property 2: Deleted documents are non-retrievable")
class DeletedDocumentNonRetrievabilityPropertyTest {

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
            val tagCount = thirdArg<Int>()
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
            val tagCount = arg<Int>(2)
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

    private val arbDocument: Arb<Document> = arbitrary {
        val id = Arb.uuid().bind().toString()
        val memberId = "member-${Arb.int(1..5).bind()}"
        val type = arbDocumentType.bind()
        val format = arbDocumentFormat.bind()
        val tags = Arb.list(arbTag, 0..5).bind()
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
     * Property 2a: For any document that is inserted and then deleted, getById returns null.
     *
     * **Validates: Requirements 1.5**
     */
    @Test
    fun `deleted document is not returned by getById`() = runTest {
        checkAll(100, arbDocument) { document ->
            // Reset store for each iteration
            documentStore.clear()
            metadataStore.clear()
            tagStore.clear()

            // Insert the document
            val insertResult = repository.insert(document)
            insertResult.isSuccess shouldBe true

            // Verify it exists before delete
            val beforeDelete = repository.getById(document.id)
            beforeDelete?.id shouldBe document.id

            // Delete the document
            val deleteResult = repository.delete(document.id)
            deleteResult.isSuccess shouldBe true

            // Verify getById returns null after deletion
            val afterDelete = repository.getById(document.id)
            afterDelete.shouldBeNull()
        }
    }

    /**
     * Property 2b: For any document that is deleted, it does not appear in getAll() results.
     *
     * **Validates: Requirements 1.5**
     */
    @Test
    fun `deleted document does not appear in getAll results`() = runTest {
        checkAll(100, arbDocument) { document ->
            // Reset store for each iteration
            documentStore.clear()
            metadataStore.clear()
            tagStore.clear()

            // Insert the document
            val insertResult = repository.insert(document)
            insertResult.isSuccess shouldBe true

            // Delete the document
            val deleteResult = repository.delete(document.id)
            deleteResult.isSuccess shouldBe true

            // Verify it does not appear in getAll results
            val allDocuments = repository.getAll(document.memberId).first()
            allDocuments.none { it.id == document.id } shouldBe true
        }
    }

    /**
     * Property 2c: For any document that is deleted, search() never returns it.
     *
     * **Validates: Requirements 1.5**
     */
    @Test
    fun `deleted document is never returned by search`() = runTest {
        checkAll(100, arbDocument) { document ->
            // Reset store for each iteration
            documentStore.clear()
            metadataStore.clear()
            tagStore.clear()

            // Insert the document
            val insertResult = repository.insert(document)
            insertResult.isSuccess shouldBe true

            // Delete the document
            val deleteResult = repository.delete(document.id)
            deleteResult.isSuccess shouldBe true

            // Search by tags (if the document had tags)
            if (document.tags.isNotEmpty()) {
                val tagQuery = SearchQuery(tags = document.tags.map { it.name })
                val tagResults = repository.search(document.memberId, tagQuery)
                tagResults.none { it.id == document.id } shouldBe true
            }

            // Search by free text using a metadata value (if the document had metadata)
            val metadataValue = document.metadata.values.firstOrNull()
            if (metadataValue != null) {
                val freeTextQuery = SearchQuery(freeText = metadataValue)
                val freeTextResults = repository.search(document.memberId, freeTextQuery)
                freeTextResults.none { it.id == document.id } shouldBe true
            }

            // Search by tags + freeText combined
            if (document.tags.isNotEmpty() && metadataValue != null) {
                val combinedQuery = SearchQuery(
                    tags = document.tags.map { it.name },
                    freeText = metadataValue
                )
                val combinedResults = repository.search(document.memberId, combinedQuery)
                combinedResults.none { it.id == document.id } shouldBe true
            }
        }
    }
}
