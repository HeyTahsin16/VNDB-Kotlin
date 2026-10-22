package com.vndblite.dcj.ui.screens.home

import com.vndblite.dcj.core.ApiError
import com.vndblite.dcj.domain.model.AppSettings
import com.vndblite.dcj.domain.model.CollectionEntry
import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.domain.model.GenrePreset
import com.vndblite.dcj.domain.model.HomeSection
import com.vndblite.dcj.domain.model.PopularTraitPreset
import com.vndblite.dcj.domain.model.Quote
import com.vndblite.dcj.domain.model.SiteStats
import com.vndblite.dcj.domain.model.VnSummary

data class HomeSectionState(
    val section: HomeSection,
    val items: List<VnSummary> = emptyList(),
    val isLoading: Boolean = false,
    val error: ApiError? = null
)

data class HomeUiState(
    val settings: AppSettings = AppSettings(),
    val sections: Map<HomeSection, HomeSectionState> = HomeSection.entries.associateWith {
        HomeSectionState(section = it)
    },
    val collectionByVnId: Map<String, CollectionEntry> = emptyMap(),
    val statusCounts: Map<CollectionStatus, Int> = emptyMap(),
    val quotes: List<Quote> = emptyList(),
    val isQuotesLoading: Boolean = false,
    val quotesError: ApiError? = null,
    val siteStats: SiteStats? = null,
    val isStatsLoading: Boolean = false,
    val statsError: ApiError? = null,
    val isRefreshingAll: Boolean = false,
    val revealedNsfwIds: Set<String> = emptySet()
)

sealed interface HomeAction {
    data object RefreshAll : HomeAction
    data class RetrySection(val section: HomeSection) : HomeAction
    data object RefreshQuotes : HomeAction
    data object RetryStats : HomeAction
    data class ToggleNsfwReveal(val id: String) : HomeAction
}

data class HomeNavigationCallbacks(
    val onOpenVnDetail: (vnId: String) -> Unit,
    val onSeeMoreSection: (section: HomeSection) -> Unit,
    val onOpenGenrePreset: (preset: GenrePreset) -> Unit,
    val onOpenTraitPreset: (preset: PopularTraitPreset) -> Unit,
    val onOpenCharacterFinder: () -> Unit,
    val onOpenRandomVn: () -> Unit
)
