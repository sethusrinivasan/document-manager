package com.app.paperstow.domain.repository

import com.app.paperstow.domain.model.LockoutState

interface AuthRepository {
    suspend fun createPin(memberId: String, pin: String): Result<Unit>
    suspend fun verifyPin(memberId: String, pin: String): Result<Boolean>
    suspend fun getFailedAttempts(memberId: String): Int
    suspend fun recordFailedAttempt(memberId: String): LockoutState
    suspend fun resetFailedAttempts(memberId: String)
    suspend fun wipeMemberData(memberId: String): Result<Unit>
}
