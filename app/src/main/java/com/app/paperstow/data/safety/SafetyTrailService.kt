package com.app.paperstow.data.safety

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.app.paperstow.R
import com.app.paperstow.debug.DebugLogger
import com.app.paperstow.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * User-started location trail. A visible notification stays up while unique
 * places are recorded. Does not use ACCESS_BACKGROUND_LOCATION.
 */
@AndroidEntryPoint
class SafetyTrailService : Service() {

    @Inject lateinit var repository: SafetyLocationRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var locationManager: LocationManager? = null
    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            scope.launch {
                repository.recordFix(location.latitude, location.longitude, location.accuracy)
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        override fun onProviderEnabled(provider: String) = Unit
        override fun onProviderDisabled(provider: String) = Unit
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            SafetyTrailPreferences.setWantsTrail(this, false)
            stopUpdates()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent?.action == ACTION_START || SafetyTrailPreferences.wantsTrail(this) || intent == null) {
            SafetyTrailPreferences.setWantsTrail(this, true)
            startInForeground()
            beginUpdates()
            return START_STICKY
        }
        stopSelf()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopUpdates()
        SafetyTrailPreferences.setRunning(false)
        scope.cancel()
        super.onDestroy()
    }

    private fun startInForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        SafetyTrailPreferences.setRunning(true)
    }

    private fun beginUpdates() {
        val manager = locationManager ?: return
        if (!hasLocationPermission()) {
            DebugLogger.w(TAG, "Location permission missing — stopping trail")
            SafetyTrailPreferences.setWantsTrail(this, false)
            stopSelf()
            return
        }
        try {
            manager.getProviders(true).forEach { provider ->
                if (provider == LocationManager.GPS_PROVIDER || provider == LocationManager.NETWORK_PROVIDER) {
                    manager.requestLocationUpdates(provider, MIN_TIME_MS, MIN_DISTANCE_METERS, listener)
                }
            }
            listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).forEach { provider ->
                if (manager.isProviderEnabled(provider)) {
                    manager.getLastKnownLocation(provider)?.let { location ->
                        scope.launch {
                            repository.recordFix(location.latitude, location.longitude, location.accuracy)
                        }
                    }
                }
            }
            DebugLogger.i(TAG, "My Trail started")
        } catch (e: SecurityException) {
            DebugLogger.e(TAG, "Location updates denied", e)
            SafetyTrailPreferences.setWantsTrail(this, false)
            stopSelf()
        }
    }

    private fun stopUpdates() {
        try {
            locationManager?.removeUpdates(listener)
        } catch (_: Exception) {
        }
        SafetyTrailPreferences.setRunning(false)
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.safety_notification_channel),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.safety_notification_channel_desc)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            this,
            1,
            Intent(this, SafetyTrailService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(getString(R.string.safety_notification_title))
            .setContentText(getString(R.string.safety_notification_text))
            .setOngoing(true)
            .setContentIntent(openApp)
            .addAction(0, getString(R.string.safety_stop), stop)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    companion object {
        const val ACTION_START = "com.app.paperstow.safety.START"
        const val ACTION_STOP = "com.app.paperstow.safety.STOP"
        private const val TAG = "SafetyTrail"
        private const val CHANNEL_ID = "family_safety_trail"
        private const val NOTIFICATION_ID = 41
        private const val MIN_TIME_MS = 2 * 60 * 1000L
        private const val MIN_DISTANCE_METERS = 100f

        fun start(context: Context) {
            val intent = Intent(context, SafetyTrailService::class.java).setAction(ACTION_START)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, SafetyTrailService::class.java).setAction(ACTION_STOP)
            context.startService(intent)
        }
    }
}
