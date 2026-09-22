package com.app.paperstow.data.scanner

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import com.app.paperstow.debug.DebugLogger
import com.app.paperstow.domain.model.DocumentFormat
import com.app.paperstow.domain.model.ImportedDocument
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-device document scan via ML Kit / Google Play services.
 *
 * Edge detection, crop, filters, and the camera UI run in Play services.
 * Paperstow does not need CAMERA permission for this path. JPEG pages are
 * read locally and fed into the normal import pipeline.
 */
@Singleton
class CameraDocumentScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val options = GmsDocumentScannerOptions.Builder()
        .setGalleryImportAllowed(false)
        .setPageLimit(MAX_PAGES)
        .setResultFormats(RESULT_FORMAT_JPEG)
        .setScannerMode(SCANNER_MODE_FULL)
        .build()

    fun getStartIntent(
        activity: Activity,
        onReady: (IntentSender) -> Unit,
        onError: (Exception) -> Unit
    ) {
        GmsDocumentScanning.getClient(options)
            .getStartScanIntent(activity)
            .addOnSuccessListener { sender ->
                DebugLogger.i(TAG, "Document scanner ready")
                onReady(sender)
            }
            .addOnFailureListener { e ->
                DebugLogger.e(TAG, "Document scanner unavailable", e)
                onError(e)
            }
    }

    fun pagesFromResult(data: Intent?): List<ImportedDocument> {
        val result = GmsDocumentScanningResult.fromActivityResultIntent(data) ?: return emptyList()
        val pages = result.pages.orEmpty()
        if (pages.isEmpty()) return emptyList()
        val stamp = System.currentTimeMillis()
        return pages.mapIndexedNotNull { index, page ->
            try {
                ImportedDocument(
                    rawBytes = readBytes(page.imageUri),
                    format = DocumentFormat.JPG,
                    originalFileName = "scan_${stamp}_${index + 1}.jpg"
                )
            } catch (e: Exception) {
                DebugLogger.e(TAG, "Failed to read scanned page $index", e)
                null
            }
        }
    }

    private fun readBytes(uri: Uri): ByteArray {
        return context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IOException("Unable to open scanned page: $uri")
    }

    companion object {
        const val MAX_PAGES = 4
        private const val TAG = "DocScanner"
    }
}

class ScannerException(message: String, cause: Throwable? = null) : Exception(message, cause)
