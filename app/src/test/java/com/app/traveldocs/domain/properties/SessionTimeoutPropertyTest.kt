package com.app.traveldocs.domain.properties

import com.app.traveldocs.data.local.auth.AuthSessionManager
import com.app.traveldocs.data.local.auth.TestableAuthSessionManager
import io.kotest.property.Arb
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/**
 * Property 4: Session timeout on inactivity
 *
 * For any authenticated session, if the elapsed time since the last activity exceeds 30 minutes,
 * isAuthenticated should return false.
 *
 * Validates: Requirements 2.4
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Tag("Feature: travel-document-manager, Property 4: Session timeout on inactivity")
@DisplayName("Property 4: Session timeout on inactivity")
class SessionTimeoutPropertyTest {

    companion object {
        private const val TIMEOUT_MS = AuthSessionManager.INACTIVITY_TIMEOUT_MS // 1_800_000L (30 min)
    }

    /**
     * Property: For any duration > 30 minutes since last activity, isAuthenticated is false.
     *
     * **Validates: Requirements 2.4**
     */
    @Test
    @DisplayName("elapsed time > 30 minutes since last activity results in isAuthenticated = false")
    fun sessionExpiredAfterInactivityExceedsTimeout() = runTest {
        val arbMemberId = Arb.string(minSize = 1, maxSize = 20)
        val arbDurationBeyondTimeout = Arb.long(range = (TIMEOUT_MS + 1)..((TIMEOUT_MS * 3)))

        checkAll(100, arbMemberId, arbDurationBeyondTimeout) { memberId, elapsedMs ->
            val testScope = TestScope()
            val sessionManager = TestableAuthSessionManager(testScope)

            sessionManager.startSession(memberId)
            assertTrue(sessionManager.isAuthenticated.value)
            assertEquals(memberId, sessionManager.currentMemberId.value)

            testScope.advanceTimeBy(elapsedMs)
            testScope.testScheduler.runCurrent()

            assertFalse(sessionManager.isAuthenticated.value,
                "Session should expire after ${elapsedMs}ms of inactivity (timeout is ${TIMEOUT_MS}ms)")
            assertNull(sessionManager.currentMemberId.value,
                "currentMemberId should be null after session timeout")
        }
    }

    /**
     * Property: For any duration <= 30 minutes since last activity, isAuthenticated remains true.
     *
     * **Validates: Requirements 2.4**
     */
    @Test
    @DisplayName("elapsed time <= 30 minutes since last activity keeps isAuthenticated = true")
    fun sessionRemainsActiveWithinTimeout() = runTest {
        val arbMemberId = Arb.string(minSize = 1, maxSize = 20)
        // Stay strictly below the timeout - 1ms buffer to avoid race conditions at boundary
        val arbDurationWithinTimeout = Arb.long(range = 0L..(TIMEOUT_MS - 1))

        checkAll(100, arbMemberId, arbDurationWithinTimeout) { memberId, elapsedMs ->
            val testScope = TestScope()
            val sessionManager = TestableAuthSessionManager(testScope)

            sessionManager.startSession(memberId)

            testScope.advanceTimeBy(elapsedMs)
            testScope.testScheduler.runCurrent()

            assertTrue(sessionManager.isAuthenticated.value,
                "Session should remain active after ${elapsedMs}ms (timeout is ${TIMEOUT_MS}ms)")
            assertEquals(memberId, sessionManager.currentMemberId.value,
                "currentMemberId should remain '$memberId' within timeout period")
        }
    }

    /**
     * Property: resetInactivityTimer() resets the clock — after reset + less than 30min, still authenticated.
     *
     * **Validates: Requirements 2.4**
     */
    @Test
    @DisplayName("resetInactivityTimer resets the timeout clock so session survives beyond original timeout")
    fun resetInactivityTimerExtendsSession() = runTest {
        val arbMemberId = Arb.string(minSize = 1, maxSize = 20)
        // Time before reset: some portion of the timeout window
        val arbTimeBeforeReset = Arb.long(range = 1L..(TIMEOUT_MS - 1))
        // Time after reset: stay within the new timeout window
        val arbTimeAfterReset = Arb.long(range = 0L..(TIMEOUT_MS - 1))

        checkAll(100, arbMemberId, arbTimeBeforeReset, arbTimeAfterReset) { memberId, beforeReset, afterReset ->
            val testScope = TestScope()
            val sessionManager = TestableAuthSessionManager(testScope)

            sessionManager.startSession(memberId)

            // Advance time by some amount before resetting
            testScope.advanceTimeBy(beforeReset)
            testScope.testScheduler.runCurrent()

            // Reset the timer — this should restart the 30-minute window
            sessionManager.resetInactivityTimer()

            // Advance by less than 30 minutes from the reset point
            testScope.advanceTimeBy(afterReset)
            testScope.testScheduler.runCurrent()

            assertTrue(sessionManager.isAuthenticated.value,
                "Session should remain active: ${beforeReset}ms before reset + ${afterReset}ms after reset (total from reset < ${TIMEOUT_MS}ms)")
            assertEquals(memberId, sessionManager.currentMemberId.value,
                "currentMemberId should remain '$memberId' after timer reset within timeout")
        }
    }
}
