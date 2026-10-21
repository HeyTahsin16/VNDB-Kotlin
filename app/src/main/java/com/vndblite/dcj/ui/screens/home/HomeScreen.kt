package com.vndblite.dcj.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vndblite.dcj.LocalAppContainer
import com.vndblite.dcj.R
import com.vndblite.dcj.domain.model.HomeSection
import com.vndblite.dcj.domain.model.SortField
import com.vndblite.dcj.domain.model.ThemeId
import com.vndblite.dcj.domain.model.Trait
import com.vndblite.dcj.domain.model.VnSummary
import com.vndblite.dcj.ui.components.PreviewData
import com.vndblite.dcj.ui.components.SectionUiState
import com.vndblite.dcj.ui.format.titleResId
import com.vndblite.dcj.ui.theme.VndbTheme

@Composable
fun HomeScreen(
    onOpenVnDetail: (VnSummary) -> Unit,
    onSeeMoreSection: (HomeSection) -> Unit,
    onGoToCollectionTab: () -> Unit,
    onOpenTraitFinder: (Trait?) -> Unit,
    onOpenRandomVn: () -> Unit,
    viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.factory(LocalAppContainer.current)
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(
        state = state,
        onAction = { action ->
            when (action) {
                is HomeAction.OpenVnDetail -> onOpenVnDetail(action.vn)
                is HomeAction.SeeMoreSection -> onSeeMoreSection(action.section)
                HomeAction.GoToCollectionTab -> onGoToCollectionTab()
                is HomeAction.OpenTraitFinder -> onOpenTraitFinder(action.presetTrait)
                HomeAction.OpenRandomVn -> onOpenRandomVn()
                else -> viewModel.onAction(action)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    state: HomeUiState,
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val remoteSectionsOrder = listOf(
        HomeSection.LATEST_RELEASES,
        HomeSection.UPCOMING_RELEASES,
        HomeSection.TOP_RATED,
        HomeSection.TOP_VOTED,
        HomeSection.POPULAR_ONGOING
    )

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { onAction(HomeAction.RefreshAll) },
        modifier = modifier
            .fillMaxSize()
            .testTag("home_pull_to_refresh")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item(key = "home_header") {
                HomeTopHeader()
            }

            item(key = "home_discovery_banner") {
                HomeDiscoveryBanner(
                    onOpenTraitFinder = { onAction(HomeAction.OpenTraitFinder(it)) },
                    onOpenRandomVn = { onAction(HomeAction.OpenRandomVn) }
                )
            }

            item(key = "home_quotes") {
                HomeQuotesBanner(
                    quotesState = state.quotesState,
                    displayedQuote = state.displayedQuote,
                    onShuffle = { onAction(HomeAction.ShuffleQuote) },
                    onRetry = { onAction(HomeAction.RetryQuotes) },
                    onOpenVn = { onAction(HomeAction.OpenVnDetail(it)) }
                )
            }

            if (state.collectionCarousel.isNotEmpty()) {
                item(key = "home_collection_carousel") {
                    HomeVnCarouselSection(
                        title = stringResource(R.string.home_section_my_collection),
                        seeMoreTag = "see_more_${HomeSection.COLLECTION.id}",
                        loadState = SectionUiState.Content(state.collectionCarousel),
                        displayConfig = HomeCarouselDisplayConfig(
                            subtitleSortField = SortField.RATING,
                            collectionEntries = state.collectionEntries,
                            showCoverCensor = state.settings.showCoverCensor,
                            nsfwRevealedIds = state.nsfwRevealedIds
                        ),
                        onSeeMore = { onAction(HomeAction.GoToCollectionTab) },
                        onRetry = {},
                        onVnClick = { onAction(HomeAction.OpenVnDetail(it)) },
                        onToggleNsfwReveal = { onAction(HomeAction.ToggleNsfwReveal(it)) }
                    )
                }
            }

            items(remoteSectionsOrder, key = { "section_${it.id}" }) { section ->
                val loadState = state.remoteSections[section] ?: SectionUiState.Loading
                HomeVnCarouselSection(
                    title = stringResource(section.titleResId()),
                    seeMoreTag = "see_more_${section.id}",
                    loadState = loadState,
                    displayConfig = HomeCarouselDisplayConfig(
                        subtitleSortField = section.sortField,
                        collectionEntries = state.collectionEntries,
                        showCoverCensor = state.settings.showCoverCensor,
                        nsfwRevealedIds = state.nsfwRevealedIds
                    ),
                    onSeeMore = { onAction(HomeAction.SeeMoreSection(section)) },
                    onRetry = { onAction(HomeAction.RetrySection(section)) },
                    onVnClick = { onAction(HomeAction.OpenVnDetail(it)) },
                    onToggleNsfwReveal = { onAction(HomeAction.ToggleNsfwReveal(it)) }
                )
            }

            if (state.settings.showChart) {
                item(key = "home_stats_chart") {
                    HomeStatsChartCard(
                        statsState = state.statsState,
                        onRetry = { onAction(HomeAction.RetryStats) }
                    )
                }
            }

            item(key = "home_bottom_spacer") {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeContentPreview() {
    VndbTheme(themeId = ThemeId.AGL, includeBackground = true) {
        HomeContent(
            state = HomeUiState(
                collectionCarousel = listOf(PreviewData.sampleVnSummary),
                remoteSections = mapOf(
                    HomeSection.LATEST_RELEASES to SectionUiState.Content(listOf(PreviewData.sampleVnSummary))
                ),
                quotesState = SectionUiState.Content(listOf(PreviewData.sampleQuote)),
                displayedQuote = PreviewData.sampleQuote,
                statsState = SectionUiState.Content(PreviewData.sampleSiteStats)
            ),
            onAction = {}
        )
    }
}
