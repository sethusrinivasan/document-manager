package com.app.traveldocs.data.local.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.app.traveldocs.debug.DebugLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityPreferences @Inject constructor(@dagger.hilt.android.qualifiers.ApplicationContext private val context: Context) {

    companion object {
        private const val PREFS_NAME = "security_alert_prefs"
        private const val KEY_PHONE = "recovery_phone"
        private const val KEY_EMAIL = "recovery_email"
        private const val KEY_SMS_ENABLED = "sms_alerts_enabled"
        private const val KEY_LAST_ALERT_TIME = "last_alert_time"
    }

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                PREFS_NAME, masterKey, context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            DebugLogger.e("SecurityPrefs", "Failed to create encrypted prefs, falling back", e)
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    var recoveryPhone: String
        get() = prefs.getString(KEY_PHONE, "") ?: ""
        set(value) { prefs.edit().putString(KEY_PHONE, value).apply() }

    var recoveryEmail: String
        get() = prefs.getString(KEY_EMAIL, "") ?: ""
        set(value) { prefs.edit().putString(KEY_EMAIL, value).apply() }

    var smsAlertsEnabled: Boolean
        get() = prefs.getBoolean(KEY_SMS_ENABLED, true)
        set(value) { prefs.edit().putBoolean(KEY_SMS_ENABLED, value).apply() }

    var lastAlertTime: Long
        get() = prefs.getLong(KEY_LAST_ALERT_TIME, 0L)
        set(value) { prefs.edit().putLong(KEY_LAST_ALERT_TIME, value).apply() }

    fun isConfigured(): Boolean = recoveryPhone.isNotBlank() || recoveryEmail.isNotBlank()

    fun isValidPhone(phone: String): Boolean = phone.matches(Regex("^\\+?[0-9]{7,15}$"))
    fun isValidEmail(email: String): Boolean = email.matches(Regex("^[^@]+@[^@]+\\.[^@]+$"))
}
