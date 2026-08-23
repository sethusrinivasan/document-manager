package com.app.traveldocs.domain.properties

import androidx.room.withTransaction
import com.app.traveldocs.data.local.DocumentRepositoryImpl
import com.app.traveldocs.data.local.TravelDocsDatabase
import com.app.traveldocs.data.local.dao.DocumentDao
import com.app.traveldocs.data.local.dao.DocumentMetadataDao
import com.app.traveldocs.data.local.dao.DocumentTagDao
import com.app.traveldocs.data.local.entity.DocumentEntity
import com.app.traveldocs.domain.model.Document
import com.app.traveldocs.domain.model.DocumentFormat
import com.app.traveldocs.domain.model.DocumentType
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import io.mockk.captureLambda
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag as JUnitTag
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals

/**
 * Property 1: Document index consistency
 *
 * For any sequence of insert and delete operations on a member's document collection,
 * the count returned by getCount() should always equal the number of successful inserts
 * minus the number of successful deletes.
 *
 * **Validates: Requirements 1.2**
 */
@DisplayName("Property 1: Document index consistency")
@JUnitTag("Feature: travel-document-manager, Property 1: Document index consistency")
class DocumentIndexConsistencyPropertyTest {

    private lateinit var database: TravelDocsDatabase
    private lateinit var documentDao: DocumentDao
    private lateinit var documentMetadataDao: DocumentMetadataDao
    private lateinit var documentTagDao: DocumentTagDao
    private lateinit var repository: DocumentRepositoryImpl

    // In-memory store to simulate the database
    private val documentStore = mutableMapOf<String, DocumentEntity>()

    private val testMemberId = "member-test"

    @BeforeEach
    fun setUp() {
        documentStore.clear()

        database = mockk(relaxed = true)
        documentDao = mockk(relaxed = true)
        documentMetadataDao = mockk(relaxed = true)
        documentTagDao = mockk(relaxed = true)

        mockkStatic("androidx.room.RoomDatabaseKt")
        coEvery { database.withTransaction<Any>(captureLambda()) } coAnswers {
            lambda<suspend () -> Any>().invoke()
        }

        // Mock insert to add to in-memory store
        coEvery { documentDao.insert(any()) } coAnswers {
            val entity = firstArg<DocumentEntity>()
            documentStore[entity.id] = entity
        }

        // Mock delete to remove from in-memory store
        coEvery { documentDao.delete(any()) } coAnswers {
            val docId = firstArg<String>()
            documentStore.remove(docId)
            Unit
        }

        // Mock getCount to return current store size for the member
        coEvery { documentDao.getCount(any()) } coAnswers {
            val memberId = firstArg<String>()
            documentStore.values.count { it.memberId == memberId }
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

    // Sealed class representing document operations
    sealed class DocOperation {
        data class Insert(val document: Document) : DocOperation()
        data class Delete(val documentId: String) : DocOperation()
    }

    // Generator for a Document with a given id
    private fun arbDocument(id: String): Document {
        return Document(
            id = id,
            memberId = testMemberId,
            type = DocumentType.PASSPORT,
            format = DocumentFormat.PDF,
            originalFileName = "doc_$id.pdf",
            metadata = emptyMap(),
            tags = emptyList(),
            createdAt = Instant.ofEpochMilli(1700000000000L),
            updatedAt = Instant.ofEpochMilli(1700000000000L),
            extractionConfidence = 0.95f,
            requiresManualReview = false
        )
    }

    // Generator for a sequence of operations
    private val arbOperations: Arb<List<DocOperation>> = arbitrary { rs ->
        val size = Arb.int(1..50).bind()
        var nextId = 0
        val insertedIds = mutableListOf<String>()
        val operations = mutableListOf<DocOperation>()

        repeat(size) {
            // 70% chance of insert, 30% chance of delete (if there are inserted docs)
            val shouldInsert = insertedIds.isEmpty() || rs.random.nextDouble() < 0.7
            if (shouldInsert) {
                val id = "doc-${nextId++}"
                val doc = arbDocument(id)
                operations.add(DocOperation.Insert(doc))
                insertedIds.add(id)
            } else {
                // Pick a random inserted doc to delete
                val idx = rs.random.nextInt(insertedIds.size)
                val id = insertedIds.removeAt(idx)
                operations.add(DocOperation.Delete(id))
            }
        }
        operations
    }

    @Test
    @DisplayName("getCount equals successful inserts minus successful deletes for any operation sequence")
    fun `getCount equals successful inserts minus successful deletes`() = runTest {
        checkAll(100, arbOperations) { operations ->
            // Reset state for each test case
            documentStore.clear()

            var expectedCount = 0

            for (op in operations) {
                when (op) {
                    is DocOperation.Insert -> {
                        val result = repository.insert(op.document)
                        if (result.isSuccess) {
                            expectedCount++
                        }
                        // If capacity limit reached (100), insert fails and count doesn't change
                    }
                    is DocOperation.Delete -> {
                        val result = repository.delete(op.documentId)
                        if (result.isSuccess) {
                            expectedCount--
                        }
                    }
                }
            }

            val actualCount = repository.getCount(testMemberId)
            assertEquals(
                expectedCount,
                actualCount,
                "After ${operations.size} operations, expected count=$expectedCount but got $actualCount"
            )
        }
    }

    @Test
    @DisplayName("inserts beyond 100-document capacity fail and do not increment count")
    fun `inserts beyond capacity limit do not increment count`() = runTest {
        checkAll(100, Arb.int(100..120)) { totalInserts ->
            // Reset state
            documentStore.clear()

            var successCount = 0

            for (i in 0 until totalInserts) {
                val doc = arbDocument("cap-doc-$i")
                val result = repository.insert(doc)
                if (result.isSuccess) {
                    successCount++
                }
            }

            val actualCount = repository.getCount(testMemberId)

            // Capacity limit is 100, so at most 100 should succeed
            assertEquals(
                successCount,
                actualCount,
                "Count should equal number of successful inserts (max 100)"
            )
            assert(successCount <= 100) {
                "Successful inserts ($successCount) should not exceed capacity limit of 100"
            }
            assertEquals(minOf(totalInserts, 100), successCount)
        }
    }

    @Test
    @DisplayName("interleaved inserts and deletes maintain consistent count")
    fun `interleaved inserts and deletes maintain consistent count`() = runTest {
        val arbInterleavedOps: Arb<List<DocOperation>> = arbitrary { rs ->
            val insertCount = Arb.int(5..30).bind()
            val deleteCount = Arb.int(1..insertCount).bind()
            val operations = mutableListOf<DocOperation>()
            val insertedIds = mutableListOf<String>()

            // First, insert some documents
            for (i in 0 until insertCount) {
                val id = "interleave-$i"
                operations.add(DocOperation.Insert(arbDocument(id)))
                insertedIds.add(id)
            }

            // Then, randomly delete some
            val toDelete = insertedIds.shuffled(rs.random).take(deleteCount)
            for (id in toDelete) {
                operations.add(DocOperation.Delete(id))
            }

            operations
        }

        checkAll(100, arbInterleavedOps) { operations ->
            documentStore.clear()

            var expectedCount = 0

            for (op in operations) {
                when (op) {
                    is DocOperation.Insert -> {
                        val result = repository.insert(op.document)
                        if (result.isSuccess) {
                            expectedCount++
                        }
                    }
                    is DocOperation.Delete -> {
                        val result = repository.delete(op.documentId)
                        if (result.isSuccess) {
                            expectedCount--
                        }
                    }
                }
            }

            val actualCount = repository.getCount(testMemberId)
            assertEquals(
                expectedCount,
                actualCount,
                "Interleaved ops: expected count=$expectedCount but got $actualCount"
            )
        }
    }
}
