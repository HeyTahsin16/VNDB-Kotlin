package com.vndblite.dcj.ui.screens.home

import androidx.compose.runtime.Immutable
import com.vndblite.dcj.core.UiText
import com.vndblite.dcj.domain.model.AppSettings
import com.vndblite.dcj.domain.model.CollectionEntry
import com.vndblite.dcj.domain.model.HomeSection
import com.vndblite.dcj.domain.model.Quote
import com.vndblite.dcj.domain.model.SiteStats
import com.vndblite.dcj.domain.model.Trait
import com.vndblite.dcj.domain.model.VnSummary
import com.vndblite.dcj.ui.components.SectionUiState

@Immutable
data class HomeUiState(
    val isRefreshing: Boolean = false,
    val collectionCarousel: List<VnSummary> = emptyList(),
    val collectionEntries: Map<String, CollectionEntry> = emptyMap(),
    val remoteSections: Map<HomeSection, SectionUiState<List<VnSummary>>> = emptyMap(),
    val quotesState: SectionUiState<List<Quote>> = SectionUiState.Loading,
    val displayedQuote: Quote? = null,
    val statsState: SectionUiState<SiteStats> = SectionUiState.Loading,
    val settings: AppSettings = AppSettings(),
    val nsfwRevealedIds: Set<String> = emptySet()
)

sealed interface HomeAction {
    data object RefreshAll : HomeAction
    data class RetrySection(val section: HomeSection) : HomeAction
    data object RetryQuotes : HomeAction
    data object ShuffleQuote : HomeAction
    data object RetryStats : HomeAction
    data class OpenVnDetail(val vn: VnSummary) : HomeAction
    data class SeeMoreSection(val section: HomeSection) : HomeAction
    data object GoToCollectionTab : HomeAction
    data class OpenTraitFinder(val presetTrait: Trait? = null) : HomeAction
    data object OpenRandomVn : HomeAction
    data class ToggleNsfwReveal(val vnId: String) : HomeAction
}
