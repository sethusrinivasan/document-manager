package com.app.traveldocs.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.traveldocs.debug.DebugLogger
import com.app.traveldocs.domain.model.SearchResult
import com.app.traveldocs.domain.repository.SearchEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchEngine: SearchEngine
) : ViewModel() {

    private val _results = MutableStateFlow<SearchResult?>(null)
    val results: StateFlow<SearchResult?> = _results.asStateFlow()
    private var searchJob: Job? = null

    init {
        DebugLogger.i("SearchVM", "ViewModel created successfully (SearchEngine injected)")
    }

    fun search(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) { _results.value = null; return }
        searchJob = viewModelScope.launch {
            delay(300)
            com.app.traveldocs.debug.UsageTelemetry.action("Search", "query")
            DebugLogger.i("SearchVM", "Searching: '$query'")
            try {
                val result = searchEngine.searchNaturalLanguage("default-member", query)
                _results.value = result
                when (result) {
                    is SearchResult.DocumentResults -> DebugLogger.i("SearchVM", "Results: ${result.documents.size} documents")
                    is SearchResult.TravelChecklist -> DebugLogger.i("SearchVM", "Results: travel checklist with ${result.checklist.requiredDocuments.size} items")
                    is SearchResult.NeedMoreInfo -> DebugLogger.i("SearchVM", "Results: need more info (${result.missingParams})")
                }
            } catch (e: Exception) {
                DebugLogger.e("SearchVM", "Search error: ${e::class.simpleName}: ${e.message}", e)
                _results.value = SearchResult.DocumentResults(emptyList())
            }
        }
    }
}
