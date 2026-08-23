package com.app.traveldocs.data.scanner

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.app.traveldocs.domain.model.DocumentFormat
import com.app.traveldocs.domain.model.ImportedDocument
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
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
 * Camera-based document scanner that wraps ML Kit's Document Scanner API.
 *
 * ML Kit Document Scanner provides edge detection, perspective correction,
 * and image enhancement automatically. This class configures the scanner
 * for single-page document capture in FULL mode and processes results
 * into [ImportedDocument] instances.
 */
@Singleton
class CameraDocumentScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val scannerOptions: GmsDocumentScannerOptions = GmsDocumentScannerOptions.Builder()
        .setPageLimit(1)
        .setGalleryImportAllowed(false)
        .setResultFormats(RESULT_FORMAT_JPEG)
        .setScannerMode(SCANNER_MODE_FULL)
        .build()

    /**
     * Creates and returns the ML Kit Document Scanner client configured for
     * single-page scanning in FULL mode with JPEG output.
     */
    fun getScanner(): GmsDocumentScanner {
        return GmsDocumentScanning.getClient(scannerOptions)
    }

    /**
     * Processes the scanning result from the Activity result callback.
     *
     * Reads the first scanned page URI and converts it into an [ImportedDocument]
     * with raw JPEG bytes.
     *
     * @param resultCode The Activity result code from the scanner intent
     * @param data The Intent data returned from the scanner activity
     * @return Result containing the ImportedDocument on success or an error on failure
     */
    fun processResult(resultCode: Int, data: Intent?): Result<ImportedDocument> {
        return try {
            val scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(data)
                ?: return Result.failure(
                    ScannerException("Document scanning was cancelled or returned no result")
                )

            val pages = scanningResult.pages
            if (pages.isNullOrEmpty()) {
                return Result.failure(
                    ScannerException("No pages were captured during scanning")
                )
            }

            val pageUri = pages[0].imageUri
            val rawBytes = readBytesFromUri(pageUri)

            Result.success(
                ImportedDocument(
                    rawBytes = rawBytes,
                    format = DocumentFormat.JPG,
                    originalFileName = "scanned_document.jpg"
                )
            )
        } catch (e: IOException) {
            Result.failure(
                ScannerException("Failed to read scanned document: ${e.message}", e)
            )
        } catch (e: SecurityException) {
            Result.failure(
                ScannerException("Permission denied when reading scanned document: ${e.message}", e)
            )
        } catch (e: Exception) {
            Result.failure(
                ScannerException("Unexpected error processing scan result: ${e.message}", e)
            )
        }
    }

    /**
     * Processes a scanning result directly from [GmsDocumentScanningResult].
     *
     * @param scanningResult The result object from ML Kit Document Scanner
     * @return Result containing the ImportedDocument on success or an error on failure
     */
    fun processResult(scanningResult: GmsDocumentScanningResult): Result<ImportedDocument> {
        return try {
            val pages = scanningResult.pages
            if (pages.isNullOrEmpty()) {
                return Result.failure(
                    ScannerException("No pages were captured during scanning")
                )
            }

            val pageUri = pages[0].imageUri
            val rawBytes = readBytesFromUri(pageUri)

            Result.success(
                ImportedDocument(
                    rawBytes = rawBytes,
                    format = DocumentFormat.JPG,
                    originalFileName = "scanned_document.jpg"
                )
            )
        } catch (e: IOException) {
            Result.failure(
                ScannerException("Failed to read scanned document: ${e.message}", e)
            )
        } catch (e: SecurityException) {
            Result.failure(
                ScannerException("Permission denied when reading scanned document: ${e.message}", e)
            )
        } catch (e: Exception) {
            Result.failure(
                ScannerException("Unexpected error processing scan result: ${e.message}", e)
            )
        }
    }

    /**
     * Reads raw bytes from a content URI using the application's content resolver.
     */
    internal fun readBytesFromUri(uri: Uri): ByteArray {
        return context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.readBytes()
        } ?: throw IOException("Unable to open input stream for URI: $uri")
    }
}

/**
 * Exception type for document scanning errors, providing descriptive messages
 * about what went wrong during the scanning process.
 */
class ScannerException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
