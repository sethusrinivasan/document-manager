package com.app.traveldocs.data.local.auth

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.app.traveldocs.debug.DebugLogger
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles security alerts on PIN lockout events.
 * Instead of sending SMS (which requires SEND_SMS restricted permission),
 * we show a local notification and log the event. The user can view
 * the alert history in Settings.
 *
 * We originally used SMS for alerts but Google Play requires a Permissions Declaration Form
 * for SEND_SMS and will reject document-manager apps that request it. Local notifications
 * accomplish the same thing without the permission headache.
 */
@Singleton
class SecurityAlertService @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext
    private val context: Context,
    private val securityPreferences: SecurityPreferences
) {
    fun onLockoutTriggered() {
        if (!securityPreferences.smsAlertsEnabled || !securityPreferences.isConfigured()) {
            DebugLogger.w("SecurityAlert", "Alerts disabled or not configured")
            return
        }
        val timestamp = SimpleDateFormat("HH:mm dd-MMM-yyyy", Locale.US).format(Date())
        val location = getLocationString()
        val message = "Document Manager alert: PIN failed 3 times at $timestamp. $location"
        showSecurityNotification(message)
        securityPreferences.lastAlertTime = System.currentTimeMillis()
        DebugLogger.i("SecurityAlert", "Lockout alert dispatched at $timestamp")
    }

    private fun showSecurityNotification(message: String) {
        try {
            val channelId = "security_alerts"
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(channelId, "Security Alerts", NotificationManager.IMPORTANCE_HIGH)
                channel.description = "Alerts when PIN lockout is triggered"
                notificationManager.createNotificationChannel(channel)
            }

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Security Alert")
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            notificationManager.notify(8888, notification)
            DebugLogger.i("SecurityAlert", "Security notification shown")
        } catch (e: Exception) {
            DebugLogger.e("SecurityAlert", "Failed to show notification", e)
        }
    }

    @Suppress("MissingPermission")
    private fun getLocationString(): String {
        return try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return "Location: n/a"
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val loc = lm.getLastKnownLocation(LocationManager.FUSED_PROVIDER) ?: lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (loc != null) "Location: ${loc.latitude},${loc.longitude}" else "Location: n/a"
        } catch (_: Exception) { "Location: n/a" }
    }
}
