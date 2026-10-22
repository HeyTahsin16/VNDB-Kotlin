package com.vndblite.dcj.ui.screens.collection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vndblite.dcj.R
import com.vndblite.dcj.domain.model.CollectionFilters
import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.domain.model.DevStatus
import com.vndblite.dcj.domain.model.Language
import com.vndblite.dcj.domain.model.Platform
import com.vndblite.dcj.ui.components.AssetImage
import com.vndblite.dcj.ui.components.StatusChip
import com.vndblite.dcj.ui.format.flagAssetUri
import com.vndblite.dcj.ui.format.iconAssetUri
import com.vndblite.dcj.ui.format.labelResId
import com.vndblite.dcj.ui.theme.VndbThemeTokens
import com.vndblite.dcj.ui.theme.vndbFilterChipBorder
import com.vndblite.dcj.ui.theme.vndbFilterChipColors
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CollectionMetadataFiltersSection(
    draftFilters: CollectionFilters,
    onUpdateFilters: (CollectionFilters) -> Unit
) {
    val spec = VndbThemeTokens.spec
    Text(
        text = stringResource(R.string.collection_filter_min_age_heading),
        style = MaterialTheme.typography.titleMedium,
        color = spec.accentColor,
        fontWeight = FontWeight.Bold
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf<Int?>(null, 12, 16, 18).forEach { age ->
            val selected = draftFilters.minAge == age
            FilterChip(
                selected = selected,
                onClick = { onUpdateFilters(draftFilters.copy(minAge = age)) },
                label = {
                    Text(
                        text = if (age == null) {
                            stringResource(R.string.collection_filter_age_all)
                        } else {
                            stringResource(R.string.collection_filter_age_plus, age)
                        },
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = vndbFilterChipColors(spec),
                border = vndbFilterChipBorder(selected, spec)
            )
        }
    }

    Text(
        text = stringResource(R.string.search_sheet_dev_status_heading),
        style = MaterialTheme.typography.titleMedium,
        color = spec.accentColor,
        fontWeight = FontWeight.Bold
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DevStatus.entries.forEach { devStatus ->
            val selected = devStatus in draftFilters.devStatuses
            FilterChip(
                selected = selected,
                onClick = {
                    val next = if (selected) draftFilters.devStatuses - devStatus else draftFilters.devStatuses + devStatus
                    onUpdateFilters(draftFilters.copy(devStatuses = next))
                },
                label = {
                    Text(
                        text = stringResource(devStatus.labelResId()),
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = vndbFilterChipColors(spec),
                border = vndbFilterChipBorder(selected, spec)
            )
        }
    }

    Text(
        text = stringResource(R.string.search_sheet_origin_lang_heading),
        style = MaterialTheme.typography.titleMedium,
        color = spec.accentColor,
        fontWeight = FontWeight.Bold
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Language.originLanguages.forEach { lang ->
            val selected = lang in draftFilters.originalLanguages
            FilterChip(
                selected = selected,
                onClick = {
                    val next = if (selected) {
                        draftFilters.originalLanguages - lang
                    } else {
                        draftFilters.originalLanguages + lang
                    }
                    onUpdateFilters(draftFilters.copy(originalLanguages = next))
                },
                leadingIcon = {
                    AssetImage(
                        assetPath = lang.flagAssetUri(),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = {
                    Text(
                        text = stringResource(lang.labelResId()),
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = vndbFilterChipColors(spec),
                border = vndbFilterChipBorder(selected, spec)
            )
        }
    }

    Text(
        text = stringResource(R.string.search_sheet_platforms_heading),
        style = MaterialTheme.typography.titleMedium,
        color = spec.accentColor,
        fontWeight = FontWeight.Bold
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Platform.entries.forEach { platform ->
            val selected = platform in draftFilters.platforms
            FilterChip(
                selected = selected,
                onClick = {
                    val next = if (selected) draftFilters.platforms - platform else draftFilters.platforms + platform
                    onUpdateFilters(draftFilters.copy(platforms = next))
                },
                leadingIcon = {
                    AssetImage(
                        assetPath = platform.iconAssetUri(),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = {
                    Text(
                        text = stringResource(platform.labelResId()),
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = vndbFilterChipColors(spec),
                border = vndbFilterChipBorder(selected, spec)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BatchEditCollectionDialog(
    selectedCount: Int,
    defaultStatus: CollectionStatus,
    onDismiss: () -> Unit,
    onConfirm: (status: CollectionStatus, overrideVote: Int?) -> Unit
) {
    val spec = VndbThemeTokens.spec
    val semantic = VndbThemeTokens.colors
    var status by remember(defaultStatus) { mutableStateOf(defaultStatus) }
    var keepExistingVote by remember { mutableStateOf(true) }
    var customVote by remember { mutableIntStateOf(80) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = spec.surfaceColor,
        titleContentColor = spec.onSurfaceColor,
        textContentColor = spec.onSurfaceColor,
        title = {
            Text(
                text = stringResource(R.string.collection_batch_dialog_title, selectedCount),
                style = MaterialTheme.typography.titleLarge,
                color = spec.accentColor,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.collection_batch_status_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = spec.accentColor,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CollectionStatus.entries.forEach { st ->
                        StatusChip(
                            status = st,
                            selected = status == st,
                            onClick = { status = st }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = keepExistingVote,
                        onCheckedChange = { keepExistingVote = it }
                    )
                    Text(
                        text = stringResource(R.string.collection_batch_keep_votes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = spec.onSurfaceColor
                    )
                }

                if (!keepExistingVote) {
                    Text(
                        text = stringResource(
                            R.string.collection_batch_override_vote,
                            customVote / 10,
                            customVote
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = spec.accentColor
                    )
                    Slider(
                        value = (customVote / 10).toFloat(),
                        onValueChange = { customVote = it.roundToInt() * 10 },
                        valueRange = 1f..10f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = spec.accentColor,
                            activeTrackColor = spec.accentColor
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(status, if (keepExistingVote) null else customVote) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = spec.accentColor,
                    contentColor = semantic.onAccentButton
                )
            ) {
                Text(stringResource(R.string.action_apply), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel), color = spec.onSurfaceColor)
            }
        }
    )
}
