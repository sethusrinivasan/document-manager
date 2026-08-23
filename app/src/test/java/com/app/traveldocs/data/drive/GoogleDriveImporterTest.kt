package com.app.traveldocs.data.drive

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import com.app.traveldocs.domain.model.DocumentFormat
import com.google.api.services.drive.Drive
import com.google.api.services.drive.Drive.Files
import com.google.api.services.drive.Drive.Files.Get
import com.google.api.services.drive.model.File
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.OutputStream

class GoogleDriveImporterTest {

    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver
    private lateinit var driveServiceProvider: DriveServiceProvider
    private lateinit var importer: GoogleDriveImporter

    @BeforeEach
    fun setUp() {
        context = mockk(relaxed = true)
        contentResolver = mockk(relaxed = true)
        driveServiceProvider = mockk()
        every { context.contentResolver } returns contentResolver
        importer = GoogleDriveImporter(context, driveServiceProvider)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Nested
    @DisplayName("Format resolution from MIME type")
    inner class MimeTypeResolutionTests {

        @Test
        fun `resolves PDF from application-pdf MIME type`() {
            val result = importer.resolveFormatFromMimeType("application/pdf")
            assertEquals(DocumentFormat.PDF, result)
        }

        @Test
        fun `resolves JPG from image-jpeg MIME type`() {
            val result = importer.resolveFormatFromMimeType("image/jpeg")
            assertEquals(DocumentFormat.JPG, result)
        }

        @Test
        fun `resolves JPG from image-jpg MIME type`() {
            val result = importer.resolveFormatFromMimeType("image/jpg")
            assertEquals(DocumentFormat.JPG, result)
        }

        @Test
        fun `resolves PNG from image-png MIME type`() {
            val result = importer.resolveFormatFromMimeType("image/png")
            assertEquals(DocumentFormat.PNG, result)
        }

        @Test
        fun `returns null for unsupported MIME type`() {
            val result = importer.resolveFormatFromMimeType("application/msword")
            assertEquals(null, result)
        }

        @Test
        fun `returns null for null MIME type`() {
            val result = importer.resolveFormatFromMimeType(null)
            assertEquals(null, result)
        }

        @Test
        fun `handles case-insensitive MIME types`() {
            val result = importer.resolveFormatFromMimeType("Application/PDF")
            assertEquals(DocumentFormat.PDF, result)
        }
    }

    @Nested
    @DisplayName("Format resolution from filename")
    inner class FileNameResolutionTests {

        @Test
        fun `resolves PDF from filename with pdf extension`() {
            val result = importer.resolveFormatFromFileName("passport.pdf")
            assertEquals(DocumentFormat.PDF, result)
        }

        @Test
        fun `resolves JPG from filename with jpg extension`() {
            val result = importer.resolveFormatFromFileName("visa.jpg")
            assertEquals(DocumentFormat.JPG, result)
        }

        @Test
        fun `resolves JPG from filename with jpeg extension`() {
            val result = importer.resolveFormatFromFileName("ticket.jpeg")
            assertEquals(DocumentFormat.JPG, result)
        }

        @Test
        fun `resolves PNG from filename with png extension`() {
            val result = importer.resolveFormatFromFileName("insurance.png")
            assertEquals(DocumentFormat.PNG, result)
        }

        @Test
        fun `returns null for unsupported extension`() {
            val result = importer.resolveFormatFromFileName("document.docx")
            assertEquals(null, result)
        }

        @Test
        fun `returns null for null filename`() {
            val result = importer.resolveFormatFromFileName(null)
            assertEquals(null, result)
        }

        @Test
        fun `handles uppercase extensions`() {
            val result = importer.resolveFormatFromFileName("passport.PDF")
            assertEquals(DocumentFormat.PDF, result)
        }
    }

    @Nested
    @DisplayName("Import from content URI")
    inner class ImportFromUriTests {

        @Test
        fun `successfully imports PDF from content URI`() = runTest {
            val uri = mockk<Uri>()
            val pdfBytes = byteArrayOf(0x25, 0x50, 0x44, 0x46) // %PDF

            every { contentResolver.getType(uri) } returns "application/pdf"
            every { contentResolver.openInputStream(uri) } returns ByteArrayInputStream(pdfBytes)
            mockCursorWithFileName(uri, "passport.pdf")

            val result = importer.importFromUri(uri)

            assertTrue(result.isSuccess)
            val doc = result.getOrThrow()
            assertEquals(DocumentFormat.PDF, doc.format)
            assertEquals("passport.pdf", doc.originalFileName)
            assertTrue(doc.rawBytes.contentEquals(pdfBytes))
        }

        @Test
        fun `successfully imports JPG from content URI`() = runTest {
            val uri = mockk<Uri>()
            val jpgBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())

            every { contentResolver.getType(uri) } returns "image/jpeg"
            every { contentResolver.openInputStream(uri) } returns ByteArrayInputStream(jpgBytes)
            mockCursorWithFileName(uri, "visa_photo.jpg")

            val result = importer.importFromUri(uri)

            assertTrue(result.isSuccess)
            val doc = result.getOrThrow()
            assertEquals(DocumentFormat.JPG, doc.format)
            assertEquals("visa_photo.jpg", doc.originalFileName)
        }

        @Test
        fun `fails with descriptive message for unsupported format`() = runTest {
            val uri = mockk<Uri>()

            every { contentResolver.getType(uri) } returns "application/msword"
            mockCursorWithFileName(uri, "document.docx")

            val result = importer.importFromUri(uri)

            assertTrue(result.isFailure)
            val error = result.exceptionOrNull()!!
            assertTrue(error is IOException)
            assertTrue(error.message!!.contains("Unsupported document format"))
            assertTrue(error.message!!.contains("PDF, JPG, and PNG"))
        }

        @Test
        fun `falls back to filename extension when MIME type is null`() = runTest {
            val uri = mockk<Uri>()
            val pngBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)

            every { contentResolver.getType(uri) } returns null
            every { contentResolver.openInputStream(uri) } returns ByteArrayInputStream(pngBytes)
            mockCursorWithFileName(uri, "ticket.png")

            val result = importer.importFromUri(uri)

            assertTrue(result.isSuccess)
            assertEquals(DocumentFormat.PNG, result.getOrThrow().format)
        }

        @Test
        fun `fails with descriptive message on IOException`() = runTest {
            val uri = mockk<Uri>()

            every { contentResolver.getType(uri) } returns "application/pdf"
            mockCursorWithFileName(uri, "document.pdf")
            every { contentResolver.openInputStream(uri) } throws IOException("Connection reset")

            val result = importer.importFromUri(uri)

            assertTrue(result.isFailure)
            val error = result.exceptionOrNull()!!
            assertTrue(error is IOException)
            assertTrue(error.message!!.contains("Failed to import document"))
            assertTrue(error.message!!.contains("network connection"))
        }

        @Test
        fun `fails with descriptive message on SecurityException`() = runTest {
            val uri = mockk<Uri>()

            every { contentResolver.getType(uri) } returns "application/pdf"
            mockCursorWithFileName(uri, "document.pdf")
            every { contentResolver.openInputStream(uri) } throws SecurityException("Permission denied")

            val result = importer.importFromUri(uri)

            assertTrue(result.isFailure)
            val error = result.exceptionOrNull()!!
            assertTrue(error is IOException)
            assertTrue(error.message!!.contains("Access denied"))
            assertTrue(error.message!!.contains("re-authorize"))
        }

        @Test
        fun `handles null input stream gracefully`() = runTest {
            val uri = mockk<Uri>()

            every { contentResolver.getType(uri) } returns "application/pdf"
            every { contentResolver.openInputStream(uri) } returns null
            mockCursorWithFileName(uri, "document.pdf")

            val result = importer.importFromUri(uri)

            assertTrue(result.isFailure)
            val error = result.exceptionOrNull()!!
            assertTrue(error is IOException)
        }
    }

    @Nested
    @DisplayName("Import from Drive file ID")
    inner class ImportFromDriveFileIdTests {

        @Test
        fun `fails with descriptive message when not authenticated`() = runTest {
            every { driveServiceProvider.getDriveService() } returns null

            val result = importer.importFromDriveFileId("abc123")

            assertTrue(result.isFailure)
            val error = result.exceptionOrNull()!!
            assertTrue(error is IOException)
            assertTrue(error.message!!.contains("authentication failed"))
            assertTrue(error.message!!.contains("sign in"))
        }

        @Test
        fun `fails with descriptive message for unsupported Drive file format`() = runTest {
            val driveService = mockk<Drive>()
            val files = mockk<Files>()
            val getRequest = mockk<Get>()
            val file = File().apply {
                name = "spreadsheet.xlsx"
                mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            }

            every { driveServiceProvider.getDriveService() } returns driveService
            every { driveService.files() } returns files
            every { files.get("file123") } returns getRequest
            every { getRequest.setFields("name,mimeType") } returns getRequest
            every { getRequest.execute() } returns file

            val result = importer.importFromDriveFileId("file123")

            assertTrue(result.isFailure)
            val error = result.exceptionOrNull()!!
            assertTrue(error is IOException)
            assertTrue(error.message!!.contains("Unsupported document format"))
        }

        @Test
        fun `fails with descriptive message on network error during download`() = runTest {
            val driveService = mockk<Drive>()
            val files = mockk<Files>()
            val getMetaRequest = mockk<Get>()
            val getDownloadRequest = mockk<Get>()
            val file = File().apply {
                name = "passport.pdf"
                mimeType = "application/pdf"
            }

            every { driveServiceProvider.getDriveService() } returns driveService
            every { driveService.files() } returns files
            every { files.get("file123") } returns getMetaRequest andThen getDownloadRequest
            every { getMetaRequest.setFields("name,mimeType") } returns getMetaRequest
            every { getMetaRequest.execute() } returns file
            every { getDownloadRequest.executeMediaAndDownloadTo(any<OutputStream>()) } throws IOException("Network timeout")

            val result = importer.importFromDriveFileId("file123")

            assertTrue(result.isFailure)
            val error = result.exceptionOrNull()!!
            assertTrue(error is IOException)
            assertTrue(error.message!!.contains("Failed to download"))
            assertTrue(error.message!!.contains("network connection"))
        }

        @Test
        fun `successfully downloads and imports PDF from Drive`() = runTest {
            val pdfBytes = byteArrayOf(0x25, 0x50, 0x44, 0x46)
            val driveService = mockk<Drive>()
            val files = mockk<Files>()
            val getMetaRequest = mockk<Get>()
            val getDownloadRequest = mockk<Get>()
            val file = File().apply {
                name = "passport.pdf"
                mimeType = "application/pdf"
            }

            every { driveServiceProvider.getDriveService() } returns driveService
            every { driveService.files() } returns files
            every { files.get("file123") } returns getMetaRequest andThen getDownloadRequest
            every { getMetaRequest.setFields("name,mimeType") } returns getMetaRequest
            every { getMetaRequest.execute() } returns file
            every { getDownloadRequest.executeMediaAndDownloadTo(any<OutputStream>()) } answers {
                val outputStream = firstArg<OutputStream>()
                outputStream.write(pdfBytes)
            }

            val result = importer.importFromDriveFileId("file123")

            assertTrue(result.isSuccess)
            val doc = result.getOrThrow()
            assertEquals(DocumentFormat.PDF, doc.format)
            assertEquals("passport.pdf", doc.originalFileName)
            assertTrue(doc.rawBytes.contentEquals(pdfBytes))
        }
    }

    private fun mockCursorWithFileName(uri: Uri, fileName: String?) {
        val cursor = mockk<Cursor>(relaxed = true)
        every { contentResolver.query(uri, null, null, null, null) } returns cursor
        every { cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME) } returns 0
        every { cursor.moveToFirst() } returns true
        every { cursor.getString(0) } returns fileName
    }
}
