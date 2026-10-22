package com.vndblite.dcj.ui.format

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import com.vndblite.dcj.core.AppLog

/**
 * Single centralized helper for launching external URLs (`ACTION_VIEW`) and determining
 * wide-screen layout adaptability (A6, A9, A10).
 */
object ExternalUriOpener {
    private const val TAG = "ExternalUriOpener"
    private const val VNDB_ORIGIN = "https://vndb.org"

    fun normalizeExternalUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        return when {
            trimmed.isEmpty() -> VNDB_ORIGIN
            trimmed.startsWith("/") -> "$VNDB_ORIGIN$trimmed"
            trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            else -> "https://$trimmed"
        }
    }

    fun openUrl(context: Context, rawUrl: String): Boolean {
        val normalized = normalizeExternalUrl(rawUrl)
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalized)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            AppLog.w(TAG, "Failed to open external URL: $normalized", e)
            false
        }
    }
}

fun Context.openExternalUrl(rawUrl: String): Boolean =
    ExternalUriOpener.openUrl(this, rawUrl)

/**
 * Single canonical helper determining whether the current window should use a wide-screen
 * layout (NavigationRail / multi-column grid) instead of repeating `isLandscape` checks (A6).
 */
@Composable
fun rememberIsWideLayout(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.orientation == Configuration.ORIENTATION_LANDSCAPE ||
        configuration.screenWidthDp >= WIDE_LAYOUT_MIN_WIDTH_DP
}

const val WIDE_LAYOUT_MIN_WIDTH_DP: Int = 600
