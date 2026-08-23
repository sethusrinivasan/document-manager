package com.app.traveldocs.data.local

import android.content.Context

/**
 * Simple SharedPreferences-backed feature flags.
 *
 * No server, no remote config, no complexity. Toggle in Settings, persists across restarts.
 * All flags default to OFF — users only see stable features until they explicitly opt in.
 *
 * Why not a proper feature flag service? Because this app has no backend.
 * The whole point is zero network dependency for core functionality.
 */
object FeatureFlags {
    private const val PREFS = "feature_flags"

    fun isExperimentalEnabled(context: Context) = prefs(context).getBoolean("experimental", false)
    fun isGoogleDriveEnabled(context: Context) = prefs(context).getBoolean("google_drive", false)
    fun isS3Enabled(context: Context) = prefs(context).getBoolean("s3_storage", false)
    fun isGpsTrackingEnabled(context: Context) = prefs(context).getBoolean("gps_tracking", false)
    fun setGpsTracking(context: Context, v: Boolean) = prefs(context).edit().putBoolean("gps_tracking", v).apply()

    fun isBackupRestoreEnabled(context: Context) = prefs(context).getBoolean("backup_restore", false)
    fun isWifiShareEnabled(context: Context) = prefs(context).getBoolean("wifi_share", false)
    fun setWifiShare(context: Context, v: Boolean) = prefs(context).edit().putBoolean("wifi_share", v).apply()

    fun isAudioPlaybackEnabled(context: Context) = prefs(context).getBoolean("audio_playback", false)
    fun setAudioPlayback(context: Context, v: Boolean) = prefs(context).edit().putBoolean("audio_playback", v).apply()

    fun isExtendedFormatsEnabled(context: Context) = prefs(context).getBoolean("extended_formats", false)
    fun setExtendedFormats(context: Context, v: Boolean) = prefs(context).edit().putBoolean("extended_formats", v).apply()

    fun setExperimental(context: Context, v: Boolean) = prefs(context).edit().putBoolean("experimental", v).apply()
    fun setGoogleDrive(context: Context, v: Boolean) = prefs(context).edit().putBoolean("google_drive", v).apply()
    fun setS3(context: Context, v: Boolean) = prefs(context).edit().putBoolean("s3_storage", v).apply()
    fun setBackupRestore(context: Context, v: Boolean) = prefs(context).edit().putBoolean("backup_restore", v).apply()

    /**
     * Per-format toggles. Each extended image format can be individually enabled/disabled.
     * Defaults to ON when extended formats master toggle is enabled.
     */
    fun isFormatEnabled(context: Context, format: String) = prefs(context).getBoolean("format_$format", true)
    fun setFormatEnabled(context: Context, format: String, v: Boolean) = prefs(context).edit().putBoolean("format_$format", v).apply()

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
