package com.app.traveldocs.data.backup

import android.content.Context
import com.app.traveldocs.data.drive.DriveServiceProvider
import com.app.traveldocs.debug.DebugLogger
import com.google.api.client.http.FileContent
import com.google.api.services.drive.model.File as DriveFile
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DriveBackupUploader @Inject constructor(
    private val driveServiceProvider: DriveServiceProvider
) {
    fun upload(context: Context, zipFile: File, fileName: String): Result<String> {
        return try {
            val service = driveServiceProvider.getDriveService()
                ?: return Result.failure(Exception("Google Drive not authenticated. Please sign in to Google in device settings."))

            DebugLogger.i("DriveBackup", "Uploading $fileName (${zipFile.length() / 1024}KB) to Drive...")

            val metadata = DriveFile().apply {
                name = fileName
                mimeType = "application/zip"
                parents = listOf("appDataFolder")
            }

            val content = FileContent("application/zip", zipFile)
            val uploaded = service.files().create(metadata, content)
                .setFields("id,name,size")
                .execute()

            DebugLogger.i("DriveBackup", "Upload complete: id=${uploaded.id}, name=${uploaded.name}")
            Result.success("Uploaded to Google Drive: ${uploaded.name}")
        } catch (e: Exception) {
            DebugLogger.e("DriveBackup", "Upload failed", e)
            Result.failure(Exception("Drive upload failed: ${e.message}"))
        }
    }
}
