package com.app.traveldocs.data.importer

import com.app.traveldocs.domain.model.DocumentFormat
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Validates file content by inspecting magic bytes and determines the actual document format.
 * Returns null if the format is unsupported or the content doesn't match the expected magic bytes.
 */
@Singleton
class DocumentFormatValidator @Inject constructor() {

    companion object {
        // PDF magic bytes: %PDF (0x25 0x50 0x44 0x46)
        private val PDF_MAGIC = byteArrayOf(0x25, 0x50, 0x44, 0x46)

        // JPEG magic bytes: FF D8 FF
        private val JPEG_MAGIC = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())

        // PNG magic bytes: 89 50 4E 47 (‰PNG)
        private val PNG_MAGIC = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)
    }

    /**
     * Validates file content and detects the format based on magic bytes.
     *
     * @param bytes The raw file content
     * @param mimeType Optional MIME type hint from the content resolver
     * @return The detected [DocumentFormat], or null if unsupported or content validation fails
     */
    fun validateAndDetectFormat(bytes: ByteArray, mimeType: String?): DocumentFormat? {
        if (bytes.isEmpty()) return null

        // First, try to detect format from magic bytes
        val detectedFormat = detectFromMagicBytes(bytes)

        // If we detected a format from magic bytes, use it
        if (detectedFormat != null) return detectedFormat

        // If magic bytes don't match any known format, try MIME type as fallback
        // but still require magic bytes to match for security
        return null
    }

    /**
     * Detects format purely from magic bytes in the file content.
     */
    fun detectFromMagicBytes(bytes: ByteArray): DocumentFormat? {
        if (bytes.isEmpty()) return null

        return when {
            isPdf(bytes) -> DocumentFormat.PDF
            isJpeg(bytes) -> DocumentFormat.JPG
            isPng(bytes) -> DocumentFormat.PNG
            isWebP(bytes) -> DocumentFormat.WEBP
            isBmp(bytes) -> DocumentFormat.BMP
            isGif(bytes) -> DocumentFormat.GIF
            else -> null
        }
    }

    /**
     * Validates that the file content matches the expected format.
     *
     * @param bytes The raw file content
     * @param expectedFormat The format the file claims to be
     * @return true if the magic bytes match the expected format
     */
    fun validateContent(bytes: ByteArray, expectedFormat: DocumentFormat): Boolean {
        if (bytes.isEmpty()) return false

        return when (expectedFormat) {
            DocumentFormat.PDF -> isPdf(bytes)
            DocumentFormat.JPG -> isJpeg(bytes)
            DocumentFormat.PNG -> isPng(bytes)
            DocumentFormat.VIDEO -> true
            DocumentFormat.WEBP -> isWebP(bytes)
            DocumentFormat.HEIC -> true // HEIC detection is complex; trust MIME type
            DocumentFormat.BMP -> isBmp(bytes)
            DocumentFormat.GIF -> isGif(bytes)
            DocumentFormat.DICOM -> isDicom(bytes)
            DocumentFormat.AUDIO -> true  // Audio formats validated by MIME
            DocumentFormat.UNKNOWN -> true  // Accept anything
        }
    }

    private fun isPdf(bytes: ByteArray): Boolean {
        return bytes.size >= PDF_MAGIC.size &&
            bytes.take(PDF_MAGIC.size).toByteArray().contentEquals(PDF_MAGIC)
    }

    private fun isJpeg(bytes: ByteArray): Boolean {
        return bytes.size >= JPEG_MAGIC.size &&
            bytes.take(JPEG_MAGIC.size).toByteArray().contentEquals(JPEG_MAGIC)
    }

    private fun isPng(bytes: ByteArray): Boolean {
        return bytes.size >= PNG_MAGIC.size &&
            bytes.take(PNG_MAGIC.size).toByteArray().contentEquals(PNG_MAGIC)
    }

    private fun isWebP(bytes: ByteArray): Boolean {
        // RIFF....WEBP
        return bytes.size >= 12 &&
            bytes[0] == 0x52.toByte() && bytes[1] == 0x49.toByte() &&
            bytes[2] == 0x46.toByte() && bytes[3] == 0x46.toByte() &&
            bytes[8] == 0x57.toByte() && bytes[9] == 0x45.toByte() &&
            bytes[10] == 0x42.toByte() && bytes[11] == 0x50.toByte()
    }

    private fun isBmp(bytes: ByteArray): Boolean {
        // BM header
        return bytes.size >= 2 && bytes[0] == 0x42.toByte() && bytes[1] == 0x4D.toByte()
    }

    private fun isDicom(bytes: ByteArray): Boolean {
        // DICM magic at offset 128
        return bytes.size > 132 &&
            bytes[128] == 0x44.toByte() && bytes[129] == 0x49.toByte() &&
            bytes[130] == 0x43.toByte() && bytes[131] == 0x4D.toByte()
    }

    private fun isGif(bytes: ByteArray): Boolean {
        // GIF87a or GIF89a
        return bytes.size >= 6 &&
            bytes[0] == 0x47.toByte() && bytes[1] == 0x49.toByte() &&
            bytes[2] == 0x46.toByte() && bytes[3] == 0x38.toByte()
    }
}
