package com.vndblite.dcj.ui.screens.home

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vndblite.dcj.R
import com.vndblite.dcj.domain.model.CollectionEntry
import com.vndblite.dcj.domain.model.HomeSection
import com.vndblite.dcj.ui.components.AssetImage
import com.vndblite.dcj.ui.components.AssetPaths
import com.vndblite.dcj.ui.components.ErrorStateView
import com.vndblite.dcj.ui.components.LoadingStateView
import com.vndblite.dcj.ui.components.VnGridCard
import com.vndblite.dcj.ui.format.seeMoreTestTag
import com.vndblite.dcj.ui.format.titleRes
import com.vndblite.dcj.ui.theme.LocalThemeSpec

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onAction: (HomeAction) -> Unit,
    navigation: HomeNavigationCallbacks,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen_list"),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item(key = "home_header") {
            HomeGreetingHeader(
                isRefreshing = uiState.isRefreshingAll,
                onRefreshAll = { onAction(HomeAction.RefreshAll) },
                onOpenCharacterFinder = navigation.onOpenCharacterFinder,
                onOpenRandomVn = navigation.onOpenRandomVn
            )
        }

        items(HomeSection.entries, key = { it.name }) { section ->
            val sectionState = uiState.sections[section] ?: HomeSectionState(section)
            val previewLimit = uiState.settings.maxPreviewItem.coerceIn(5, 50)
            HomeCarouselRow(
                sectionState = sectionState,
                maxItems = previewLimit,
                showCoverCensor = uiState.settings.showCoverCensor,
                revealedNsfwIds = uiState.revealedNsfwIds,
                collectionByVnId = uiState.collectionByVnId,
                onToggleNsfw = { vnId -> onAction(HomeAction.ToggleNsfwReveal(vnId)) },
                onRetry = { onAction(HomeAction.RetrySection(section)) },
                onSeeMore = { navigation.onSeeMoreSection(section) },
                onOpenVnDetail = navigation.onOpenVnDetail
            )
        }

        item(key = "home_genres") {
            GenrePresetsSection(onSelectGenre = navigation.onOpenGenrePreset)
        }

        item(key = "home_traits") {
            PopularTraitsSection(onSelectTrait = navigation.onOpenTraitPreset)
        }

        item(key = "home_quotes") {
            MemorableQuotesSection(
                quotes = uiState.quotes,
                isLoading = uiState.isQuotesLoading,
                error = uiState.quotesError,
                showCoverCensor = uiState.settings.showCoverCensor,
                revealedNsfwIds = uiState.revealedNsfwIds,
                onRefreshQuotes = { onAction(HomeAction.RefreshQuotes) },
                onOpenVnDetail = navigation.onOpenVnDetail
            )
        }

        if (uiState.settings.showChart) {
            item(key = "home_collection_chart") {
                CollectionChartSection(statusCounts = uiState.statusCounts)
            }
        }

        item(key = "home_site_stats") {
            SiteStatsSection(
                stats = uiState.siteStats,
                isLoading = uiState.isStatsLoading,
                error = uiState.statsError,
                onRetry = { onAction(HomeAction.RetryStats) }
            )
        }
    }
}

@Composable
private fun HomeGreetingHeader(
    isRefreshing: Boolean,
    onRefreshAll: () -> Unit,
    onOpenCharacterFinder: () -> Unit,
    onOpenRandomVn: () -> Unit
) {
    val themeSpec = LocalThemeSpec.current
    Surface(
        color = themeSpec.surfaceColor.copy(alpha = 0.85f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AssetImage(
                    assetPath = AssetPaths.APP_ICON,
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = themeSpec.accentColor
                    )
                    Text(
                        text = stringResource(R.string.home_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = themeSpec.contentColor.copy(alpha = 0.85f)
                    )
                }
                IconButton(
                    onClick = onRefreshAll,
                    enabled = !isRefreshing,
                    modifier = Modifier.testTag("home_refresh_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.action_refresh),
                        tint = themeSpec.accentColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenCharacterFinder,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = themeSpec.accentColor,
                        contentColor = themeSpec.onAccentColor
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_trait_finder_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonSearch,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.home_trait_finder_button),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1
                    )
                }
                OutlinedButton(
                    onClick = onOpenRandomVn,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_random_vn_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Casino,
                        contentDescription = null,
                        tint = themeSpec.accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.home_random_vn_button),
                        style = MaterialTheme.typography.labelLarge,
                        color = themeSpec.contentColor,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeCarouselRow(
    sectionState: HomeSectionState,
    maxItems: Int,
    showCoverCensor: Boolean,
    revealedNsfwIds: Set<String>,
    collectionByVnId: Map<String, CollectionEntry>,
    onToggleNsfw: (String) -> Unit,
    onRetry: () -> Unit,
    onSeeMore: () -> Unit,
    onOpenVnDetail: (String) -> Unit
) {
    val themeSpec = LocalThemeSpec.current
    val section = sectionState.section
    val items = sectionState.items.take(maxItems)

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(section.titleRes()),
                style = MaterialTheme.typography.titleLarge,
                color = themeSpec.accentColor
            )
            TextButton(
                onClick = onSeeMore,
                modifier = Modifier.testTag(section.seeMoreTestTag())
            ) {
                Text(
                    text = stringResource(R.string.action_see_more),
                    color = themeSpec.accentColor,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = themeSpec.accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        when {
            sectionState.isLoading && items.isEmpty() -> LoadingStateView()
            sectionState.error != null && items.isEmpty() -> ErrorStateView(
                error = sectionState.error,
                onRetry = onRetry,
                retryTestTag = "retry_${section.name.lowercase()}"
            )
            items.isEmpty() && section == HomeSection.COLLECTION -> {
                Text(
                    text = stringResource(R.string.home_collection_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = themeSpec.contentColor.copy(alpha = 0.8f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
            else -> {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items, key = { "${section.name}_${it.id}" }) { vn ->
                        Box(modifier = Modifier.width(152.dp)) {
                            VnGridCard(
                                vn = vn,
                                sortField = section.sortField,
                                showCoverCensorGlobal = showCoverCensor,
                                isNsfwRevealed = vn.id in revealedNsfwIds,
                                onToggleNsfwReveal = { onToggleNsfw(vn.id) },
                                collectionEntry = collectionByVnId[vn.id],
                                onClick = { onOpenVnDetail(vn.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}
