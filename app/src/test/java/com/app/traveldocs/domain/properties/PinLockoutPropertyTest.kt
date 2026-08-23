package com.app.traveldocs.domain.properties

import com.app.traveldocs.data.local.EncryptedDatabaseProvider
import com.app.traveldocs.data.local.auth.AuthRepositoryImpl
import com.app.traveldocs.data.local.auth.TimeProvider
import com.app.traveldocs.data.local.crypto.PinHasher
import com.app.traveldocs.data.local.dao.FamilyMemberDao
import com.app.traveldocs.data.local.entity.FamilyMemberEntity
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/**
 * Property 3: PIN lockout after threshold failures
 *
 * For any sequence of PIN verification attempts for a member, if the number of
 * consecutive failures reaches 3, the system should enter a locked state. If
 * consecutive failures reach 5, the system should trigger a data wipe.
 *
 * **Validates: Requirements 2.2, 2.3**
 */
@Tag("property-test")
@DisplayName("Feature: travel-document-manager, Property 3: PIN lockout after threshold failures")
// Tag: Feature: travel-document-manager, Property 3: PIN lockout after threshold failures
class PinLockoutPropertyTest {

    private lateinit var familyMemberDao: FamilyMemberDao
    private lateinit var pinHasher: PinHasher
    private lateinit var encryptedDatabaseProvider: EncryptedDatabaseProvider
    private lateinit var timeProvider: TimeProvider
    private lateinit var authRepository: AuthRepositoryImpl

    @BeforeEach
    fun setup() {
        familyMemberDao = mockk(relaxed = true)
        pinHasher = mockk(relaxed = true)
        encryptedDatabaseProvider = mockk(relaxed = true)
        timeProvider = mockk()

        coEvery { timeProvider.currentTimeMillis() } returns 1_000_000L

        authRepository = AuthRepositoryImpl(
            familyMemberDao = familyMemberDao,
            pinHasher = pinHasher,
            encryptedDatabaseProvider = encryptedDatabaseProvider,
            timeProvider = timeProvider
        )
    }

    /**
     * For any number of consecutive failures N where N >= 3 but N < 5,
     * the system should enter a locked state (isLocked = true, shouldWipe = false).
     *
     * **Validates: Requirements 2.2**
     */
    @Test
    fun `for any sequence of 3 or 4 consecutive failures the system enters locked state`() = runTest {
        checkAll(100, Arb.int(3, 4), Arb.string(6, 12)) { failedAttemptsSoFar, memberId ->
            // Given: a member with (failedAttemptsSoFar - 1) existing failures
            // (because recordFailedAttempt increments by 1)
            val existingFailures = failedAttemptsSoFar - 1
            val memberEntity = createMemberEntity(memberId, failedAttempts = existingFailures)

            val updatedEntitySlot = slot<FamilyMemberEntity>()
            coEvery { familyMemberDao.getById(memberId) } returns memberEntity
            coEvery { familyMemberDao.update(capture(updatedEntitySlot)) } returns Unit

            // When: another failed attempt is recorded
            val lockoutState = authRepository.recordFailedAttempt(memberId)

            // Then: the system enters a locked state
            assertTrue(lockoutState.isLocked) {
                "Expected isLocked=true after $failedAttemptsSoFar consecutive failures"
            }
            assertFalse(lockoutState.shouldWipe) {
                "Expected shouldWipe=false for $failedAttemptsSoFar failures (< 5)"
            }
            assertTrue(lockoutState.remainingLockSeconds > 0) {
                "Expected positive remainingLockSeconds when locked"
            }
        }
    }

    /**
     * For any number of consecutive failures N where N >= 5,
     * the system should trigger a data wipe (shouldWipe = true).
     *
     * **Validates: Requirements 2.3**
     */
    @Test
    fun `for any sequence of 5 or more consecutive failures the system triggers data wipe`() = runTest {
        checkAll(100, Arb.int(5, 20), Arb.string(6, 12)) { failedAttemptsSoFar, memberId ->
            // Given: a member with (failedAttemptsSoFar - 1) existing failures
            val existingFailures = failedAttemptsSoFar - 1
            val memberEntity = createMemberEntity(memberId, failedAttempts = existingFailures)

            coEvery { familyMemberDao.getById(memberId) } returns memberEntity
            coEvery { familyMemberDao.update(any()) } returns Unit
            coEvery { encryptedDatabaseProvider.deleteDatabaseFile(memberId) } returns true

            // When: another failed attempt is recorded
            val lockoutState = authRepository.recordFailedAttempt(memberId)

            // Then: the system triggers a data wipe
            assertTrue(lockoutState.shouldWipe) {
                "Expected shouldWipe=true after $failedAttemptsSoFar consecutive failures"
            }
            assertTrue(lockoutState.isLocked) {
                "Expected isLocked=true when data wipe is triggered"
            }
        }
    }

    /**
     * For any number of consecutive failures N where N < 3,
     * the system should NOT enter a locked state.
     *
     * **Validates: Requirements 2.2**
     */
    @Test
    fun `fewer than 3 consecutive failures never trigger lockout`() = runTest {
        checkAll(100, Arb.int(1, 2), Arb.string(6, 12)) { failedAttemptsSoFar, memberId ->
            // Given: a member with (failedAttemptsSoFar - 1) existing failures
            val existingFailures = failedAttemptsSoFar - 1
            val memberEntity = createMemberEntity(memberId, failedAttempts = existingFailures)

            coEvery { familyMemberDao.getById(memberId) } returns memberEntity
            coEvery { familyMemberDao.update(any()) } returns Unit

            // When: another failed attempt is recorded
            val lockoutState = authRepository.recordFailedAttempt(memberId)

            // Then: the system does NOT lock
            assertFalse(lockoutState.isLocked) {
                "Expected isLocked=false with only $failedAttemptsSoFar consecutive failures"
            }
            assertFalse(lockoutState.shouldWipe) {
                "Expected shouldWipe=false with only $failedAttemptsSoFar consecutive failures"
            }
            assertTrue(lockoutState.remainingLockSeconds == 0) {
                "Expected remainingLockSeconds=0 when not locked"
            }
        }
    }

    /**
     * For any sequence of failures followed by a successful PIN verification,
     * the failure counter resets to 0.
     *
     * **Validates: Requirements 2.2, 2.3**
     */
    @Test
    fun `successful PIN verification resets the failure counter`() = runTest {
        checkAll(100, Arb.int(1, 4), Arb.string(6, 12)) { failedAttemptsBefore, memberId ->
            // Given: a member with some failed attempts (but below wipe threshold)
            val memberEntity = createMemberEntity(
                memberId,
                failedAttempts = failedAttemptsBefore,
                pinSalt = "dGVzdHNhbHQ=" // base64 "testsalt"
            )

            val updatedEntitySlot = slot<FamilyMemberEntity>()
            coEvery { familyMemberDao.getById(memberId) } returns memberEntity
            coEvery { familyMemberDao.update(capture(updatedEntitySlot)) } returns Unit
            coEvery { pinHasher.verifyPin(any(), any(), any()) } returns true

            // When: a correct PIN is verified
            val result = authRepository.verifyPin(memberId, "1234")

            // Then: the result is successful
            assertTrue(result.isSuccess)
            assertTrue(result.getOrNull() == true)

            // And: the failure counter is reset to 0
            assertTrue(updatedEntitySlot.captured.failedAttempts == 0) {
                "Expected failedAttempts=0 after successful verification, " +
                    "but got ${updatedEntitySlot.captured.failedAttempts}"
            }
            assertTrue(updatedEntitySlot.captured.lockedUntil == null) {
                "Expected lockedUntil=null after successful verification"
            }
        }
    }

    /**
     * For any sequence of N consecutive failures reaching the lockout threshold,
     * if a successful verification occurs, subsequent failures start counting from 0 again.
     *
     * **Validates: Requirements 2.2, 2.3**
     */
    @Test
    fun `after reset, failures accumulate from zero again`() = runTest {
        checkAll(50, Arb.string(6, 12)) { memberId ->
            // Given: a member that was reset (0 failures)
            val memberEntity = createMemberEntity(memberId, failedAttempts = 0)

            coEvery { familyMemberDao.getById(memberId) } returns memberEntity
            coEvery { familyMemberDao.update(any()) } returns Unit

            // When: recording a single failed attempt from zero
            val lockoutState = authRepository.recordFailedAttempt(memberId)

            // Then: no lockout (only 1 failure, below threshold of 3)
            assertFalse(lockoutState.isLocked) {
                "Expected isLocked=false with 1 failure after reset"
            }
            assertFalse(lockoutState.shouldWipe) {
                "Expected shouldWipe=false with 1 failure after reset"
            }
        }
    }

    private fun createMemberEntity(
        memberId: String,
        failedAttempts: Int = 0,
        pinHash: String = "hashedPin123",
        pinSalt: String = "dGVzdHNhbHQ="
    ): FamilyMemberEntity {
        return FamilyMemberEntity(
            id = memberId,
            name = "Test Member",
            pinHash = pinHash,
            pinSalt = pinSalt,
            createdAt = 1_000_000L,
            failedAttempts = failedAttempts,
            lockedUntil = null
        )
    }
}
