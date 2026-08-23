package com.app.traveldocs.domain.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DocumentFormatTest {

    @Test
    fun `all expected formats exist`() {
        val formats = DocumentFormat.values().map { it.name }
        assertTrue("PDF" in formats)
        assertTrue("JPG" in formats)
        assertTrue("PNG" in formats)
        assertTrue("VIDEO" in formats)
        assertTrue("WEBP" in formats)
        assertTrue("HEIC" in formats)
        assertTrue("BMP" in formats)
        assertTrue("GIF" in formats)
        assertTrue("DICOM" in formats)
        assertTrue("UNKNOWN" in formats)
    }

    @Test
    fun `UNKNOWN format exists for unsupported files`() {
        assertNotNull(DocumentFormat.valueOf("UNKNOWN"))
    }
}
