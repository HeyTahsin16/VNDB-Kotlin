package com.vndblite.dcj.ui.format

import com.vndblite.dcj.R
import com.vndblite.dcj.core.ApiError

/**
 * Maps typed [ApiError] instances to user-facing localized [UiText] without
 * exposing raw server response bodies or authentication tokens (N4, N7, D2).
 */
fun ApiError.toUiText(): UiText = when (this) {
    ApiError.Offline -> UiText.StringResource(R.string.error_offline)
    ApiError.Timeout -> UiText.StringResource(R.string.error_timeout)
    is ApiError.RateLimited -> {
        val seconds = retryAfterSeconds
        if (seconds != null && seconds > 0L) {
            UiText.StringResource(R.string.error_rate_limited_seconds, seconds)
        } else {
            UiText.StringResource(R.string.error_rate_limited)
        }
    }
    ApiError.Unauthorized -> UiText.StringResource(R.string.error_unauthorized)
    is ApiError.MissingPermission -> UiText.StringResource(
        R.string.error_missing_permission,
        missingPermissions.sorted().joinToString(", ")
    )
    is ApiError.BadRequest -> UiText.StringResource(R.string.error_bad_request, statusCode)
    is ApiError.NotFound -> UiText.StringResource(R.string.error_not_found, statusCode)
    is ApiError.Server -> UiText.StringResource(R.string.error_server, statusCode)
    ApiError.Parse -> UiText.StringResource(R.string.error_parse)
    ApiError.SyncInProgress -> UiText.StringResource(R.string.error_sync_in_progress)
    is ApiError.Unknown -> UiText.StringResource(R.string.error_unknown)
}
