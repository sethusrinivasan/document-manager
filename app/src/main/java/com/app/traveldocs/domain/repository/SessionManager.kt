package com.app.traveldocs.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface SessionManager {
    val isAuthenticated: StateFlow<Boolean>
    val currentMemberId: StateFlow<String?>
    fun startSession(memberId: String)
    fun endSession()
    fun resetInactivityTimer()
}
