package com.app.traveldocs.presentation.documents

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.traveldocs.debug.DebugLogger
import com.app.traveldocs.domain.model.Document
import com.app.traveldocs.domain.repository.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BulkDeleteState(
    val isRunning: Boolean = false,
    val total: Int = 0,
    val processed: Int = 0,
    val currentName: String = "",
    val isComplete: Boolean = false
)

@HiltViewModel
class DocumentListViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _documents = MutableStateFlow<List<Document>>(emptyList())
    val documents: StateFlow<List<Document>> = _documents.asStateFlow()

    private val _recycleBin = MutableStateFlow<List<Document>>(emptyList())
    val recycleBin: StateFlow<List<Document>> = _recycleBin.asStateFlow()

    private val _showingTrash = MutableStateFlow(false)
    val showingTrash: StateFlow<Boolean> = _showingTrash.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds: StateFlow<Set<String>> = _selectedIds.asStateFlow()

    private val _selectionMode = MutableStateFlow(false)
    val selectionMode: StateFlow<Boolean> = _selectionMode.asStateFlow()

    private val _bulkDeleteState = MutableStateFlow(BulkDeleteState())
    val bulkDeleteState: StateFlow<BulkDeleteState> = _bulkDeleteState.asStateFlow()

    private val trashedDocs = mutableListOf<Document>()
    private var deleteJob: Job? = null

    private var collectJob: kotlinx.coroutines.Job? = null

    init { startCollecting() }

    private fun startCollecting() {
        collectJob?.cancel()
        collectJob = viewModelScope.launch {
            try {
                documentRepository.getAll("default-member").collect { docs ->
                    _documents.value = docs.sortedByDescending { it.createdAt }
                }
            } catch (e: Exception) {
                com.app.traveldocs.debug.DebugLogger.e("DocList", "Database query failed (may need app restart after failed restore)", e)
                _documents.value = emptyList()
            }
        }
    }

    /**
     * Force refresh — cancels the current Flow collection and starts a new one.
     * Use after restore or any operation that replaces the database file.
     */
    fun forceRefresh() {
        com.app.traveldocs.debug.DebugLogger.i("DocList", "Force refresh triggered")
        startCollecting()
    }

    fun toggleSelection(docId: String) {
        val current = _selectedIds.value.toMutableSet()
        if (current.contains(docId)) current.remove(docId) else current.add(docId)
        _selectedIds.value = current
        _selectionMode.value = current.isNotEmpty()
    }

    fun selectAll() {
        _selectedIds.value = _documents.value.map { it.id }.toSet()
        _selectionMode.value = true
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
        _selectionMode.value = false
    }

    fun bulkDeleteSelected() {
        val ids = _selectedIds.value.toList()
        if (ids.isEmpty()) return
        val docs = _documents.value.filter { it.id in ids }
        DebugLogger.i("DocList", "Bulk delete: ${ids.size} documents")

        _bulkDeleteState.value = BulkDeleteState(isRunning = true, total = docs.size)
        deleteJob = viewModelScope.launch {
            for ((index, doc) in docs.withIndex()) {
                _bulkDeleteState.value = _bulkDeleteState.value.copy(processed = index, currentName = doc.originalFileName ?: "Document")
                documentRepository.delete(doc.id)
                trashedDocs.add(doc)
            }
            _bulkDeleteState.value = BulkDeleteState(isComplete = true, total = docs.size, processed = docs.size)
            _recycleBin.value = trashedDocs.toList()
            clearSelection()
            DebugLogger.i("DocList", "Bulk delete complete: ${docs.size} moved to trash")
        }
    }

    fun dismissBulkDelete() {
        _bulkDeleteState.value = BulkDeleteState()
    }

    fun moveToTrash(document: Document) {
        viewModelScope.launch {
            DebugLogger.i("DocList", "Trash: ${document.originalFileName}")
            documentRepository.delete(document.id)
            trashedDocs.add(document)
            _recycleBin.value = trashedDocs.toList()
        }
    }

    fun restoreFromTrash(document: Document) {
        viewModelScope.launch {
            documentRepository.insert(document)
            trashedDocs.remove(document)
            _recycleBin.value = trashedDocs.toList()
        }
    }

    fun permanentlyDelete(document: Document) {
        trashedDocs.remove(document)
        _recycleBin.value = trashedDocs.toList()
    }

    fun emptyTrash() {
        trashedDocs.clear()
        _recycleBin.value = emptyList()
    }

    fun toggleTrashView() { _showingTrash.value = !_showingTrash.value }

    fun shareSelected(context: Context) {
        val docs = _documents.value.filter { it.id in _selectedIds.value }
        if (docs.isEmpty()) return
        viewModelScope.launch {
            ShareHelper.shareDocuments(context, docs, documentRepository as com.app.traveldocs.domain.repository.DocumentFileStorage)
            clearSelection()
        }
    }}
