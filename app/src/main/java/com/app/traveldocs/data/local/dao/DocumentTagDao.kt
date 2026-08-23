package com.app.traveldocs.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.app.traveldocs.data.local.entity.DocumentTagEntity

@Dao
interface DocumentTagDao {

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
        AND documentId IN (SELECT id FROM documents WHERE memberId = :memberId)
        """
    )
    suspend fun deleteTagGlobally(memberId: String, tag: String)

    @Query("SELECT COUNT(*) FROM document_tags WHERE documentId = :documentId")
    suspend fun getCount(documentId: String): Int

    @Query(
        """
        SELECT DISTINCT dt.* FROM document_tags dt
        INNER JOIN documents d ON dt.documentId = d.id
        WHERE d.memberId = :memberId
        """
    )
    suspend fun getAllTagsForMember(memberId: String): List<DocumentTagEntity>

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
        WHERE tag = :oldName AND documentId IN (SELECT id FROM documents WHERE memberId = :memberId)
        """
    )
    suspend fun renameTag(memberId: String, oldName: String, newName: String)
}
