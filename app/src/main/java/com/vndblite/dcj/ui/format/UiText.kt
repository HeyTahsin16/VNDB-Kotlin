package com.vndblite.dcj.ui.format

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * Presentation wrapper for user-visible strings so ViewModels and formatters
 * can reference localized string resources without holding a [Context] (D2).
 */
sealed interface UiText {
    data class DynamicString(val value: String) : UiText

    class StringResource(
        @param:StringRes val resId: Int,
        vararg val args: Any
    ) : UiText {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is StringResource) return false
            return resId == other.resId && args.contentEquals(other.args)
        }

        override fun hashCode(): Int = 31 * resId + args.contentHashCode()
    }

    @Composable
    fun asString(): String = when (this) {
        is DynamicString -> value
        is StringResource -> stringResource(resId, *args)
    }

    fun asString(context: Context): String = when (this) {
        is DynamicString -> value
        is StringResource -> context.getString(resId, *args)
    }
}
