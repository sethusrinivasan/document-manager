package com.app.traveldocs.data.local.storage

import android.content.Context
import com.app.traveldocs.domain.model.DocumentFormat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Unit tests for [DocumentFileStorageImpl].
 *
 * Since DocumentFileStorageImpl uses Android KeyStore for encryption,
 * these tests use a testable subclass that replaces the Android-specific
 * crypto with a test-friendly implementation while preserving all other logic.
 */
class DocumentFileStorageImplTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var context: Context
    private lateinit var storage: TestableDocumentFileStorage

    @BeforeEach
    fun setup() {
        context = mockk(relaxed = true)
        every { context.filesDir } returns tempDir
        storage = TestableDocumentFileStorage(context)
    }

    @Test
    fun `store creates encrypted file in member directory`() = runTest {
        val memberId = "member-1"
        val fileData = "Hello, World!".toByteArray()

        val result = storage.store(memberId, fileData, DocumentFormat.PDF)

        assertTrue(result.isSuccess)
        val fileId = result.getOrThrow()
        assertNotNull(fileId)

        // Verify file exists in the correct directory
        val memberDir = File(tempDir, "docs${File.separator}$memberId")
        assertTrue(memberDir.exists())
        assertTrue(memberDir.isDirectory)

        val encFile = File(memberDir, "$fileId.enc")
        assertTrue(encFile.exists())
        // Encrypted data should not equal plain data
        assertFalse(encFile.readBytes().contentEquals(fileData))
    }

    @Test
    fun `retrieve decrypts file correctly`() = runTest {
        val memberId = "member-1"
        val originalData = "Passport document content".toByteArray()

        val storeResult = storage.store(memberId, originalData, DocumentFormat.PDF)
        assertTrue(storeResult.isSuccess)
        val fileId = storeResult.getOrThrow()

        val retrieveResult = storage.retrieve(fileId)
        assertTrue(retrieveResult.isSuccess)
        assertTrue(retrieveResult.getOrThrow().contentEquals(originalData))
    }

    @Test
    fun `retrieve returns failure for non-existent file`() = runTest {
        val result = storage.retrieve("non-existent-id")

        assertTrue(result.isFailure)
    }

    @Test
    fun `secureDelete overwrites and removes file`() = runTest {
        val memberId = "member-1"
        val fileData = "Sensitive data".toByteArray()

        val storeResult = storage.store(memberId, fileData, DocumentFormat.JPG)
        val fileId = storeResult.getOrThrow()

        // Verify file exists
        val memberDir = File(tempDir, "docs${File.separator}$memberId")
        val encFile = File(memberDir, "$fileId.enc")
        assertTrue(encFile.exists())

        val deleteResult = storage.secureDelete(fileId)
        assertTrue(deleteResult.isSuccess)

        // File should no longer exist
        assertFalse(encFile.exists())
    }

    @Test
    fun `secureDelete returns failure for non-existent file`() = runTest {
        val result = storage.secureDelete("non-existent-id")

        assertTrue(result.isFailure)
    }

    @Test
    fun `store creates unique file IDs`() = runTest {
        val memberId = "member-1"
        val fileData = "Document content".toByteArray()

        val result1 = storage.store(memberId, fileData, DocumentFormat.PDF)
        val result2 = storage.store(memberId, fileData, DocumentFormat.PDF)

        assertTrue(result1.isSuccess)
        assertTrue(result2.isSuccess)

        val id1 = result1.getOrThrow()
        val id2 = result2.getOrThrow()
        assertFalse(id1 == id2)
    }

    @Test
    fun `store creates member directory if it does not exist`() = runTest {
        val memberId = "new-member"
        val memberDir = File(tempDir, "docs${File.separator}$memberId")
        assertFalse(memberDir.exists())

        val result = storage.store(memberId, "data".toByteArray(), DocumentFormat.PNG)

        assertTrue(result.isSuccess)
        assertTrue(memberDir.exists())
    }

    @Test
    fun `store and retrieve works for all document formats`() = runTest {
        val memberId = "member-1"

        for (format in DocumentFormat.entries) {
            val data = "Content for $format".toByteArray()
            val storeResult = storage.store(memberId, data, format)
            assertTrue(storeResult.isSuccess)

            val retrieveResult = storage.retrieve(storeResult.getOrThrow())
            assertTrue(retrieveResult.isSuccess)
            assertTrue(retrieveResult.getOrThrow().contentEquals(data))
        }
    }

    @Test
    fun `store and retrieve works for empty data`() = runTest {
        val memberId = "member-1"
        val emptyData = ByteArray(0)

        val storeResult = storage.store(memberId, emptyData, DocumentFormat.PDF)
        assertTrue(storeResult.isSuccess)

        val retrieveResult = storage.retrieve(storeResult.getOrThrow())
        assertTrue(retrieveResult.isSuccess)
        assertEquals(0, retrieveResult.getOrThrow().size)
    }

    @Test
    fun `store and retrieve works for large data`() = runTest {
        val memberId = "member-1"
        val largeData = ByteArray(1_000_000) { (it % 256).toByte() }

        val storeResult = storage.store(memberId, largeData, DocumentFormat.PDF)
        assertTrue(storeResult.isSuccess)

        val retrieveResult = storage.retrieve(storeResult.getOrThrow())
        assertTrue(retrieveResult.isSuccess)
        assertTrue(retrieveResult.getOrThrow().contentEquals(largeData))
    }

    @Test
    fun `secureDelete makes file non-retrievable`() = runTest {
        val memberId = "member-1"
        val fileData = "Secret".toByteArray()

        val storeResult = storage.store(memberId, fileData, DocumentFormat.PDF)
        val fileId = storeResult.getOrThrow()

        storage.secureDelete(fileId)

        val retrieveResult = storage.retrieve(fileId)
        assertTrue(retrieveResult.isFailure)
    }

    @Test
    fun `files from different members are stored in separate directories`() = runTest {
        val member1 = "member-1"
        val member2 = "member-2"
        val data = "Document".toByteArray()

        val id1 = storage.store(member1, data, DocumentFormat.PDF).getOrThrow()
        val id2 = storage.store(member2, data, DocumentFormat.PDF).getOrThrow()

        val dir1 = File(tempDir, "docs${File.separator}$member1")
        val dir2 = File(tempDir, "docs${File.separator}$member2")

        assertTrue(File(dir1, "$id1.enc").exists())
        assertTrue(File(dir2, "$id2.enc").exists())
        assertFalse(File(dir1, "$id2.enc").exists())
        assertFalse(File(dir2, "$id1.enc").exists())
    }
}
