package com.app.traveldocs.data.scanner

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.app.traveldocs.domain.model.DocumentFormat
import com.google.android.gms.mlkit.vision.documentscanner.GmsDocumentScanningResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.IOException

@DisplayName("CameraDocumentScanner")
class CameraDocumentScannerTest {

    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver
    private lateinit var scanner: CameraDocumentScanner

    @BeforeEach
    fun setup() {
        context = mockk(relaxed = true)
        contentResolver = mockk(relaxed = true)
        every { context.contentResolver } returns contentResolver
        scanner = CameraDocumentScanner(context)
    }

    @AfterEach
    fun tearDown() {
        try {
            unmockkStatic(GmsDocumentScanningResult::class)
        } catch (_: Exception) {
            // May not have been mocked
        }
    }

    @Nested
    @DisplayName("processResult(resultCode, data)")
    inner class ProcessResultFromIntent {

        @BeforeEach
        fun setupStaticMock() {
            mockkStatic(GmsDocumentScanningResult::class)
        }

        @Test
        @DisplayName("returns failure when scanning result is null (cancelled)")
        fun returnsFailureWhenResultNull() {
            val intent = mockk<Intent>()
            every { GmsDocumentScanningResult.fromActivityResultIntent(intent) } returns null

            val result = scanner.processResult(-1, intent)

            assertTrue(result.isFailure)
            val exception = result.exceptionOrNull()
            assertNotNull(exception)
            assertTrue(exception is ScannerException)
            assertTrue(exception!!.message!!.contains("cancelled"))
        }

        @Test
        @DisplayName("returns failure when pages list is empty")
        fun returnsFailureWhenPagesEmpty() {
            val intent = mockk<Intent>()
            val scanningResult = mockk<GmsDocumentScanningResult>()
            every { GmsDocumentScanningResult.fromActivityResultIntent(intent) } returns scanningResult
            every { scanningResult.pages } returns emptyList()

            val result = scanner.processResult(-1, intent)

            assertTrue(result.isFailure)
            val exception = result.exceptionOrNull()
            assertTrue(exception is ScannerException)
            assertTrue(exception!!.message!!.contains("No pages"))
        }

        @Test
        @DisplayName("returns failure when pages list is null")
        fun returnsFailureWhenPagesNull() {
            val intent = mockk<Intent>()
            val scanningResult = mockk<GmsDocumentScanningResult>()
            every { GmsDocumentScanningResult.fromActivityResultIntent(intent) } returns scanningResult
            every { scanningResult.pages } returns null

            val result = scanner.processResult(-1, intent)

            assertTrue(result.isFailure)
            val exception = result.exceptionOrNull()
            assertTrue(exception is ScannerException)
            assertTrue(exception!!.message!!.contains("No pages"))
        }

        @Test
        @DisplayName("returns ImportedDocument with JPG format on success")
        fun returnsImportedDocumentOnSuccess() {
            val intent = mockk<Intent>()
            val scanningResult = mockk<GmsDocumentScanningResult>()
            val page = mockk<GmsDocumentScanningResult.Page>()
            val uri = mockk<Uri>()
            val imageBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())

            every { GmsDocumentScanningResult.fromActivityResultIntent(intent) } returns scanningResult
            every { scanningResult.pages } returns listOf(page)
            every { page.imageUri } returns uri
            every { contentResolver.openInputStream(uri) } returns ByteArrayInputStream(imageBytes)

            val result = scanner.processResult(0, intent)

            assertTrue(result.isSuccess)
            val document = result.getOrNull()!!
            assertEquals(DocumentFormat.JPG, document.format)
            assertTrue(document.rawBytes.contentEquals(imageBytes))
            assertEquals("scanned_document.jpg", document.originalFileName)
        }

        @Test
        @DisplayName("returns failure with descriptive message on IOException")
        fun returnsFailureOnIOException() {
            val intent = mockk<Intent>()
            val scanningResult = mockk<GmsDocumentScanningResult>()
            val page = mockk<GmsDocumentScanningResult.Page>()
            val uri = mockk<Uri>()

            every { GmsDocumentScanningResult.fromActivityResultIntent(intent) } returns scanningResult
            every { scanningResult.pages } returns listOf(page)
            every { page.imageUri } returns uri
            every { contentResolver.openInputStream(uri) } throws IOException("File not found")

            val result = scanner.processResult(0, intent)

            assertTrue(result.isFailure)
            val exception = result.exceptionOrNull()
            assertTrue(exception is ScannerException)
            assertTrue(exception!!.message!!.contains("Failed to read"))
        }

        @Test
        @DisplayName("returns failure with descriptive message on SecurityException")
        fun returnsFailureOnSecurityException() {
            val intent = mockk<Intent>()
            val scanningResult = mockk<GmsDocumentScanningResult>()
            val page = mockk<GmsDocumentScanningResult.Page>()
            val uri = mockk<Uri>()

            every { GmsDocumentScanningResult.fromActivityResultIntent(intent) } returns scanningResult
            every { scanningResult.pages } returns listOf(page)
            every { page.imageUri } returns uri
            every { contentResolver.openInputStream(uri) } throws SecurityException("No permission")

            val result = scanner.processResult(0, intent)

            assertTrue(result.isFailure)
            val exception = result.exceptionOrNull()
            assertTrue(exception is ScannerException)
            assertTrue(exception!!.message!!.contains("Permission denied"))
        }
    }

    @Nested
    @DisplayName("processResult(GmsDocumentScanningResult)")
    inner class ProcessResultFromScanningResult {

        @Test
        @DisplayName("returns failure when pages list is empty")
        fun returnsFailureWhenPagesEmpty() {
            val scanningResult = mockk<GmsDocumentScanningResult>()
            every { scanningResult.pages } returns emptyList()

            val result = scanner.processResult(scanningResult)

            assertTrue(result.isFailure)
            val exception = result.exceptionOrNull()
            assertTrue(exception is ScannerException)
            assertTrue(exception!!.message!!.contains("No pages"))
        }

        @Test
        @DisplayName("returns ImportedDocument with JPG format on success")
        fun returnsImportedDocumentOnSuccess() {
            val scanningResult = mockk<GmsDocumentScanningResult>()
            val page = mockk<GmsDocumentScanningResult.Page>()
            val uri = mockk<Uri>()
            val imageBytes = byteArrayOf(0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte())

            every { scanningResult.pages } returns listOf(page)
            every { page.imageUri } returns uri
            every { contentResolver.openInputStream(uri) } returns ByteArrayInputStream(imageBytes)

            val result = scanner.processResult(scanningResult)

            assertTrue(result.isSuccess)
            val document = result.getOrNull()!!
            assertEquals(DocumentFormat.JPG, document.format)
            assertTrue(document.rawBytes.contentEquals(imageBytes))
            assertEquals("scanned_document.jpg", document.originalFileName)
        }

        @Test
        @DisplayName("returns failure on IOException during URI reading")
        fun returnsFailureOnIOException() {
            val scanningResult = mockk<GmsDocumentScanningResult>()
            val page = mockk<GmsDocumentScanningResult.Page>()
            val uri = mockk<Uri>()

            every { scanningResult.pages } returns listOf(page)
            every { page.imageUri } returns uri
            every { contentResolver.openInputStream(uri) } throws IOException("Stream error")

            val result = scanner.processResult(scanningResult)

            assertTrue(result.isFailure)
            val exception = result.exceptionOrNull()
            assertTrue(exception is ScannerException)
            assertTrue(exception!!.message!!.contains("Failed to read"))
        }
    }

    @Nested
    @DisplayName("readBytesFromUri")
    inner class ReadBytesFromUri {

        @Test
        @DisplayName("reads bytes from content resolver input stream")
        fun readsBytesFromInputStream() {
            val uri = mockk<Uri>()
            val expectedBytes = byteArrayOf(1, 2, 3, 4, 5)
            every { contentResolver.openInputStream(uri) } returns ByteArrayInputStream(expectedBytes)

            val result = scanner.readBytesFromUri(uri)

            assertTrue(result.contentEquals(expectedBytes))
        }

        @Test
        @DisplayName("throws IOException when input stream is null")
        fun throwsWhenInputStreamNull() {
            val uri = mockk<Uri>()
            every { contentResolver.openInputStream(uri) } returns null

            var thrownException: IOException? = null
            try {
                scanner.readBytesFromUri(uri)
            } catch (e: IOException) {
                thrownException = e
            }

            assertNotNull(thrownException)
            assertTrue(thrownException!!.message!!.contains("Unable to open input stream"))
        }

        @Test
        @DisplayName("reads large byte arrays correctly")
        fun readsLargeByteArrays() {
            val uri = mockk<Uri>()
            val largeBytes = ByteArray(1024 * 1024) { (it % 256).toByte() } // 1MB
            every { contentResolver.openInputStream(uri) } returns ByteArrayInputStream(largeBytes)

            val result = scanner.readBytesFromUri(uri)

            assertTrue(result.contentEquals(largeBytes))
            assertEquals(1024 * 1024, result.size)
        }
    }
}
