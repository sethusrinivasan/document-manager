package com.app.traveldocs.presentation.review

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.traveldocs.debug.DebugLogger
import com.app.traveldocs.domain.model.Document
import com.app.traveldocs.domain.repository.DocumentRepository
import com.app.traveldocs.domain.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val tagRepository: TagRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _untaggedDocs = MutableStateFlow<List<Document>>(emptyList())
    val untaggedDocs: StateFlow<List<Document>> = _untaggedDocs.asStateFlow()

    private val _reviewDocs = MutableStateFlow<List<Document>>(emptyList())
    val reviewDocs: StateFlow<List<Document>> = _reviewDocs.asStateFlow()

    private val _allTags = MutableStateFlow<List<String>>(emptyList())
    val allTags: StateFlow<List<String>> = _allTags.asStateFlow()

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            val docs = documentRepository.getAll("default-member").first()

            // Untagged = no tags at all, or only system tags
            _untaggedDocs.value = docs.filter { doc ->
                doc.tags.isEmpty() || doc.tags.all { it.name.startsWith("__") }
            }

            // Needs review = flagged by OCR or confidence < 80%
            _reviewDocs.value = docs.filter { it.requiresManualReview }

            // All existing user tags (for the picker)
            _allTags.value = docs.flatMap { it.tags }
                .map { it.name }
                .filter { !it.startsWith("__") }
                .distinct()
                .sorted()

            DebugLogger.d("ReviewVM", "Loaded: ${_untaggedDocs.value.size} untagged, ${_reviewDocs.value.size} need review")
        }
    }

    fun assignTagToDocuments(docIds: List<String>, tagName: String) {
        viewModelScope.launch {
            DebugLogger.i("ReviewVM", "Assigning tag '$tagName' to ${docIds.size} documents")
            for (id in docIds) {
                tagRepository.addTag(id, tagName)
            }
            loadData() // Refresh lists
        }
    }

    fun markReviewed(documentId: String) {
        viewModelScope.launch {
            DebugLogger.i("ReviewVM", "Marking doc $documentId as reviewed")
            val doc = documentRepository.getById(documentId) ?: return@launch
            val updated = doc.copy(requiresManualReview = false, updatedAt = java.time.Instant.now())
            documentRepository.delete(documentId)
            documentRepository.insert(updated)
            loadData()
        }
    }
}
