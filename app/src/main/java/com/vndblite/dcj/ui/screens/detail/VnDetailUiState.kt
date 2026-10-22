package com.vndblite.dcj.ui.screens.detail

import com.vndblite.dcj.core.ApiError
import com.vndblite.dcj.domain.model.AppSettings
import com.vndblite.dcj.domain.model.Character
import com.vndblite.dcj.domain.model.CollectionEntry
import com.vndblite.dcj.domain.model.Quote
import com.vndblite.dcj.domain.model.Review
import com.vndblite.dcj.domain.model.Tag
import com.vndblite.dcj.domain.model.Trait
import com.vndblite.dcj.domain.model.VnDetails
import com.vndblite.dcj.domain.model.VnSummary
import com.vndblite.dcj.ui.components.CollectionEntryDraft

enum class VnDetailTab {
    OVERVIEW,
    CAST_AND_REVIEWS,
    RELATIONS_AND_LINKS
}

data class VnDetailUiState(
    val vnId: String,
    val settings: AppSettings = AppSettings(),
    val summary: VnSummary? = null,
    val details: VnDetails? = null,
    val collectionEntry: CollectionEntry? = null,
    val characters: List<Character> = emptyList(),
    val quotes: List<Quote> = emptyList(),
    val reviews: List<Review> = emptyList(),
    val expandedReviewTexts: Map<String, String> = emptyMap(),
    val loadingFullReviewIds: Set<String> = emptySet(),
    val selectedTab: VnDetailTab = VnDetailTab.OVERVIEW,
    val spoilerLevel: Int = 0,
    val isLoadingDetails: Boolean = false,
    val detailsError: ApiError? = null,
    val isLoadingExtras: Boolean = false,
    val isCollectionDialogOpen: Boolean = false,
    val selectedCharacter: Character? = null,
    val fullscreenScreenshotUrl: String? = null,
    val revealedNsfwIds: Set<String> = emptySet()
)

sealed interface VnDetailAction {
    data object Refresh : VnDetailAction
    data class SelectTab(val tab: VnDetailTab) : VnDetailAction
    data class SelectSpoilerLevel(val level: Int) : VnDetailAction
    data object OpenCollectionDialog : VnDetailAction
    data object DismissCollectionDialog : VnDetailAction
    data class SaveCollectionEntry(val draft: CollectionEntryDraft) : VnDetailAction
    data object RemoveCollectionEntry : VnDetailAction
    data class SelectCharacter(val character: Character) : VnDetailAction
    data object DismissCharacterBio : VnDetailAction
    data class OpenFullscreenScreenshot(val fullUrl: String) : VnDetailAction
    data object CloseFullscreenScreenshot : VnDetailAction
    data class ExpandFullReview(val reviewId: String) : VnDetailAction
    data class ToggleNsfwReveal(val id: String) : VnDetailAction
}

data class VnDetailNavigationCallbacks(
    val onNavigateBack: () -> Unit,
    val onOpenRelatedVn: (vnId: String) -> Unit,
    val onSearchByTag: (tag: Tag) -> Unit,
    val onSearchByTrait: (trait: Trait) -> Unit
)
