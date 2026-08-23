package com.app.traveldocs.debug

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.app.traveldocs.R
import com.app.traveldocs.presentation.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Foreground service that logs GPS coordinates at a configurable interval.
 * Shows a persistent notification so the user is always aware it's running.
 * Skips logging if location hasn't changed (< 10m movement).
 */
class LocationTrackingService : Service() {

    companion object {
        const val CHANNEL_ID = "location_tracking_channel"
        const val NOTIFICATION_ID = 1001
        const val PREFS_NAME = "location_tracking_prefs"
        const val KEY_INTERVAL_MS = "interval_ms"
        const val DEFAULT_INTERVAL_MS = 60_000L // 1 minute
        const val MIN_MOVE_METERS = 10.0f

        const val ACTION_START = "com.app.traveldocs.START_TRACKING"
        const val ACTION_STOP = "com.app.traveldocs.STOP_TRACKING"

        fun getInterval(context: Context): Long {
            val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            return prefs.getLong(KEY_INTERVAL_MS, DEFAULT_INTERVAL_MS)
        }

        fun setInterval(context: Context, intervalMs: Long) {
            val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            prefs.edit().putLong(KEY_INTERVAL_MS, intervalMs).apply()
        }

        fun start(context: Context) {
            context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putBoolean("is_tracking", true).apply()
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putBoolean("is_tracking", false).apply()
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var trackingJob: Job? = null
    private var lastLat: Double? = null
    private var lastLng: Double? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        DebugLogger.i("LocationService", "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Always call startForeground immediately to avoid ForegroundServiceDidNotStartInTimeException
        startForeground(NOTIFICATION_ID, buildNotification())
        when (intent?.action) {
            ACTION_STOP -> {
                DebugLogger.i("LocationService", "Stop requested")
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                startTracking()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        trackingJob?.cancel()
        scope.cancel()
        DebugLogger.i("LocationService", "Service destroyed")
        super.onDestroy()
    }

    private fun startTracking() {
        trackingJob?.cancel()
        trackingJob = scope.launch {
            DebugLogger.i("LocationService", "Tracking started, interval=${getInterval(this@LocationTrackingService)}ms")
            while (true) {
                val intervalMs = getInterval(this@LocationTrackingService)
                delay(intervalMs)
                logLocationIfMoved()
            }
        }
    }

    @Suppress("MissingPermission")
    private fun logLocationIfMoved() {
        try {
            val hasPermission = checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                DebugLogger.w("LocationService", "No location permission")
                return
            }

            val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val loc = lm.getLastKnownLocation(LocationManager.FUSED_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            if (loc == null) {
                DebugLogger.d("LocationService", "No location fix available")
                return
            }

            val prevLat = lastLat
            val prevLng = lastLng

            if (prevLat != null && prevLng != null) {
                val results = FloatArray(1)
                Location.distanceBetween(prevLat, prevLng, loc.latitude, loc.longitude, results)
                if (results[0] < MIN_MOVE_METERS) {
                    return // Skip — hasn't moved enough
                }
            }

            lastLat = loc.latitude
            lastLng = loc.longitude
            DebugLogger.i("LocationService", "GPS: lat=" + loc.latitude + ", lng=" + loc.longitude + ", accuracy=" + loc.accuracy + "m")
            scope.launch { storeTrackPoint(loc.latitude, loc.longitude, loc.accuracy) }
        } catch (e: Exception) {
            DebugLogger.e("LocationService", "Error getting location", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Logs travel route in background"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingOpen = PendingIntent.getActivity(
            this, 0, openIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, LocationTrackingService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val intervalSec = getInterval(this) / 1000

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Logging travel route")
            .setContentText("GPS logged every ${intervalSec}s (only when moving)")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingOpen)
            .addAction(android.R.drawable.ic_delete, "Stop", pendingStop)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private val db by lazy { androidx.room.Room.databaseBuilder(applicationContext, com.app.traveldocs.data.local.TravelDocsDatabase::class.java, "traveldocs.db").fallbackToDestructiveMigration().build() }

    private suspend fun storeTrackPoint(lat: Double, lng: Double, accuracy: Float) {
        try {
            val entity = com.app.traveldocs.data.local.entity.GpsTrackEntity(
                latitude = lat, longitude = lng, accuracy = accuracy,
                timestamp = System.currentTimeMillis(), isMoving = true
            )
            db.gpsTrackDao().insert(entity)
        } catch (e: Exception) {
            DebugLogger.e("LocationService", "Failed to store GPS track", e)
        }
    }}
