package com.app.paperstow.data.local

import android.content.Context
import com.app.paperstow.debug.DebugLogger
import com.app.paperstow.domain.repository.DocumentFileStorage
import com.app.paperstow.domain.repository.DocumentRepository
import com.app.paperstow.domain.repository.MetadataExtractor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class IndexRebuildState(
    val running: Boolean = false,
    val current: Int = 0,
    val total: Int = 0,
    val fileName: String = "",
    val excerpt: String = "",
    val done: Boolean = false,
    val error: String? = null
)

@Singleton
class SearchIndexRebuilder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val documentRepository: DocumentRepository,
    private val fileStorage: DocumentFileStorage,
    private val metadataExtractor: MetadataExtractor
) {
    private val _state = MutableStateFlow(IndexRebuildState())
    val state: StateFlow<IndexRebuildState> = _state.asStateFlow()

    suspend fun rebuild() = withContext(Dispatchers.IO) {
        if (_state.value.running) return@withContext
        try {
            val docs = documentRepository.getAll("default-member").first()
            _state.value = IndexRebuildState(running = true, total = docs.size)
            docs.forEachIndexed { index, doc ->
                val name = doc.originalFileName ?: doc.id
                _state.value = _state.value.copy(
                    current = index + 1,
                    fileName = name,
                    excerpt = "Reading…"
                )
                val bytes = fileStorage.retrieve(doc.id).getOrNull()
                val text = if (bytes == null) {
                    ""
                } else {
                    extractText(doc.format, bytes)
                }
                documentRepository.updateOcrText(doc.id, text)
                _state.value = _state.value.copy(
                    current = index + 1,
                    fileName = name,
                    excerpt = text.ifBlank { "(no text found)" }
                )
                DebugLogger.i("SearchIndex", "Indexed ${index + 1}/${docs.size} $name chars=${text.length}")
            }
            _state.value = _state.value.copy(running = false, done = true, excerpt = _state.value.excerpt)
        } catch (e: Exception) {
            DebugLogger.e("SearchIndex", "Rebuild failed", e)
            _state.value = _state.value.copy(running = false, error = e.message ?: "Rebuild failed")
        }
    }

    private suspend fun extractText(format: com.app.paperstow.domain.model.DocumentFormat, bytes: ByteArray): String {
        if (SearchIndex.isPlainText(format)) {
            return SearchIndex.textForImport(format, bytes, "")
        }
        val chunks = SearchIndex.imagesForOcr(format, bytes, context.cacheDir).mapNotNull { page ->
            val outcome = metadataExtractor.extract(page)
            outcome.onFailure { e -> DebugLogger.w("SearchIndex", "OCR failed: ${e.message}") }
            outcome.getOrNull()?.rawText
        }
        return chunks.joinToString("\n").trim().take(SearchIndex.MAX_CHARS)
    }
}
