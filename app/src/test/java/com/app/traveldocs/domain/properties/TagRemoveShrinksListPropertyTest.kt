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
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Property 14: Removing a tag shrinks the tag list
 *
 * For any document containing tag T, removing T should decrease the tag count by one
 * and T should not appear in the resulting tag list.
 *
 * **Validates: Requirements 6.3**
 */
@DisplayName("Property Test")
// Removed JUnitTag - invalid syntax
class TagRemoveShrinksListPropertyTest {

    private lateinit var documentTagDao: DocumentTagDao
    private lateinit var repository: TagRepositoryImpl

    // In-memory store simulating the database
    private val tagStore = mutableListOf<DocumentTagEntity>()

    @BeforeEach
    fun setUp() {
        tagStore.clear()

        documentTagDao = mockk(relaxed = true)

        // Mock insert to add to in-memory store
        coEvery { documentTagDao.insert(any()) } coAnswers {
            val entity = firstArg<DocumentTagEntity>()
            val exists = tagStore.any { it.documentId == entity.documentId && it.tag == entity.tag }
            if (!exists) {
                tagStore.add(entity)
            }
        }

        // Mock getByDocumentId to return from in-memory store
        coEvery { documentTagDao.getByDocumentId(any()) } coAnswers {
            val docId = firstArg<String>()
            tagStore.filter { it.documentId == docId }
        }

        // Mock delete to remove from in-memory store
        coEvery { documentTagDao.delete(any(), any()) } coAnswers {
            val docId = firstArg<String>()
            val tag = secondArg<String>()
            tagStore.removeAll { it.documentId == docId && it.tag == tag }
            Unit
        }

        // Mock getCount to return count from in-memory store
        coEvery { documentTagDao.getCount(any()) } coAnswers {
            val docId = firstArg<String>()
            tagStore.count { it.documentId == docId }
        }

        repository = TagRepositoryImpl(documentTagDao)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    // Generator for a document ID
    private val arbDocumentId: Arb<String> = arbitrary {
        "doc-${Arb.string(minSize = 5, maxSize = 10).bind()}"
    }

    // Generator for a document ID and a list of unique tag names
    private val arbDocumentWithTags: Arb<Pair<String, List<String>>> = arbitrary { rs ->
        val docId = arbDocumentId.bind()
        val tagCount = Arb.int(1..15).bind()
        val tags = (1..tagCount).map { "tag-$it-${rs.random.nextInt(1000)}" }.distinct()
        Pair(docId, tags)
    }

    @Test
    @DisplayName("Removing an existing tag decreases the tag count by exactly one")
    fun `removing an existing tag decreases count by one`() = runTest {
        checkAll(100, arbDocumentWithTags) { (documentId, tagNames) ->
            // Reset store for each iteration
            tagStore.clear()

            // Add all tags to the document
            for (tagName in tagNames) {
                repository.addTag(documentId, tagName)
            }

            val initialCount = repository.getTagsForDocument(documentId).size
            assertEquals(tagNames.size, initialCount, "Initial tag count should match added tags")

            // Pick a random tag to remove
            val tagToRemove = tagNames.random()

            // Remove the tag
            val result = repository.removeTag(documentId, tagToRemove)
            assertTrue(result.isSuccess, "removeTag should succeed")

            // Verify count decreased by exactly one
            val newCount = repository.getTagsForDocument(documentId).size
            assertEquals(
                initialCount - 1,
                newCount,
                "After removing '$tagToRemove', count should decrease from $initialCount to ${initialCount - 1}"
            )
        }
    }

    @Test
    @DisplayName("Removed tag is absent from the resulting tag list")
    fun `removed tag is absent from the tag list`() = runTest {
        checkAll(100, arbDocumentWithTags) { (documentId, tagNames) ->
            // Reset store for each iteration
            tagStore.clear()

            // Add all tags to the document
            for (tagName in tagNames) {
                repository.addTag(documentId, tagName)
            }

            // Pick a random tag to remove
            val tagToRemove = tagNames.random()

            // Remove the tag
            repository.removeTag(documentId, tagToRemove)

            // Verify the removed tag is not in the list
            val remainingTags = repository.getTagsForDocument(documentId)
            val remainingTagNames = remainingTags.map { it.name }

            assertFalse(
                remainingTagNames.contains(tagToRemove),
                "Tag '$tagToRemove' should not appear in the tag list after removal"
            )
        }
    }

    @Test
    @DisplayName("Removing a tag preserves all other tags")
    fun `removing a tag preserves all other tags`() = runTest {
        checkAll(100, arbDocumentWithTags) { (documentId, tagNames) ->
            // Reset store for each iteration
            tagStore.clear()

            // Add all tags to the document
            for (tagName in tagNames) {
                repository.addTag(documentId, tagName)
            }

            // Pick a random tag to remove
            val tagToRemove = tagNames.random()
            val expectedRemaining = tagNames.filter { it != tagToRemove }.sorted()

            // Remove the tag
            repository.removeTag(documentId, tagToRemove)

            // Verify all other tags are still present
            val remainingTags = repository.getTagsForDocument(documentId).map { it.name }.sorted()

            assertEquals(
                expectedRemaining,
                remainingTags,
                "All tags except '$tagToRemove' should remain after removal"
            )
        }
    }
}
