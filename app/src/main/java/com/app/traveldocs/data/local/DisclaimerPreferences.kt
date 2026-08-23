package com.app.traveldocs.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DisclaimerPreferences @Inject constructor(@ApplicationContext private val context: Context) {
    companion object {
        private const val PREFS = "disclaimer_prefs"
        private const val KEY_ACCEPTED = "disclaimer_accepted"
        private const val KEY_TIMESTAMP = "accepted_timestamp"
        private const val KEY_TELEMETRY = "telemetry_consented"
    }
    private val prefs by lazy { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    val disclaimerAccepted: Boolean get() = prefs.getBoolean(KEY_ACCEPTED, false)
    val telemetryConsented: Boolean get() = prefs.getBoolean(KEY_TELEMETRY, false)

    fun acceptDisclaimer(telemetryConsent: Boolean) {
        prefs.edit()
            .putBoolean(KEY_ACCEPTED, true)
            .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
            .putBoolean(KEY_TELEMETRY, telemetryConsent)
            .apply()
    }

    fun setTelemetryConsent(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_TELEMETRY, enabled).apply()
    }
}
