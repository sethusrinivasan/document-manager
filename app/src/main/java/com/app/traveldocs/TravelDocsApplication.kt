package com.app.traveldocs

import android.app.Application
import com.app.traveldocs.debug.CrashHandler
import com.app.traveldocs.debug.DebugLogger
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point.
 *
 * Two things happen here at startup:
 * 1. Debug logger gets initialized (writes to logcat + file + in-memory buffer)
 * 2. Crash handler hooks into the uncaught exception pipeline so we never lose a stack trace
 *
 * Everything else (DI, Room, etc.) is handled by Hilt and lazy initialization.
 */
@HiltAndroidApp
class TravelDocsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DebugLogger.init(this)
        CrashHandler.install(this)
        DebugLogger.i("App", "Document Manager started. Build: ${BuildConfig.VERSION_NAME}")
        // verifyDatabaseTables() — disabled: was deleting restored DBs. Room handles schema itself.
    }

    /**
     * Defensive check: if the database exists but has a corrupted/incompatible schema,
     * delete it and let Room recreate cleanly on first access.
     * 
     * IMPORTANT: We never manually CREATE TABLE for Room-managed tables.
     * Room generates its own DDL with exact column types, constraints, and indices.
     * Manual creation produces schema mismatches that Room rejects.
     */
    private fun verifyDatabaseTables() {
        try {
            val dbFile = getDatabasePath("traveldocs.db")
            if (!dbFile.exists()) return // Room will create on first access

            val db = android.database.sqlite.SQLiteDatabase.openDatabase(dbFile.path, null, android.database.sqlite.SQLiteDatabase.OPEN_READONLY)
            val cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='documents'", null)
            val hasDocumentsTable = cursor.count > 0
            cursor.close()
            db.close()

            if (!hasDocumentsTable) {
                DebugLogger.w("App", "Database exists but missing 'documents' table — deleting so Room can recreate")
                deleteDatabase("traveldocs.db")
                java.io.File(dbFile.path + "-wal").delete()
                java.io.File(dbFile.path + "-shm").delete()
                DebugLogger.i("App", "Corrupted DB removed. Room will create fresh tables on first query.")
                com.app.traveldocs.debug.UsageTelemetry.action("App", "corrupted_db_deleted")
            }
        } catch (e: Exception) {
            DebugLogger.e("App", "Database verification failed (non-fatal)", e)
        }
    }
}
