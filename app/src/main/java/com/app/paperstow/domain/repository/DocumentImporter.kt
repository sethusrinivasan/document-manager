package com.app.paperstow.domain.repository

import android.net.Uri
import com.app.paperstow.domain.model.DocumentFormat
import com.app.paperstow.domain.model.ImportedDocument

interface DocumentImporter {
    suspend fun importFromCamera(): Result<ImportedDocument>
    suspend fun importFromGoogleDrive(fileUri: Uri): Result<ImportedDocument>
    suspend fun importFromFile(uri: Uri): Result<ImportedDocument>
    fun getSupportedFormats(): List<DocumentFormat>
}
