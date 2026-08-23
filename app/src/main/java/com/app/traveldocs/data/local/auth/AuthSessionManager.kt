package com.app.traveldocs.data.local.auth

import com.app.traveldocs.debug.DebugLogger
import com.app.traveldocs.domain.repository.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthSessionManager @Inject constructor() : SessionManager {

    companion object {
        const val INACTIVITY_TIMEOUT_MS = 1_800_000L // 30 minutes
    }

    internal var scope: CoroutineScope = CoroutineScope(SupervisorJob())

    private val _isAuthenticated = MutableStateFlow(false)
    override val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _currentMemberId = MutableStateFlow<String?>(null)
    override val currentMemberId: StateFlow<String?> = _currentMemberId.asStateFlow()

    private var inactivityJob: Job? = null

    override fun startSession(memberId: String) {
        DebugLogger.i("Session", "startSession: memberId=$memberId")
        _currentMemberId.value = memberId
        _isAuthenticated.value = true
        resetInactivityTimer()
    }

    override fun endSession() {
        DebugLogger.i("Session", "endSession: memberId=${_currentMemberId.value}")
        inactivityJob?.cancel()
        inactivityJob = null
        _isAuthenticated.value = false
        _currentMemberId.value = null
    }

    override fun resetInactivityTimer() {
        DebugLogger.d("Session", "resetInactivityTimer (30min countdown)")
        inactivityJob?.cancel()
        inactivityJob = scope.launch {
            delay(INACTIVITY_TIMEOUT_MS)
            DebugLogger.w("Session", "Inactivity timeout reached — ending session")
            endSession()
        }
    }
}
