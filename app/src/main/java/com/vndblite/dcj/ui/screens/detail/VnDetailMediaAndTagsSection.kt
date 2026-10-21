package com.vndblite.dcj.ui.screens.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vndblite.dcj.R
import com.vndblite.dcj.domain.model.Tag
import com.vndblite.dcj.domain.model.VnImage
import com.vndblite.dcj.ui.components.NsfwAsyncImage
import com.vndblite.dcj.ui.theme.VndbThemeTokens
import com.vndblite.dcj.ui.theme.vndbFilterChipBorder
import com.vndblite.dcj.ui.theme.vndbFilterChipColors

/**
 * Screenshot carousel using `screenshot.previewUrl` for the strip and `screenshot.url`
 * for the full-size viewer dialog (N6).
 */
@Composable
internal fun VnDetailScreenshotsCard(
    screenshots: List<VnImage>,
    showCoverCensor: Boolean,
    nsfwRevealedIds: Set<String>,
    onToggleReveal: (String) -> Unit
) {
    val spec = VndbThemeTokens.spec
    val semantic = VndbThemeTokens.colors
    var activeViewerIndex by rememberSaveable { mutableIntStateOf(-1) }

    Surface(
        color = spec.surfaceColor.copy(alpha = 0.85f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.detail_screenshots_heading, screenshots.size),
                style = MaterialTheme.typography.titleLarge,
                color = spec.accentColor,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
                contentPadding = PaddingValues(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(screenshots, key = { idx, s -> "${s.url}_$idx" }) { index, shot ->
                    val revealKey = "shot_${shot.url.ifBlank { index.toString() }}"
                    val isExplicit = shot.isExplicit
                    val shouldBlur = isExplicit && showCoverCensor && (revealKey !in nsfwRevealedIds)

                    Box(
                        modifier = Modifier
                            .width(180.dp)
                            .height(104.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(role = Role.Button) { activeViewerIndex = index }
                            .testTag("screenshot_thumb_$index")
                    ) {
                        NsfwAsyncImage(
                            imageUrl = shot.previewUrl,
                            contentDescription = null,
                            shouldBlur = shouldBlur,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (isExplicit && showCoverCensor) {
                            Surface(
                                color = semantic.nsfwBadgeContainer,
                                shape = RoundedCornerShape(6.dp),
                                onClick = { onToggleReveal(revealKey) },
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (shouldBlur) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = stringResource(
                                        if (shouldBlur) R.string.card_nsfw_toggle_cd_hidden
                                        else R.string.card_nsfw_toggle_cd_revealed
                                    ),
                                    tint = semantic.nsfwBadgeTint,
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val viewingShot = screenshots.getOrNull(activeViewerIndex)
    if (viewingShot != null) {
        Dialog(
            onDismissRequest = { activeViewerIndex = -1 },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .testTag("screenshot_viewer_dialog"),
                contentAlignment = Alignment.Center
            ) {
                NsfwAsyncImage(
                    imageUrl = viewingShot.url.ifBlank { viewingShot.previewUrl },
                    contentDescription = null,
                    shouldBlur = false,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 10f)
                        .clip(RoundedCornerShape(14.dp))
                )
                IconButton(
                    onClick = { activeViewerIndex = -1 },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.detail_screenshot_viewer_close_cd),
                        tint = semantic.cardOverlayText
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun VnDetailTagsCard(
    tags: List<Tag>,
    maxSpoilerLevel: Int,
    onSelectSpoilerLevel: (Int) -> Unit,
    onTagClick: (Tag) -> Unit
) {
    val spec = VndbThemeTokens.spec
    val filteredTags = tags.filter { it.spoilerLevel <= maxSpoilerLevel }

    Surface(
        color = spec.surfaceColor.copy(alpha = 0.85f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.detail_tags_heading, filteredTags.size),
                    style = MaterialTheme.typography.titleLarge,
                    color = spec.accentColor,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        0 to R.string.detail_spoilers_none,
                        1 to R.string.detail_spoilers_minor,
                        2 to R.string.detail_spoilers_all
                    ).forEach { (level, resId) ->
                        val selected = maxSpoilerLevel == level
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectSpoilerLevel(level) },
                            label = {
                                Text(
                                    text = stringResource(resId),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = vndbFilterChipColors(spec),
                            border = vndbFilterChipBorder(selected, spec)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                filteredTags.forEach { tag ->
                    AssistChip(
                        onClick = { onTagClick(tag) },
                        label = { Text(tag.name) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = spec.onSurfaceColor.copy(alpha = 0.08f),
                            labelColor = spec.onSurfaceColor
                        ),
                        border = BorderStroke(1.dp, spec.accentColor.copy(alpha = 0.45f))
                    )
                }
            }
        }
    }
}
