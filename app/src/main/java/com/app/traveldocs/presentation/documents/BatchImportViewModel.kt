package com.app.traveldocs.presentation.documents

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.traveldocs.debug.DebugLogger
import com.app.traveldocs.domain.model.DocumentFormat
import com.app.traveldocs.domain.model.ImportedDocument
import com.app.traveldocs.domain.repository.TagRepository
import com.app.traveldocs.domain.usecase.DocumentImportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BatchImportState(
    val isIdle: Boolean = true,
    val isRunning: Boolean = false,
    val totalFiles: Int = 0,
    val processedCount: Int = 0,
    val currentFileName: String = "",
    val importedCount: Int = 0,
    val skippedCount: Int = 0,
    val isComplete: Boolean = false,
    val isCancelled: Boolean = false
)

data class FileToImport(
    val uri: Uri,
    val name: String,
    val mimeType: String?,
    val folderTags: List<String> = emptyList()  // Each subfolder level becomes a separate tag
)

/**
 * Handles importing multiple files at once — from local folders or Drive.
 *
 * The interesting bit: when importing from a folder, each subfolder level becomes a tag.
 * So if you point it at "Travel/" which has "Passports/john.pdf" and "Tickets/flight.pdf",
 * you get automatic tags "Passports" and "Tickets" without the user lifting a finger.
 *
 * Import is fully cancellable. Already-imported files are kept; we don't roll back on cancel.
 */
@HiltViewModel
class BatchImportViewModel @Inject constructor(
    private val importUseCase: DocumentImportUseCase,
    private val tagRepository: TagRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(BatchImportState())
    val state: StateFlow<BatchImportState> = _state.asStateFlow()
    private var importJob: Job? = null

    /**
     * Import from a local folder.
     * @param folderUri The URI of the selected folder (from ACTION_OPEN_DOCUMENT_TREE)
     * @param includeSubfolders If true, recursively scan subfolders. If false, only import files at root level.
     */
    /**
     * Import from a local folder. Enumerates and imports in chunks to keep the UI responsive.
     * The folder scan itself happens on Dispatchers.IO (DocumentFile uses Binder IPC which is slow).
     * Files are processed one at a time with state updates between each — never blocks main thread.
     */
    fun importFromLocalFolder(folderUri: Uri, includeSubfolders: Boolean = true) {
        _state.value = BatchImportState(isIdle = false, isRunning = true, totalFiles = -1) // -1 = unknown total
        importJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            DebugLogger.i("BatchImport", "Starting folder scan (recursive=$includeSubfolders)")
            // Phase 1: Enumerate files on IO thread (DocumentFile is slow IPC)
            val files = enumerateLocalFiles(folderUri, includeSubfolders)
            DebugLogger.i("BatchImport", "Found ${files.size} supported files")

            if (files.isEmpty()) {
                _state.value = BatchImportState(isIdle = false, isComplete = true, totalFiles = 0)
                return@launch
            }

            // Phase 2: Import files one at a time, yielding between each for cancellation
            _state.value = _state.value.copy(totalFiles = files.size)
            var imported = 0; var skipped = 0
            for ((index, file) in files.withIndex()) {
                // Check cancellation before each file
                if (!isActive) break

                _state.value = _state.value.copy(processedCount = index, currentFileName = file.name)

                try {
                    val bytes = context.contentResolver.openInputStream(file.uri)?.use { it.readBytes() }
                    if (bytes == null || bytes.isEmpty()) { skipped++; continue }
                    val format = mimeToFormat(file.mimeType)
                    val doc = ImportedDocument(rawBytes = bytes, format = format, originalFileName = file.name)
                    val result = importUseCase.importAndProcess(doc, "default-member")
                    if (result.isSuccess && file.folderTags.isNotEmpty()) {
                        val importedDoc = result.getOrNull()
                        if (importedDoc != null) {
                            for (tag in file.folderTags) {
                                try { tagRepository.addTag(importedDoc.id, tag) } catch (_: Exception) {}
                            }
                        }
                    }
                    if (result.isSuccess) imported++ else skipped++
                } catch (e: Exception) {
                    DebugLogger.e("BatchImport", "Failed: ${file.name}", e)
                    skipped++
                }

                // Yield briefly to keep coroutine cancellable and allow state observation
                kotlinx.coroutines.yield()
            }

            _state.value = _state.value.copy(
                isRunning = false, isComplete = true,
                processedCount = files.size, importedCount = imported, skippedCount = skipped
            )
            com.app.traveldocs.debug.UsageTelemetry.funnelComplete("batch_import")
            DebugLogger.i("BatchImport", "Complete: $imported imported, $skipped skipped")
        }
    }

    fun importFromDriveFiles(files: List<FileToImport>) {
        DebugLogger.i("BatchImport", "Drive folder: ${files.size} files to import")
        startBatchImport(files)
    }

    fun cancel() {
        importJob?.cancel()
        _state.value = _state.value.copy(isRunning = false, isCancelled = true)
        DebugLogger.w("BatchImport", "Cancelled by user at ${_state.value.processedCount}/${_state.value.totalFiles}")
    }

    fun reset() { _state.value = BatchImportState() }

    private fun startBatchImport(files: List<FileToImport>) {
        if (files.isEmpty()) {
            _state.value = BatchImportState(isIdle = false, isComplete = true, totalFiles = 0)
            return
        }
        _state.value = BatchImportState(isIdle = false, isRunning = true, totalFiles = files.size)
        importJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            var imported = 0; var skipped = 0
            for ((index, file) in files.withIndex()) {
                _state.value = _state.value.copy(processedCount = index, currentFileName = file.name)
                try {
                    val bytes = context.contentResolver.openInputStream(file.uri)?.use { it.readBytes() }
                    if (bytes == null || bytes.isEmpty()) { skipped++; continue }
                    val format = mimeToFormat(file.mimeType)
                    val doc = ImportedDocument(rawBytes = bytes, format = format, originalFileName = file.name)
                    val result = importUseCase.importAndProcess(doc, "default-member")
                    if (result.isSuccess && file.folderTags.isNotEmpty()) {
                        // Auto-tag with each subfolder level as a separate tag
                        val importedDoc = result.getOrNull()
                        if (importedDoc != null) {
                            for (tag in file.folderTags) {
                                try {
                                    tagRepository.addTag(importedDoc.id, tag)
                                } catch (e: Exception) {
                                    DebugLogger.w("BatchImport", "Failed to auto-tag with folder: $tag", e)
                                }
                            }
                            DebugLogger.d("BatchImport", "Auto-tagged '${file.name}' with folder tags: ${file.folderTags}")
                        }
                    }
                    if (result.isSuccess) imported++ else skipped++
                } catch (e: Exception) {
                    DebugLogger.e("BatchImport", "Failed: ${file.name}", e)
                    skipped++
                }
            }
            _state.value = _state.value.copy(isRunning = false, isComplete = true, processedCount = files.size, importedCount = imported, skippedCount = skipped)
            com.app.traveldocs.debug.UsageTelemetry.funnelComplete("batch_import")
            DebugLogger.i("BatchImport", "Complete: $imported imported, $skipped skipped")
        }
    }

    /**
     * Enumerate files from a local folder tree.
     * If includeSubfolders=true, recursively scans all subfolders.
     * Each subfolder level in the path becomes a separate tag.
     * e.g. root/Travel/2025/doc.pdf -> tags ["Travel", "2025"]
     * Files at the root level get no folder-derived tags.
     */
    private fun enumerateLocalFiles(treeUri: Uri, includeSubfolders: Boolean): List<FileToImport> {
        val files = mutableListOf<FileToImport>()
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return files
        for (child in root.listFiles()) {
            if (child.isDirectory) {
                if (includeSubfolders) {
                    val pathTags = mutableListOf<String>()
                    val folderName = child.name?.trim()?.takeIf { it.isNotEmpty() }
                    if (folderName != null) pathTags.add(folderName)
                    collectFilesRecursive(child, files, pathTags)
                }
            } else {
                val mime = child.type ?: ""
                if (isSupportedMime(mime)) {
                    files.add(FileToImport(child.uri, child.name ?: "file", mime))
                }
            }
        }
        return files
    }

    /**
     * Recursively collect files, accumulating each folder level as a tag.
     * pathTags represents the subfolder names from root to current directory.
     */
    private fun collectFilesRecursive(dir: DocumentFile, out: MutableList<FileToImport>, pathTags: List<String>) {
        for (child in dir.listFiles()) {
            if (child.isDirectory) {
                val folderName = child.name?.trim()?.takeIf { it.isNotEmpty() }
                val deeperTags = if (folderName != null) pathTags + folderName else pathTags
                collectFilesRecursive(child, out, deeperTags)
            } else {
                val mime = child.type ?: ""
                if (isSupportedMime(mime)) {
                    out.add(FileToImport(child.uri, child.name ?: "file", mime, folderTags = pathTags))
                }
            }
        }
    }

    private fun isSupportedMime(mime: String): Boolean {
        return mime.contains("pdf") || mime.contains("jpeg") || mime.contains("jpg") || mime.contains("png") || mime.contains("video") || mime.contains("webp") || mime.contains("heic") || mime.contains("heif") || mime.contains("bmp") || mime.contains("gif") || mime.contains("dicom") || mime.contains(".dcm")
    }

    private fun mimeToFormat(mime: String?): DocumentFormat = when {
        mime?.contains("pdf") == true -> DocumentFormat.PDF
        mime?.contains("png") == true -> DocumentFormat.PNG
        mime?.contains("jpeg") == true || mime?.contains("jpg") == true -> DocumentFormat.JPG
        mime?.contains("video") == true -> DocumentFormat.VIDEO
        mime?.contains("webp") == true -> DocumentFormat.WEBP
        mime?.contains("heic") == true || mime?.contains("heif") == true -> DocumentFormat.HEIC
        mime?.contains("bmp") == true -> DocumentFormat.BMP
        mime?.contains("gif") == true -> DocumentFormat.GIF
        mime?.contains("dicom") == true -> DocumentFormat.DICOM
        mime?.contains("audio") == true || mime?.contains("mp3") == true -> DocumentFormat.AUDIO
        else -> DocumentFormat.UNKNOWN
    }
}
