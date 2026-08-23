package com.app.traveldocs.domain.repository

import com.app.traveldocs.domain.model.Document
import com.app.traveldocs.domain.model.SearchResult

interface SearchEngine {
    suspend fun searchByTags(memberId: String, tags: List<String>): List<Document>
    suspend fun searchFreeForm(memberId: String, query: String): List<Document>
    suspend fun searchNaturalLanguage(memberId: String, query: String): SearchResult
}
