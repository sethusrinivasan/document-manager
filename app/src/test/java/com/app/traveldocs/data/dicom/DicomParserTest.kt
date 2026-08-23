package com.app.traveldocs.data.dicom

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DicomParserTest {
    private val parser = DicomParser()

    @Test
    fun `isDicom returns false for empty bytes`() {
        assertFalse(DicomParser.isDicom(ByteArray(0)))
    }

    @Test
    fun `isDicom returns false for small file`() {
        assertFalse(DicomParser.isDicom(ByteArray(100)))
    }

    @Test
    fun `isDicom returns true for valid DICM magic`() {
        val bytes = ByteArray(200)
        bytes[128] = 'D'.code.toByte()
        bytes[129] = 'I'.code.toByte()
        bytes[130] = 'C'.code.toByte()
        bytes[131] = 'M'.code.toByte()
        assertTrue(DicomParser.isDicom(bytes))
    }

    @Test
    fun `parse returns null for non-DICOM data`() {
        assertNull(parser.parse(ByteArray(50)))
        assertNull(parser.parse("not a dicom file".toByteArray()))
    }

    @Test
    fun `toBitmap returns null for zero-dimension image`() {
        val img = DicomParser.DicomImage(
            rows = 0, columns = 0, bitsAllocated = 8, bitsStored = 8,
            highBit = 7, pixelRepresentation = 0, samplesPerPixel = 1,
            photometricInterpretation = "MONOCHROME2",
            windowCenter = null, windowWidth = null,
            pixelData = ByteArray(100), metadata = emptyMap()
        )
        assertNull(parser.toBitmap(img))
    }
}
