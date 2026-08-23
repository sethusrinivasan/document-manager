package com.app.traveldocs.domain.repository

import com.app.traveldocs.domain.model.Document
import com.app.traveldocs.domain.model.SearchQuery
import kotlinx.coroutines.flow.Flow

interface DocumentRepository {
    suspend fun getAll(memberId: String): Flow<List<Document>>
    suspend fun getById(documentId: String): Document?
    suspend fun insert(document: Document): Result<String>
    suspend fun delete(documentId: String): Result<Unit>
    suspend fun getCount(memberId: String): Int
    suspend fun search(memberId: String, query: SearchQuery): List<Document>
}
