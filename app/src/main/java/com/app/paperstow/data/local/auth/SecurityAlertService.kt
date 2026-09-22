package com.app.paperstow.data.local.auth

import android.content.Context
import com.app.paperstow.debug.DebugLogger
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Records PIN lockout events locally. Does not send SMS or show notifications
 * (those require extra Play declarations).
 */
@Singleton
class SecurityAlertService @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext
    private val context: Context,
    private val securityPreferences: SecurityPreferences
) {
    fun onLockoutTriggered() {
        val timestamp = SimpleDateFormat("HH:mm dd-MMM-yyyy", Locale.US).format(Date())
        securityPreferences.lastAlertTime = System.currentTimeMillis()
        DebugLogger.i("SecurityAlert", "PIN lockout at $timestamp")
    }
}
