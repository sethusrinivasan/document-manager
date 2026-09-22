package com.app.paperstow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.app.paperstow.data.local.entity.DocumentTagEntity

@Dao
interface DocumentTagDao {

    companion object {
        const val CATALOG_DOCUMENT_ID = "__tag_catalog__"
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: DocumentTagEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(tags: List<DocumentTagEntity>)

    @Query("SELECT * FROM document_tags WHERE documentId = :documentId")
    suspend fun getByDocumentId(documentId: String): List<DocumentTagEntity>

    @Query("DELETE FROM document_tags WHERE documentId = :documentId AND tag = :tag")
    suspend fun delete(documentId: String, tag: String)

    @Query("DELETE FROM document_tags WHERE documentId = :documentId")
    suspend fun deleteAllForDocument(documentId: String)

    @Query(
        """
        DELETE FROM document_tags
        WHERE tag = :tag
        AND (
            documentId = :catalogId
            OR documentId IN (SELECT id FROM documents WHERE memberId = :memberId)
        )
        """
    )
    suspend fun deleteTagGlobally(
        memberId: String,
        tag: String,
        catalogId: String = CATALOG_DOCUMENT_ID
    )

    @Query("SELECT COUNT(*) FROM document_tags WHERE documentId = :documentId")
    suspend fun getCount(documentId: String): Int

    @Query(
        """
        SELECT DISTINCT dt.* FROM document_tags dt
        LEFT JOIN documents d ON dt.documentId = d.id
        WHERE d.memberId = :memberId OR dt.documentId = :catalogId
        """
    )
    suspend fun getAllTagsForMember(
        memberId: String,
        catalogId: String = CATALOG_DOCUMENT_ID
    ): List<DocumentTagEntity>

    @Query(
        """
        SELECT COUNT(DISTINCT dt.documentId) FROM document_tags dt
        INNER JOIN documents d ON dt.documentId = d.id
        WHERE d.memberId = :memberId AND dt.tag = :tag
        """
    )
    suspend fun getDocumentCountForTag(memberId: String, tag: String): Int

    @Query(
        """
        UPDATE document_tags SET tag = :newName
        WHERE tag = :oldName AND (
            documentId = :catalogId
            OR documentId IN (SELECT id FROM documents WHERE memberId = :memberId)
        )
        """
    )
    suspend fun renameTag(
        memberId: String,
        oldName: String,
        newName: String,
        catalogId: String = CATALOG_DOCUMENT_ID
    )
}
