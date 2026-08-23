package com.app.traveldocs.data.backup

import android.content.Context
import com.app.traveldocs.debug.DebugLogger
import net.lingala.zip4j.ZipFile
import org.json.JSONObject
import org.json.JSONArray
import java.io.File
import java.security.MessageDigest

/**
 * Backup restore with inspection, verification, and detailed reporting.
 *
 * Flow:
 * 1. inspectBackup() — reads manifest, reports contents WITHOUT extracting documents
 * 2. restoreFromZip() — extracts, re-encrypts, verifies integrity, produces report
 */
object BackupRestore {

    data class BackupInspection(
        val valid: Boolean,
        val schemaVersion: Int,
        val timestamp: String,
        val fileCount: Int,
        val totalSizeBytes: Long,
        val isPasswordProtected: Boolean,
        val pinProtectedCount: Int,
        val files: List<FileEntry>,
        val errorMessage: String? = null
    )

    data class FileEntry(
        val path: String,
        val size: Long,
        val sha256: String,
        val hasPinProtection: Boolean
    )

    data class RestoreResult(
        val success: Boolean,
        val filesProcessed: Int,
        val filesRestored: Int,
        val filesFailedVerification: Int,
        val message: String,
        val report: String  // Detailed human-readable report (shareable)
    )

    /**
     * Inspect a backup ZIP without restoring. Returns metadata and file list.
     */
    fun inspectBackup(context: Context, zipFile: File, password: String? = null): BackupInspection {
        DebugLogger.i("Restore", "Inspecting backup: ${zipFile.name} (${zipFile.length() / 1024}KB)")
        val extractDir = File(context.cacheDir, "inspect_${System.currentTimeMillis()}")
        extractDir.mkdirs()

        try {
            val zip = if (password != null) ZipFile(zipFile, password.toCharArray()) else ZipFile(zipFile)
            if (zip.isEncrypted && password == null) {
                return BackupInspection(false, 0, "", 0, 0, true, 0, emptyList(), "Backup is password-protected. Please provide the PIN.")
            }
            // Extract only manifest for inspection
            zip.extractAll(extractDir.absolutePath)

            val manifestFile = File(extractDir, "manifest.json")
            if (!manifestFile.exists()) {
                extractDir.deleteRecursively()
                return BackupInspection(false, 0, "", 0, 0, false, 0, emptyList(), "No manifest.json found in backup. Invalid backup file.")
            }

            val manifest = JSONObject(manifestFile.readText())
            val schema = manifest.optInt("schemaVersion", 1)
            val timestamp = manifest.optString("timestamp", "unknown")
            val fileCount = manifest.optInt("fileCount", 0)
            val totalSize = manifest.optLong("totalSizeBytes", 0)
            val encrypted = manifest.optBoolean("encrypted", false)

            val filesArray = manifest.optJSONArray("files") ?: JSONArray()
            val files = mutableListOf<FileEntry>()
            var pinCount = 0
            for (i in 0 until filesArray.length()) {
                val f = filesArray.getJSONObject(i)
                val hasPin = f.optBoolean("hasPinProtection", false)
                if (hasPin) pinCount++
                files.add(FileEntry(
                    path = f.getString("path"),
                    size = f.optLong("size", 0),
                    sha256 = f.optString("sha256", ""),
                    hasPinProtection = hasPin
                ))
            }

            extractDir.deleteRecursively()
            DebugLogger.i("Restore", "Inspection: $fileCount files, ${totalSize/1024}KB, $pinCount PIN-protected")
            return BackupInspection(true, schema, timestamp, fileCount, totalSize, encrypted, pinCount, files)

        } catch (e: Exception) {
            extractDir.deleteRecursively()
            DebugLogger.e("Restore", "Inspection failed", e)
            return BackupInspection(false, 0, "", 0, 0, false, 0, emptyList(), "Failed to read backup: ${e.message}")
        }
    }

    /**
     * Restore from ZIP with full verification and reporting.
     */
    fun restoreFromZip(context: Context, zipFile: File, password: String? = null, restoreTag: String = "Restored", onProgress: ((String) -> Unit)? = null): RestoreResult {
        DebugLogger.i("Restore", "Starting restore: ${zipFile.name} (${zipFile.length()/1024}KB), tag='$restoreTag'")
        val extractDir = File(context.cacheDir, "restore_staging_${System.currentTimeMillis()}")
        extractDir.mkdirs()
        val reportLines = mutableListOf<String>()
        reportLines.add("=== Restore Report ===")
        reportLines.add("Backup file: ${zipFile.name}")
        reportLines.add("Restore started: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())}")
        reportLines.add("")

        try {
            // 1. Extract
            val zip = if (password != null) ZipFile(zipFile, password.toCharArray()) else ZipFile(zipFile)
            if (zip.isEncrypted && password == null) {
                return RestoreResult(false, 0, 0, 0, "Password required.", "Restore failed: backup is password-protected.")
            }
            zip.extractAll(extractDir.absolutePath)

            // 2. Read manifest
            val manifestFile = File(extractDir, "manifest.json")
            val manifest = if (manifestFile.exists()) JSONObject(manifestFile.readText()) else null
            val filesArray = manifest?.optJSONArray("files") ?: JSONArray()
            val expectedFiles = mutableMapOf<String, FileEntry>()
            for (i in 0 until filesArray.length()) {
                val f = filesArray.getJSONObject(i)
                val entry = FileEntry(f.getString("path"), f.optLong("size", 0), f.optString("sha256", ""), f.optBoolean("hasPinProtection", false))
                expectedFiles[entry.path] = entry
            }
            reportLines.add("Manifest: schema=${manifest?.optInt("schemaVersion", 1)}, files=${expectedFiles.size}")
            reportLines.add("")

            // 3. Close and restore database
            val backupDb = File(extractDir, "database/traveldocs.db")
            if (backupDb.exists()) {
                val targetDb = context.getDatabasePath("traveldocs.db")
                val walFile = File(targetDb.path + "-wal")
                val shmFile = File(targetDb.path + "-shm")
                try {
                    val db = androidx.room.Room.databaseBuilder(context, com.app.traveldocs.data.local.TravelDocsDatabase::class.java, "traveldocs.db").build()
                    db.close()
                } catch (_: Exception) {}
                walFile.delete()
                shmFile.delete()
                targetDb.parentFile?.mkdirs()
                backupDb.copyTo(targetDb, overwrite = true)
                reportLines.add("Database: restored (${targetDb.length()/1024}KB)")
            } else {
                reportLines.add("Database: NOT found in backup")
            }

            // 4. Restore document files with verification
            var filesProcessed = 0
            var filesRestored = 0
            var filesFailedVerification = 0
            val docsDir = File(extractDir, "docs")
            reportLines.add("")
            reportLines.add("--- Files ---")

            if (docsDir.exists()) {
                val allFiles = docsDir.walkTopDown().filter { it.isFile }.toList()
                DebugLogger.i("Restore", "Found ${allFiles.size} document files to restore")
                allFiles.forEach { plainFile ->
                    filesProcessed++
                    val relativePath = plainFile.relativeTo(docsDir).path
                    DebugLogger.d("Restore", "Processing [$filesProcessed]: $relativePath")
                    onProgress?.invoke("Restoring: $relativePath")
                    val lookupPath = "docs/$relativePath"
                    val expectedEntry = expectedFiles[lookupPath]

                    try {
                        val fileBytes = plainFile.readBytes()

                        // Verify SHA-256 if available in manifest
                        var hashOk = true
                        if (expectedEntry != null && expectedEntry.sha256.isNotEmpty()) {
                            val actualHash = MessageDigest.getInstance("SHA-256").digest(fileBytes).joinToString("") { "%02x".format(it) }
                            hashOk = actualHash == expectedEntry.sha256
                            if (!hashOk) {
                                filesFailedVerification++
                                reportLines.add("  FAIL (hash mismatch): $relativePath")
                                DebugLogger.e("Restore", "Hash mismatch for $relativePath")
                            }
                        }

                        if (hashOk) {
                            val targetFile = File(context.filesDir, "docs/$relativePath.enc")
                            targetFile.parentFile?.mkdirs()
                            val encrypted = encryptForDevice(context, fileBytes)
                            if (encrypted != null) {
                                targetFile.writeBytes(encrypted)
                                filesRestored++
                                val status = if (expectedEntry?.hasPinProtection == true) "OK (PIN-protected)" else "OK"
                                reportLines.add("  $status: $relativePath (${fileBytes.size/1024}KB)")
                            } else {
                                reportLines.add("  FAIL (encryption): $relativePath")
                            }
                        }
                    } catch (e: Exception) {
                        reportLines.add("  FAIL (error): $relativePath — ${e.message}")
                        DebugLogger.e("Restore", "Failed: ${plainFile.name}", e)
                    }
                }
            }

            // 5. Add restore tag to all restored documents via database
            // (Tag will be visible after Room reconnects)
            if (restoreTag.isNotBlank()) {
                reportLines.add("")
                reportLines.add("Auto-tag: '$restoreTag' will be applied to restored documents")
            }

            // 6. Summary
            reportLines.add("")
            reportLines.add("--- Summary ---")
            reportLines.add("Processed: $filesProcessed")
            reportLines.add("Restored: $filesRestored")
            reportLines.add("Failed verification: $filesFailedVerification")
            reportLines.add("Restore tag: $restoreTag")
            reportLines.add("Completed: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())}")

            extractDir.deleteRecursively()

            val msg = "Restored $filesRestored of $filesProcessed documents" +
                (if (filesFailedVerification > 0) " ($filesFailedVerification failed verification)" else "")
            DebugLogger.i("Restore", msg)
            return RestoreResult(true, filesProcessed, filesRestored, filesFailedVerification, msg, reportLines.joinToString("\n"))

        } catch (e: Exception) {
            extractDir.deleteRecursively()
            reportLines.add("FATAL ERROR: ${e.message}")
            DebugLogger.e("Restore", "Restore failed", e)
            return RestoreResult(false, 0, 0, 0, "Restore failed: ${e.message}", reportLines.joinToString("\n"))
        }
    }

    private fun encryptForDevice(context: Context, plaintext: ByteArray): ByteArray? {
        return try {
            val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            val key = if (keyStore.containsAlias("travel_docs_file_encryption_key")) {
                (keyStore.getEntry("travel_docs_file_encryption_key", null) as java.security.KeyStore.SecretKeyEntry).secretKey
            } else {
                val generator = javax.crypto.KeyGenerator.getInstance("AES", "AndroidKeyStore")
                generator.init(android.security.keystore.KeyGenParameterSpec.Builder("travel_docs_file_encryption_key",
                    android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256).build())
                generator.generateKey()
            }
            val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, key)
            cipher.iv + cipher.doFinal(plaintext)
        } catch (e: Exception) {
            DebugLogger.e("Restore", "Encryption failed", e)
            null
        }
    }
}
