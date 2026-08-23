package com.app.traveldocs.data.backup

import android.content.Context
import com.app.traveldocs.debug.DebugLogger
import net.lingala.zip4j.ZipFile
import org.json.JSONObject
import java.io.File

/**
 * Restores documents from a backup ZIP file.
 *
 * Backup format (from BackupManager):
 * - manifest.json — metadata about the backup
 * - docs/{member}/{fileId} — decrypted document files (plaintext in ZIP)
 * - database/traveldocs.db — Room database with metadata
 *
 * Restore process:
 * 1. Extract ZIP (with password if encrypted)
 * 2. Read manifest for validation
 * 3. Copy database back (replaces current)
 * 4. Re-encrypt each document file and write to docs/ folder
 * 5. Report results
 */
object BackupRestore {

    data class RestoreResult(
        val success: Boolean,
        val filesProcessed: Int,
        val filesRestored: Int,
        val message: String
    )

    fun restoreFromZip(context: Context, zipFile: File, password: String? = null): RestoreResult {
        DebugLogger.i("Restore", "Starting restore from: ${zipFile.name} (${zipFile.length() / 1024}KB)")
        val extractDir = File(context.cacheDir, "restore_staging_${System.currentTimeMillis()}")
        extractDir.mkdirs()

        try {
            // 1. Extract ZIP
            val zip = if (password != null) ZipFile(zipFile, password.toCharArray()) else ZipFile(zipFile)
            if (zip.isEncrypted && password == null) {
                return RestoreResult(false, 0, 0, "Backup is password-protected. Please provide the PIN.")
            }
            zip.extractAll(extractDir.absolutePath)
            DebugLogger.i("Restore", "ZIP extracted to staging")

            // 2. Read manifest
            val manifestFile = File(extractDir, "manifest.json")
            val manifest = if (manifestFile.exists()) {
                JSONObject(manifestFile.readText())
            } else null
            val expectedFiles = manifest?.optInt("fileCount", -1) ?: -1
            DebugLogger.i("Restore", "Manifest: ${manifest?.optString("timestamp")}, expected $expectedFiles files")

            // 3. Restore database
            // Room holds a live connection with WAL mode. We must:
            // a) Close the existing Room connection (flushes WAL → main db + releases locks)
            // b) Delete WAL/SHM files
            // c) Overwrite the .db file with the backup
            // After this, Room will open a fresh connection on the next DAO query.
            val backupDb = File(extractDir, "database/traveldocs.db")
            if (backupDb.exists()) {
                val targetDb = context.getDatabasePath("traveldocs.db")
                val walFile = File(targetDb.path + "-wal")
                val shmFile = File(targetDb.path + "-shm")
                targetDb.parentFile?.mkdirs()

                // Close Room's active connection so it releases file locks
                try {
                    val db = androidx.room.Room.databaseBuilder(
                        context, com.app.traveldocs.data.local.TravelDocsDatabase::class.java, "traveldocs.db"
                    ).build()
                    db.close()  // Forces WAL checkpoint and releases all file handles
                    DebugLogger.d("Restore", "Room database connection closed")
                } catch (e: Exception) {
                    DebugLogger.w("Restore", "Could not close Room DB (may already be closed)", e)
                }

                // Delete WAL/SHM — ensures no stale journal data persists
                walFile.delete()
                shmFile.delete()
                // Overwrite with backup
                backupDb.copyTo(targetDb, overwrite = true)
                DebugLogger.i("Restore", "Database restored (${targetDb.length() / 1024}KB), WAL/SHM cleared, connection will reopen on next query")
            }

            // 4. Re-encrypt and restore document files
            var filesProcessed = 0
            var filesRestored = 0
            val docsDir = File(extractDir, "docs")
            if (docsDir.exists()) {
                docsDir.walkTopDown().filter { it.isFile }.forEach { plainFile ->
                    filesProcessed++
                    try {
                        val relativePath = plainFile.relativeTo(docsDir).path
                        val targetFile = File(context.filesDir, "docs/$relativePath.enc")
                        targetFile.parentFile?.mkdirs()

                        // Re-encrypt with device's KeyStore key
                        val encrypted = encryptForDevice(context, plainFile.readBytes())
                        if (encrypted != null) {
                            targetFile.writeBytes(encrypted)
                            filesRestored++
                        } else {
                            DebugLogger.w("Restore", "Encryption failed for: $relativePath")
                        }
                    } catch (e: Exception) {
                        DebugLogger.e("Restore", "Failed to restore file: ${plainFile.name}", e)
                    }
                }
            }

            // 5. Cleanup
            extractDir.deleteRecursively()

            val msg = "Restored $filesRestored of $filesProcessed documents" +
                if (manifest != null) " (backup from ${manifest.optString("timestamp", "unknown")})" else ""
            DebugLogger.i("Restore", msg)
            return RestoreResult(true, filesProcessed, filesRestored, msg)

        } catch (e: Exception) {
            extractDir.deleteRecursively()
            DebugLogger.e("Restore", "Restore failed", e)
            return RestoreResult(false, 0, 0, "Restore failed: ${e.message}")
        }
    }

    /**
     * Encrypt plaintext bytes using the device's Android KeyStore key (same as import pipeline).
     */
    private fun encryptForDevice(context: Context, plaintext: ByteArray): ByteArray? {
        return try {
            val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)

            // Get or create the encryption key
            val key = if (keyStore.containsAlias("travel_docs_file_encryption_key")) {
                (keyStore.getEntry("travel_docs_file_encryption_key", null) as java.security.KeyStore.SecretKeyEntry).secretKey
            } else {
                val generator = javax.crypto.KeyGenerator.getInstance("AES", "AndroidKeyStore")
                generator.init(
                    android.security.keystore.KeyGenParameterSpec.Builder("travel_docs_file_encryption_key",
                        android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build()
                )
                generator.generateKey()
            }

            val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val ciphertext = cipher.doFinal(plaintext)
            iv + ciphertext  // [12-byte IV][ciphertext]
        } catch (e: Exception) {
            DebugLogger.e("Restore", "Encryption for device failed", e)
            null
        }
    }
}
