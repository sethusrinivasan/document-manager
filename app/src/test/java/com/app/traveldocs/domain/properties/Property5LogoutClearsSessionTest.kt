package com.app.traveldocs.domain.properties

import com.app.traveldocs.data.local.auth.TestableAuthSessionManager
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/**
 * Property 5: Logout clears session
 *
 * For any active session, after calling endSession(), isAuthenticated should be false
 * and currentMemberId should be null.
 *
 * Validates: Requirements 2.5
 */
@Tag("Feature: travel-document-manager, Property 5: Logout clears session")
@OptIn(ExperimentalCoroutinesApi::class)
class Property5LogoutClearsSessionTest {

    private val memberIdArb = Arb.string(minSize = 1, maxSize = 20)

    /**
     * **Validates: Requirements 2.5**
     *
     * For any memberId and any session state, after calling endSession(),
     * isAuthenticated is always false.
     */
    @Test
    fun `endSession always sets isAuthenticated to false`() = runTest {
        val sessionManager = TestableAuthSessionManager(testScope = this)

        checkAll(100, memberIdArb) { memberId ->
            sessionManager.startSession(memberId)
            sessionManager.endSession()

            assertFalse(sessionManager.isAuthenticated.value) {
                "isAuthenticated must be false after endSession() for memberId='$memberId'"
            }
        }
    }

    /**
     * **Validates: Requirements 2.5**
     *
     * For any memberId, after calling endSession(), currentMemberId is always null.
     */
    @Test
    fun `endSession always sets currentMemberId to null`() = runTest {
        val sessionManager = TestableAuthSessionManager(testScope = this)

        checkAll(100, memberIdArb) { memberId ->
            sessionManager.startSession(memberId)
            sessionManager.endSession()

            assertNull(sessionManager.currentMemberId.value) {
                "currentMemberId must be null after endSession() for memberId='$memberId'"
            }
        }
    }

    /**
     * **Validates: Requirements 2.5**
     *
     * Multiple start/end session cycles always leave state cleared after end.
     * For any sequence of memberIds, repeatedly starting and ending sessions
     * always results in cleared state after the final endSession().
     */
    @Test
    fun `multiple start-end cycles always leave state cleared`() = runTest {
        val sessionManager = TestableAuthSessionManager(testScope = this)

        checkAll(100, memberIdArb, memberIdArb, memberIdArb) { id1, id2, id3 ->
            // Cycle through multiple sessions
            sessionManager.startSession(id1)
            sessionManager.endSession()

            sessionManager.startSession(id2)
            sessionManager.endSession()

            sessionManager.startSession(id3)
            sessionManager.endSession()

            assertFalse(sessionManager.isAuthenticated.value) {
                "isAuthenticated must be false after multiple start/end cycles"
            }
            assertNull(sessionManager.currentMemberId.value) {
                "currentMemberId must be null after multiple start/end cycles"
            }
        }
    }
}
