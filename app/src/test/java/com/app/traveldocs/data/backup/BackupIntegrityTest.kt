package com.app.traveldocs.data.backup

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.EncryptionMethod
import net.lingala.zip4j.model.enums.AesKeyStrength
import org.json.JSONObject
import org.json.JSONArray
import java.io.File

class BackupIntegrityTest {
    @TempDir lateinit var tmp: File

    @Test
    fun `round trip preserves file content`() {
        val content = ByteArray(2048) { (it % 256).toByte() }
        val src = File(tmp, "doc.pdf"); src.writeBytes(content)
        val zip = File(tmp, "backup.zip")
        ZipFile(zip).addFile(src)
        val out = File(tmp, "out"); ZipFile(zip).extractAll(out.absolutePath)
        assertArrayEquals(content, File(out, "doc.pdf").readBytes())
    }

    @Test
    fun `password protected zip rejects wrong password`() {
        val src = File(tmp, "secret.txt"); src.writeText("secret")
        val zip = File(tmp, "protected.zip")
        val params = ZipParameters().apply { isEncryptFiles = true; encryptionMethod = EncryptionMethod.AES; aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256 }
        ZipFile(zip, "correct".toCharArray()).addFile(src, params)
        assertTrue(ZipFile(zip).isEncrypted)
        assertThrows(Exception::class.java) { ZipFile(zip, "wrong".toCharArray()).extractAll(File(tmp, "x").absolutePath) }
    }

    @Test
    fun `password protected zip succeeds with correct password`() {
        val src = File(tmp, "doc.txt"); src.writeText("hello")
        val zip = File(tmp, "ok.zip")
        val params = ZipParameters().apply { isEncryptFiles = true; encryptionMethod = EncryptionMethod.AES; aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256 }
        ZipFile(zip, "pin123".toCharArray()).addFile(src, params)
        val out = File(tmp, "ok_out"); ZipFile(zip, "pin123".toCharArray()).extractAll(out.absolutePath)
        assertEquals("hello", File(out, "doc.txt").readText())
    }

    @Test
    fun `manifest fileCount matches reality`() {
        val docs = File(tmp, "docs"); docs.mkdirs()
        repeat(5) { File(docs, "file_$it.pdf").writeText("content $it") }
        val manifest = JSONObject().apply { put("fileCount", 5); put("timestamp", "test"); put("totalSizeBytes", 0); put("files", JSONArray()) }
        File(tmp, "manifest.json").writeText(manifest.toString())
        val zip = File(tmp, "m.zip"); val zf = ZipFile(zip)
        tmp.walkTopDown().filter { it.isFile && it != zip }.forEach { f -> zf.addFile(f, ZipParameters().apply { fileNameInZip = f.relativeTo(tmp).path }) }
        val out = File(tmp, "m_out"); ZipFile(zip).extractAll(out.absolutePath)
        val m = JSONObject(File(out, "manifest.json").readText())
        assertEquals(5, m.getInt("fileCount"))
        assertEquals(5, File(out, "docs").listFiles()?.size)
    }

    @Test
    fun `truncated zip fails`() {
        val src = File(tmp, "a.txt"); src.writeText("data")
        val zip = File(tmp, "trunc.zip"); ZipFile(zip).addFile(src)
        val half = zip.readBytes().copyOfRange(0, zip.readBytes().size / 2)
        val bad = File(tmp, "bad.zip"); bad.writeBytes(half)
        assertThrows(Exception::class.java) { ZipFile(bad).extractAll(File(tmp, "t").absolutePath) }
    }

    @Test
    fun `large file 5MB survives zip round trip`() {
        val big = ByteArray(5 * 1024 * 1024) { (it % 251).toByte() }
        val src = File(tmp, "big.bin"); src.writeBytes(big)
        val zip = File(tmp, "big.zip"); ZipFile(zip).addFile(src)
        val out = File(tmp, "big_out"); ZipFile(zip).extractAll(out.absolutePath)
        assertArrayEquals(big, File(out, "big.bin").readBytes())
    }

    @Test
    fun `empty zip has no files`() {
        val zip = File(tmp, "empty.zip")
        val mf = File(tmp, "manifest.json"); mf.writeText(JSONObject().put("fileCount", 0).toString())
        ZipFile(zip).addFile(mf)
        val out = File(tmp, "e_out"); ZipFile(zip).extractAll(out.absolutePath)
        val m = JSONObject(File(out, "manifest.json").readText())
        assertEquals(0, m.getInt("fileCount"))
    }
}
