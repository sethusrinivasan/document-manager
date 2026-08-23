package com.app.traveldocs.data.local.auth

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Abstraction over system time for testability.
 */
interface TimeProvider {
    fun currentTimeMillis(): Long
}

@Singleton
class SystemTimeProvider @Inject constructor() : TimeProvider {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}
