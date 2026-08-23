package com.app.traveldocs.presentation.documents

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.traveldocs.debug.DebugLogger
import com.app.traveldocs.domain.model.Document
import com.app.traveldocs.domain.model.DocumentFormat
import com.app.traveldocs.domain.model.ImportedDocument
import com.app.traveldocs.domain.repository.DocumentRepository
import com.app.traveldocs.domain.usecase.DocumentImportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.security.MessageDigest
import javax.inject.Inject

data class DuplicateInfo(val existingDoc: Document, val newBytes: ByteArray, val newFormat: DocumentFormat, val newFileName: String)

data class ImportUiState(
    val isImporting: Boolean = false,
    val importedDocument: Document? = null,
    val error: String? = null,
    val duplicateFound: DuplicateInfo? = null
)

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val importUseCase: DocumentImportUseCase,
    private val documentRepository: DocumentRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(ImportUiState())
    val state: StateFlow<ImportUiState> = _state.asStateFlow()

    fun importFile(uri: Uri) {
        viewModelScope.launch {
            _state.value = ImportUiState(isImporting = true)
            com.app.traveldocs.debug.UsageTelemetry.funnelStart("single_import")
            DebugLogger.i("ImportVM", "Starting import from URI: $uri")
            try {
                val contentResolver = context.contentResolver
                val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes == null || bytes.isEmpty()) {
                    _state.value = ImportUiState(error = "Failed to read file or file is empty")
                    return@launch
                }
                val mimeType = contentResolver.getType(uri)
                val format = when {
                    mimeType?.contains("pdf") == true -> DocumentFormat.PDF
                    mimeType?.contains("video") == true -> DocumentFormat.VIDEO
                    mimeType?.contains("png") == true -> DocumentFormat.PNG
                    mimeType?.contains("jpeg") == true || mimeType?.contains("jpg") == true -> DocumentFormat.JPG
                    mimeType?.contains("webp") == true -> DocumentFormat.WEBP
                    mimeType?.contains("heic") == true || mimeType?.contains("heif") == true -> DocumentFormat.HEIC
                    mimeType?.contains("bmp") == true -> DocumentFormat.BMP
                    mimeType?.contains("gif") == true -> DocumentFormat.GIF
                    mimeType?.contains("dicom") == true -> DocumentFormat.DICOM
                    mimeType?.contains("audio") == true || mimeType?.contains("mp3") == true -> if (com.app.traveldocs.data.local.FeatureFlags.isAudioPlaybackEnabled(context)) DocumentFormat.AUDIO else DocumentFormat.UNKNOWN
                    else -> DocumentFormat.UNKNOWN  // Unsupported format, import anyway
                }
                val fileName = getFileName(uri) ?: "imported_document"
                val fileHash = computeSha256(bytes)
                DebugLogger.d("ImportVM", "File: $fileName, hash=${fileHash.take(16)}..., size=${bytes.size}")

                // Dedup check
                val existingDocs = documentRepository.getAll("default-member").first()
                val duplicate = existingDocs.find { it.originalFileName == fileName }
                if (duplicate != null) {
                    DebugLogger.w("ImportVM", "Duplicate detected: '${fileName}' already exists (id=${duplicate.id})")
                    _state.value = ImportUiState(duplicateFound = DuplicateInfo(duplicate, bytes, format, fileName))
                    return@launch
                }
                performImport(bytes, format, fileName)
            } catch (e: Exception) {
                DebugLogger.e("ImportVM", "Import exception", e)
                _state.value = ImportUiState(error = e.message ?: "Unexpected error")
            }
        }
    }

    fun confirmReplace() {
        val dup = _state.value.duplicateFound ?: return
        viewModelScope.launch {
            _state.value = ImportUiState(isImporting = true)
            DebugLogger.i("ImportVM", "Replacing duplicate: ${dup.newFileName}")
            documentRepository.delete(dup.existingDoc.id)
            performImport(dup.newBytes, dup.newFormat, dup.newFileName)
        }
    }

    fun cancelDuplicate() {
        DebugLogger.i("ImportVM", "Duplicate import cancelled")
        _state.value = ImportUiState()
    }

    private suspend fun performImport(bytes: ByteArray, format: DocumentFormat, fileName: String) {
        val result = importUseCase.importAndProcess(ImportedDocument(bytes, format, fileName), "default-member")
        result.onSuccess { doc ->
            DebugLogger.i("ImportVM", "Import OK: type=${doc.type}, confidence=${doc.extractionConfidence}")
            val prefs = context.getSharedPreferences("traveldocs_stats", Context.MODE_PRIVATE)
            prefs.edit().putInt("doc_count", prefs.getInt("doc_count", 0) + 1).apply()
            _state.value = ImportUiState(importedDocument = doc)
        }
        result.onFailure { e ->
            com.app.traveldocs.debug.UsageTelemetry.userError("Import", "import_failed")
                    DebugLogger.e("ImportVM", "Import failed", e)
            _state.value = ImportUiState(error = e.message ?: "Import failed")
        }
    }

    fun clearState() { _state.value = ImportUiState() }

    private fun getFileName(uri: Uri): String? {
        context.contentResolver.query(uri, null, null, null, null)?.use {
            if (it.moveToFirst()) {
                val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) return it.getString(idx)
            }
        }
        return uri.lastPathSegment
    }

    private fun computeSha256(bytes: ByteArray): String {
        return MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    }

    fun importFromBytes(bytes: ByteArray, format: com.app.traveldocs.domain.model.DocumentFormat, fileName: String) {
        viewModelScope.launch {
            _state.value = ImportUiState(isImporting = true)
            DebugLogger.i("ImportVM", "Importing from bytes: $fileName (${bytes.size} bytes)")
            try {
                val result = importUseCase.importAndProcess(com.app.traveldocs.domain.model.ImportedDocument(bytes, format, fileName), "default-member")
                result.onSuccess { doc ->
                    DebugLogger.i("ImportVM", "Camera import OK: type=${doc.type}")
                    val prefs = context.getSharedPreferences("traveldocs_stats", android.content.Context.MODE_PRIVATE)
                    prefs.edit().putInt("doc_count", prefs.getInt("doc_count", 0) + 1).apply()
                    _state.value = ImportUiState(importedDocument = doc)
                }
                result.onFailure { e -> _state.value = ImportUiState(error = e.message ?: "Import failed") }
            } catch (e: Exception) { _state.value = ImportUiState(error = e.message ?: "Error") }
        }
    }
}
