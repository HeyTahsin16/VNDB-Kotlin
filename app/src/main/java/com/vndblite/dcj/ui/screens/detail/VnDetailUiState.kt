package com.vndblite.dcj.ui.screens.detail

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.vndblite.dcj.R
import com.vndblite.dcj.core.UiText
import com.vndblite.dcj.domain.model.AppSettings
import com.vndblite.dcj.domain.model.Character
import com.vndblite.dcj.domain.model.CollectionEntry
import com.vndblite.dcj.domain.model.Quote
import com.vndblite.dcj.domain.model.Review
import com.vndblite.dcj.domain.model.Tag
import com.vndblite.dcj.domain.model.Trait
import com.vndblite.dcj.domain.model.VnDetails
import com.vndblite.dcj.domain.model.VnSummary
import com.vndblite.dcj.ui.components.CollectionEntryFormPayload
import com.vndblite.dcj.ui.components.SectionUiState

enum class VnDetailTab(
    @StringRes val labelResId: Int,
    val testTag: String
) {
    GENERAL(R.string.detail_tab_general, "detail_tab_general"),
    CHARACTERS(R.string.detail_tab_characters, "detail_tab_characters"),
    RELATIONS(R.string.detail_tab_relations, "detail_tab_relations"),
    QUOTES(R.string.detail_tab_quotes, "detail_tab_quotes"),
    REVIEWS(R.string.detail_tab_reviews, "detail_tab_reviews")
}

@Immutable
data class CharactersTabPayload(
    val characters: List<Character> = emptyList(),
    val isTruncated: Boolean = false
)

@Immutable
data class VnDetailUiState(
    val vnId: String = "",
    val initialTitle: String = "",
    val summary: VnSummary? = null,
    val details: VnDetails? = null,
    val isRefreshingMain: Boolean = false,
    val mainError: UiText? = null,
    val charactersState: SectionUiState<CharactersTabPayload> = SectionUiState.Loading,
    val quotesState: SectionUiState<List<Quote>> = SectionUiState.Loading,
    val reviewsState: SectionUiState<List<Review>> = SectionUiState.Loading,
    val collectionEntry: CollectionEntry? = null,
    val relatedCollectionEntries: Map<String, CollectionEntry> = emptyMap(),
    val isAccountSynced: Boolean = false,
    val settings: AppSettings = AppSettings(),
    val nsfwRevealedIds: Set<String> = emptySet(),
    val selectedTab: VnDetailTab = VnDetailTab.GENERAL,
    val maxTagSpoilerLevel: Int = 0
) {
    val displayTitle: String
        get() = summary?.title?.takeIf { it.isNotBlank() } ?: initialTitle.ifBlank { vnId }
}

sealed interface VnDetailAction {
    data object Back : VnDetailAction
    data class SelectTab(val tab: VnDetailTab) : VnDetailAction
    data class SetTagSpoilerLevel(val level: Int) : VnDetailAction
    data object RetryMainDetails : VnDetailAction
    data object RetryCharacters : VnDetailAction
    data object RetryQuotes : VnDetailAction
    data object RetryReviews : VnDetailAction
    data class SaveCollectionEntry(val payload: CollectionEntryFormPayload) : VnDetailAction
    data object RemoveCollectionEntry : VnDetailAction
    data class ToggleNsfwReveal(val id: String) : VnDetailAction
    data class OpenRelatedVn(val vn: VnSummary) : VnDetailAction
    data class FilterByTag(val tag: Tag) : VnDetailAction
    data class FilterByTrait(val trait: Trait) : VnDetailAction
}
