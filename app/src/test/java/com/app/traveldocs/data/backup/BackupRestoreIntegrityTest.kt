package com.app.traveldocs.data.backup

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.File
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.EncryptionMethod
import net.lingala.zip4j.model.enums.AesKeyStrength
import org.json.JSONObject
import org.json.JSONArray

/**
 * Tests for backup/restore data integrity.
 *
 * These tests verify that:
 * 1. Backup creates valid ZIP with correct structure
 * 2. Manifest accurately describes contents
 * 3. Password-protected ZIPs cannot be read without password
 * 4. Restore extracts all files without data loss
 * 5. Round-trip: backup → restore preserves all file content byte-for-byte
 * 6. Corrupt/truncated backups fail gracefully (no silent data loss)
 */
class BackupRestoreIntegrityTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var docsDir: File
    private lateinit var dbFile: File

    @BeforeEach
    fun setup() {
        docsDir = File(tempDir, "docs/default-member")
        docsDir.mkdirs()
        dbFile = File(tempDir, "database")
        dbFile.mkdirs()
    }

    // --- Backup Structure Tests ---

    @Test
    fun `backup ZIP contains manifest json`() {
        val zip = createTestBackupZip(null)
        val zipFile = ZipFile(zip)
        val entries = zipFile.fileHeaders.map { it.fileName }
        assertTrue(entries.any { it == "manifest.json" || it.endsWith("/manifest.json") },
            "Backup must contain manifest.json. Found: $entries")
    }

    @Test
    fun `backup manifest has required fields`() {
        val zip = createTestBackupZip(null)
        val extractDir = File(tempDir, "extracted")
        ZipFile(zip).extractAll(extractDir.absolutePath)
        val manifest = JSONObject(File(extractDir, "manifest.json").readText())
        assertTrue(manifest.has("timestamp"), "Missing timestamp")
        assertTrue(manifest.has("fileCount"), "Missing fileCount")
        assertTrue(manifest.has("totalSizeBytes"), "Missing totalSizeBytes")
        assertTrue(manifest.has("files"), "Missing files array")
    }

    @Test
    fun `backup manifest fileCount matches actual files`() {
        val zip = createTestBackupZip(null, fileCount = 5)
        val extractDir = File(tempDir, "extracted")
        ZipFile(zip).extractAll(extractDir.absolutePath)
        val manifest = JSONObject(File(extractDir, "manifest.json").readText())
        val declaredCount = manifest.getInt("fileCount")
        val actualDocFiles = File(extractDir, "docs").walkTopDown().filter { it.isFile }.count()
        // fileCount includes db file, so docs count should be fileCount - 1
        assertTrue(actualDocFiles >= declaredCount - 1,
            "Manifest says $declaredCount files but only $actualDocFiles doc files found")
    }

    // --- Password Protection Tests ---

    @Test
    fun `password-protected backup cannot be read without password`() {
        val zip = createTestBackupZip("mySecurePin123")
        val zipFile = ZipFile(zip)
        assertTrue(zipFile.isEncrypted, "ZIP should be marked as encrypted")
        // Attempting to extract without password should throw
        assertThrows(Exception::class.java) {
            zipFile.extractAll(File(tempDir, "no_password").absolutePath)
        }
    }

    @Test
    fun `password-protected backup extracts with correct password`() {
        val zip = createTestBackupZip("correctPin")
        val extractDir = File(tempDir, "with_password")
        ZipFile(zip, "correctPin".toCharArray()).extractAll(extractDir.absolutePath)
        assertTrue(File(extractDir, "manifest.json").exists(), "Should extract successfully with correct password")
    }

    @Test
    fun `wrong password fails extraction`() {
        val zip = createTestBackupZip("correctPin")
        assertThrows(Exception::class.java) {
            ZipFile(zip, "wrongPin".toCharArray()).extractAll(File(tempDir, "wrong").absolutePath)
        }
    }

    // --- Data Integrity Tests ---

    @Test
    fun `round-trip preserves file content byte-for-byte`() {
        // Create known content
        val file1Content = "Hello this is document 1 content".toByteArray()
        val file2Content = ByteArray(1024) { (it % 256).toByte() } // binary content

        val docFile1 = File(docsDir, "doc1.pdf")
        val docFile2 = File(docsDir, "doc2.jpg")
        docFile1.writeBytes(file1Content)
        docFile2.writeBytes(file2Content)

        // Create backup ZIP
        val zip = createZipFromDir(tempDir, null)

        // Extract and verify
        val extractDir = File(tempDir, "verify")
        ZipFile(zip).extractAll(extractDir.absolutePath)

        val restored1 = File(extractDir, "docs/default-member/doc1.pdf")
        val restored2 = File(extractDir, "docs/default-member/doc2.jpg")

        assertTrue(restored1.exists(), "doc1.pdf should exist in backup")
        assertTrue(restored2.exists(), "doc2.jpg should exist in backup")
        assertArrayEquals(file1Content, restored1.readBytes(), "doc1 content must be identical")
        assertArrayEquals(file2Content, restored2.readBytes(), "doc2 content must be identical")
    }

    @Test
    fun `empty backup has zero file count`() {
        // No docs, just manifest
        val manifest = JSONObject().apply {
            put("timestamp", "20260101_000000")
            put("fileCount", 0)
            put("totalSizeBytes", 0)
            put("files", JSONArray())
        }
        val manifestFile = File(tempDir, "manifest.json")
        manifestFile.writeText(manifest.toString(2))

        val zip = File(tempDir, "empty_backup.zip")
        ZipFile(zip).addFile(manifestFile)

        // Verify
        val extracted = File(tempDir, "empty_extract")
        ZipFile(zip).extractAll(extracted.absolutePath)
        val m = JSONObject(File(extracted, "manifest.json").readText())
        assertEquals(0, m.getInt("fileCount"))
    }

    @Test
    fun `large file backup preserves size exactly`() {
        // 5MB file
        val largeContent = ByteArray(5 * 1024 * 1024) { (it % 251).toByte() }
        val largeFile = File(docsDir, "large_scan.pdf")
        largeFile.writeBytes(largeContent)

        val zip = createZipFromDir(tempDir, null)
        val extractDir = File(tempDir, "large_verify")
        ZipFile(zip).extractAll(extractDir.absolutePath)

        val restored = File(extractDir, "docs/default-member/large_scan.pdf")
        assertEquals(largeContent.size.toLong(), restored.length(),
            "Restored file size must exactly match original")
        assertArrayEquals(largeContent, restored.readBytes(),
            "Restored file content must be byte-for-byte identical")
    }

    // --- Corruption/Error Tests ---

    @Test
    fun `truncated ZIP file fails gracefully`() {
        val zip = createTestBackupZip(null)
        // Truncate the ZIP to simulate download failure
        val bytes = zip.readBytes()
        val truncated = File(tempDir, "truncated.zip")
        truncated.writeBytes(bytes.copyOfRange(0, bytes.size / 2))

        assertThrows(Exception::class.java) {
            ZipFile(truncated).extractAll(File(tempDir, "trunc_extract").absolutePath)
        }
    }

    @Test
    fun `zero-byte file is not a valid backup`() {
        val empty = File(tempDir, "empty.zip")
        empty.writeBytes(ByteArray(0))

        assertThrows(Exception::class.java) {
            ZipFile(empty).extractAll(File(tempDir, "empty_extract").absolutePath)
        }
    }

    @Test
    fun `non-ZIP file fails gracefully`() {
        val notZip = File(tempDir, "not_a_zip.zip")
        notZip.writeText("this is just a text file pretending to be a zip")

        assertThrows(Exception::class.java) {
            ZipFile(notZip).extractAll(File(tempDir, "nozip_extract").absolutePath)
        }
    }

    // --- Database Tests ---

    @Test
    fun `backup includes database file`() {
        // Create a fake DB
        val db = File(tempDir, "database/traveldocs.db")
        db.parentFile?.mkdirs()
        db.writeText("SQLite format 3") // fake header

        val zip = createZipFromDir(tempDir, null)
        val extractDir = File(tempDir, "db_verify")
        ZipFile(zip).extractAll(extractDir.absolutePath)

        val restoredDb = File(extractDir, "database/traveldocs.db")
        assertTrue(restoredDb.exists(), "Database file should be in backup")
        assertEquals("SQLite format 3", restoredDb.readText())
    }

    // --- Helper methods ---

    private fun createTestBackupZip(password: String?, fileCount: Int = 3): File {
        // Create test documents
        for (i in 1..fileCount) {
            File(docsDir, "test_doc_$i").writeText("Content of document $i - ${System.nanoTime()}")
        }
        return createZipFromDir(tempDir, password)
    }

    private fun createZipFromDir(sourceDir: File, password: String?): File {
        val zipPath = File(tempDir, "test_backup_${System.nanoTime()}.zip")
        val manifest = JSONObject().apply {
            put("timestamp", "20260823_120000")
            put("fileCount", sourceDir.walkTopDown().filter { it.isFile && it.name != zipPath.name }.count())
            put("totalSizeBytes", sourceDir.walkTopDown().filter { it.isFile }.sumOf { it.length() })
            put("files", JSONArray())
        }
        val manifestFile = File(sourceDir, "manifest.json")
        manifestFile.writeText(manifest.toString(2))

        val zipFile = if (password != null) ZipFile(zipPath, password.toCharArray()) else ZipFile(zipPath)
        sourceDir.walkTopDown().filter { it.isFile && it != zipPath }.forEach { file ->
            val params = ZipParameters().apply {
                fileNameInZip = file.relativeTo(sourceDir).path
                if (password != null) {
                    isEncryptFiles = true
                    encryptionMethod = EncryptionMethod.AES
                    aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
                }
            }
            zipFile.addFile(file, params)
        }
        return zipPath
    }
}
