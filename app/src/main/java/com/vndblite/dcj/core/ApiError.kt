package com.vndblite.dcj.core

/**
 * Typed domain/network error hierarchy that never exposes raw server response bodies
 * or authentication tokens to the UI (N4, N7, Rule 6).
 */
sealed interface ApiError {
    data object Offline : ApiError
    data object Timeout : ApiError
    data class RateLimited(val retryAfterSeconds: Long? = null) : ApiError
    data object Unauthorized : ApiError
    data class MissingPermission(val missingPermissions: Set<String>) : ApiError
    data class BadRequest(val statusCode: Int = 400) : ApiError
    data class NotFound(val statusCode: Int = 404) : ApiError
    data class Server(val statusCode: Int) : ApiError
    data object Parse : ApiError
    data class Unknown(val summary: String? = null) : ApiError
}
