package com.app.traveldocs.data.importer

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import com.app.traveldocs.domain.model.DocumentFormat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@DisplayName("FileDocumentImporter")
class FileDocumentImporterTest {

    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver
    private lateinit var formatValidator: DocumentFormatValidator
    private lateinit var importer: FileDocumentImporter

    private val testUri: Uri = mockk(relaxed = true)

    @BeforeEach
    fun setUp() {
        context = mockk(relaxed = true)
        contentResolver = mockk(relaxed = true)
        formatValidator = DocumentFormatValidator()

        every { context.contentResolver } returns contentResolver

        importer = FileDocumentImporter(context, formatValidator)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    private fun mockFileRead(bytes: ByteArray) {
        every { contentResolver.openInputStream(testUri) } returns ByteArrayInputStream(bytes)
    }

    private fun mockMimeType(mimeType: String?) {
        every { contentResolver.getType(testUri) } returns mimeType
    }

    private fun mockFileName(fileName: String?) {
        if (fileName != null) {
            val cursor = mockk<Cursor>(relaxed = true)
            every { contentResolver.query(testUri, null, null, null, null) } returns cursor
            every { cursor.moveToFirst() } returns true
            every { cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME) } returns 0
            every { cursor.getString(0) } returns fileName
            every { cursor.close() } returns Unit
        } else {
            every { contentResolver.query(testUri, null, null, null, null) } returns null
            every { testUri.lastPathSegment } returns null
        }
    }

    @Nested
    @DisplayName("importFromFile - successful imports")
    inner class SuccessfulImports {

        @Test
        @DisplayName("imports valid PDF file successfully")
        fun `imports valid PDF file successfully`() = runTest {
            val pdfBytes = byteArrayOf(0x25, 0x50, 0x44, 0x46) + "PDF content here".toByteArray()
            mockFileRead(pdfBytes)
            mockMimeType("application/pdf")
            mockFileName("passport.pdf")

            val result = importer.importFromFile(testUri)

            assertTrue(result.isSuccess)
            val doc = result.getOrNull()
            assertNotNull(doc)
            assertEquals(DocumentFormat.PDF, doc.format)
            assertEquals("passport.pdf", doc.originalFileName)
            assertTrue(doc.rawBytes.contentEquals(pdfBytes))
        }

        @Test
        @DisplayName("imports valid JPEG file successfully")
        fun `imports valid JPEG file successfully`() = runTest {
            val jpegBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()) + "JPEG data".toByteArray()
            mockFileRead(jpegBytes)
            mockMimeType("image/jpeg")
            mockFileName("ticket.jpg")

            val result = importer.importFromFile(testUri)

            assertTrue(result.isSuccess)
            val doc = result.getOrNull()
            assertNotNull(doc)
            assertEquals(DocumentFormat.JPG, doc.format)
            assertEquals("ticket.jpg", doc.originalFileName)
        }

        @Test
        @DisplayName("imports valid PNG file successfully")
        fun `imports valid PNG file successfully`() = runTest {
            val pngBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47) + "PNG image data".toByteArray()
            mockFileRead(pngBytes)
            mockMimeType("image/png")
            mockFileName("visa.png")

            val result = importer.importFromFile(testUri)

            assertTrue(result.isSuccess)
            val doc = result.getOrNull()
            assertNotNull(doc)
            assertEquals(DocumentFormat.PNG, doc.format)
            assertEquals("visa.png", doc.originalFileName)
        }

        @Test
        @DisplayName("imports file without mime type using magic bytes detection")
        fun `imports file without mime type using magic bytes detection`() = runTest {
            val pdfBytes = byteArrayOf(0x25, 0x50, 0x44, 0x46) + "content".toByteArray()
            mockFileRead(pdfBytes)
            mockMimeType(null)
            mockFileName("document.pdf")

            val result = importer.importFromFile(testUri)

            assertTrue(result.isSuccess)
            val doc = result.getOrNull()
            assertNotNull(doc)
            assertEquals(DocumentFormat.PDF, doc.format)
        }

        @Test
        @DisplayName("imports file with no filename")
        fun `imports file with no filename`() = runTest {
            val jpegBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()) + "data".toByteArray()
            mockFileRead(jpegBytes)
            mockMimeType("image/jpeg")
            mockFileName(null)

            val result = importer.importFromFile(testUri)

            assertTrue(result.isSuccess)
            val doc = result.getOrNull()
            assertNotNull(doc)
            assertEquals(DocumentFormat.JPG, doc.format)
        }
    }

    @Nested
    @DisplayName("importFromFile - error cases")
    inner class ErrorCases {

        @Test
        @DisplayName("returns failure for empty file")
        fun `returns failure for empty file`() = runTest {
            mockFileRead(byteArrayOf())
            mockMimeType("application/pdf")
            mockFileName("empty.pdf")

            val result = importer.importFromFile(testUri)

            assertTrue(result.isFailure)
            val error = result.exceptionOrNull()
            assertIs<IllegalArgumentException>(error)
            assertTrue(error.message!!.contains("empty", ignoreCase = true))
        }

        @Test
        @DisplayName("returns failure for unsupported format")
        fun `returns failure for unsupported format`() = runTest {
            val gifBytes = byteArrayOf(0x47, 0x49, 0x46, 0x38, 0x39, 0x61) // GIF89a
            mockFileRead(gifBytes)
            mockMimeType("image/gif")
            mockFileName("image.gif")

            val result = importer.importFromFile(testUri)

            assertTrue(result.isFailure)
            val error = result.exceptionOrNull()
            assertIs<IllegalArgumentException>(error)
            assertTrue(error.message!!.contains("Unsupported", ignoreCase = true) ||
                error.message!!.contains("does not match", ignoreCase = true))
        }

        @Test
        @DisplayName("returns failure when content does not match claimed format")
        fun `returns failure when content does not match claimed format`() = runTest {
            // File claims to be PDF (mime type) but actually has JPEG magic bytes
            val jpegBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()) + "data".toByteArray()
            mockFileRead(jpegBytes)
            mockMimeType("application/pdf")
            mockFileName("fake.pdf")

            val result = importer.importFromFile(testUri)

            assertTrue(result.isFailure)
            val error = result.exceptionOrNull()
            assertIs<IllegalArgumentException>(error)
            assertTrue(error.message!!.contains("mismatch", ignoreCase = true))
        }

        @Test
        @DisplayName("returns failure for IOException during read")
        fun `returns failure for IOException during read`() = runTest {
            every { contentResolver.openInputStream(testUri) } throws IOException("Disk error")
            mockMimeType("application/pdf")
            mockFileName("file.pdf")

            val result = importer.importFromFile(testUri)

            assertTrue(result.isFailure)
            val error = result.exceptionOrNull()
            assertIs<IOException>(error)
        }

        @Test
        @DisplayName("returns failure when input stream is null")
        fun `returns failure when input stream is null`() = runTest {
            every { contentResolver.openInputStream(testUri) } returns null
            mockMimeType("application/pdf")
            mockFileName("file.pdf")

            val result = importer.importFromFile(testUri)

            assertTrue(result.isFailure)
            val error = result.exceptionOrNull()
            assertIs<IOException>(error)
            assertTrue(error.message!!.contains("Failed to read", ignoreCase = true))
        }

        @Test
        @DisplayName("returns failure for SecurityException")
        fun `returns failure for SecurityException`() = runTest {
            every { contentResolver.openInputStream(testUri) } throws SecurityException("No permission")
            mockMimeType("application/pdf")
            mockFileName("file.pdf")

            val result = importer.importFromFile(testUri)

            assertTrue(result.isFailure)
            val error = result.exceptionOrNull()
            assertIs<IOException>(error)
            assertTrue(error.message!!.contains("Permission denied", ignoreCase = true))
        }

        @Test
        @DisplayName("returns failure for random bytes with no recognizable format")
        fun `returns failure for random bytes with no recognizable format`() = runTest {
            val randomBytes = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08)
            mockFileRead(randomBytes)
            mockMimeType(null)
            mockFileName("mystery.xyz")

            val result = importer.importFromFile(testUri)

            assertTrue(result.isFailure)
            val error = result.exceptionOrNull()
            assertIs<IllegalArgumentException>(error)
            assertTrue(error.message!!.contains("Unsupported", ignoreCase = true))
        }
    }

    @Nested
    @DisplayName("getSupportedFormats")
    inner class GetSupportedFormats {

        @Test
        @DisplayName("returns PDF, JPG, PNG")
        fun `returns PDF JPG PNG`() {
            val formats = importer.getSupportedFormats()

            assertEquals(3, formats.size)
            assertTrue(formats.contains(DocumentFormat.PDF))
            assertTrue(formats.contains(DocumentFormat.JPG))
            assertTrue(formats.contains(DocumentFormat.PNG))
        }
    }
}
