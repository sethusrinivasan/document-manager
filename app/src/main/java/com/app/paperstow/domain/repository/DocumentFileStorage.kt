package com.app.paperstow.domain.repository

import com.app.paperstow.domain.model.DocumentFormat

interface DocumentFileStorage {
    suspend fun store(memberId: String, fileData: ByteArray, format: DocumentFormat): Result<String>
    suspend fun retrieve(fileId: String): Result<ByteArray>
    suspend fun replace(fileId: String, fileData: ByteArray): Result<Unit>
    suspend fun secureDelete(fileId: String): Result<Unit>
}
