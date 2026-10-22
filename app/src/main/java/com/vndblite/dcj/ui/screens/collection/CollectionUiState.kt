package com.vndblite.dcj.ui.screens.collection

import com.vndblite.dcj.core.ApiError
import com.vndblite.dcj.domain.model.AppSettings
import com.vndblite.dcj.domain.model.CollectionFilters
import com.vndblite.dcj.domain.model.CollectionItemWithVn
import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.domain.model.DevStatus
import com.vndblite.dcj.domain.model.Language
import com.vndblite.dcj.domain.model.Platform
import com.vndblite.dcj.domain.model.SortField
import com.vndblite.dcj.domain.model.SortOption
import com.vndblite.dcj.domain.model.SyncReport
import com.vndblite.dcj.domain.model.UserAccount
import com.vndblite.dcj.sync.SyncProgress
import com.vndblite.dcj.ui.components.CollectionEntryDraft

data class CollectionUiState(
    val settings: AppSettings = AppSettings(),
    val selectedTab: CollectionStatus = CollectionStatus.PLAYING,
    val isSearchVisible: Boolean = false,
    val filters: CollectionFilters = CollectionFilters(status = CollectionStatus.PLAYING),
    val sort: SortOption = SortOption.DEFAULT_LOCAL,
    val items: List<CollectionItemWithVn> = emptyList(),
    val statusCounts: Map<CollectionStatus, Int> = emptyMap(),
    val selectedVnIds: Set<String> = emptySet(),
    val editingItem: CollectionItemWithVn? = null,
    val activeAccount: UserAccount? = null,
    val syncProgress: SyncProgress = SyncProgress(),
    val lastSyncReport: SyncReport? = null,
    val syncError: ApiError? = null,
    val revealedNsfwIds: Set<String> = emptySet()
) {
    val isSelectionMode: Boolean
        get() = selectedVnIds.isNotEmpty()

    val totalCollectionCount: Int
        get() = statusCounts.values.sum()
}

sealed interface CollectionAction {
    data class SelectTab(val status: CollectionStatus) : CollectionAction
    data object ToggleSearchVisibility : CollectionAction
    data class UpdateSearchQuery(val query: String) : CollectionAction
    data class SelectSort(val field: SortField, val reverse: Boolean) : CollectionAction
    data class SelectMinAge(val minAge: Int?) : CollectionAction
    data class ToggleLanguage(val language: Language) : CollectionAction
    data class ToggleOriginalLanguage(val language: Language) : CollectionAction
    data class TogglePlatform(val platform: Platform) : CollectionAction
    data class ToggleDevStatus(val devStatus: DevStatus) : CollectionAction
    data object ResetFilters : CollectionAction
    data class ToggleSelectVn(val vnId: String) : CollectionAction
    data object SelectAllInCurrentTab : CollectionAction
    data object ClearSelection : CollectionAction
    data class BatchUpdateSelected(
        val newStatus: CollectionStatus?,
        val newVote: Int?,
        val updateVote: Boolean
    ) : CollectionAction
    data object BatchDeleteSelected : CollectionAction
    data class OpenEditDialog(val item: CollectionItemWithVn) : CollectionAction
    data object DismissEditDialog : CollectionAction
    data class SaveEditedEntry(val vnId: String, val draft: CollectionEntryDraft) : CollectionAction
    data class RemoveEditedEntry(val vnId: String) : CollectionAction
    data object TriggerSync : CollectionAction
    data object DismissSyncFeedback : CollectionAction
    data class ToggleNsfwReveal(val id: String) : CollectionAction
}
