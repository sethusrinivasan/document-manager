package com.app.paperstow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.app.paperstow.data.local.entity.GpsTrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GpsTrackDao {
    @Insert
    suspend fun insert(track: GpsTrackEntity)

    @Query("SELECT * FROM gps_tracks ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastTrack(): GpsTrackEntity?

    @Query("SELECT * FROM gps_tracks WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    suspend fun getTracksBetween(startTime: Long, endTime: Long): List<GpsTrackEntity>

    @Query("SELECT * FROM gps_tracks ORDER BY timestamp DESC")
    fun observeAllNewestFirst(): Flow<List<GpsTrackEntity>>

    @Query("SELECT * FROM gps_tracks ORDER BY timestamp ASC")
    suspend fun getAllTracks(): List<GpsTrackEntity>

    @Query("SELECT COUNT(*) FROM gps_tracks")
    suspend fun getCount(): Int

    @Query("UPDATE gps_tracks SET timestamp = :timestamp, batteryPercent = :batteryPercent WHERE id = :id")
    suspend fun updateTimestampAndBattery(id: Long, timestamp: Long, batteryPercent: Int)

    @Query("DELETE FROM gps_tracks WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("DELETE FROM gps_tracks")
    suspend fun deleteAll()
}
