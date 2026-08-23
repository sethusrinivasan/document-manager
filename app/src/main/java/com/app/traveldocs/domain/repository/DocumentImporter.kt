package com.app.traveldocs.domain.repository

import android.net.Uri
import com.app.traveldocs.domain.model.DocumentFormat
import com.app.traveldocs.domain.model.ImportedDocument

interface DocumentImporter {
    suspend fun importFromCamera(): Result<ImportedDocument>
    suspend fun importFromGoogleDrive(fileUri: Uri): Result<ImportedDocument>
    suspend fun importFromFile(uri: Uri): Result<ImportedDocument>
    fun getSupportedFormats(): List<DocumentFormat>
}
