package com.app.paperstow.data.safety

import android.content.Context
import com.app.paperstow.data.local.dao.GpsTrackDao
import com.app.paperstow.data.local.entity.GpsTrackEntity
import com.app.paperstow.domain.safety.SafetyPlace
import com.app.paperstow.domain.safety.UniqueLocations
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SafetyLocationRepository @Inject constructor(
    private val dao: GpsTrackDao,
    @ApplicationContext private val context: Context
) {
    fun observeLast24Hours(): Flow<List<SafetyPlace>> {
        return dao.observeAllNewestFirst().map { rows ->
            val cutoff = System.currentTimeMillis() - UniqueLocations.WINDOW_MS
            rows.filter { it.timestamp >= cutoff }.map { it.toPlace() }
        }
    }

    suspend fun pruneExpired() {
        dao.deleteOlderThan(System.currentTimeMillis() - UniqueLocations.WINDOW_MS)
    }

    suspend fun recordFix(latitude: Double, longitude: Double, accuracy: Float, nowMs: Long = System.currentTimeMillis()) {
        if (accuracy > UniqueLocations.MAX_ACCURACY_METERS) return
        pruneExpired()
        val batteryPercent = DeviceBattery.remainingPercent(context)
        val last = dao.getLastTrack()
        if (last != null && !UniqueLocations.isNewPlace(last.latitude, last.longitude, latitude, longitude)) {
            dao.updateTimestampAndBattery(last.id, nowMs, batteryPercent)
            return
        }
        dao.insert(
            GpsTrackEntity(
                latitude = latitude,
                longitude = longitude,
                accuracy = accuracy,
                timestamp = nowMs,
                isMoving = last != null,
                batteryPercent = batteryPercent
            )
        )
    }

    suspend fun clearAll() {
        dao.deleteAll()
    }

    private fun GpsTrackEntity.toPlace() = SafetyPlace(
        latitude = latitude,
        longitude = longitude,
        timestamp = timestamp,
        batteryPercent = batteryPercent
    )
}
