package com.vndblite.dcj.core

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.vndblite.dcj.R

/**
 * Lightweight wrapper for user-facing text produced outside `@Composable` functions
 * (such as ViewModels, repositories, and error mappers) so non-UI code never hardcodes
 * English strings or holds a `Context` reference (Rule 4, A11, F5).
 */
@Immutable
sealed interface UiText {

    data class DynamicString(val value: String) : UiText

    class StringResource(
        @StringRes val resId: Int,
        vararg val args: Any
    ) : UiText {
        val formatArgs: List<Any> = args.toList()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is StringResource) return false
            return resId == other.resId && formatArgs == other.formatArgs
        }

        override fun hashCode(): Int = 31 * resId + formatArgs.hashCode()

        override fun toString(): String = "StringResource(resId=$resId, args=$formatArgs)"
    }

    class PluralResource(
        @PluralsRes val resId: Int,
        val count: Int,
        vararg val args: Any
    ) : UiText {
        val formatArgs: List<Any> = args.toList()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is PluralResource) return false
            return resId == other.resId && count == other.count && formatArgs == other.formatArgs
        }

        override fun hashCode(): Int {
            var result = resId
            result = 31 * result + count
            result = 31 * result + formatArgs.hashCode()
            return result
        }

        override fun toString(): String = "PluralResource(resId=$resId, count=$count, args=$formatArgs)"
    }

    fun asString(context: Context): String = when (this) {
        is DynamicString -> value
        is StringResource -> {
            val resolvedArgs = formatArgs.map { arg ->
                if (arg is UiText) arg.asString(context) else arg
            }.toTypedArray()
            if (resolvedArgs.isEmpty()) {
                context.getString(resId)
            } else {
                context.getString(resId, *resolvedArgs)
            }
        }
        is PluralResource -> {
            val resolvedArgs = formatArgs.map { arg ->
                if (arg is UiText) arg.asString(context) else arg
            }.toTypedArray()
            if (resolvedArgs.isEmpty()) {
                context.resources.getQuantityString(resId, count)
            } else {
                context.resources.getQuantityString(resId, count, *resolvedArgs)
            }
        }
    }

    @Composable
    fun asString(): String = when (this) {
        is DynamicString -> value
        is StringResource -> {
            val resolvedArgs = formatArgs.map { arg ->
                if (arg is UiText) arg.asString() else arg
            }.toTypedArray()
            if (resolvedArgs.isEmpty()) {
                stringResource(resId)
            } else {
                stringResource(resId, *resolvedArgs)
            }
        }
        is PluralResource -> {
            val resolvedArgs = formatArgs.map { arg ->
                if (arg is UiText) arg.asString() else arg
            }.toTypedArray()
            if (resolvedArgs.isEmpty()) {
                pluralStringResource(resId, count)
            } else {
                pluralStringResource(resId, count, *resolvedArgs)
            }
        }
    }
}

/**
 * Maps a typed [ApiError] to a user-friendly localized [UiText] without ever exposing
 * raw HTTP response bodies or secrets (Rule 6, F5, N4, N7).
 */
fun ApiError.toUiText(): UiText = when (this) {
    ApiError.Offline -> UiText.StringResource(R.string.connection_error)
    ApiError.Timeout -> UiText.StringResource(R.string.error_timeout)
    is ApiError.RateLimited -> {
        val seconds = retryAfterSeconds?.takeIf { it > 0L }
        if (seconds != null) {
            UiText.StringResource(R.string.error_rate_limited_seconds, seconds)
        } else {
            UiText.StringResource(R.string.error_rate_limited)
        }
    }
    ApiError.Unauthorized -> UiText.StringResource(R.string.error_unauthorized)
    is ApiError.MissingPermission -> {
        val missingJoined = missingPermissions.sorted().joinToString(", ")
        UiText.StringResource(R.string.error_missing_permissions, missingJoined)
    }
    is ApiError.BadRequest -> UiText.StringResource(R.string.error_bad_request, statusCode)
    is ApiError.NotFound -> UiText.StringResource(R.string.error_not_found)
    is ApiError.Server -> UiText.StringResource(R.string.error_server, statusCode)
    ApiError.Parse -> UiText.StringResource(R.string.error_parse)
    is ApiError.Unknown -> UiText.StringResource(R.string.error_unknown)
}
