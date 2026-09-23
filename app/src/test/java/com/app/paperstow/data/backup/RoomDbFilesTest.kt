package com.app.paperstow.data.backup

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.io.File

class RoomDbFilesTest {
    @Test
    fun `missing file is not a valid documents database`() {
        val f = File.createTempFile("missing", ".db")
        f.delete()
        val check = RoomDbFiles.inspect(f)
        assertFalse(check.validSqlite)
        assertFalse(check.hasDocumentsTable)
    }

    @Test
    fun `tiny file is rejected before sqlite open`() {
        val f = File.createTempFile("tiny", ".db")
        f.writeBytes(ByteArray(20))
        try {
            val check = RoomDbFiles.inspect(f)
            assertFalse(check.validSqlite)
            assertFalse(check.hasDocumentsTable)
        } finally {
            f.delete()
        }
    }

    @Test
    fun `non sqlite header is rejected`() {
        val f = File.createTempFile("text", ".db")
        // Must be >= 100 bytes so inspect() reaches the header check, not the size gate.
        f.writeText("this is not a sqlite database file!!".repeat(4))
        try {
            val check = RoomDbFiles.inspect(f)
            assertFalse(check.validSqlite)
            assertFalse(check.hasDocumentsTable)
            assertEquals("Not a SQLite file", check.error)
        } finally {
            f.delete()
        }
    }
}
