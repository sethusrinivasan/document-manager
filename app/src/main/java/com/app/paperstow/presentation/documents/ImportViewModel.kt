package com.app.paperstow.presentation.documents

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.paperstow.data.importer.DocumentFormatValidator
import com.app.paperstow.data.scanner.CameraDocumentScanner
import com.app.paperstow.debug.DebugLogger
import com.app.paperstow.domain.model.Document
import com.app.paperstow.domain.model.DocumentFormat
import com.app.paperstow.domain.model.ImportedDocument
import com.app.paperstow.domain.repository.DocumentRepository
import com.app.paperstow.domain.usecase.DocumentImportUseCase
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
    private val formatValidator: DocumentFormatValidator,
    private val documentScanner: CameraDocumentScanner,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(ImportUiState())
    val state: StateFlow<ImportUiState> = _state.asStateFlow()

    fun importFile(uri: Uri) {
        viewModelScope.launch {
            _state.value = ImportUiState(isImporting = true)
            com.app.paperstow.debug.UsageTelemetry.funnelStart("single_import")
            DebugLogger.i("ImportVM", "Starting import from URI: $uri")
            try {
                val contentResolver = context.contentResolver
                val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes == null || bytes.isEmpty()) {
                    _state.value = ImportUiState(error = "Failed to read file or file is empty")
                    return@launch
                }
                val mimeType = contentResolver.getType(uri)
                val fileName = getFileName(uri) ?: "imported_document"
                val format = formatValidator.detect(bytes, mimeType, fileName)
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
            com.app.paperstow.debug.UsageTelemetry.userError("Import", "import_failed")
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

    fun startDocumentScan(
        activity: android.app.Activity,
        onReady: (android.content.IntentSender) -> Unit,
        onUnavailable: () -> Unit
    ) {
        documentScanner.getStartIntent(activity, onReady) { onUnavailable() }
    }

    fun importScanResult(data: android.content.Intent?) {
        val pages = documentScanner.pagesFromResult(data)
        importScannedPages(pages)
    }

    fun importFromBytes(bytes: ByteArray, format: com.app.paperstow.domain.model.DocumentFormat, fileName: String) {
        viewModelScope.launch {
            _state.value = ImportUiState(isImporting = true)
            DebugLogger.i("ImportVM", "Importing from bytes: $fileName (${bytes.size} bytes)")
            try {
                val result = importUseCase.importAndProcess(com.app.paperstow.domain.model.ImportedDocument(bytes, format, fileName), "default-member")
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

    fun importScannedPages(pages: List<ImportedDocument>) {
        if (pages.isEmpty()) {
            _state.value = ImportUiState(error = "No pages were captured")
            return
        }
        viewModelScope.launch {
            _state.value = ImportUiState(isImporting = true)
            var last: Document? = null
            var imported = 0
            var lastError: String? = null
            for (page in pages) {
                val result = importUseCase.importAndProcess(page, "default-member")
                result.onSuccess { doc ->
                    last = doc
                    imported++
                }
                result.onFailure { e ->
                    lastError = e.message
                    DebugLogger.e("ImportVM", "Scan page failed: ${page.originalFileName}", e)
                }
            }
            val prefs = context.getSharedPreferences("traveldocs_stats", Context.MODE_PRIVATE)
            prefs.edit().putInt("doc_count", prefs.getInt("doc_count", 0) + imported).apply()
            if (last != null) {
                DebugLogger.i("ImportVM", "Scanned $imported page(s)")
                _state.value = ImportUiState(importedDocument = last)
            } else {
                _state.value = ImportUiState(error = lastError ?: "Scan import failed")
            }
        }
    }
}
