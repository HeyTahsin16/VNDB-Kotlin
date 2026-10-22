package com.vndblite.dcj.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.vndblite.dcj.R
import com.vndblite.dcj.domain.model.VnSummary
import com.vndblite.dcj.ui.components.AssetImage
import com.vndblite.dcj.ui.components.AssetPaths
import com.vndblite.dcj.ui.components.CollectionEntryDialog
import com.vndblite.dcj.ui.components.ErrorStateView
import com.vndblite.dcj.ui.components.LoadingStateView
import com.vndblite.dcj.ui.components.NsfwAsyncImage
import com.vndblite.dcj.ui.format.VnFormatters
import com.vndblite.dcj.ui.format.labelRes
import com.vndblite.dcj.ui.screens.characters.CharacterBioDialog
import com.vndblite.dcj.ui.theme.ColorTokens
import com.vndblite.dcj.ui.theme.LocalThemeSpec
import com.vndblite.dcj.ui.theme.badgeColor

@Composable
fun VnDetailScreen(
    uiState: VnDetailUiState,
    onAction: (VnDetailAction) -> Unit,
    navigation: VnDetailNavigationCallbacks,
    modifier: Modifier = Modifier
) {
    val themeSpec = LocalThemeSpec.current

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = navigation.onNavigateBack,
                modifier = Modifier.testTag("vn_detail_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    tint = themeSpec.accentColor
                )
            }
            Text(
                text = uiState.summary?.title ?: uiState.vnId,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = themeSpec.contentColor,
                maxLines = 1,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
            )
            IconButton(
                onClick = { onAction(VnDetailAction.Refresh) },
                modifier = Modifier.testTag("vn_detail_refresh_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.action_refresh),
                    tint = themeSpec.accentColor
                )
            }
        }

        when {
            uiState.isLoadingDetails && uiState.summary == null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingStateView()
                }
            }
            uiState.detailsError != null && uiState.summary == null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ErrorStateView(
                        error = uiState.detailsError,
                        onRetry = { onAction(VnDetailAction.Refresh) }
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("vn_detail_scroll"),
                    contentPadding = PaddingValues(bottom = 28.dp)
                ) {
                    item(key = "detail_hero_header") {
                        VnDetailHeroHeader(
                            uiState = uiState,
                            onToggleCoverNsfw = { onAction(VnDetailAction.ToggleNsfwReveal(uiState.vnId)) },
                            onOpenCollectionDialog = { onAction(VnDetailAction.OpenCollectionDialog) }
                        )
                    }

                    item(key = "detail_tabs_bar") {
                        TabRow(
                            selectedTabIndex = uiState.selectedTab.ordinal,
                            containerColor = themeSpec.surfaceColor.copy(alpha = 0.88f),
                            contentColor = themeSpec.accentColor
                        ) {
                            VnDetailTab.entries.forEach { tab ->
                                val labelRes = when (tab) {
                                    VnDetailTab.OVERVIEW -> R.string.detail_tab_overview
                                    VnDetailTab.CAST_AND_REVIEWS -> R.string.detail_tab_cast_reviews
                                    VnDetailTab.RELATIONS_AND_LINKS -> R.string.detail_tab_relations_links
                                }
                                Tab(
                                    selected = uiState.selectedTab == tab,
                                    onClick = { onAction(VnDetailAction.SelectTab(tab)) },
                                    text = { Text(stringResource(labelRes)) },
                                    modifier = Modifier.testTag("vn_detail_tab_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }

                    item(key = "detail_tab_body") {
                        when (uiState.selectedTab) {
                            VnDetailTab.OVERVIEW -> VnDetailOverviewTab(
                                uiState = uiState,
                                onAction = onAction,
                                onSearchByTag = navigation.onSearchByTag
                            )
                            VnDetailTab.CAST_AND_REVIEWS -> VnDetailCastAndReviewsTab(
                                uiState = uiState,
                                onAction = onAction
                            )
                            VnDetailTab.RELATIONS_AND_LINKS -> VnDetailRelationsAndLinksTab(
                                uiState = uiState,
                                onOpenRelatedVn = navigation.onOpenRelatedVn
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.isCollectionDialogOpen) {
        val summary = uiState.summary ?: VnSummary(id = uiState.vnId, title = uiState.vnId)
        CollectionEntryDialog(
            vn = summary,
            existingEntry = uiState.collectionEntry,
            onDismiss = { onAction(VnDetailAction.DismissCollectionDialog) },
            onSave = { draft -> onAction(VnDetailAction.SaveCollectionEntry(draft)) },
            onRemove = if (uiState.collectionEntry != null) {
                { onAction(VnDetailAction.RemoveCollectionEntry) }
            } else {
                null
            }
        )
    }

    uiState.selectedCharacter?.let { character ->
        CharacterBioDialog(
            character = character,
            showCoverCensor = uiState.settings.showCoverCensor,
            isNsfwRevealed = character.id in uiState.revealedNsfwIds,
            onToggleNsfwReveal = { onAction(VnDetailAction.ToggleNsfwReveal(character.id)) },
            onSelectTrait = navigation.onSearchByTrait,
            onOpenVnDetail = navigation.onOpenRelatedVn,
            onDismiss = { onAction(VnDetailAction.DismissCharacterBio) }
        )
    }

    uiState.fullscreenScreenshotUrl?.let { fullUrl ->
        FullscreenScreenshotDialog(
            imageUrl = fullUrl,
            onDismiss = { onAction(VnDetailAction.CloseFullscreenScreenshot) }
        )
    }
}

@Composable
private fun VnDetailHeroHeader(
    uiState: VnDetailUiState,
    onToggleCoverNsfw: () -> Unit,
    onOpenCollectionDialog: () -> Unit
) {
    val themeSpec = LocalThemeSpec.current
    val summary = uiState.summary
    val details = uiState.details
    val entry = uiState.collectionEntry
    val isExplicit = summary?.image?.isExplicit == true
    val isRevealed = uiState.vnId in uiState.revealedNsfwIds
    val shouldBlur = isExplicit && uiState.settings.showCoverCensor && !isRevealed

    Surface(
        color = themeSpec.surfaceColor.copy(alpha = 0.85f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 118.dp, height = 168.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    val coverUrl = summary?.image?.url.orEmpty()
                    if (coverUrl.isNotBlank()) {
                        NsfwAsyncImage(
                            imageUrl = coverUrl,
                            contentDescription = summary?.title,
                            shouldBlur = shouldBlur,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(themeSpec.contentColor.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BrokenImage,
                                contentDescription = stringResource(R.string.label_no_cover),
                                tint = themeSpec.contentColor.copy(alpha = 0.5f)
                            )
                        }
                    }

                    if (isExplicit && uiState.settings.showCoverCensor) {
                        Surface(
                            color = ColorTokens.ScrimDarkLight,
                            shape = RoundedCornerShape(6.dp),
                            onClick = onToggleCoverNsfw,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(4.dp)
                                .testTag("vn_detail_nsfw_toggle")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = if (shouldBlur) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = stringResource(R.string.nsfw_toggle_cd),
                                    tint = ColorTokens.NsfwWarningTint,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (shouldBlur) {
                                        stringResource(R.string.nsfw_badge_reveal)
                                    } else {
                                        stringResource(R.string.nsfw_badge_hide)
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ColorTokens.CardOverlayText
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = summary?.title ?: uiState.vnId,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = themeSpec.accentColor
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AssetImage(
                            assetPath = AssetPaths.languageFlagPath(summary?.originalLanguage),
                            contentDescription = summary?.originalLanguage,
                            modifier = Modifier.width(18.dp).height(12.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = ColorTokens.RatingStarGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(
                                R.string.rating_with_votes_format,
                                VnFormatters.formatRating(summary?.rating),
                                summary?.voteCount ?: 0
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = themeSpec.contentColor
                        )
                    }

                    Text(
                        text = "${stringResource(R.string.detail_label_released)}: ${VnFormatters.formatReleaseDate(summary?.released).asString()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = themeSpec.contentColor
                    )

                    Text(
                        text = "${stringResource(R.string.detail_label_length)}: ${VnFormatters.formatPlayLength(details?.lengthMinutes, summary?.lengthBucket).asString()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = themeSpec.contentColor
                    )

                    details?.devStatus?.let { devStatus ->
                        Surface(
                            color = devStatus.badgeColor().copy(alpha = 0.24f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = stringResource(devStatus.labelRes()),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = themeSpec.contentColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onOpenCollectionDialog,
                colors = ButtonDefaults.buttonColors(
                    containerColor = entry?.status?.badgeColor() ?: themeSpec.accentColor,
                    contentColor = ColorTokens.BadgeDarkText
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("vn_detail_collection_fab")
            ) {
                Icon(
                    imageVector = if (entry != null) Icons.Default.BookmarkAdded else Icons.Default.BookmarkAdd,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                val buttonText = if (entry != null) {
                    val votePart = VnFormatters.formatVoteTenPoint(entry.vote)?.let { " • ★ $it/10" }.orEmpty()
                    "${stringResource(entry.status.labelRes())}$votePart"
                } else {
                    stringResource(R.string.add_to_collection)
                }
                Text(
                    text = buttonText,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun FullscreenScreenshotDialog(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ColorTokens.ScrimDarkHeavy)
                .testTag("fullscreen_screenshot_dialog"),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = stringResource(R.string.detail_section_screenshots),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .testTag("close_fullscreen_screenshot_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.action_close),
                    tint = ColorTokens.CardOverlayText
                )
            }
        }
    }
}
