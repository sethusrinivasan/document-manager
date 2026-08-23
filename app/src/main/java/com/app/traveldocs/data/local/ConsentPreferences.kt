package com.app.traveldocs.data.local

import android.content.Context
import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConsentPreferences @Inject constructor(@dagger.hilt.android.qualifiers.ApplicationContext private val context: Context) {

    companion object {
        private const val PREFS_NAME = "encryption_consent"
        private const val KEY_HAS_CONSENTED = "has_consented"
        private const val KEY_REGION = "selected_region"
        private const val KEY_ENCRYPTION_ENABLED = "encryption_enabled"
        private const val KEY_PIN_WARNING_ACKNOWLEDGED = "pin_warning_acked"

        // Countries where personal-use encryption is generally permitted
        val ENCRYPTION_PERMITTED_REGIONS = listOf(
            "United States", "United Kingdom", "Germany", "France", "Canada",
            "Australia", "Japan", "Singapore", "India", "Brazil", "Mexico",
            "Italy", "Spain", "Netherlands", "Sweden", "Norway", "Denmark",
            "Switzerland", "New Zealand", "South Korea", "Ireland", "Portugal",
            "Belgium", "Austria", "Finland", "Poland", "Czech Republic"
        )

        // Countries with encryption restrictions (simplified list)
        val ENCRYPTION_RESTRICTED_REGIONS = listOf(
            "China", "Russia", "Belarus", "Kazakhstan", "Turkmenistan",
            "Iran", "Myanmar", "North Korea", "Cuba"
        )

        val ALL_REGIONS = (ENCRYPTION_PERMITTED_REGIONS + ENCRYPTION_RESTRICTED_REGIONS).sorted()
    }

    private val prefs: SharedPreferences
        get() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val hasConsented: Boolean get() = prefs.getBoolean(KEY_HAS_CONSENTED, false)
    val selectedRegion: String get() = prefs.getString(KEY_REGION, "") ?: ""
    val encryptionEnabled: Boolean get() = prefs.getBoolean(KEY_ENCRYPTION_ENABLED, false)
    val pinWarningAcknowledged: Boolean get() = prefs.getBoolean(KEY_PIN_WARNING_ACKNOWLEDGED, false)

    fun isEncryptionPermittedForRegion(region: String): Boolean {
        return region in ENCRYPTION_PERMITTED_REGIONS
    }

    fun saveConsent(region: String, encryptionEnabled: Boolean, pinWarningAcked: Boolean) {
        prefs.edit()
            .putBoolean(KEY_HAS_CONSENTED, true)
            .putString(KEY_REGION, region)
            .putBoolean(KEY_ENCRYPTION_ENABLED, encryptionEnabled)
            .putBoolean(KEY_PIN_WARNING_ACKNOWLEDGED, pinWarningAcked)
            .apply()
    }
}
