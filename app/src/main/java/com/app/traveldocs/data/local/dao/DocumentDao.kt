package com.app.traveldocs.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.app.traveldocs.data.local.entity.DocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(document: DocumentEntity)

    @Query("SELECT * FROM documents WHERE memberId = :memberId ORDER BY updatedAt DESC")
    fun getAllByMemberId(memberId: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :documentId")
    suspend fun getById(documentId: String): DocumentEntity?

    @Query("DELETE FROM documents WHERE id = :documentId")
    suspend fun delete(documentId: String)

    @Query("SELECT COUNT(*) FROM documents WHERE memberId = :memberId")
    suspend fun getCount(memberId: String): Int

    @Query(
        """
        SELECT DISTINCT d.* FROM documents d
        LEFT JOIN document_tags dt ON d.id = dt.documentId
        LEFT JOIN document_metadata dm ON d.id = dm.documentId
        WHERE d.memberId = :memberId
        AND (:freeText IS NULL OR dt.tag LIKE '%' || :freeText || '%' OR dm.value LIKE '%' || :freeText || '%')
        ORDER BY d.updatedAt DESC
        """
    )
    suspend fun searchByFreeText(memberId: String, freeText: String?): List<DocumentEntity>

    @Query(
        """
        SELECT d.* FROM documents d
        INNER JOIN document_tags dt ON d.id = dt.documentId
        WHERE d.memberId = :memberId AND dt.tag IN (:tags)
        GROUP BY d.id
        HAVING COUNT(DISTINCT dt.tag) = :tagCount
        ORDER BY d.updatedAt DESC
        """
    )
    suspend fun searchByTags(memberId: String, tags: List<String>, tagCount: Int): List<DocumentEntity>

    @Query(
        """
        SELECT d.* FROM documents d
        INNER JOIN document_tags dt ON d.id = dt.documentId
        LEFT JOIN document_metadata dm ON d.id = dm.documentId
        WHERE d.memberId = :memberId
        AND dt.tag IN (:tags)
        AND (:freeText IS NULL OR dt.tag LIKE '%' || :freeText || '%' OR dm.value LIKE '%' || :freeText || '%')
        GROUP BY d.id
        HAVING COUNT(DISTINCT dt.tag) = :tagCount
        ORDER BY d.updatedAt DESC
        """
    )
    suspend fun searchByTagsAndFreeText(
        memberId: String,
        tags: List<String>,
        tagCount: Int,
        freeText: String?
    ): List<DocumentEntity>
}
