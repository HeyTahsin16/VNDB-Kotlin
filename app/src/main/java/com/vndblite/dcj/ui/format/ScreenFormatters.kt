package com.vndblite.dcj.ui.format

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.vndblite.dcj.R
import com.vndblite.dcj.core.AppLog
import com.vndblite.dcj.core.UiText
import com.vndblite.dcj.core.toUiText
import com.vndblite.dcj.domain.model.Character
import com.vndblite.dcj.domain.model.GenrePreset
import com.vndblite.dcj.domain.model.SyncReport
import com.vndblite.dcj.domain.model.Trait
import com.vndblite.dcj.sync.SyncProgress
import com.vndblite.dcj.sync.SyncStage

private const val FORMATTER_TAG = "ScreenFormatters"

/**
 * Single helper determining whether the screen is in a wide layout (landscape or >= 600dp width) (A6).
 */
@Composable
fun isWideLayout(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.orientation == Configuration.ORIENTATION_LANDSCAPE ||
        configuration.screenWidthDp >= 600
}

/**
 * Centralized external URL launcher replacing duplicated `Intent(ACTION_VIEW, Uri.parse(...))` blocks (A9, A10).
 */
fun openExternalUrl(context: Context, url: String) {
    val trimmed = url.trim()
    if (trimmed.isEmpty()) return
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(trimmed)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        AppLog.w(FORMATTER_TAG, "Failed to open external URL: $trimmed", e)
    }
}

val Trait.displayLabel: String
    get() = if (!groupName.isNullOrBlank()) "$groupName: $name" else name

fun Trait.displayLabel(): String =
    if (!groupName.isNullOrBlank()) "$groupName: $name" else name

@androidx.annotation.StringRes
fun GenrePreset.labelResId(): Int = when (this) {
    GenrePreset.ACTION -> R.string.genre_action
    GenrePreset.COMEDY -> R.string.genre_comedy
    GenrePreset.DRAMA -> R.string.genre_drama
    GenrePreset.FANTASY -> R.string.genre_fantasy
    GenrePreset.HORROR -> R.string.genre_horror
    GenrePreset.MYSTERY -> R.string.genre_mystery
    GenrePreset.ROMANCE -> R.string.genre_romance
    GenrePreset.SCIFI -> R.string.genre_scifi
}

fun GenrePreset.genrePresetImageUri(): String =
    com.vndblite.dcj.ui.components.AssetPaths.genrePresetImage(name)

@Composable
fun GenrePreset.displayLabel(): String = stringResource(labelResId())

@Composable
fun Character.formatStatsLine(): String {
    val parts = mutableListOf<String>()
    sex?.let { parts += it.displayLabel() }
    ageYears?.takeIf { it > 0 }?.let { parts += stringResource(R.string.char_stats_age_format, it) }
    heightCm?.takeIf { it > 0 }?.let { parts += stringResource(R.string.char_stats_height_format, it) }
    weightKg?.takeIf { it > 0 }?.let { parts += stringResource(R.string.char_stats_weight_format, it) }
    measurements?.let { m ->
        val b = m.bustCm ?: 0
        val w = m.waistCm ?: 0
        val h = m.hipsCm ?: 0
        if (b > 0 && w > 0 && h > 0) {
            parts += stringResource(R.string.char_stats_bust_waist_hips, b, w, h)
        }
    }
    bloodType?.takeIf { it.isNotBlank() }?.let {
        parts += stringResource(R.string.char_stats_blood_format, it.uppercase())
    }
    birthday?.let { bday ->
        if (bday.month in 1..12 && bday.day in 1..31) {
            parts += stringResource(R.string.char_stats_bday_format, bday.month, bday.day)
        }
    }
    return parts.joinToString(" • ")
}

fun SyncReport.toSummaryUiText(): UiText {
    val removedTotal = deletedLocal + deletedRemote
    return when {
        isCompleteSuccess -> UiText.StringResource(
            R.string.sync_report_complete_success,
            pushed,
            pulled,
            removedTotal
        )
        hasPartialFailures -> UiText.StringResource(
            R.string.sync_report_partial_failure,
            pushed,
            pulled,
            failed.size
        )
        else -> UiText.StringResource(
            R.string.sync_report_failed,
            failed.size
        )
    }
}

fun SyncProgress.toStatusUiText(): UiText? = when (stage) {
    SyncStage.IDLE -> null
    SyncStage.AUTHENTICATING -> UiText.StringResource(R.string.sync_stage_authenticating)
    SyncStage.FETCHING_REMOTE_LIST -> UiText.StringResource(R.string.sync_stage_fetching_remote)
    SyncStage.PLANNING -> UiText.StringResource(R.string.sync_stage_planning)
    SyncStage.DELETING_REMOTE -> UiText.StringResource(
        R.string.sync_stage_deleting_remote,
        completedOperations + 1,
        totalOperations.coerceAtLeast(1)
    )
    SyncStage.PUSHING_LOCAL_CHANGES -> UiText.StringResource(
        R.string.sync_stage_pushing_local,
        completedOperations + 1,
        totalOperations.coerceAtLeast(1)
    )
    SyncStage.APPLYING_LOCAL_UPDATES -> UiText.StringResource(R.string.sync_stage_applying_local)
    SyncStage.CACHING_SUMMARIES -> UiText.StringResource(R.string.sync_stage_caching_summaries)
    SyncStage.COMPLETED -> lastReport?.toSummaryUiText()
    SyncStage.FAILED -> lastError?.toUiText() ?: lastReport?.toSummaryUiText()
}
