package com.app.paperstow.domain.repository

import com.app.paperstow.domain.model.Document
import com.app.paperstow.domain.model.SearchQuery
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface DocumentRepository {
    suspend fun getAll(memberId: String): Flow<List<Document>>
    suspend fun getById(documentId: String): Document?
    suspend fun insert(document: Document): Result<String>
    suspend fun updateName(documentId: String, originalFileName: String, updatedAt: Instant): Result<Unit>
    suspend fun delete(documentId: String): Result<Unit>
    suspend fun getCount(memberId: String): Int
    suspend fun search(memberId: String, query: SearchQuery): List<Document>
    suspend fun updateOcrText(documentId: String, ocrText: String): Result<Unit>
}
