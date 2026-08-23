package com.app.traveldocs.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.app.traveldocs.data.local.entity.GpsTrackEntity

@Dao
interface GpsTrackDao {
    @Insert
    suspend fun insert(track: GpsTrackEntity)

    @Query("SELECT * FROM gps_tracks ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastTrack(): GpsTrackEntity?

    @Query("SELECT * FROM gps_tracks WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    suspend fun getTracksBetween(startTime: Long, endTime: Long): List<GpsTrackEntity>

    @Query("SELECT * FROM gps_tracks ORDER BY timestamp ASC")
    suspend fun getAllTracks(): List<GpsTrackEntity>

    @Query("SELECT COUNT(*) FROM gps_tracks")
    suspend fun getCount(): Int

    @Query("DELETE FROM gps_tracks")
    suspend fun deleteAll()
}
