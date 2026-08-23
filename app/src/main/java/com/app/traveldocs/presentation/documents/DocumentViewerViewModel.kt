package com.app.traveldocs.presentation.documents

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.traveldocs.debug.DebugLogger
import com.app.traveldocs.domain.model.Document
import com.app.traveldocs.domain.model.Tag
import com.app.traveldocs.domain.repository.DocumentFileStorage
import com.app.traveldocs.domain.repository.DocumentRepository
import com.app.traveldocs.domain.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * ViewModel for the document preview screen.
 *
 * Key design decision: when the user switches documents, we immediately clear the previous
 * document's bytes and show a loading state. This prevents the old document from being
 * visible while the new one decrypts — which was confusing and looked like a bug.
 *
 * File loading is cancellable: if the user navigates away mid-load, we cancel the coroutine
 * rather than letting it finish and update state for a screen that's no longer visible.
 */
@HiltViewModel
class DocumentViewerViewModel @Inject constructor(
    private val fileStorage: DocumentFileStorage,
    private val tagRepository: TagRepository,
    private val documentRepository: DocumentRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _fileBytes = MutableStateFlow<ByteArray?>(null)
    val fileBytes: StateFlow<ByteArray?> = _fileBytes.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _tags = MutableStateFlow<List<Tag>>(emptyList())
    val tags: StateFlow<List<Tag>> = _tags.asStateFlow()

    private val _filePath = MutableStateFlow("")
    val filePath: StateFlow<String> = _filePath.asStateFlow()

    private var currentDocId: String = ""
    private var loadJob: kotlinx.coroutines.Job? = null

    fun loadFile(documentId: String) {
        // Cancel any in-progress load (user switched to a different document)
        loadJob?.cancel()

        // Immediately clear previous document's content so stale data is never shown
        currentDocId = documentId
        _fileBytes.value = null
        _tags.value = emptyList()
        _filePath.value = ""
        _loading.value = true

        loadJob = viewModelScope.launch {
            DebugLogger.d("ViewerVM", "Loading file for doc: $documentId")

            // Load file bytes (decrypts from .enc file)
            val result = fileStorage.retrieve(documentId)

            // Check cancellation before updating state
            if (!isActive) return@launch

            result.onSuccess { bytes ->
                DebugLogger.i("ViewerVM", "File loaded: ${bytes.size} bytes")
                _fileBytes.value = bytes
            }
            result.onFailure { e ->
                DebugLogger.e("ViewerVM", "Failed to load file", e)
                _fileBytes.value = null
            }

            if (!isActive) return@launch

            // Load tags
            val docTags = tagRepository.getTagsForDocument(documentId)
            if (!isActive) return@launch
            _tags.value = docTags

            // Resolve file path
            val docsDir = File(context.filesDir, "docs")
            val encFile = findEncFile(docsDir, documentId)
            _filePath.value = encFile?.absolutePath ?: "Unknown"

            _loading.value = false
        }
    }

    /**
     * Cancel the current file load operation (e.g., user pressed back while loading).
     */
    fun cancelLoad() {
        loadJob?.cancel()
        _loading.value = false
    }

    fun addTag(tagName: String) {
        if (tagName.isBlank() || currentDocId.isEmpty()) return
        viewModelScope.launch {
            DebugLogger.i("ViewerVM", "Adding tag '$tagName' to doc $currentDocId")
            tagRepository.addTag(currentDocId, tagName.trim())
            _tags.value = tagRepository.getTagsForDocument(currentDocId)
        }
    }

    fun removeTag(tagName: String) {
        if (currentDocId.isEmpty()) return
        viewModelScope.launch {
            DebugLogger.i("ViewerVM", "Removing tag '$tagName' from doc $currentDocId")
            tagRepository.removeTag(currentDocId, tagName)
            _tags.value = tagRepository.getTagsForDocument(currentDocId)
        }
    }

    fun renameDocument(document: Document, newName: String) {
        if (newName.isBlank()) return
        val sanitizedName = com.app.traveldocs.data.local.InputSanitizer.sanitizeFilename(newName)
        viewModelScope.launch {
            DebugLogger.i("ViewerVM", "Renaming doc ${document.id} to '$newName'")
            val updated = document.copy(originalFileName = sanitizedName, updatedAt = java.time.Instant.now())
            documentRepository.delete(document.id)
            documentRepository.insert(updated)
        }
    }

    private fun findEncFile(dir: File, fileId: String): File? {
        if (!dir.exists()) return null
        dir.listFiles()?.forEach { sub ->
            if (sub.isDirectory) {
                val f = File(sub, "$fileId.enc")
                if (f.exists()) return f
            }
        }
        return null
    }
}
