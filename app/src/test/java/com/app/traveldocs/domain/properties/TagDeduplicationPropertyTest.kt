package com.app.traveldocs.domain.properties

import com.app.traveldocs.data.local.TagRepositoryImpl
import com.app.traveldocs.data.local.dao.DocumentTagDao
import com.app.traveldocs.data.local.entity.DocumentTagEntity
import com.app.traveldocs.domain.model.Tag
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
 * Property 15: Tag deduplication (idempotence)
 *
 * For any document and any tag string T, adding T multiple times should result
 * in exactly one occurrence of T in the document's tag list.
 *
 * **Validates: Requirements 6.4**
 */
@DisplayName("Property 15: Tag deduplication (idempotence)")
@JUnitTag("Feature: travel-document-manager, Property 15: Tag deduplication (idempotence)")
class TagDeduplicationPropertyTest {

    private lateinit var documentTagDao: DocumentTagDao
    private lateinit var repository: TagRepositoryImpl

    // In-memory store simulating the database
    private val tagStore = mutableListOf<DocumentTagEntity>()

    private val testDocumentId = "doc-test-123"

    @BeforeEach
    fun setUp() {
        tagStore.clear()

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

        repository = TagRepositoryImpl(documentTagDao)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    // Generator for valid tag names (non-empty alphanumeric strings)
    private val arbTagName: Arb<String> = Arb.string(minSize = 1, maxSize = 30)

    // Generator for how many times to add the same tag (at least 2 to test deduplication)
    private val arbRepeatCount: Arb<Int> = Arb.int(2..10)

    @Test
    @DisplayName("Adding the same tag multiple times results in exactly one occurrence")
    fun `adding same tag multiple times results in exactly one occurrence`() = runTest {
        checkAll(100, arbTagName, arbRepeatCount) { tagName, repeatCount ->
            // Reset state for each iteration
            tagStore.clear()

            // Add the same tag multiple times
            repeat(repeatCount) {
                val result = repository.addTag(testDocumentId, tagName)
                assertTrue(result.isSuccess, "addTag should succeed")
            }

            // Verify only one occurrence exists
            val tags = repository.getTagsForDocument(testDocumentId)
            val occurrences = tags.count { it.name == tagName }

            assertEquals(
                1,
                occurrences,
                "Tag '$tagName' added $repeatCount times should have exactly 1 occurrence, but found $occurrences"
            )
        }
    }

    @Test
    @DisplayName("Adding duplicate tag does not increase total tag count")
    fun `adding duplicate tag does not increase total tag count`() = runTest {
        checkAll(100, arbTagName, arbRepeatCount) { tagName, repeatCount ->
            // Reset state for each iteration
            tagStore.clear()

            // Add tag once and record count
            repository.addTag(testDocumentId, tagName)
            val countAfterFirst = repository.getTagsForDocument(testDocumentId).size

            // Add the same tag additional times
            repeat(repeatCount - 1) {
                repository.addTag(testDocumentId, tagName)
            }

            // Count should remain the same
            val countAfterAll = repository.getTagsForDocument(testDocumentId).size

            assertEquals(
                countAfterFirst,
                countAfterAll,
                "Tag count should not increase when adding duplicate tag '$tagName'"
            )
        }
    }

    @Test
    @DisplayName("Multiple distinct tags each appear exactly once even when added multiple times")
    fun `multiple distinct tags each appear exactly once when added multiple times`() = runTest {
        // Generator for a list of distinct tag names
        val arbDistinctTags: Arb<List<String>> = arbitrary {
            val count = Arb.int(2..10).bind()
            (1..count).map { "tag-$it-${Arb.string(minSize = 1, maxSize = 10).bind()}" }
        }

        checkAll(100, arbDistinctTags, arbRepeatCount) { tagNames, repeatCount ->
            // Reset state for each iteration
            tagStore.clear()

            // Add each tag multiple times
            for (tagName in tagNames) {
                repeat(repeatCount) {
                    repository.addTag(testDocumentId, tagName)
                }
            }

            // Verify each tag appears exactly once
            val tags = repository.getTagsForDocument(testDocumentId)

            for (tagName in tagNames) {
                val occurrences = tags.count { it.name == tagName }
                assertEquals(
                    1,
                    occurrences,
                    "Tag '$tagName' should appear exactly once but found $occurrences"
                )
            }

            // Total count should equal number of distinct tag names
            assertEquals(
                tagNames.size,
                tags.size,
                "Total tag count (${tags.size}) should equal number of distinct tags (${tagNames.size})"
            )
        }
    }
}
