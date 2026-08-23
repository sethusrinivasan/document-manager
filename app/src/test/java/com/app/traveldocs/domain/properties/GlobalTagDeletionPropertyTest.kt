package com.app.traveldocs.domain.properties

import com.app.traveldocs.data.local.TagRepositoryImpl
import com.app.traveldocs.data.local.dao.DocumentTagDao
import com.app.traveldocs.data.local.entity.DocumentTagEntity
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag as JUnitTag
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Property 16: Global tag deletion removes from all documents
 *
 * For any tag T deleted globally for a member, no document belonging to that member
 * should contain T in its tag list afterward.
 *
 * **Validates: Requirements 6.6**
 */
@DisplayName("Property 16: Global tag deletion removes from all documents")
@JUnitTag("Feature: travel-document-manager, Property 16: Global tag deletion removes from all documents")
class GlobalTagDeletionPropertyTest {

    private lateinit var documentTagDao: DocumentTagDao
    private lateinit var repository: TagRepositoryImpl

    // In-memory store simulating the database
    private val tagStore = mutableListOf<DocumentTagEntity>()

    // Simulated documents table (needed for deleteTagGlobally JOIN)
    private data class SimulatedDocument(val id: String, val memberId: String)
    private val documentStore = mutableListOf<SimulatedDocument>()

    @BeforeEach
    fun setUp() {
        tagStore.clear()
        documentStore.clear()

        documentTagDao = mockk(relaxed = true)

        // Mock insert: add to store only if not already present (simulates OnConflictStrategy.IGNORE)
        coEvery { documentTagDao.insert(any()) } coAnswers {
            val entity = firstArg<DocumentTagEntity>()
            val exists = tagStore.any { it.documentId == entity.documentId && it.tag == entity.tag }
            if (!exists) {
                tagStore.add(entity)
            }
        }

        // Mock getByDocumentId: return tags for given document from store
        coEvery { documentTagDao.getByDocumentId(any()) } coAnswers {
            val docId = firstArg<String>()
            tagStore.filter { it.documentId == docId }
        }

        // Mock getCount: return count for given document from store
        coEvery { documentTagDao.getCount(any()) } coAnswers {
            val docId = firstArg<String>()
            tagStore.count { it.documentId == docId }
        }

        // Mock deleteTagGlobally: remove tag from all documents belonging to a member
        coEvery { documentTagDao.deleteTagGlobally(any(), any()) } coAnswers {
            val memberId = firstArg<String>()
            val tag = secondArg<String>()
            // Simulates: DELETE FROM document_tags WHERE tag = :tag
            //            AND documentId IN (SELECT id FROM documents WHERE memberId = :memberId)
            val memberDocIds = documentStore.filter { it.memberId == memberId }.map { it.id }.toSet()
            tagStore.removeAll { it.tag == tag && it.documentId in memberDocIds }
            Unit
        }

        // Mock getAllTagsForMember: return all tags for documents belonging to a member
        coEvery { documentTagDao.getAllTagsForMember(any()) } coAnswers {
            val memberId = firstArg<String>()
            val memberDocIds = documentStore.filter { it.memberId == memberId }.map { it.id }.toSet()
            tagStore.filter { it.documentId in memberDocIds }
        }

        repository = TagRepositoryImpl(documentTagDao)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    // Generator for member IDs
    private val arbMemberId: Arb<String> = arbitrary {
        "member-${Arb.string(minSize = 3, maxSize = 8).bind()}"
    }

    // Generator for a tag name
    private val arbTagName: Arb<String> = Arb.string(minSize = 1, maxSize = 20)

    // Generator for number of documents
    private val arbDocCount: Arb<Int> = Arb.int(2..8)

    /**
     * Helper to set up documents and tags for a member.
     * Creates multiple documents, all sharing a common tag plus some unique tags.
     */
    private fun setUpMemberDocumentsWithSharedTag(
        memberId: String,
        docCount: Int,
        sharedTag: String
    ): List<String> {
        val docIds = (1..docCount).map { "$memberId-doc-$it" }

        // Register documents
        for (docId in docIds) {
            documentStore.add(SimulatedDocument(id = docId, memberId = memberId))
        }

        // Add the shared tag to all documents
        for (docId in docIds) {
            tagStore.add(
                DocumentTagEntity(
                    documentId = docId,
                    tag = sharedTag,
                    isAutoGenerated = false,
                    createdAt = System.currentTimeMillis()
                )
            )
        }

        // Add some unique tags to each document so they're not empty after deletion
        for ((index, docId) in docIds.withIndex()) {
            tagStore.add(
                DocumentTagEntity(
                    documentId = docId,
                    tag = "unique-tag-$index",
                    isAutoGenerated = false,
                    createdAt = System.currentTimeMillis()
                )
            )
        }

        return docIds
    }

    @Test
    @DisplayName("After global tag deletion, no member document contains the deleted tag")
    fun `global tag deletion removes tag from all member documents`() = runTest {
        checkAll(100, arbMemberId, arbTagName, arbDocCount) { memberId, tagName, docCount ->
            // Reset stores for each iteration
            tagStore.clear()
            documentStore.clear()

            // Set up documents with the shared tag
            val docIds = setUpMemberDocumentsWithSharedTag(memberId, docCount, tagName)

            // Verify the tag exists on all documents before deletion
            for (docId in docIds) {
                val tags = repository.getTagsForDocument(docId)
                assertTrue(
                    tags.any { it.name == tagName },
                    "Tag '$tagName' should exist on document '$docId' before global deletion"
                )
            }

            // Perform global tag deletion
            val result = repository.deleteTagGlobally(memberId, tagName)
            assertTrue(result.isSuccess, "deleteTagGlobally should succeed")

            // Verify no document belonging to this member still has the tag
            for (docId in docIds) {
                val tagsAfter = repository.getTagsForDocument(docId)
                val tagNames = tagsAfter.map { it.name }
                assertTrue(
                    tagName !in tagNames,
                    "Tag '$tagName' should not exist on document '$docId' after global deletion"
                )
            }
        }
    }

    @Test
    @DisplayName("Global tag deletion does not affect other member's documents")
    fun `global tag deletion does not affect other members`() = runTest {
        checkAll(100, arbMemberId, arbTagName, arbDocCount) { memberId, tagName, docCount ->
            // Reset stores for each iteration
            tagStore.clear()
            documentStore.clear()

            // Set up documents for the target member
            setUpMemberDocumentsWithSharedTag(memberId, docCount, tagName)

            // Set up documents for a different member with the same tag
            val otherMemberId = "other-$memberId"
            val otherDocIds = setUpMemberDocumentsWithSharedTag(otherMemberId, docCount, tagName)

            // Delete the tag globally for the target member only
            val result = repository.deleteTagGlobally(memberId, tagName)
            assertTrue(result.isSuccess, "deleteTagGlobally should succeed")

            // Verify the other member's documents still have the tag
            for (docId in otherDocIds) {
                val tagsAfter = repository.getTagsForDocument(docId)
                val tagNames = tagsAfter.map { it.name }
                assertTrue(
                    tagName in tagNames,
                    "Tag '$tagName' on other member's document '$docId' should NOT be affected by global deletion"
                )
            }
        }
    }

    @Test
    @DisplayName("Global tag deletion preserves other tags on the member's documents")
    fun `global tag deletion preserves other tags`() = runTest {
        checkAll(100, arbMemberId, arbTagName, arbDocCount) { memberId, tagName, docCount ->
            // Reset stores for each iteration
            tagStore.clear()
            documentStore.clear()

            // Set up documents with the shared tag
            val docIds = setUpMemberDocumentsWithSharedTag(memberId, docCount, tagName)

            // Record the non-target tags for each document before deletion
            val expectedTagsByDoc = docIds.associateWith { docId ->
                repository.getTagsForDocument(docId)
                    .map { it.name }
                    .filter { it != tagName }
                    .sorted()
            }

            // Perform global tag deletion
            val result = repository.deleteTagGlobally(memberId, tagName)
            assertTrue(result.isSuccess, "deleteTagGlobally should succeed")

            // Verify other tags are preserved
            for (docId in docIds) {
                val remainingTags = repository.getTagsForDocument(docId).map { it.name }.sorted()
                assertEquals(
                    expectedTagsByDoc[docId],
                    remainingTags,
                    "Other tags on document '$docId' should be preserved after global deletion of '$tagName'"
                )
            }
        }
    }
}
