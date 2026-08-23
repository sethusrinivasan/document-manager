package com.app.traveldocs.data.importer

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.app.traveldocs.domain.model.DocumentFormat
import com.app.traveldocs.domain.model.ImportedDocument
import com.app.traveldocs.domain.repository.DocumentImporter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implements file import with format validation for the Document Manager.
 * Accepts PDF, JPG, and PNG formats, validates content via magic bytes,
 * and returns descriptive errors for unsupported or invalid files.
 */
@Singleton
class FileDocumentImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val formatValidator: DocumentFormatValidator
) : DocumentImporter {

    companion object {
        private val MIME_TYPE_TO_FORMAT = mapOf(
            "application/pdf" to DocumentFormat.PDF,
            "image/jpeg" to DocumentFormat.JPG,
            "image/jpg" to DocumentFormat.JPG,
            "image/png" to DocumentFormat.PNG
        )

        private val EXTENSION_TO_FORMAT = mapOf(
            "pdf" to DocumentFormat.PDF,
            "jpg" to DocumentFormat.JPG,
            "jpeg" to DocumentFormat.JPG,
            "png" to DocumentFormat.PNG
        )
    }

    override suspend fun importFromCamera(): Result<ImportedDocument> {
        // Camera import is handled by a separate ML Kit Scanner component (task 6.1)
        return Result.failure(UnsupportedOperationException("Camera import not implemented in FileDocumentImporter"))
    }

    override suspend fun importFromGoogleDrive(fileUri: Uri): Result<ImportedDocument> {
        // Google Drive import is handled by a separate component (task 6.2)
        return Result.failure(UnsupportedOperationException("Google Drive import not implemented in FileDocumentImporter"))
    }

    override suspend fun importFromFile(uri: Uri): Result<ImportedDocument> {
        return try {
            val contentResolver = context.contentResolver

            // Read file bytes
            val bytes = readFileBytes(contentResolver, uri)
                ?: return Result.failure(IOException("Failed to read file content from URI"))

            // Validate non-empty
            if (bytes.isEmpty()) {
                return Result.failure(
                    IllegalArgumentException("File is empty. Please select a file with content.")
                )
            }

            // Determine format from MIME type or file extension
            val mimeType = contentResolver.getType(uri)
            val fileName = getFileName(contentResolver, uri)
            val hintFormat = getFormatFromMimeType(mimeType) ?: getFormatFromFileName(fileName)

            // Validate and detect format using magic bytes
            val detectedFormat = formatValidator.validateAndDetectFormat(bytes, mimeType)

            if (detectedFormat == null) {
                // Magic bytes don't match any supported format
                val formatDescription = if (hintFormat != null) {
                    "File claims to be ${hintFormat.name} but content does not match expected format."
                } else {
                    val extension = fileName?.substringAfterLast('.', "") ?: "unknown"
                    "Unsupported file format '$extension'. Supported formats: PDF, JPG, PNG."
                }
                return Result.failure(IllegalArgumentException(formatDescription))
            }

            // If we have a hint format from MIME/extension, verify it matches detected format
            if (hintFormat != null && hintFormat != detectedFormat) {
                return Result.failure(
                    IllegalArgumentException(
                        "File content mismatch: file claims to be ${hintFormat.name} " +
                            "but actual content is ${detectedFormat.name}."
                    )
                )
            }

            Result.success(
                ImportedDocument(
                    rawBytes = bytes,
                    format = detectedFormat,
                    originalFileName = fileName
                )
            )
        } catch (e: IOException) {
            Result.failure(
                IOException("Failed to read file: ${e.message}", e)
            )
        } catch (e: SecurityException) {
            Result.failure(
                IOException("Permission denied when reading file: ${e.message}", e)
            )
        }
    }

    override fun getSupportedFormats(): List<DocumentFormat> {
        return listOf(DocumentFormat.PDF, DocumentFormat.JPG, DocumentFormat.PNG)
    }

    private fun readFileBytes(contentResolver: ContentResolver, uri: Uri): ByteArray? {
        return contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.readBytes()
        }
    }

    private fun getFileName(contentResolver: ContentResolver, uri: Uri): String? {
        // Try to get display name from content resolver
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    return cursor.getString(nameIndex)
                }
            }
        }
        // Fallback to last path segment
        return uri.lastPathSegment
    }

    private fun getFormatFromMimeType(mimeType: String?): DocumentFormat? {
        if (mimeType == null) return null
        return MIME_TYPE_TO_FORMAT[mimeType.lowercase()]
    }

    private fun getFormatFromFileName(fileName: String?): DocumentFormat? {
        if (fileName == null) return null
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return EXTENSION_TO_FORMAT[extension]
    }
}
