package com.app.paperstow.data.local

import android.content.Context

/**
 * Legacy SharedPreferences flags.
 *
 * Experimental cloud, GPS, WiFi, Auto, and DICOM features were removed from the
 * Play Store build. These readers stay so existing preference files do not crash
 * older call sites, but all default to OFF and setters are no-ops for removed features.
 */
object FeatureFlags {
    fun isExperimentalEnabled(context: Context) = false
    fun isGoogleDriveEnabled(context: Context) = false
    fun isS3Enabled(context: Context) = false
    fun isGpsTrackingEnabled(context: Context) = false
    fun setGpsTracking(context: Context, v: Boolean) { /* removed */ }

    fun isBackupRestoreEnabled(context: Context) = true
    fun isWifiShareEnabled(context: Context) = false
    fun setWifiShare(context: Context, v: Boolean) { /* removed */ }

    fun isAudioPlaybackEnabled(context: Context) = false
    fun setAudioPlayback(context: Context, v: Boolean) { /* removed */ }

    fun isExtendedFormatsEnabled(context: Context) = false
    fun setExtendedFormats(context: Context, v: Boolean) { /* removed */ }

    fun setExperimental(context: Context, v: Boolean) { /* removed */ }
    fun setGoogleDrive(context: Context, v: Boolean) { /* removed */ }
    fun setS3(context: Context, v: Boolean) { /* removed */ }
    fun setBackupRestore(context: Context, v: Boolean) { /* always on */ }

    fun isFormatEnabled(context: Context, format: String) = format != "dicom"
    fun setFormatEnabled(context: Context, format: String, v: Boolean) { /* removed */ }
}
