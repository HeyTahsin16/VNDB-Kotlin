package com.vndblite.dcj.ui.screens.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vndblite.dcj.R
import com.vndblite.dcj.core.AppLog
import com.vndblite.dcj.domain.model.Language
import com.vndblite.dcj.domain.model.Platform
import com.vndblite.dcj.domain.model.Review
import com.vndblite.dcj.domain.model.Tag
import com.vndblite.dcj.ui.components.AssetImage
import com.vndblite.dcj.ui.components.AssetPaths
import com.vndblite.dcj.ui.components.BbCodeText
import com.vndblite.dcj.ui.components.NsfwAsyncImage
import com.vndblite.dcj.ui.format.labelRes
import com.vndblite.dcj.ui.screens.characters.CharacterGridCard
import com.vndblite.dcj.ui.theme.ColorTokens
import com.vndblite.dcj.ui.theme.LocalThemeSpec
import com.vndblite.dcj.ui.theme.vndbFilterChipBorder
import com.vndblite.dcj.ui.theme.vndbFilterChipColors
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VnDetailOverviewTab(
    uiState: VnDetailUiState,
    onAction: (VnDetailAction) -> Unit,
    onSearchByTag: (Tag) -> Unit
) {
    val themeSpec = LocalThemeSpec.current
    val summary = uiState.summary
    val details = uiState.details

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            color = themeSpec.surfaceColor.copy(alpha = 0.82f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.detail_section_synopsis),
                    style = MaterialTheme.typography.titleMedium,
                    color = themeSpec.accentColor
                )
                val desc = summary?.description?.takeIf { it.isNotBlank() }
                if (desc != null) {
                    BbCodeText(rawText = desc)
                } else {
                    Text(
                        text = stringResource(R.string.detail_no_synopsis),
                        style = MaterialTheme.typography.bodyMedium,
                        color = themeSpec.contentColor.copy(alpha = 0.75f)
                    )
                }
            }
        }

        if (details != null) {
            MetadataSummaryCard(details = details)

            if (details.screenshots.isNotEmpty()) {
                ScreenshotsCard(
                    screenshots = details.screenshots,
                    showCoverCensor = uiState.settings.showCoverCensor,
                    revealedNsfwIds = uiState.revealedNsfwIds,
                    onOpenScreenshot = { url -> onAction(VnDetailAction.OpenFullscreenScreenshot(url)) }
                )
            }

            TagsSectionCard(
                tags = details.tags,
                spoilerLevel = uiState.spoilerLevel,
                onSelectSpoilerLevel = { onAction(VnDetailAction.SelectSpoilerLevel(it)) },
                onSearchByTag = onSearchByTag
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MetadataSummaryCard(
    details: com.vndblite.dcj.domain.model.VnDetails
) {
    val themeSpec = LocalThemeSpec.current
    Surface(
        color = themeSpec.surfaceColor.copy(alpha = 0.82f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.detail_section_metadata),
                style = MaterialTheme.typography.titleMedium,
                color = themeSpec.accentColor
            )
            if (details.developers.isNotEmpty()) {
                Text(
                    text = "${stringResource(R.string.detail_label_developers)}: ${details.developers.joinToString(", ") { it.name }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = themeSpec.contentColor
                )
            }
            if (details.aliases.isNotEmpty()) {
                Text(
                    text = "${stringResource(R.string.detail_label_aliases)}: ${details.aliases.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = themeSpec.contentColor.copy(alpha = 0.88f)
                )
            }
            if (details.minAges.isNotEmpty()) {
                val maxAge = details.minAges.maxOrNull() ?: 0
                val ageLabel = if (maxAge > 0) "${maxAge}+" else stringResource(R.string.collection_filter_min_age_all_ages)
                Text(
                    text = "${stringResource(R.string.detail_label_min_age)}: $ageLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = themeSpec.contentColor
                )
            }
            if (details.languages.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.detail_label_languages),
                    style = MaterialTheme.typography.labelLarge,
                    color = themeSpec.accentColor
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    details.languages.forEach { langCode ->
                        val lang = Language.fromCode(langCode)
                        val baseName = stringResource(lang.labelRes())
                        val isMtl = langCode in details.machineTranslatedLanguages
                        val label = if (isMtl) stringResource(R.string.lang_mtl_suffix, baseName) else baseName
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AssetImage(
                                assetPath = AssetPaths.languageFlagPath(lang),
                                contentDescription = null,
                                modifier = Modifier.width(16.dp).height(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall,
                                color = themeSpec.contentColor
                            )
                        }
                    }
                }
            }
            if (details.platforms.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.detail_label_platforms),
                    style = MaterialTheme.typography.labelLarge,
                    color = themeSpec.accentColor
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    details.platforms.forEach { platCode ->
                        val platform = Platform.fromCode(platCode)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AssetImage(
                                assetPath = AssetPaths.platformIconPath(platform),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(platform.labelRes()),
                                style = MaterialTheme.typography.bodySmall,
                                color = themeSpec.contentColor
                            )
                        }
                    }
                }
            }
            if (details.releasesTruncated) {
                Text(
                    text = stringResource(R.string.detail_releases_truncated_note),
                    style = MaterialTheme.typography.labelSmall,
                    fontStyle = FontStyle.Italic,
                    color = themeSpec.contentColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun ScreenshotsCard(
    screenshots: List<com.vndblite.dcj.domain.model.VnImage>,
    showCoverCensor: Boolean,
    revealedNsfwIds: Set<String>,
    onOpenScreenshot: (String) -> Unit
) {
    val themeSpec = LocalThemeSpec.current
    Surface(
        color = themeSpec.surfaceColor.copy(alpha = 0.82f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.detail_section_screenshots),
                style = MaterialTheme.typography.titleMedium,
                color = themeSpec.accentColor,
                modifier = Modifier.padding(horizontal = 14.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(screenshots, key = { it.url }) { shot ->
                    val shouldBlur = shot.isExplicit && showCoverCensor && (shot.url !in revealedNsfwIds)
                    NsfwAsyncImage(
                        imageUrl = shot.previewUrl,
                        contentDescription = stringResource(R.string.detail_section_screenshots),
                        shouldBlur = shouldBlur,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(width = 180.dp, height = 104.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .semantics { role = Role.Button }
                            .clickable { onOpenScreenshot(shot.url) }
                            .testTag("detail_screenshot_thumb")
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsSectionCard(
    tags: List<Tag>,
    spoilerLevel: Int,
    onSelectSpoilerLevel: (Int) -> Unit,
    onSearchByTag: (Tag) -> Unit
) {
    val themeSpec = LocalThemeSpec.current
    val visibleTags = tags.filter { it.spoilerLevel <= spoilerLevel }

    Surface(
        color = themeSpec.surfaceColor.copy(alpha = 0.82f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.detail_section_tags),
                style = MaterialTheme.typography.titleMedium,
                color = themeSpec.accentColor
            )
            SpoilerLevelSelector(
                selectedLevel = spoilerLevel,
                onSelectLevel = onSelectSpoilerLevel
            )
            if (visibleTags.isEmpty()) {
                Text(
                    text = stringResource(R.string.detail_no_tags),
                    style = MaterialTheme.typography.bodyMedium,
                    color = themeSpec.contentColor.copy(alpha = 0.75f)
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    visibleTags.forEach { tag ->
                        val ratingText = String.format(Locale.US, "%.1f", tag.rating)
                        FilterChip(
                            selected = false,
                            onClick = { onSearchByTag(tag) },
                            label = { Text("${tag.name} ($ratingText)") },
                            colors = vndbFilterChipColors(),
                            border = vndbFilterChipBorder(selected = false),
                            modifier = Modifier.testTag("detail_tag_${tag.id}")
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SpoilerLevelSelector(
    selectedLevel: Int,
    onSelectLevel: (Int) -> Unit
) {
    val levels = listOf(
        0 to R.string.spoiler_level_none,
        1 to R.string.spoiler_level_minor,
        2 to R.string.spoiler_level_major
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        levels.forEach { (level, labelRes) ->
            val selected = selectedLevel == level
            FilterChip(
                selected = selected,
                onClick = { onSelectLevel(level) },
                label = { Text(stringResource(labelRes)) },
                colors = vndbFilterChipColors(),
                border = vndbFilterChipBorder(selected = selected),
                modifier = Modifier.testTag("spoiler_level_$level")
            )
        }
    }
}

@Composable
fun VnDetailCastAndReviewsTab(
    uiState: VnDetailUiState,
    onAction: (VnDetailAction) -> Unit
) {
    val themeSpec = LocalThemeSpec.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            color = themeSpec.surfaceColor.copy(alpha = 0.82f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.detail_section_characters),
                    style = MaterialTheme.typography.titleMedium,
                    color = themeSpec.accentColor
                )
                if (uiState.characters.isEmpty()) {
                    Text(
                        text = stringResource(R.string.detail_no_characters),
                        style = MaterialTheme.typography.bodyMedium,
                        color = themeSpec.contentColor.copy(alpha = 0.75f)
                    )
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(uiState.characters, key = { it.id }) { character ->
                            Box(modifier = Modifier.width(136.dp)) {
                                CharacterGridCard(
                                    character = character,
                                    showCoverCensor = uiState.settings.showCoverCensor,
                                    isNsfwRevealed = character.id in uiState.revealedNsfwIds,
                                    onClick = { onAction(VnDetailAction.SelectCharacter(character)) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (uiState.quotes.isNotEmpty()) {
            Surface(
                color = themeSpec.surfaceColor.copy(alpha = 0.82f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.detail_section_quotes),
                        style = MaterialTheme.typography.titleMedium,
                        color = themeSpec.accentColor
                    )
                    uiState.quotes.forEach { quote ->
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.FormatQuote,
                                contentDescription = null,
                                tint = themeSpec.accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "“${quote.text}”",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontStyle = FontStyle.Italic,
                                    color = themeSpec.contentColor
                                )
                                quote.characterName?.takeIf { it.isNotBlank() }?.let { speaker ->
                                    Text(
                                        text = "— $speaker",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = themeSpec.accentColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Surface(
            color = themeSpec.surfaceColor.copy(alpha = 0.82f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.detail_section_reviews),
                    style = MaterialTheme.typography.titleMedium,
                    color = themeSpec.accentColor
                )
                if (uiState.reviews.isEmpty()) {
                    Text(
                        text = stringResource(R.string.detail_no_reviews),
                        style = MaterialTheme.typography.bodyMedium,
                        color = themeSpec.contentColor.copy(alpha = 0.75f)
                    )
                } else {
                    uiState.reviews.forEachIndexed { idx, review ->
                        if (idx > 0) {
                            HorizontalDivider(color = themeSpec.contentColor.copy(alpha = 0.15f))
                        }
                        ReviewItemBlock(
                            review = review,
                            expandedFullText = uiState.expandedReviewTexts[review.id],
                            isLoadingFullText = review.id in uiState.loadingFullReviewIds,
                            onExpandReview = { onAction(VnDetailAction.ExpandFullReview(review.id)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewItemBlock(
    review: Review,
    expandedFullText: String?,
    isLoadingFullText: Boolean,
    onExpandReview: () -> Unit
) {
    val themeSpec = LocalThemeSpec.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = review.username,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = themeSpec.accentColor
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                review.ratingText?.takeIf { it.isNotBlank() }?.let { r ->
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = ColorTokens.RatingStarGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = r,
                        style = MaterialTheme.typography.labelMedium,
                        color = themeSpec.contentColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                val dateLabel = review.date?.toString() ?: review.rawDate.orEmpty()
                if (dateLabel.isNotBlank()) {
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = themeSpec.contentColor.copy(alpha = 0.75f)
                    )
                }
            }
        }
        BbCodeText(rawText = expandedFullText ?: review.text)
        if (review.hasReadMore && expandedFullText == null) {
            TextButton(
                onClick = onExpandReview,
                enabled = !isLoadingFullText,
                modifier = Modifier.testTag("expand_review_${review.id}")
            ) {
                Text(
                    text = if (isLoadingFullText) {
                        stringResource(R.string.label_loading)
                    } else {
                        stringResource(R.string.detail_review_read_more)
                    },
                    color = themeSpec.accentColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun VnDetailRelationsAndLinksTab(
    uiState: VnDetailUiState,
    onOpenRelatedVn: (String) -> Unit
) {
    val themeSpec = LocalThemeSpec.current
    val context = LocalContext.current
    val relations = uiState.details?.relations.orEmpty()
    val links = uiState.details?.externalLinks.orEmpty()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            color = themeSpec.surfaceColor.copy(alpha = 0.82f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.detail_section_relations),
                    style = MaterialTheme.typography.titleMedium,
                    color = themeSpec.accentColor
                )
                if (relations.isEmpty()) {
                    Text(
                        text = stringResource(R.string.detail_no_relations),
                        style = MaterialTheme.typography.bodyMedium,
                        color = themeSpec.contentColor.copy(alpha = 0.75f)
                    )
                } else {
                    relations.forEach { rel ->
                        val title = rel.relatedVn?.title?.takeIf { it.isNotBlank() } ?: rel.id
                        Surface(
                            color = themeSpec.contentColor.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { role = Role.Button }
                                .clickable { onOpenRelatedVn(rel.id) }
                                .testTag("detail_relation_${rel.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = themeSpec.contentColor
                                    )
                                    val relLabel = stringResource(rel.relationType.labelRes())
                                    val sub = if (rel.isOfficial) {
                                        relLabel
                                    } else {
                                        "$relLabel (${stringResource(R.string.relation_unofficial)})"
                                    }
                                    Text(
                                        text = sub,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = themeSpec.accentColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Surface(
            color = themeSpec.surfaceColor.copy(alpha = 0.82f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.detail_section_links),
                    style = MaterialTheme.typography.titleMedium,
                    color = themeSpec.accentColor
                )
                if (links.isEmpty()) {
                    Text(
                        text = stringResource(R.string.detail_no_links),
                        style = MaterialTheme.typography.bodyMedium,
                        color = themeSpec.contentColor.copy(alpha = 0.75f)
                    )
                } else {
                    links.forEach { url ->
                        Surface(
                            color = themeSpec.contentColor.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { role = Role.Button }
                                .clickable {
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    } catch (e: Exception) {
                                        AppLog.w("VnDetailRelationsAndLinksTab", "Failed to open link $url", e)
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = url,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = themeSpec.accentColor,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = stringResource(R.string.action_open_in_browser),
                                    tint = themeSpec.accentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
