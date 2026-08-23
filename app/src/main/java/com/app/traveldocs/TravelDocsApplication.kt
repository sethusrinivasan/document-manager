package com.app.traveldocs

import android.app.Application
import com.app.traveldocs.debug.CrashHandler
import com.app.traveldocs.debug.DebugLogger
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point.
 *
 * Two things happen here at startup:
 * 1. Debug logger gets initialized (writes to logcat + file + in-memory buffer)
 * 2. Crash handler hooks into the uncaught exception pipeline so we never lose a stack trace
 *
 * Everything else (DI, Room, etc.) is handled by Hilt and lazy initialization.
 */
@HiltAndroidApp
class TravelDocsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DebugLogger.init(this)
        CrashHandler.install(this)
        DebugLogger.i("App", "Document Manager started. Build: ${BuildConfig.VERSION_NAME}")
    }
}
