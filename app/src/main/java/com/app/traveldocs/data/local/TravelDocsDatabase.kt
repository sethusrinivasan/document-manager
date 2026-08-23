package com.app.traveldocs.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.app.traveldocs.data.local.dao.DocumentDao
import com.app.traveldocs.data.local.dao.DocumentMetadataDao
import com.app.traveldocs.data.local.dao.DocumentTagDao
import com.app.traveldocs.data.local.dao.FamilyMemberDao
import com.app.traveldocs.data.local.dao.GpsTrackDao
import com.app.traveldocs.data.local.entity.DocumentEntity
import com.app.traveldocs.data.local.entity.DocumentMetadataEntity
import com.app.traveldocs.data.local.entity.DocumentTagEntity
import com.app.traveldocs.data.local.entity.FamilyMemberEntity
import com.app.traveldocs.data.local.entity.GpsTrackEntity

@Database(
    entities = [
        DocumentEntity::class,
        DocumentMetadataEntity::class,
        DocumentTagEntity::class,
        FamilyMemberEntity::class,
        GpsTrackEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class TravelDocsDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun documentMetadataDao(): DocumentMetadataDao
    abstract fun documentTagDao(): DocumentTagDao
    abstract fun familyMemberDao(): FamilyMemberDao
    abstract fun gpsTrackDao(): GpsTrackDao
}
