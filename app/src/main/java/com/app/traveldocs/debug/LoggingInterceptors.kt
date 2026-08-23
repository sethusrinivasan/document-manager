package com.app.traveldocs.debug

/**
 * Extension functions and utilities for instrumenting key operations with debug logging.
 * These are designed to be added to existing implementations without modifying interfaces.
 */

/**
 * Logs the start and result of a suspending operation, including timing.
 */
suspend inline fun <T> loggedOperation(
    component: String,
    operation: String,
    crossinline block: suspend () -> T
): T {
    val startTime = System.currentTimeMillis()
    DebugLogger.d(component, "→ $operation")
    return try {
        val result = block()
        val elapsed = System.currentTimeMillis() - startTime
        DebugLogger.i(component, "✓ $operation completed in ${elapsed}ms")
        result
    } catch (e: Exception) {
        val elapsed = System.currentTimeMillis() - startTime
        DebugLogger.e(component, "✗ $operation failed after ${elapsed}ms", e)
        throw e
    }
}

/**
 * Logs the start and result of a synchronous operation, including timing.
 */
inline fun <T> loggedSync(
    component: String,
    operation: String,
    block: () -> T
): T {
    val startTime = System.currentTimeMillis()
    DebugLogger.d(component, "→ $operation")
    return try {
        val result = block()
        val elapsed = System.currentTimeMillis() - startTime
        DebugLogger.i(component, "✓ $operation completed in ${elapsed}ms")
        result
    } catch (e: Exception) {
        val elapsed = System.currentTimeMillis() - startTime
        DebugLogger.e(component, "✗ $operation failed after ${elapsed}ms", e)
        throw e
    }
}

/**
 * Wraps a Result<T> with logging.
 */
fun <T> Result<T>.logged(component: String, operation: String): Result<T> {
    this.onSuccess {
        DebugLogger.i(component, "✓ $operation → success")
    }
    this.onFailure { e ->
        DebugLogger.e(component, "✗ $operation → failure", e)
    }
    return this
}
