package com.app.traveldocs.domain.model

sealed class AppError {
    data class StorageError(val message: String, val cause: Throwable?) : AppError()
    data class AuthError(val type: AuthErrorType) : AppError()
    data class ImportError(val message: String, val format: String?) : AppError()
    data class ExtractionError(val message: String) : AppError()
}

enum class AuthErrorType {
    INVALID_PIN,
    LOCKED_OUT,
    WIPED,
    SESSION_EXPIRED
}
