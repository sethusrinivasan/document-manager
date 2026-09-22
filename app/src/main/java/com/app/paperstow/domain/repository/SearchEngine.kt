package com.app.paperstow.domain.repository

import com.app.paperstow.domain.model.Document
import com.app.paperstow.domain.model.SearchResult

interface SearchEngine {
    suspend fun searchByTags(memberId: String, tags: List<String>): List<Document>
    suspend fun searchFreeForm(memberId: String, query: String): List<Document>
    suspend fun searchNaturalLanguage(memberId: String, query: String): SearchResult
}
