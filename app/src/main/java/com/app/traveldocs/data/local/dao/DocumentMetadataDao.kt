package com.app.traveldocs.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.app.traveldocs.data.local.entity.DocumentMetadataEntity

@Dao
interface DocumentMetadataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(metadata: DocumentMetadataEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(metadata: List<DocumentMetadataEntity>)

    @Query("SELECT * FROM document_metadata WHERE documentId = :documentId")
    suspend fun getByDocumentId(documentId: String): List<DocumentMetadataEntity>

    @Query("DELETE FROM document_metadata WHERE documentId = :documentId")
    suspend fun deleteByDocumentId(documentId: String)
}
