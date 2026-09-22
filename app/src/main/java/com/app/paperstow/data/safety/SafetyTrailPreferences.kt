package com.app.paperstow.data.safety

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SafetyTrailPreferences {
    private const val PREFS = "safety_trail_prefs"
    private const val KEY_WANTED = "user_wants_trail"

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    fun wantsTrail(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_WANTED, false)

    fun setWantsTrail(context: Context, wanted: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_WANTED, wanted)
            .apply()
    }

    fun setRunning(running: Boolean) {
        _running.value = running
    }
}
