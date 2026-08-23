package com.app.traveldocs.data.drive

import com.app.traveldocs.debug.DebugLogger
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.app.traveldocs.domain.model.DocumentFormat
import com.app.traveldocs.domain.model.ImportedDocument
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles importing documents from Google Drive or local content URIs.
 *
 * For content URIs (from Drive Picker activity results), uses ContentResolver to read bytes directly.
 * For Drive file IDs, uses the Drive REST API to download the file.
 */
@Singleton
class GoogleDriveImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val driveServiceProvider: DriveServiceProvider
) {

    /**
     * Imports a document from a content URI (e.g., from Google Drive Picker result).
     * Reads the file content, determines format, and returns an ImportedDocument.
     */
    suspend fun importFromUri(uri: Uri): Result<ImportedDocument> {
        DebugLogger.i("Drive", "importFromUri: uri=$uri")
        return try {
            val contentResolver = context.contentResolver

            val mimeType = contentResolver.getType(uri)
            val fileName = getFileName(contentResolver, uri)
            val format = resolveFormatFromMimeType(mimeType)
                ?: resolveFormatFromFileName(fileName)
                ?: return Result.failure(
                    IOException(
                        "Unsupported document format. Only PDF, JPG, and PNG files are supported. MIME type: $mimeType"
                    )
                )

            val rawBytes = readBytesFromUri(contentResolver, uri)

            Result.success(
                ImportedDocument(
                    rawBytes = rawBytes,
                    format = format,
                    originalFileName = fileName
                )
            )
        } catch (e: IOException) {
            Result.failure(
                IOException(
                    "Failed to import document: ${e.message}. Please check your network connection and try again.",
                    e
                )
            )
        } catch (e: SecurityException) {
            Result.failure(
                IOException(
                    "Access denied. Please re-authorize Google Drive access and try again.",
                    e
                )
            )
        } catch (e: Exception) {
            Result.failure(
                IOException(
                    "An unexpected error occurred during document import: ${e.message}",
                    e
                )
            )
        }
    }

    /**
     * Imports a document from Google Drive using a file ID.
     * Uses the Drive REST API to download the file content.
     */
    suspend fun importFromDriveFileId(fileId: String): Result<ImportedDocument> {
        DebugLogger.i("Drive", "importFromDriveFileId: fileId=$fileId")
        return try {
            val driveService = driveServiceProvider.getDriveService()
                ?: return Result.failure(
                    IOException(
                        "Google Drive authentication failed. Please sign in to Google Drive and try again."
                    )
                )

            val file = driveService.files().get(fileId)
                .setFields("name,mimeType")
                .execute()

            val mimeType = file.mimeType
            val fileName = file.name
            val format = resolveFormatFromMimeType(mimeType)
                ?: resolveFormatFromFileName(fileName)
                ?: return Result.failure(
                    IOException(
                        "Unsupported document format. Only PDF, JPG, and PNG files are supported. File: $fileName, MIME type: $mimeType"
                    )
                )

            val outputStream = ByteArrayOutputStream()
            driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)
            val rawBytes = outputStream.toByteArray()

            Result.success(
                ImportedDocument(
                    rawBytes = rawBytes,
                    format = format,
                    originalFileName = fileName
                )
            )
        } catch (e: IOException) {
            Result.failure(
                IOException(
                    "Failed to download file from Google Drive: ${e.message}. Please check your network connection and try again.",
                    e
                )
            )
        } catch (e: SecurityException) {
            Result.failure(
                IOException(
                    "Google Drive access denied. Please re-authorize and try again.",
                    e
                )
            )
        } catch (e: Exception) {
            Result.failure(
                IOException(
                    "An unexpected error occurred while downloading from Google Drive: ${e.message}",
                    e
                )
            )
        }
    }

    private fun readBytesFromUri(contentResolver: ContentResolver, uri: Uri): ByteArray {
        return contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.readBytes()
        } ?: throw IOException("Unable to open document. The file may have been moved or deleted.")
    }

    private fun getFileName(contentResolver: ContentResolver, uri: Uri): String? {
        var name: String? = null
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }

    internal fun resolveFormatFromMimeType(mimeType: String?): DocumentFormat? {
        return when (mimeType?.lowercase()) {
            "application/pdf" -> DocumentFormat.PDF
            "image/jpeg", "image/jpg" -> DocumentFormat.JPG
            "image/png" -> DocumentFormat.PNG
            else -> null
        }
    }

    internal fun resolveFormatFromFileName(fileName: String?): DocumentFormat? {
        if (fileName == null) return null
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "pdf" -> DocumentFormat.PDF
            "jpg", "jpeg" -> DocumentFormat.JPG
            "png" -> DocumentFormat.PNG
            else -> null
        }
    }
}
