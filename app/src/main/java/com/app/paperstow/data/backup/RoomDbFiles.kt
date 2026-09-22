package com.app.paperstow.data.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.app.paperstow.data.local.TravelDocsDatabase
import com.app.paperstow.debug.DebugLogger
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.io.File

/**
 * Safe snapshot / swap of the Room file. Copying traveldocs.db while Room still
 * has WAL open is what produced backups (and restores) with no documents table.
 */
object RoomDbFiles {

    data class Inspection(
        val validSqlite: Boolean,
        val hasDocumentsTable: Boolean,
        val documentCount: Int,
        val userVersion: Int,
        val tables: List<String>,
        val error: String? = null
    )

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DatabaseEntryPoint {
        fun database(): TravelDocsDatabase
    }

    fun liveFile(context: Context): File = context.getDatabasePath("traveldocs.db")

    fun closeLiveDatabase(context: Context) {
        try {
            val db = EntryPointAccessors.fromApplication(context, DatabaseEntryPoint::class.java).database()
            if (db.isOpen) {
                db.close()
                DebugLogger.i("DB", "Closed live Room database")
            }
        } catch (e: Exception) {
            DebugLogger.w("DB", "Could not close live Room database: ${e.message}")
        }
    }

    fun snapshot(context: Context, dest: File) {
        dest.parentFile?.mkdirs()
        if (dest.exists()) dest.delete()
        val live = liveFile(context)
        if (!live.exists()) {
            throw IllegalStateException("No live database to back up")
        }
        val source = SQLiteDatabase.openDatabase(live.path, null, SQLiteDatabase.OPEN_READWRITE)
        try {
            try {
                val escaped = dest.absolutePath.replace("'", "''")
                source.execSQL("VACUUM INTO '$escaped'")
            } catch (e: Exception) {
                DebugLogger.w("DB", "VACUUM INTO failed (${e.message}); checkpoint + copy")
                source.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).close()
                live.copyTo(dest, overwrite = true)
                return
            }
        } finally {
            if (source.isOpen) source.close()
        }
        val check = inspect(dest)
        if (!check.hasDocumentsTable) {
            throw IllegalStateException(
                "Snapshot is missing the documents table (tables=${check.tables})"
            )
        }
    }

    fun inspect(file: File): Inspection {
        if (!file.exists() || file.length() < 100) {
            return Inspection(false, false, 0, 0, emptyList(), "Database file is missing or too small (${file.length()} bytes)")
        }
        val header = ByteArray(16)
        file.inputStream().use { input ->
            var read = 0
            while (read < header.size) {
                val n = input.read(header, read, header.size - read)
                if (n < 0) break
                read += n
            }
        }
        val magic = String(header, Charsets.US_ASCII)
        if (!magic.startsWith("SQLite format 3")) {
            return Inspection(false, false, 0, 0, emptyList(), "Not a SQLite file")
        }
        return try {
            val db = SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)
            try {
                val tables = mutableListOf<String>()
                db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name", null).use { cursor ->
                    while (cursor.moveToNext()) tables += cursor.getString(0)
                }
                val hasDocuments = tables.contains("documents")
                val count = if (hasDocuments) {
                    db.rawQuery("SELECT COUNT(*) FROM documents", null).use { cursor ->
                        if (cursor.moveToFirst()) cursor.getInt(0) else 0
                    }
                } else 0
                val version = db.rawQuery("PRAGMA user_version", null).use { cursor ->
                    if (cursor.moveToFirst()) cursor.getInt(0) else 0
                }
                Inspection(true, hasDocuments, count, version, tables)
            } finally {
                db.close()
            }
        } catch (e: Exception) {
            Inspection(false, false, 0, 0, emptyList(), e.message)
        }
    }

    fun deleteSidecars(dbFile: File) {
        File(dbFile.path + "-wal").delete()
        File(dbFile.path + "-shm").delete()
        File(dbFile.path + "-journal").delete()
    }
}
