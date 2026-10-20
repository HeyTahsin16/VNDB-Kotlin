package com.vndblite.dcj.data.remote.http

import com.vndblite.dcj.core.ApiError
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.SerializationException
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response

/**
 * Exception carrier for a pre-mapped [ApiError] inside `runSuspendCatching` blocks.
 */
class ApiErrorException(val apiError: ApiError) : Exception()

/**
 * Suspends an OkHttp [Call] via [suspendCancellableCoroutine] so cancelling the calling coroutine
 * immediately invokes [Call.cancel] on the underlying HTTP request (N4).
 */
suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation {
        cancel()
    }
    enqueue(
        object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (!continuation.isActive) {
                    response.close()
                    return
                }
                continuation.resume(response)
            }

            override fun onFailure(call: Call, e: IOException) {
                if (!continuation.isActive) return
                continuation.resumeWithException(e)
            }
        }
    )
}

/**
 * Maps an HTTP status code and optional `Retry-After` header to a typed [ApiError]
 * without leaking raw server response bodies to user-facing messages (N4, Rule 6).
 */
fun mapHttpStatusToApiError(statusCode: Int, retryAfterHeader: String? = null): ApiError {
    return when (statusCode) {
        400 -> ApiError.BadRequest(statusCode)
        401, 403 -> ApiError.Unauthorized
        404 -> ApiError.NotFound(statusCode)
        429 -> ApiError.RateLimited(parseRetryAfterSeconds(retryAfterHeader))
        in 500..599 -> ApiError.Server(statusCode)
        else -> ApiError.Unknown("HTTP $statusCode")
    }
}

/**
 * Maps a [Throwable] from network or JSON parsing operations to a typed [ApiError] (N4).
 */
fun mapThrowableToApiError(throwable: Throwable): ApiError {
    return when (throwable) {
        is ApiErrorException -> throwable.apiError
        is SocketTimeoutException,
        is InterruptedIOException -> ApiError.Timeout
        is UnknownHostException,
        is ConnectException -> ApiError.Offline
        is SerializationException -> ApiError.Parse
        is IOException -> ApiError.Offline
        else -> ApiError.Unknown(throwable::class.simpleName)
    }
}

fun parseRetryAfterSeconds(headerValue: String?): Long? {
    val trimmed = headerValue?.trim().orEmpty()
    if (trimmed.isEmpty()) return null
    return trimmed.toLongOrNull()?.coerceAtLeast(1L)
}
