package com.app.paperstow.data.backup

import android.content.Context
import com.app.paperstow.BuildConfig
import com.app.paperstow.debug.DebugLogger
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.CompressionLevel
import net.lingala.zip4j.model.enums.CompressionMethod
import net.lingala.zip4j.model.enums.EncryptionMethod
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor() {

    data class BackupResult(val zipFile: File, val fileCount: Int, val totalBytes: Long)

    /**
     * Creates a ZIP with decrypted document contents so it can be restored on this
     * or another device. [backupPin] is optional: when blank, the archive is not encrypted.
     */
    fun createBackupZip(
        context: Context,
        backupPin: String? = null,
        includeTrailGpx: Boolean = false
    ): BackupResult {
        val pin = backupPin?.trim().orEmpty()
        val protect = pin.isNotEmpty()
        if (protect && pin.length < 4) {
            throw IllegalArgumentException("Backup password must be at least 4 characters")
        }
        DebugLogger.i("Backup", "Creating transportable backup archive (password=${protect})...")
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val zipPath = File(context.cacheDir, "backup_$timestamp.zip")
        val tempDir = File(context.cacheDir, "backup_staging_$timestamp")
        tempDir.mkdirs()

        var fileCount = 0
        var totalBytes = 0L
        val fileManifest = JSONArray()

        // 1. Decrypt document files and stage them
        val docsDir = File(context.filesDir, "docs")
        if (docsDir.exists()) {
            docsDir.walkTopDown().filter { it.isFile && it.name.endsWith(".enc") }.forEach { encFile ->
                try {
                    val decrypted = decryptFile(context, encFile)
                    if (decrypted != null) {
                        val relativePath = encFile.relativeTo(docsDir).path.replace(".enc", "")
                        val fileId = encFile.nameWithoutExtension
                        val outFile = File(tempDir, "docs/$relativePath")
                        outFile.parentFile?.mkdirs()
                        outFile.writeBytes(decrypted)

                        fileCount++
                        totalBytes += decrypted.size
                        val hash = java.security.MessageDigest.getInstance("SHA-256")
                            .digest(decrypted).joinToString("") { "%02x".format(it) }
                        fileManifest.put(JSONObject().apply {
                            put("path", "docs/$relativePath")
                            put("size", decrypted.size)
                            put("sha256", hash)
                            put("originalFileId", fileId)
                        })
                    }
                } catch (e: Exception) {
                    DebugLogger.e("Backup", "Failed to decrypt ${encFile.name}", e)
                }
            }
        }

        val destDb = File(tempDir, "database/traveldocs.db")
        RoomDbFiles.snapshot(context, destDb)
        val dbCheck = RoomDbFiles.inspect(destDb)
        DebugLogger.i("Backup", "DB snapshot: docs=${dbCheck.documentCount} tables=${dbCheck.tables} v=${dbCheck.userVersion}")
        fileCount++
        totalBytes += destDb.length()

        if (includeTrailGpx) {
            try {
                val gpx = exportTrailGpx(context)
                val trailFile = File(tempDir, "trail/my_trail.gpx")
                trailFile.parentFile?.mkdirs()
                trailFile.writeText(gpx)
                fileCount++
                totalBytes += trailFile.length()
                fileManifest.put(JSONObject().apply {
                    put("path", "trail/my_trail.gpx")
                    put("size", trailFile.length())
                    put("sha256", java.security.MessageDigest.getInstance("SHA-256")
                        .digest(gpx.toByteArray()).joinToString("") { "%02x".format(it) })
                })
                DebugLogger.i("Backup", "Included My Trail as trail/my_trail.gpx")
            } catch (e: Exception) {
                DebugLogger.w("Backup", "Could not export trail GPX: ${e.message}")
            }
        }

        // 3. Write manifest with schema and file hashes
        val manifest = JSONObject().apply {
            put("schemaVersion", 2)
            put("timestamp", timestamp)
            put("appVersion", BuildConfig.VERSION_NAME)
            put("fileCount", fileCount)
            put("totalSizeBytes", totalBytes)
            put("encrypted", protect)
            put("transportable", true)
            put("files", fileManifest)
        }
        File(tempDir, "manifest.json").writeText(manifest.toString(2))

        val zipFile = if (protect) ZipFile(zipPath, pin.toCharArray()) else ZipFile(zipPath)
        val params = ZipParameters().apply {
            compressionMethod = CompressionMethod.DEFLATE
            compressionLevel = CompressionLevel.NORMAL
            if (protect) {
                isEncryptFiles = true
                encryptionMethod = EncryptionMethod.AES
                aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
            }
        }
        tempDir.walkTopDown().filter { it.isFile }.forEach { file ->
            val entryName = file.relativeTo(tempDir).path
            params.fileNameInZip = entryName
            zipFile.addFile(file, params)
        }

        // 5. Cleanup staging
        tempDir.deleteRecursively()

        DebugLogger.i("Backup", "Backup created: ${zipPath.name}, $fileCount files, ${totalBytes / 1024}KB, password=${protect}")
        return BackupResult(zipPath, fileCount, totalBytes)
    }

    private fun exportTrailGpx(context: Context): String {
        val places = mutableListOf<com.app.paperstow.domain.safety.SafetyPlace>()
        val dbFile = context.getDatabasePath("traveldocs.db")
        if (dbFile.exists()) {
            val db = android.database.sqlite.SQLiteDatabase.openDatabase(
                dbFile.path, null, android.database.sqlite.SQLiteDatabase.OPEN_READONLY
            )
            try {
                db.rawQuery(
                    "SELECT latitude, longitude, timestamp, batteryPercent FROM gps_tracks ORDER BY timestamp ASC",
                    null
                ).use { cursor ->
                    val lat = cursor.getColumnIndex("latitude")
                    val lon = cursor.getColumnIndex("longitude")
                    val time = cursor.getColumnIndex("timestamp")
                    val bat = cursor.getColumnIndex("batteryPercent")
                    while (cursor.moveToNext()) {
                        places += com.app.paperstow.domain.safety.SafetyPlace(
                            latitude = cursor.getDouble(lat),
                            longitude = cursor.getDouble(lon),
                            timestamp = cursor.getLong(time),
                            batteryPercent = if (bat >= 0) cursor.getInt(bat) else -1
                        )
                    }
                }
            } finally {
                db.close()
            }
        }
        return com.app.paperstow.domain.safety.GpxTrail.toGpx(places)
    }

    /**
     * Decrypt a .enc file using the same key from Android KeyStore.
     */
    private fun decryptFile(context: Context, encFile: File): ByteArray? {
        return try {
            val encData = encFile.readBytes()
            if (encData.size <= 12) return null
            val iv = encData.copyOfRange(0, 12)
            val ciphertext = encData.copyOfRange(12, encData.size)

            val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            val key = (keyStore.getEntry("travel_docs_file_encryption_key", null) as java.security.KeyStore.SecretKeyEntry).secretKey

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            cipher.doFinal(ciphertext)
        } catch (e: Exception) {
            DebugLogger.e("Backup", "Decrypt failed: ${encFile.name}", e)
            null
        }
    }

    fun suggestedFolderName(): String = "Paperstow_Backup"

    fun suggestedFileName(): String {
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "paperstow_backup_$ts.zip"
    }
}
