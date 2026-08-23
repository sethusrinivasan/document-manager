package com.app.traveldocs.debug

import android.app.ActivityManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Captures system-level telemetry: resource usage, connectivity, GPS location.
 *
 * - On launch/resume: logs memory, CPU info, top apps, connectivity, GPS
 * - Background: wakes every 60s to check GPS, only logs if location changed
 */
class SystemTelemetry(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var locationJob: Job? = null
    private var lastLoggedLat: Double? = null
    private var lastLoggedLng: Double? = null
    private val locationManager by lazy {
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    companion object {
        private const val TAG = "Telemetry"
        private const val LOCATION_CHANGE_THRESHOLD_METERS = 10.0
        private const val POLL_INTERVAL_MS = 60_000L
    }

    /**
     * Call on onCreate/onResume. Logs full system snapshot.
     */
    fun captureSnapshot(event: String) {
        scope.launch {
            DebugLogger.i(TAG, "═══ System Snapshot ($event) ═══")
            logMemoryAndCpu()
            logTopApps()
            logConnectivity()
            logCurrentLocation()
        }
    }

    /**
     * Start background GPS polling (every 60s, only logs if moved).
     */
    fun startLocationPolling() {
        stopLocationPolling()
        locationJob = scope.launch {
            DebugLogger.d(TAG, "GPS polling started (60s interval)")
            while (true) {
                delay(POLL_INTERVAL_MS)
                logLocationIfChanged()
            }
        }
    }

    /**
     * Stop background GPS polling.
     */
    fun stopLocationPolling() {
        locationJob?.cancel()
        locationJob = null
    }

    private fun logMemoryAndCpu() {
        try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)

            val totalMb = memInfo.totalMem / (1024 * 1024)
            val availMb = memInfo.availMem / (1024 * 1024)
            val usedMb = totalMb - availMb
            val usedPercent = (usedMb * 100) / totalMb

            DebugLogger.i(TAG, "Memory: ${usedMb}MB / ${totalMb}MB (${usedPercent}% used, avail=${availMb}MB, lowMem=${memInfo.lowMemory})")

            // Battery
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val batteryLevel = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
            val isCharging = batteryManager?.isCharging ?: false
            DebugLogger.i(TAG, "Battery: ${batteryLevel}% ${if (isCharging) "(charging)" else "(discharging)"}")

            // Runtime memory for our process
            val runtime = Runtime.getRuntime()
            val appMaxMb = runtime.maxMemory() / (1024 * 1024)
            val appTotalMb = runtime.totalMemory() / (1024 * 1024)
            val appFreeMb = runtime.freeMemory() / (1024 * 1024)
            val appUsedMb = appTotalMb - appFreeMb
            DebugLogger.d(TAG, "App heap: used=${appUsedMb}MB, total=${appTotalMb}MB, max=${appMaxMb}MB")
        } catch (e: Exception) {
            DebugLogger.e(TAG, "Failed to read memory/CPU info", e)
        }
    }

    private fun logTopApps() {
        try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

            // getRunningAppProcesses gives us running processes
            val processes = activityManager.runningAppProcesses ?: emptyList()

            // Sort by importance (lower = more important/active)
            val top5 = processes
                .sortedBy { it.importance }
                .take(5)

            DebugLogger.i(TAG, "Top 5 active processes:")
            top5.forEachIndexed { idx, proc ->
                val importanceLabel = when {
                    proc.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND -> "FOREGROUND"
                    proc.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE -> "VISIBLE"
                    proc.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE -> "SERVICE"
                    else -> "BACKGROUND"
                }
                DebugLogger.d(TAG, "  ${idx + 1}. ${proc.processName} [$importanceLabel] pid=${proc.pid}")
            }
        } catch (e: Exception) {
            DebugLogger.e(TAG, "Failed to get top apps", e)
        }
    }

    private fun logConnectivity() {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork
            if (network == null) {
                DebugLogger.w(TAG, "Connectivity: NO ACTIVE NETWORK (offline)")
                return
            }

            val caps = cm.getNetworkCapabilities(network)
            if (caps == null) {
                DebugLogger.w(TAG, "Connectivity: network present but no capabilities")
                return
            }

            val type = when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
                else -> "Other"
            }

            val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            val downMbps = caps.linkDownstreamBandwidthKbps / 1000
            val upMbps = caps.linkUpstreamBandwidthKbps / 1000

            DebugLogger.i(TAG, "Connectivity: $type | internet=$hasInternet | validated=$validated | down=${downMbps}Mbps | up=${upMbps}Mbps")
        } catch (e: Exception) {
            DebugLogger.e(TAG, "Failed to read connectivity", e)
        }
    }

    @Suppress("MissingPermission")
    private fun logCurrentLocation() {
        try {
            if (!hasLocationPermission()) {
                DebugLogger.w(TAG, "GPS: Location permission not granted — skipping")
                return
            }

            val providers = locationManager.getProviders(true)
            DebugLogger.d(TAG, "GPS: Available providers: $providers")

            // Try to get last known location from best available provider
            val location = getLastKnownLocation()
            if (location != null) {
                lastLoggedLat = location.latitude
                lastLoggedLng = location.longitude
                DebugLogger.i(TAG, "GPS: lat=${location.latitude}, lng=${location.longitude}, accuracy=${location.accuracy}m, provider=${location.provider}, age=${getLocationAgeMs(location)}ms")
            } else {
                DebugLogger.w(TAG, "GPS: No last known location available")
            }
        } catch (e: SecurityException) {
            DebugLogger.w(TAG, "GPS: Permission denied", e)
        } catch (e: Exception) {
            DebugLogger.e(TAG, "GPS: Failed to read location", e)
        }
    }

    @Suppress("MissingPermission")
    private fun logLocationIfChanged() {
        try {
            if (!hasLocationPermission()) return

            val location = getLastKnownLocation() ?: return

            val prevLat = lastLoggedLat
            val prevLng = lastLoggedLng

            if (prevLat != null && prevLng != null) {
                val distance = FloatArray(1)
                Location.distanceBetween(prevLat, prevLng, location.latitude, location.longitude, distance)
                if (distance[0] < LOCATION_CHANGE_THRESHOLD_METERS) {
                    // Location hasn't changed enough — skip logging
                    return
                }
            }

            lastLoggedLat = location.latitude
            lastLoggedLng = location.longitude
            DebugLogger.i(TAG, "GPS (periodic): lat=${location.latitude}, lng=${location.longitude}, accuracy=${location.accuracy}m")
        } catch (e: Exception) {
            // Silently skip on errors during background polling
        }
    }

    @Suppress("MissingPermission")
    private fun getLastKnownLocation(): Location? {
        val providers = listOf(LocationManager.FUSED_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        for (provider in providers) {
            try {
                val loc = locationManager.getLastKnownLocation(provider)
                if (loc != null) return loc
            } catch (_: Exception) {
                // Try next provider
            }
        }
        return null
    }

    private fun hasLocationPermission(): Boolean {
        val fine = context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                coarse == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun getLocationAgeMs(location: Location): Long {
        return System.currentTimeMillis() - location.time
    }
}
