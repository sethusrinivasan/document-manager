package com.app.paperstow.debug

import android.content.Context
import android.content.Intent
import android.os.Build
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Global uncaught exception handler that:
 * 1. Logs crashes to DebugLogger (on-device debug log file)
 * 2. Stores a local crash report the user can share later
 * 3. Delegates to the default handler (system crash dialog)
 *
 * Play Console collects crash reports from users who opt in. This handler does
 * not show notifications (no POST_NOTIFICATIONS permission).
 */
class CrashHandler(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            val stackTrace = sw.toString()

            DebugLogger.e("CRASH", "!!! UNCAUGHT EXCEPTION on thread '${thread.name}' !!!")
            DebugLogger.e("CRASH", "Exception: ${throwable::class.simpleName}: ${throwable.message}")
            DebugLogger.e("CRASH", "Stack trace:\n$stackTrace")

            // Log cause chain (up to 5 levels)
            var cause = throwable.cause
            var depth = 0
            while (cause != null && depth < 5) {
                DebugLogger.e("CRASH", "Caused by [$depth]: ${cause::class.simpleName}: ${cause.message}")
                cause = cause.cause
                depth++
            }

            // Build crash report summary for feedback
            val crashSummary = buildString {
                appendLine("App: Paperstow")
                appendLine("Version: ${getAppVersion()}")
                appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
                appendLine("Thread: ${thread.name}")
                appendLine("Exception: ${throwable::class.simpleName}: ${throwable.message}")
                appendLine()
                appendLine("Stack trace (first 2000 chars):")
                append(stackTrace.take(2000))
            }

            storeCrashReport(crashSummary)

        } catch (_: Exception) {
            // Don't let logging crash the crash handler
        }

        // Delegate to the default handler (shows the system crash dialog)
        defaultHandler?.uncaughtException(thread, throwable)
    }

    private fun getAppVersion(): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${pInfo.versionName} (${pInfo.longVersionCode})"
        } catch (_: Exception) { "unknown" }
    }

    private fun storeCrashReport(report: String) {
        try {
            val file = java.io.File(context.filesDir, "last_crash_report.txt")
            file.writeText(report)
        } catch (_: Exception) { }
    }

    companion object {
        private const val ISSUES_URL = "https://github.com/sethusrinivasan/document-manager/issues"

        fun install(context: Context) {
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler(CrashHandler(context, defaultHandler))
            DebugLogger.i("CrashHandler", "Global crash handler installed")
        }

        /**
         * Creates an email intent with crash report attached.
         * Uses ACTION_SEND which shows Android's standard share/email chooser.
         * This is the Play Store-compliant way to collect crash feedback.
         */
        fun createFeedbackIntent(crashReport: String): Intent {
            return Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Paperstow - Crash Report")
                putExtra(Intent.EXTRA_TEXT, crashReport + "\n\nFile at: $ISSUES_URL")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        /**
         * Call from Settings/About screen to let user manually send last crash report.
         */
        fun getLastCrashReport(context: Context): String? {
            val file = java.io.File(context.filesDir, "last_crash_report.txt")
            return if (file.exists()) file.readText() else null
        }

        /**
         * Clear stored crash report after user has sent it.
         */
        fun clearLastCrashReport(context: Context) {
            val file = java.io.File(context.filesDir, "last_crash_report.txt")
            if (file.exists()) file.delete()
        }
    }
}
