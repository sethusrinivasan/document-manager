package com.app.traveldocs.domain.repository

import com.app.traveldocs.domain.model.DocumentFormat

interface DocumentFileStorage {
    suspend fun store(memberId: String, fileData: ByteArray, format: DocumentFormat): Result<String>
    suspend fun retrieve(fileId: String): Result<ByteArray>
    suspend fun secureDelete(fileId: String): Result<Unit>
}
