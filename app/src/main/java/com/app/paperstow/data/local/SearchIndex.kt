package com.app.paperstow.data.local

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.app.paperstow.domain.model.DocumentFormat
import java.io.ByteArrayOutputStream
import java.io.File

object SearchIndex {
    const val MAX_CHARS = 50_000

    fun textForImport(format: DocumentFormat, bytes: ByteArray, ocrText: String): String {
        val fromFile = when (format) {
            DocumentFormat.TEXT, DocumentFormat.MARKDOWN, DocumentFormat.GPX ->
                runCatching { String(bytes, Charsets.UTF_8) }.getOrDefault("")
            else -> ""
        }
        return (ocrText.ifBlank { fromFile }).trim().take(MAX_CHARS)
    }

    fun isImage(format: DocumentFormat): Boolean = format in setOf(
        DocumentFormat.JPG, DocumentFormat.PNG, DocumentFormat.WEBP,
        DocumentFormat.HEIC, DocumentFormat.BMP, DocumentFormat.GIF
    )

    fun isPlainText(format: DocumentFormat): Boolean = format in setOf(
        DocumentFormat.TEXT, DocumentFormat.MARKDOWN, DocumentFormat.GPX
    )

    /** Image bytes ML Kit can read: the file itself, or rasterized PDF pages. */
    fun imagesForOcr(format: DocumentFormat, bytes: ByteArray, cacheDir: File): List<ByteArray> = when {
        isImage(format) -> listOf(bytes)
        format == DocumentFormat.PDF -> pdfPagesAsJpeg(bytes, cacheDir)
        else -> emptyList()
    }

    fun pdfPagesAsJpeg(bytes: ByteArray, cacheDir: File, maxPages: Int = 4): List<ByteArray> {
        if (bytes.size < 5 || String(bytes, 0, 5, Charsets.ISO_8859_1) != "%PDF-") return emptyList()
        val file = File.createTempFile("ocr", ".pdf", cacheDir)
        return try {
            file.writeBytes(bytes)
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            try {
                val count = minOf(renderer.pageCount, maxPages)
                (0 until count).mapNotNull { i ->
                    val page = renderer.openPage(i)
                    try {
                        val w = (page.width * 1.5f).toInt().coerceIn(400, 1600)
                        val h = (page.height * 1.5f).toInt().coerceIn(400, 2200)
                        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        // PdfRenderer paints onto a transparent bitmap. JPEG drops alpha,
                        // so unpainted pixels become black and ML Kit reads nothing.
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        val out = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
                        bitmap.recycle()
                        out.toByteArray()
                    } finally {
                        page.close()
                    }
                }
            } finally {
                renderer.close()
                pfd.close()
            }
        } catch (_: Exception) {
            emptyList()
        } finally {
            file.delete()
        }
    }
}
