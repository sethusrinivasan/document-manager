package com.app.traveldocs.debug

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * Centralized debug logger for the Document Manager.
 *
 * Captures logs to:
 * 1. Android Logcat (standard, filtered by tag "TravelDocs")
 * 2. In-memory ring buffer (last 500 entries, viewable in-app)
 * 3. File on device: /data/data/com.app.traveldocs/files/debug_logs/traveldocs_debug.log
 *
 * To pull logs via adb:
 *   adb shell run-as com.app.traveldocs cat files/debug_logs/traveldocs_debug.log > local_debug.log
 *
 * Or from the app's "Debug Logs" screen accessible via the floating bug icon.
 */
object DebugLogger {

    private const val TAG = "TravelDocs"
    private const val MAX_BUFFER_SIZE = 500
    private const val LOG_DIR = "debug_logs"
    private const val LOG_FILE = "traveldocs_debug.log"
    private const val MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024 // 5 MB rotation threshold

    private val buffer = ConcurrentLinkedDeque<LogEntry>()
    private val _logFlow = MutableStateFlow<List<LogEntry>>(emptyList())
    val logFlow: StateFlow<List<LogEntry>> = _logFlow.asStateFlow()

    private var logFile: File? = null
    private var initialized = false

    data class LogEntry(
        val timestamp: String,
        val level: Level,
        val component: String,
        val message: String,
        val throwable: String? = null
    ) {
        override fun toString(): String {
            val base = "[$timestamp] ${level.tag} [$component] $message"
            return if (throwable != null) "$base\n  $throwable" else base
        }
    }

    enum class Level(val tag: String) {
        DEBUG("D"),
        INFO("I"),
        WARN("W"),
        ERROR("E")
    }

    /**
     * Initialize the file-based logger. Call from Application.onCreate().
     */
    fun init(context: Context) {
        if (!com.app.traveldocs.BuildConfig.DEBUG) {
            initialized = true
            return // No file logging in release builds
        }
        if (initialized) return
        val dir = File(context.filesDir, LOG_DIR)
        dir.mkdirs()
        logFile = File(dir, LOG_FILE)
        // Rotate if file is too large
        if (logFile!!.exists() && logFile!!.length() > MAX_FILE_SIZE_BYTES) {
            val backup = File(dir, "traveldocs_debug_prev.log")
            backup.delete()
            logFile!!.renameTo(backup)
            logFile = File(dir, LOG_FILE)
        }
        initialized = true
        i("DebugLogger", "=== Logger initialized. Session started ===")
        i("DebugLogger", "Log file: ${logFile?.absolutePath}")
    }

    fun d(component: String, message: String) = log(Level.DEBUG, component, message)
    fun i(component: String, message: String) = log(Level.INFO, component, message)
    fun w(component: String, message: String, throwable: Throwable? = null) =
        log(Level.WARN, component, message, throwable)
    fun e(component: String, message: String, throwable: Throwable? = null) =
        log(Level.ERROR, component, message, throwable)

    // Single-thread executor for file writes — NEVER blocks the calling thread.
    // This is the key fix for ANR: log() can be called from main thread safely.
    private val fileWriteExecutor = java.util.concurrent.Executors.newSingleThreadExecutor { r ->
        Thread(r, "DebugLogger-FileWriter").apply { isDaemon = true }
    }

    private fun log(level: Level, component: String, message: String, throwable: Throwable? = null) {
        val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
        val throwableStr = throwable?.let { "${it::class.simpleName}: ${it.message}" }
        val entry = LogEntry(timestamp, level, component, message, throwableStr)

        // 1. Logcat (fast, never blocks)
        val logcatMsg = "[$component] $message"
        when (level) {
            Level.DEBUG -> Log.d(TAG, logcatMsg, throwable)
            Level.INFO -> Log.i(TAG, logcatMsg, throwable)
            Level.WARN -> Log.w(TAG, logcatMsg, throwable)
            Level.ERROR -> Log.e(TAG, logcatMsg, throwable)
        }

        // 2. In-memory buffer (fast, lock-free ConcurrentLinkedDeque)
        buffer.addLast(entry)
        while (buffer.size > MAX_BUFFER_SIZE) {
            buffer.pollFirst()
        }
        _logFlow.value = buffer.toList()

        // 3. File — dispatched to background thread. NEVER blocks the caller.
        fileWriteExecutor.execute { writeToFile(entry) }
    }

    private fun writeToFile(entry: LogEntry) {
        // Runs on fileWriteExecutor thread only — sequential, no contention
        try {
            logFile?.let { file ->
                FileWriter(file, true).use { writer ->
                    writer.appendLine(entry.toString())
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write debug log to file", e)
        }
    }

    /**
     * Get all buffered log entries (for displaying in UI).
     */
    fun getEntries(): List<LogEntry> = buffer.toList()

    /**
     * Clear in-memory buffer and log file.
     */
    fun clear() {
        buffer.clear()
        _logFlow.value = emptyList()
        logFile?.delete()
        i("DebugLogger", "Logs cleared")
    }

    /**
     * Get the full path to the log file on device.
     */
    fun getLogFilePath(): String = logFile?.absolutePath ?: "Not initialized"
}
