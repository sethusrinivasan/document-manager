package com.app.paperstow.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.app.paperstow.data.local.dao.DocumentDao
import com.app.paperstow.data.local.dao.DocumentMetadataDao
import com.app.paperstow.data.local.dao.DocumentTagDao
import com.app.paperstow.data.local.dao.FamilyMemberDao
import com.app.paperstow.data.local.dao.GpsTrackDao
import com.app.paperstow.data.local.entity.DocumentEntity
import com.app.paperstow.data.local.entity.DocumentMetadataEntity
import com.app.paperstow.data.local.entity.DocumentTagEntity
import com.app.paperstow.data.local.entity.FamilyMemberEntity
import com.app.paperstow.data.local.entity.GpsTrackEntity

@Database(
    entities = [
        DocumentEntity::class,
        DocumentMetadataEntity::class,
        DocumentTagEntity::class,
        FamilyMemberEntity::class,
        GpsTrackEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class TravelDocsDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun documentMetadataDao(): DocumentMetadataDao
    abstract fun documentTagDao(): DocumentTagDao
    abstract fun familyMemberDao(): FamilyMemberDao
    abstract fun gpsTrackDao(): GpsTrackDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE gps_tracks ADD COLUMN batteryPercent INTEGER NOT NULL DEFAULT -1")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE documents ADD COLUMN ocrText TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
