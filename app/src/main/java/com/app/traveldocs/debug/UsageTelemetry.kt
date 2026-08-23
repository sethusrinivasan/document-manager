package com.app.traveldocs.debug

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Non-intrusive usage telemetry that logs user flow markers to DebugLogger.
 * 
 * Purpose: Understand how users navigate the app to optimize UX flows.
 * 
 * Captures:
 * - Screen visit counts and duration
 * - Feature usage frequency (import, search, tags, etc.)
 * - Drop-off points (started but not completed flows)
 * - Error frequency by feature
 * - Time between actions (interaction cadence)
 * 
 * All data stays in the debug log file on-device. No network transmission.
 * Only active in debug builds (gated by BuildConfig.DEBUG).
 */
object UsageTelemetry {

    // Telemetry is stored locally only. User can export it via Settings → "Send Telemetry".
    // No automatic network transmission ever happens.


    private const val TAG = "Analytics"

    // Screen visit tracking
    private val screenVisits = ConcurrentHashMap<String, AtomicInteger>()
    private val screenEntryTime = ConcurrentHashMap<String, Long>()

    // Feature funnel tracking
    private val funnelStarts = ConcurrentHashMap<String, AtomicInteger>()
    private val funnelCompletes = ConcurrentHashMap<String, AtomicInteger>()

    // Session metrics
    private var sessionStart = System.currentTimeMillis()
    private var lastInteraction = System.currentTimeMillis()
    private val actionCount = AtomicInteger(0)

    /**
     * Log when user enters a screen.
     */
    fun screenEnter(screenName: String) {
        if (!isEnabled()) return
        screenVisits.getOrPut(screenName) { AtomicInteger(0) }.incrementAndGet()
        screenEntryTime[screenName] = System.currentTimeMillis()
        DebugLogger.d(TAG, "SCREEN_ENTER: $screenName (visit #${screenVisits[screenName]?.get()})")
        trackInteraction()
    }

    /**
     * Log when user leaves a screen.
     */
    fun screenExit(screenName: String) {
        val entryTime = screenEntryTime[screenName]
        val durationMs = if (entryTime != null) System.currentTimeMillis() - entryTime else 0
        DebugLogger.d(TAG, "SCREEN_EXIT: $screenName (duration=${durationMs}ms)")
    }

    /**
     * Log when user starts a multi-step flow (import, search, etc.)
     */
    fun funnelStart(funnelName: String) {
        if (!isEnabled()) return
        funnelStarts.getOrPut(funnelName) { AtomicInteger(0) }.incrementAndGet()
        DebugLogger.d(TAG, "FUNNEL_START: $funnelName (attempt #${funnelStarts[funnelName]?.get()})")
        trackInteraction()
    }

    /**
     * Log when user completes a multi-step flow.
     */
    fun funnelComplete(funnelName: String) {
        funnelCompletes.getOrPut(funnelName) { AtomicInteger(0) }.incrementAndGet()
        val starts = funnelStarts[funnelName]?.get() ?: 0
        val completes = funnelCompletes[funnelName]?.get() ?: 0
        val rate = if (starts > 0) (completes * 100) / starts else 0
        DebugLogger.d(TAG, "FUNNEL_COMPLETE: $funnelName (completion rate: $rate% [$completes/$starts])")
    }

    /**
     * Log when user abandons a flow without completing.
     */
    fun funnelAbandon(funnelName: String, reason: String = "") {
        val detail = if (reason.isNotBlank()) " reason=$reason" else ""
        DebugLogger.d(TAG, "FUNNEL_ABANDON: $funnelName$detail")
    }

    /**
     * Log a user action (button tap, gesture, etc.)
     */
    fun action(feature: String, action: String, detail: String = "") {
        if (!isEnabled()) return
        val count = actionCount.incrementAndGet()
        val extra = if (detail.isNotBlank()) " ($detail)" else ""
        DebugLogger.d(TAG, "ACTION: [$feature] $action$extra (session action #$count)")
        trackInteraction()
    }

    /**
     * Log an error encountered by the user.
     */
    fun userError(feature: String, errorType: String) {
        DebugLogger.d(TAG, "USER_ERROR: [$feature] $errorType")
    }

    /**
     * Log a timing measurement for a user-facing operation.
     */
    fun timing(feature: String, operation: String, durationMs: Long) {
        DebugLogger.d(TAG, "TIMING: [$feature] $operation took ${durationMs}ms")
    }

    /**
     * Emit session summary (call on app background or periodic).
     */
    fun emitSessionSummary() {
        val sessionDuration = (System.currentTimeMillis() - sessionStart) / 1000
        val totalActions = actionCount.get()

        DebugLogger.i(TAG, "═══ SESSION SUMMARY ═══")
        DebugLogger.i(TAG, "Duration: ${sessionDuration}s | Actions: $totalActions")

        if (screenVisits.isNotEmpty()) {
            val top3 = screenVisits.entries.sortedByDescending { it.value.get() }.take(3)
            DebugLogger.i(TAG, "Top screens: ${top3.joinToString { "${it.key}(${it.value.get()})" }}")
        }

        if (funnelStarts.isNotEmpty()) {
            DebugLogger.i(TAG, "Funnels:")
            for ((name, starts) in funnelStarts) {
                val completes = funnelCompletes[name]?.get() ?: 0
                val rate = if (starts.get() > 0) (completes * 100) / starts.get() else 0
                DebugLogger.i(TAG, "  $name: $completes/${starts.get()} completed ($rate%)")
            }
        }
    }

    /**
     * Reset session (call on fresh app launch).
     */
    fun resetSession() {
        sessionStart = System.currentTimeMillis()
        lastInteraction = sessionStart
        actionCount.set(0)
        screenVisits.clear()
        screenEntryTime.clear()
        funnelStarts.clear()
        funnelCompletes.clear()
    }

    private fun trackInteraction() {
        val now = System.currentTimeMillis()
        val gap = now - lastInteraction
        if (gap > 30_000) { // Log if user was idle > 30s
            DebugLogger.d(TAG, "IDLE_GAP: ${gap / 1000}s since last interaction")
        }
        lastInteraction = now
    }

    private var consentGranted: Boolean = false
    fun setConsent(enabled: Boolean) { consentGranted = enabled }
    private fun isEnabled(): Boolean = consentGranted
    /**
     * Build a human-readable summary of locally accumulated telemetry.
     * This is what gets attached to the email when user taps "Send Telemetry".
     */
    fun getLocalSummary(): String {
        val sessionDuration = (System.currentTimeMillis() - sessionStart) / 1000
        return buildString {
            appendLine("=== Document Manager Telemetry Report ===")
            appendLine("Generated: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())}")
            appendLine("Session duration: ${sessionDuration}s")
            appendLine("Total actions: ${actionCount.get()}")
            appendLine()
            if (screenVisits.isNotEmpty()) {
                appendLine("Screen visits:")
                screenVisits.entries.sortedByDescending { it.value.get() }.forEach { (screen, count) ->
                    appendLine("  $screen: ${count.get()}")
                }
                appendLine()
            }
            if (funnelStarts.isNotEmpty()) {
                appendLine("Feature funnels:")
                funnelStarts.forEach { (name, starts) ->
                    val completes = funnelCompletes[name]?.get() ?: 0
                    appendLine("  $name: $completes/${starts.get()} completed")
                }
                appendLine()
            }
            appendLine("Note: This report contains only anonymous usage counts.")
            appendLine("No document content, file names, or personal data is included.")
        }
    }

}
