package com.app.paperstow.domain.model

data class LockoutState(
    val isLocked: Boolean,
    val remainingLockSeconds: Int,
    val shouldWipe: Boolean
)
