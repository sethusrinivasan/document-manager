package com.app.traveldocs.data.dicom

import android.graphics.Bitmap
import com.app.traveldocs.debug.DebugLogger
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Hand-rolled DICOM parser. No library dependencies.
 *
 * Why build our own? Every existing Android DICOM lib is either GPL (can't use in Apache 2.0 project)
 * or a massive Java dependency tree we don't need. For viewing uncompressed medical images on a phone,
 * we only need ~200 lines of parsing logic.
 *
 * What it handles:
 * - Explicit VR, little-endian (the common case for modern DICOM files)
 * - 8-bit and 16-bit grayscale with window/level contrast adjustment
 * - RGB color images (3 samples per pixel)
 * - MONOCHROME1 (inverted) and MONOCHROME2 (normal) photometric interpretations
 *
 * What it does NOT handle (yet):
 * - JPEG2000 or RLE compressed pixel data
 * - Implicit VR / big-endian transfer syntaxes
 * - Multi-frame images (only first frame would render)
 * - Overlay planes
 *
 * The DICOM format is basically: 128-byte preamble → "DICM" magic → stream of tagged data elements.
 * We walk the elements looking for image dimensions, bit depth, and the pixel data blob at (7FE0,0010).
 * Then we apply window/level and dump it into an ARGB_8888 Bitmap.
 */
class DicomParser {

    data class DicomImage(
        val rows: Int,
        val columns: Int,
        val bitsAllocated: Int,
        val bitsStored: Int,
        val highBit: Int,
        val pixelRepresentation: Int,  // 0=unsigned, 1=signed
        val samplesPerPixel: Int,
        val photometricInterpretation: String,
        val windowCenter: Float?,
        val windowWidth: Float?,
        val pixelData: ByteArray,
        val metadata: Map<String, String>  // Human-readable metadata
    )

    /**
     * Parse a DICOM file and extract the image + metadata.
     * Returns null if the file cannot be parsed.
     */
    fun parse(bytes: ByteArray): DicomImage? {
        if (bytes.size < 132) return null

        // Check DICM magic at offset 128
        val magic = String(bytes, 128, 4)
        if (magic != "DICM") {
            DebugLogger.w("DICOM", "Missing DICM magic at offset 128")
            return null
        }

        var offset = 132
        var rows = 0
        var columns = 0
        var bitsAllocated = 16
        var bitsStored = 12
        var highBit = 11
        var pixelRepresentation = 0
        var samplesPerPixel = 1
        var photometric = "MONOCHROME2"
        var windowCenter: Float? = null
        var windowWidth: Float? = null
        var pixelData: ByteArray? = null
        val metadata = mutableMapOf<String, String>()

        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        while (offset < bytes.size - 8) {
            val group = buf.getShort(offset).toInt() and 0xFFFF
            val element = buf.getShort(offset + 2).toInt() and 0xFFFF
            offset += 4

            // Read VR (2 bytes) for explicit VR
            val vr = if (offset + 2 <= bytes.size) {
                String(bytes, offset, 2)
            } else break

            val isExplicitVR = vr.matches(Regex("[A-Z]{2}"))

            val valueLength: Int
            if (isExplicitVR) {
                offset += 2
                valueLength = if (vr in listOf("OB", "OD", "OF", "OL", "OW", "SQ", "UC", "UN", "UR", "UT")) {
                    // 2 bytes reserved + 4 bytes length
                    offset += 2
                    val len = buf.getInt(offset)
                    offset += 4
                    if (len == -1) 0 else len  // Undefined length sequences
                } else {
                    // 2 bytes length
                    val len = buf.getShort(offset).toInt() and 0xFFFF
                    offset += 2
                    len
                }
            } else {
                // Implicit VR: 4 bytes length
                valueLength = buf.getInt(offset)
                offset += 4
            }

            if (valueLength < 0 || offset + valueLength > bytes.size) break

            // Parse known tags
            when {
                group == 0x0028 && element == 0x0010 -> {
                    rows = buf.getShort(offset).toInt() and 0xFFFF
                    metadata["Rows"] = rows.toString()
                }
                group == 0x0028 && element == 0x0011 -> {
                    columns = buf.getShort(offset).toInt() and 0xFFFF
                    metadata["Columns"] = columns.toString()
                }
                group == 0x0028 && element == 0x0100 -> {
                    bitsAllocated = buf.getShort(offset).toInt() and 0xFFFF
                    metadata["Bits Allocated"] = bitsAllocated.toString()
                }
                group == 0x0028 && element == 0x0101 -> {
                    bitsStored = buf.getShort(offset).toInt() and 0xFFFF
                    metadata["Bits Stored"] = bitsStored.toString()
                }
                group == 0x0028 && element == 0x0102 -> {
                    highBit = buf.getShort(offset).toInt() and 0xFFFF
                }
                group == 0x0028 && element == 0x0103 -> {
                    pixelRepresentation = buf.getShort(offset).toInt() and 0xFFFF
                }
                group == 0x0028 && element == 0x0002 -> {
                    samplesPerPixel = buf.getShort(offset).toInt() and 0xFFFF
                    metadata["Samples/Pixel"] = samplesPerPixel.toString()
                }
                group == 0x0028 && element == 0x0004 -> {
                    photometric = String(bytes, offset, valueLength).trim().replace("\u0000", "")
                    metadata["Photometric"] = photometric
                }
                group == 0x0028 && element == 0x1050 -> {
                    val wcStr = String(bytes, offset, valueLength).trim().replace("\u0000", "")
                    windowCenter = wcStr.split("\\").firstOrNull()?.trim()?.toFloatOrNull()
                    metadata["Window Center"] = wcStr
                }
                group == 0x0028 && element == 0x1051 -> {
                    val wwStr = String(bytes, offset, valueLength).trim().replace("\u0000", "")
                    windowWidth = wwStr.split("\\").firstOrNull()?.trim()?.toFloatOrNull()
                    metadata["Window Width"] = wwStr
                }
                group == 0x0010 && element == 0x0010 -> {
                    metadata["Patient Name"] = String(bytes, offset, valueLength).trim().replace("\u0000", "")
                }
                group == 0x0008 && element == 0x0060 -> {
                    metadata["Modality"] = String(bytes, offset, valueLength).trim().replace("\u0000", "")
                }
                group == 0x0008 && element == 0x0020 -> {
                    metadata["Study Date"] = String(bytes, offset, valueLength).trim().replace("\u0000", "")
                }
                group == 0x0008 && element == 0x1030 -> {
                    metadata["Study Description"] = String(bytes, offset, valueLength).trim().replace("\u0000", "")
                }
                group == 0x7FE0 && element == 0x0010 -> {
                    pixelData = bytes.copyOfRange(offset, offset + valueLength)
                    break  // Pixel data is always last significant element
                }
            }

            offset += valueLength
        }

        if (pixelData == null || rows == 0 || columns == 0) {
            DebugLogger.w("DICOM", "Parse incomplete: rows=$rows cols=$columns pixelData=${pixelData?.size}")
            return null
        }

        metadata["Image Size"] = "${columns}x${rows}"
        DebugLogger.i("DICOM", "Parsed: ${columns}x${rows}, $bitsAllocated-bit, $photometric, pixelData=${pixelData.size} bytes")

        return DicomImage(
            rows = rows, columns = columns,
            bitsAllocated = bitsAllocated, bitsStored = bitsStored,
            highBit = highBit, pixelRepresentation = pixelRepresentation,
            samplesPerPixel = samplesPerPixel,
            photometricInterpretation = photometric,
            windowCenter = windowCenter, windowWidth = windowWidth,
            pixelData = pixelData, metadata = metadata
        )
    }

    /**
     * Convert parsed DICOM image to an Android Bitmap.
     * Applies window/level (contrast) adjustment for grayscale images.
     */
    fun toBitmap(image: DicomImage): Bitmap? {
        val width = image.columns
        val height = image.rows
        if (width <= 0 || height <= 0) return null

        return try {
            if (image.samplesPerPixel == 3) {
                renderRgb(image, width, height)
            } else {
                renderGrayscale(image, width, height)
            }
        } catch (e: Exception) {
            DebugLogger.e("DICOM", "Bitmap conversion failed", e)
            null
        }
    }

    private fun renderGrayscale(image: DicomImage, width: Int, height: Int): Bitmap {
        val pixelCount = width * height
        val pixels = IntArray(pixelCount)
        val buf = ByteBuffer.wrap(image.pixelData).order(ByteOrder.LITTLE_ENDIAN)

        // Calculate window/level
        val wc = image.windowCenter ?: ((1 shl image.bitsStored) / 2).toFloat()
        val ww = image.windowWidth ?: (1 shl image.bitsStored).toFloat()
        val minVal = wc - ww / 2f
        val maxVal = wc + ww / 2f

        val invert = image.photometricInterpretation == "MONOCHROME1"

        for (i in 0 until pixelCount) {
            val rawPixel: Int = if (image.bitsAllocated == 16) {
                if (i * 2 + 1 >= image.pixelData.size) break
                val v = buf.getShort(i * 2).toInt()
                if (image.pixelRepresentation == 1) v else (v and 0xFFFF)
            } else {
                // 8-bit
                if (i >= image.pixelData.size) break
                image.pixelData[i].toInt() and 0xFF
            }

            // Apply window/level
            var normalized = ((rawPixel - minVal) / (maxVal - minVal)).coerceIn(0f, 1f)
            if (invert) normalized = 1f - normalized
            val gray = (normalized * 255).toInt()
            pixels[i] = (0xFF shl 24) or (gray shl 16) or (gray shl 8) or gray
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    private fun renderRgb(image: DicomImage, width: Int, height: Int): Bitmap {
        val pixelCount = width * height
        val pixels = IntArray(pixelCount)
        val bytesPerSample = image.bitsAllocated / 8

        for (i in 0 until pixelCount) {
            val offset = i * 3 * bytesPerSample
            if (offset + 3 * bytesPerSample > image.pixelData.size) break

            val r: Int
            val g: Int
            val b: Int
            if (bytesPerSample == 1) {
                r = image.pixelData[offset].toInt() and 0xFF
                g = image.pixelData[offset + 1].toInt() and 0xFF
                b = image.pixelData[offset + 2].toInt() and 0xFF
            } else {
                r = ((image.pixelData[offset + 1].toInt() and 0xFF) shl 8 or (image.pixelData[offset].toInt() and 0xFF)) shr (image.bitsAllocated - 8)
                g = ((image.pixelData[offset + 3].toInt() and 0xFF) shl 8 or (image.pixelData[offset + 2].toInt() and 0xFF)) shr (image.bitsAllocated - 8)
                b = ((image.pixelData[offset + 5].toInt() and 0xFF) shl 8 or (image.pixelData[offset + 4].toInt() and 0xFF)) shr (image.bitsAllocated - 8)
            }
            pixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    companion object {
        /**
         * Check if a byte array is a DICOM file (has DICM magic at offset 128).
         */
        fun isDicom(bytes: ByteArray): Boolean {
            return bytes.size > 132 && String(bytes, 128, 4) == "DICM"
        }
    }
}
