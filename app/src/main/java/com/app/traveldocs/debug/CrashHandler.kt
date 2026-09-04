package com.app.traveldocs.debug

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Global uncaught exception handler that:
 * 1. Logs crashes to DebugLogger (on-device debug log file)
 * 2. Shows a notification offering to send crash feedback to the developer
 * 3. Delegates to the default handler (system crash dialog)
 *
 * Play Store compliance: Google Play Console automatically collects crash reports
 * from users who opt in. This handler adds a local notification as a secondary channel.
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
                appendLine("App: Document Manager")
                appendLine("Version: ${getAppVersion()}")
                appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
                appendLine("Thread: ${thread.name}")
                appendLine("Exception: ${throwable::class.simpleName}: ${throwable.message}")
                appendLine()
                appendLine("Stack trace (first 2000 chars):")
                append(stackTrace.take(2000))
            }

            // Store crash report for later retrieval (user can send it on next launch)
            storeCrashReport(crashSummary)

            // Show notification offering to send feedback (non-blocking)
            showCrashNotification(crashSummary)

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

    private fun showCrashNotification(crashSummary: String) {
        try {
            val channelId = "crash_feedback"
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(channelId, "Crash Reports", NotificationManager.IMPORTANCE_HIGH)
                channel.description = "Notifications when the app crashes, offering to send feedback"
                notificationManager.createNotificationChannel(channel)
            }

            val emailIntent = createFeedbackIntent(crashSummary)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, emailIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle("Document Manager crashed")
                .setContentText("Tap to send crash report to developer")
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            notificationManager.notify(9999, notification)
        } catch (_: Exception) { }
    }

    companion object {
        private const val DEVELOPER_EMAIL = "support@docvault.app"

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
                type = "message/rfc822"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(DEVELOPER_EMAIL))
                putExtra(Intent.EXTRA_SUBJECT, "Document Manager - Crash Report")
                putExtra(Intent.EXTRA_TEXT, crashReport)
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
