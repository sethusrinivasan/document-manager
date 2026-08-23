package com.app.traveldocs.data.local

import android.content.Context
import com.app.traveldocs.debug.DebugLogger
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles database corruption detection and recovery.
 * If the Room database file is corrupted, it attempts to delete and recreate.
 * All operations are offline-first — no network required.
 */
@Singleton
class DatabaseRecovery @Inject constructor() {

    /**
     * Checks if the database file exists and is valid.
     * If corrupted, deletes it so Room can recreate on next access.
     */
    fun checkAndRecover(context: Context, dbName: String): RecoveryResult {
        val dbFile = context.getDatabasePath(dbName)
        if (!dbFile.exists()) {
            DebugLogger.d("Recovery", "Database '$dbName' does not exist yet (first launch)")
            return RecoveryResult.NO_ACTION
        }

        return try {
            // Try to open the database file to check for corruption
            val walFile = File(dbFile.parent, "$dbName-wal")
            val shmFile = File(dbFile.parent, "$dbName-shm")

            // Simple corruption check: file size > 0 and starts with SQLite header
            if (dbFile.length() == 0L) {
                DebugLogger.w("Recovery", "Database '$dbName' is empty (0 bytes) — deleting for recreation")
                deleteDbFiles(dbFile, walFile, shmFile)
                return RecoveryResult.RECOVERED
            }

            // Check SQLite magic header: "SQLite format 3\000"
            val header = ByteArray(16)
            dbFile.inputStream().use { it.read(header) }
            val headerStr = String(header, 0, 15)
            if (!headerStr.startsWith("SQLite format 3")) {
                DebugLogger.e("Recovery", "Database '$dbName' has invalid header: '${headerStr.take(10)}' — CORRUPTED")
                deleteDbFiles(dbFile, walFile, shmFile)
                return RecoveryResult.RECOVERED
            }

            DebugLogger.d("Recovery", "Database '$dbName' OK (size=${dbFile.length()} bytes)")
            RecoveryResult.NO_ACTION
        } catch (e: Exception) {
            DebugLogger.e("Recovery", "Error checking database '$dbName' — deleting for safety", e)
            try {
                dbFile.delete()
            } catch (_: Exception) {}
            RecoveryResult.RECOVERED
        }
    }

    private fun deleteDbFiles(dbFile: File, walFile: File, shmFile: File) {
        dbFile.delete()
        walFile.delete()
        shmFile.delete()
        DebugLogger.i("Recovery", "Deleted corrupted database files")
    }

    enum class RecoveryResult {
        NO_ACTION,
        RECOVERED
    }
}
