package com.app.paperstow.domain.repository

import com.app.paperstow.domain.model.Tag

interface TagRepository {
    suspend fun getTagsForDocument(documentId: String): List<Tag>
    suspend fun addTag(documentId: String, tagName: String): Result<Unit>
    suspend fun removeTag(documentId: String, tagName: String): Result<Unit>
    suspend fun deleteTagGlobally(memberId: String, tagName: String): Result<Unit>
    suspend fun getAllTags(memberId: String): List<Tag>
    suspend fun createStandaloneTag(memberId: String, tagName: String): Result<Unit>
}
