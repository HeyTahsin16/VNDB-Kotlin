package com.vndblite.dcj.ui.screens.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vndblite.dcj.R
import com.vndblite.dcj.domain.model.Language
import com.vndblite.dcj.domain.model.Platform
import com.vndblite.dcj.domain.model.VnDetails
import com.vndblite.dcj.domain.model.VnSummary
import com.vndblite.dcj.ui.components.AssetImage
import com.vndblite.dcj.ui.components.AssetPaths
import com.vndblite.dcj.ui.components.BbCodeText
import com.vndblite.dcj.ui.format.displayLabel
import com.vndblite.dcj.ui.format.displayText
import com.vndblite.dcj.ui.format.formatVnLength
import com.vndblite.dcj.ui.format.labelResId
import com.vndblite.dcj.ui.theme.VndbThemeTokens

@Composable
internal fun VnDetailGeneralTabContent(
    summary: VnSummary?,
    details: VnDetails?,
    maxTagSpoilerLevel: Int,
    showCoverCensor: Boolean,
    nsfwRevealedIds: Set<String>,
    onAction: (VnDetailAction) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        VnDetailSynopsisCard(description = summary?.description)

        VnDetailMetadataCard(summary = summary, details = details)

        val screenshots = details?.screenshots.orEmpty()
        if (screenshots.isNotEmpty()) {
            VnDetailScreenshotsCard(
                screenshots = screenshots,
                showCoverCensor = showCoverCensor,
                nsfwRevealedIds = nsfwRevealedIds,
                onToggleReveal = { onAction(VnDetailAction.ToggleNsfwReveal(it)) }
            )
        }

        val tags = details?.tags.orEmpty()
        if (tags.isNotEmpty()) {
            VnDetailTagsCard(
                tags = tags,
                maxSpoilerLevel = maxTagSpoilerLevel,
                onSelectSpoilerLevel = { onAction(VnDetailAction.SetTagSpoilerLevel(it)) },
                onTagClick = { onAction(VnDetailAction.FilterByTag(it)) }
            )
        }
    }
}

@Composable
private fun VnDetailSynopsisCard(description: String?) {
    val spec = VndbThemeTokens.spec
    Surface(
        color = spec.surfaceColor.copy(alpha = 0.85f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.detail_synopsis_heading),
                style = MaterialTheme.typography.titleLarge,
                color = spec.accentColor,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            BbCodeText(
                rawText = description?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.detail_no_synopsis),
                textColor = spec.onSurfaceColor,
                linkColor = spec.accentColor,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VnDetailMetadataCard(
    summary: VnSummary?,
    details: VnDetails?
) {
    val spec = VndbThemeTokens.spec
    Surface(
        color = spec.surfaceColor.copy(alpha = 0.85f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.detail_metadata_heading),
                style = MaterialTheme.typography.titleLarge,
                color = spec.accentColor,
                fontWeight = FontWeight.Bold
            )

            MetadataKeyValueRow(
                label = stringResource(R.string.detail_release_date_label),
                value = summary?.released.displayText()
            )
            MetadataKeyValueRow(
                label = stringResource(R.string.detail_length_label),
                value = formatVnLength(details?.lengthMinutes, summary?.lengthBucket)
            )
            details?.devStatus?.let { devStatus ->
                MetadataKeyValueRow(
                    label = stringResource(R.string.detail_dev_status_label),
                    value = devStatus.displayLabel()
                )
            }
            if (!details?.developers.isNullOrEmpty()) {
                MetadataKeyValueRow(
                    label = stringResource(R.string.detail_developers_label),
                    value = details?.developers.orEmpty().joinToString(", ") { it.name }
                )
            }
            if (!details?.aliases.isNullOrEmpty()) {
                MetadataKeyValueRow(
                    label = stringResource(R.string.detail_aliases_label),
                    value = details?.aliases.orEmpty().joinToString(" • ")
                )
            }
            if (!details?.minAges.isNullOrEmpty()) {
                MetadataKeyValueRow(
                    label = stringResource(R.string.detail_min_age_label),
                    value = details?.minAges.orEmpty().joinToString(", ") { "$it+" }
                )
            }

            val languages = details?.languages.orEmpty()
            if (languages.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.detail_languages_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = spec.accentColor,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    languages.forEach { code ->
                        val langEnum = Language.fromCode(code)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            AssetImage(
                                assetPath = AssetPaths.languageFlagByCode(code),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (langEnum != null) stringResource(langEnum.labelResId()) else code,
                                style = MaterialTheme.typography.bodySmall,
                                color = spec.onSurfaceColor
                            )
                        }
                    }
                }
            }

            val platforms = details?.platforms.orEmpty()
            if (platforms.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.detail_platforms_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = spec.accentColor,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    platforms.forEach { code ->
                        val platEnum = Platform.fromCode(code)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            AssetImage(
                                assetPath = AssetPaths.platformIconByCode(code),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (platEnum != null) stringResource(platEnum.labelResId()) else code,
                                style = MaterialTheme.typography.bodySmall,
                                color = spec.onSurfaceColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataKeyValueRow(
    label: String,
    value: String
) {
    val spec = VndbThemeTokens.spec
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = spec.accentColor,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(120.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = spec.onSurfaceColor,
            modifier = Modifier.weight(1f)
        )
    }
}
