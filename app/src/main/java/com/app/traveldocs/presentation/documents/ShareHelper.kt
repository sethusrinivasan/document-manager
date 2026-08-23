package com.app.traveldocs.presentation.documents

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.app.traveldocs.debug.DebugLogger
import com.app.traveldocs.debug.UsageTelemetry
import com.app.traveldocs.domain.model.Document
import com.app.traveldocs.domain.model.DocumentFormat
import com.app.traveldocs.domain.repository.DocumentFileStorage
import java.io.File

/**
 * Decrypts documents to temp cache and launches the Android share sheet.
 * Supports single and multi-file sharing via ACTION_SEND / ACTION_SEND_MULTIPLE.
 */
object ShareHelper {

    suspend fun shareDocuments(
        context: Context,
        documents: List<Document>,
        fileStorage: DocumentFileStorage
    ) {
        if (documents.isEmpty()) return

        DebugLogger.i("Share", "Sharing ${documents.size} document(s)")
        UsageTelemetry.action("Share", "share_initiated", "count=${documents.size}")

        val cacheDir = File(context.cacheDir, "shared_docs")
        cacheDir.mkdirs()

        val urisAndMimes = mutableListOf<Pair<Uri, String>>()

        for (doc in documents) {
            try {
                val result = fileStorage.retrieve(doc.id)
                val bytes = result.getOrNull() ?: continue

                val fileName = doc.originalFileName ?: "document_${doc.id.take(8)}"
                val file = File(cacheDir, fileName)
                file.writeBytes(bytes)

                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val mime = mimeForFormat(doc.format)
                urisAndMimes.add(uri to mime)
            } catch (e: Exception) {
                DebugLogger.e("Share", "Failed to prepare ${doc.originalFileName}", e)
            }
        }

        if (urisAndMimes.isEmpty()) {
            DebugLogger.w("Share", "No files could be prepared for sharing")
            return
        }

        val intent = if (urisAndMimes.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = urisAndMimes[0].second
                putExtra(Intent.EXTRA_STREAM, urisAndMimes[0].first)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                val uriList = ArrayList(urisAndMimes.map { it.first })
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriList)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val chooser = Intent.createChooser(intent, "Share documents via")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)

        DebugLogger.i("Share", "Share sheet launched for ${urisAndMimes.size} file(s)")
        UsageTelemetry.action("Share", "share_launched", "files=${urisAndMimes.size}")
    }

    private fun mimeForFormat(format: DocumentFormat): String = when (format) {
        DocumentFormat.PDF -> "application/pdf"
        DocumentFormat.JPG -> "image/jpeg"
        DocumentFormat.PNG -> "image/png"
        DocumentFormat.VIDEO -> "video/*"
        DocumentFormat.WEBP -> "image/webp"
        DocumentFormat.HEIC -> "image/heic"
        DocumentFormat.BMP -> "image/bmp"
        DocumentFormat.GIF -> "image/gif"
        DocumentFormat.DICOM -> "application/dicom"
        DocumentFormat.AUDIO -> "audio/mpeg"
        DocumentFormat.UNKNOWN -> "application/octet-stream"
    }
}
