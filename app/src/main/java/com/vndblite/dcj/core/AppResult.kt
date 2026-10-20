package com.vndblite.dcj.core

import kotlinx.coroutines.CancellationException

/**
 * Typed outcome wrapper for network, database, and sync operations.
 * Guarantees that [CancellationException] is always rethrown (Rule 5, N4).
 */
sealed interface AppResult<out T> {
    data class Success<out T>(val data: T) : AppResult<T>
    data class Failure(val error: ApiError) : AppResult<Nothing>

    val isSuccess: Boolean
        get() = this is Success

    val isFailure: Boolean
        get() = this is Failure

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Failure -> null
    }

    fun errorOrNull(): ApiError? = when (this) {
        is Success -> null
        is Failure -> error
    }
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Failure -> this
}

inline fun <T, R> AppResult<T>.flatMap(transform: (T) -> AppResult<R>): AppResult<R> = when (this) {
    is AppResult.Success -> transform(data)
    is AppResult.Failure -> this
}

inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) action(data)
    return this
}

inline fun <T> AppResult<T>.onFailure(action: (ApiError) -> Unit): AppResult<T> {
    if (this is AppResult.Failure) action(error)
    return this
}

inline fun <T> runSuspendCatching(
    tag: String,
    operation: String,
    errorMapper: (Throwable) -> ApiError,
    block: () -> T
): AppResult<T> {
    return try {
        AppResult.Success(block())
    } catch (cancel: CancellationException) {
        throw cancel
    } catch (t: Throwable) {
        val mapped = errorMapper(t)
        AppLog.w(tag, "$operation failed with $mapped (${t::class.simpleName})", t)
        AppResult.Failure(mapped)
    }
}
