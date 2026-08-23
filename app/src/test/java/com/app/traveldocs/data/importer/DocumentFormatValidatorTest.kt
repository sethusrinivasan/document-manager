package com.app.traveldocs.data.importer

import com.app.traveldocs.domain.model.DocumentFormat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@DisplayName("DocumentFormatValidator")
class DocumentFormatValidatorTest {

    private lateinit var validator: DocumentFormatValidator

    @BeforeEach
    fun setUp() {
        validator = DocumentFormatValidator()
    }

    @Nested
    @DisplayName("validateAndDetectFormat")
    inner class ValidateAndDetectFormat {

        @Test
        @DisplayName("returns PDF for valid PDF magic bytes")
        fun `returns PDF for valid PDF magic bytes`() {
            val pdfBytes = byteArrayOf(0x25, 0x50, 0x44, 0x46) + "rest of content".toByteArray()
            val result = validator.validateAndDetectFormat(pdfBytes, "application/pdf")
            assertEquals(DocumentFormat.PDF, result)
        }

        @Test
        @DisplayName("returns JPG for valid JPEG magic bytes")
        fun `returns JPG for valid JPEG magic bytes`() {
            val jpegBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()) + "jpeg data".toByteArray()
            val result = validator.validateAndDetectFormat(jpegBytes, "image/jpeg")
            assertEquals(DocumentFormat.JPG, result)
        }

        @Test
        @DisplayName("returns PNG for valid PNG magic bytes")
        fun `returns PNG for valid PNG magic bytes`() {
            val pngBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47) + "png data".toByteArray()
            val result = validator.validateAndDetectFormat(pngBytes, "image/png")
            assertEquals(DocumentFormat.PNG, result)
        }

        @Test
        @DisplayName("returns null for empty bytes")
        fun `returns null for empty bytes`() {
            val result = validator.validateAndDetectFormat(byteArrayOf(), "application/pdf")
            assertNull(result)
        }

        @Test
        @DisplayName("returns null for unsupported format")
        fun `returns null for unsupported format`() {
            val gifBytes = byteArrayOf(0x47, 0x49, 0x46, 0x38) // GIF89
            val result = validator.validateAndDetectFormat(gifBytes, "image/gif")
            assertNull(result)
        }

        @Test
        @DisplayName("detects format from magic bytes even without mime type")
        fun `detects format from magic bytes even without mime type`() {
            val pdfBytes = byteArrayOf(0x25, 0x50, 0x44, 0x46) + "content".toByteArray()
            val result = validator.validateAndDetectFormat(pdfBytes, null)
            assertEquals(DocumentFormat.PDF, result)
        }

        @Test
        @DisplayName("returns null when bytes too short")
        fun `returns null when bytes too short`() {
            val shortBytes = byteArrayOf(0x25, 0x50) // Only 2 bytes - not enough for any format
            val result = validator.validateAndDetectFormat(shortBytes, "application/pdf")
            assertNull(result)
        }

        @Test
        @DisplayName("returns null for random bytes with valid mime type")
        fun `returns null for random bytes with valid mime type`() {
            val randomBytes = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05)
            val result = validator.validateAndDetectFormat(randomBytes, "application/pdf")
            assertNull(result)
        }
    }

    @Nested
    @DisplayName("detectFromMagicBytes")
    inner class DetectFromMagicBytes {

        @Test
        @DisplayName("detects PDF from magic bytes")
        fun `detects PDF from magic bytes`() {
            val bytes = byteArrayOf(0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34) // %PDF-1.4
            assertEquals(DocumentFormat.PDF, validator.detectFromMagicBytes(bytes))
        }

        @Test
        @DisplayName("detects JPEG from magic bytes")
        fun `detects JPEG from magic bytes`() {
            val bytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())
            assertEquals(DocumentFormat.JPG, validator.detectFromMagicBytes(bytes))
        }

        @Test
        @DisplayName("detects PNG from magic bytes")
        fun `detects PNG from magic bytes`() {
            val bytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
            assertEquals(DocumentFormat.PNG, validator.detectFromMagicBytes(bytes))
        }

        @Test
        @DisplayName("returns null for empty bytes")
        fun `returns null for empty bytes`() {
            assertNull(validator.detectFromMagicBytes(byteArrayOf()))
        }
    }

    @Nested
    @DisplayName("validateContent")
    inner class ValidateContent {

        @Test
        @DisplayName("validates PDF content correctly")
        fun `validates PDF content correctly`() {
            val pdfBytes = byteArrayOf(0x25, 0x50, 0x44, 0x46) + "content".toByteArray()
            assertTrue(validator.validateContent(pdfBytes, DocumentFormat.PDF))
        }

        @Test
        @DisplayName("rejects non-PDF content when expecting PDF")
        fun `rejects non-PDF content when expecting PDF`() {
            val jpegBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()) + "data".toByteArray()
            assertFalse(validator.validateContent(jpegBytes, DocumentFormat.PDF))
        }

        @Test
        @DisplayName("validates JPG content correctly")
        fun `validates JPG content correctly`() {
            val jpegBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()) + "data".toByteArray()
            assertTrue(validator.validateContent(jpegBytes, DocumentFormat.JPG))
        }

        @Test
        @DisplayName("validates PNG content correctly")
        fun `validates PNG content correctly`() {
            val pngBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47) + "data".toByteArray()
            assertTrue(validator.validateContent(pngBytes, DocumentFormat.PNG))
        }

        @Test
        @DisplayName("rejects empty bytes for any format")
        fun `rejects empty bytes for any format`() {
            assertFalse(validator.validateContent(byteArrayOf(), DocumentFormat.PDF))
            assertFalse(validator.validateContent(byteArrayOf(), DocumentFormat.JPG))
            assertFalse(validator.validateContent(byteArrayOf(), DocumentFormat.PNG))
        }
    }

    @Test
    fun `validates BMP magic bytes`() {
        val bmpHeader = byteArrayOf(0x42, 0x4D) + ByteArray(50)
        assertEquals(com.app.traveldocs.domain.model.DocumentFormat.BMP, validator.detectFromMagicBytes(bmpHeader))
    }

    @Test
    fun `validates GIF magic bytes`() {
        val gifHeader = "GIF89a".toByteArray() + ByteArray(50)
        assertEquals(com.app.traveldocs.domain.model.DocumentFormat.GIF, validator.detectFromMagicBytes(gifHeader))
    }

    @Test
    fun `validates WebP magic bytes`() {
        val webp = byteArrayOf(0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00, 0x57, 0x45, 0x42, 0x50) + ByteArray(50)
        assertEquals(com.app.traveldocs.domain.model.DocumentFormat.WEBP, validator.detectFromMagicBytes(webp))
    }

    @Test
    fun `UNKNOWN format always validates`() {
        assertTrue(validator.validateContent(ByteArray(10), com.app.traveldocs.domain.model.DocumentFormat.UNKNOWN))
    }

}