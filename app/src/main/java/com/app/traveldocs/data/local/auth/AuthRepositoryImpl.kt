package com.app.traveldocs.data.local.auth

import android.content.Context
import com.app.traveldocs.data.local.crypto.PinHasher
import com.app.traveldocs.data.local.dao.FamilyMemberDao
import com.app.traveldocs.data.local.entity.FamilyMemberEntity
import com.app.traveldocs.domain.model.LockoutState
import com.app.traveldocs.domain.repository.AuthRepository
import com.app.traveldocs.debug.DebugLogger
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [AuthRepository] that handles PIN authentication,
 * lockout logic, and data wipe functionality.
 *
 * Lockout policy:
 * - 3 consecutive failures → 5-minute lockout (300 seconds)
 * - 5 consecutive failures → data wipe triggered
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val familyMemberDao: FamilyMemberDao,
    private val pinHasher: PinHasher,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val timeProvider: TimeProvider
) : AuthRepository {

    companion object {
        const val LOCKOUT_THRESHOLD = 3
        const val WIPE_THRESHOLD = 5
        const val LOCKOUT_DURATION_MS = 300_000L // 5 minutes
        const val LOCKOUT_DURATION_SECONDS = 300
    }

    override suspend fun createPin(memberId: String, pin: String): Result<Unit> {
        DebugLogger.i("Auth", "createPin called for member: $memberId")
        return try {
            val salt = pinHasher.generateSalt()
            val hash = pinHasher.hashPin(pin, salt)
            val saltEncoded = Base64.getEncoder().encodeToString(salt)

            val existingMember = familyMemberDao.getById(memberId)
            if (existingMember != null) {
                // Update existing member's PIN
                familyMemberDao.update(
                    existingMember.copy(
                        pinHash = hash,
                        pinSalt = saltEncoded,
                        failedAttempts = 0,
                        lockedUntil = null
                    )
                )
            } else {
                // Create new member
                familyMemberDao.insert(
                    FamilyMemberEntity(
                        id = memberId,
                        name = memberId,
                        pinHash = hash,
                        pinSalt = saltEncoded,
                        createdAt = timeProvider.currentTimeMillis(),
                        failedAttempts = 0,
                        lockedUntil = null
                    )
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun verifyPin(memberId: String, pin: String): Result<Boolean> {
        DebugLogger.d("Auth", "verifyPin called for member: $memberId")
        return try {
            val member = familyMemberDao.getById(memberId)
                ?: return Result.failure(IllegalArgumentException("Member not found: $memberId"))

            // Check if currently locked out
            val lockedUntil = member.lockedUntil
            if (lockedUntil != null && timeProvider.currentTimeMillis() < lockedUntil) {
                return Result.success(false)
            }

            val salt = Base64.getDecoder().decode(member.pinSalt)
            val isValid = pinHasher.verifyPin(pin, salt, member.pinHash)

            if (isValid) {
                resetFailedAttempts(memberId)
            }

            Result.success(isValid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFailedAttempts(memberId: String): Int {
        val member = familyMemberDao.getById(memberId) ?: return 0
        return member.failedAttempts
    }

    override suspend fun recordFailedAttempt(memberId: String): LockoutState {
        DebugLogger.w("Auth", "recordFailedAttempt for member: $memberId")
        val member = familyMemberDao.getById(memberId)
            ?: return LockoutState(isLocked = false, remainingLockSeconds = 0, shouldWipe = false)

        val newFailedAttempts = member.failedAttempts + 1

        return when {
            newFailedAttempts >= WIPE_THRESHOLD -> {
                // 5+ failures: trigger data wipe
                familyMemberDao.update(
                    member.copy(
                        failedAttempts = newFailedAttempts,
                        lockedUntil = null
                    )
                )
                wipeMemberData(memberId)
                LockoutState(
                    isLocked = true,
                    remainingLockSeconds = 0,
                    shouldWipe = true
                )
            }
            newFailedAttempts >= LOCKOUT_THRESHOLD -> {
                // 3-4 failures: lock for 5 minutes
                val lockUntil = timeProvider.currentTimeMillis() + LOCKOUT_DURATION_MS
                familyMemberDao.update(
                    member.copy(
                        failedAttempts = newFailedAttempts,
                        lockedUntil = lockUntil
                    )
                )
                LockoutState(
                    isLocked = true,
                    remainingLockSeconds = LOCKOUT_DURATION_SECONDS,
                    shouldWipe = false
                )
            }
            else -> {
                // Under threshold: just record the attempt
                familyMemberDao.update(
                    member.copy(failedAttempts = newFailedAttempts)
                )
                LockoutState(
                    isLocked = false,
                    remainingLockSeconds = 0,
                    shouldWipe = false
                )
            }
        }
    }

    override suspend fun resetFailedAttempts(memberId: String) {
        val member = familyMemberDao.getById(memberId) ?: return
        familyMemberDao.update(
            member.copy(
                failedAttempts = 0,
                lockedUntil = null
            )
        )
    }

    override suspend fun wipeMemberData(memberId: String): Result<Unit> {
        DebugLogger.e("Auth", "!!! WIPE triggered for member: $memberId")
        return try {
            context.deleteDatabase("traveldocs.db")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
