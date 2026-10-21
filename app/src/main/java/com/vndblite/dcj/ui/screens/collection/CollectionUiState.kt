package com.vndblite.dcj.ui.screens.collection

import androidx.compose.runtime.Immutable
import com.vndblite.dcj.core.UiText
import com.vndblite.dcj.domain.model.AppSettings
import com.vndblite.dcj.domain.model.CollectionEntry
import com.vndblite.dcj.domain.model.CollectionFilters
import com.vndblite.dcj.domain.model.CollectionItemWithVn
import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.domain.model.SortOption
import com.vndblite.dcj.domain.model.UserAccount
import com.vndblite.dcj.domain.model.VnSummary
import com.vndblite.dcj.ui.components.CollectionEntryFormPayload

@Immutable
data class CollectionUiState(
    val itemsByStatus: Map<CollectionStatus, List<CollectionItemWithVn>> = emptyMap(),
    val allEntriesMap: Map<String, CollectionEntry> = emptyMap(),
    val totalCount: Int = 0,
    val filters: CollectionFilters = CollectionFilters(),
    val sort: SortOption = SortOption.DEFAULT_LOCAL,
    val isSearchActive: Boolean = false,
    val selectedVnIds: Set<String> = emptySet(),
    val settings: AppSettings = AppSettings(),
    val activeAccount: UserAccount? = null,
    val isAccountSynced: Boolean = false,
    val isSyncRunning: Boolean = false,
    val syncStatusText: UiText? = null,
    val nsfwRevealedIds: Set<String> = emptySet()
)

sealed interface CollectionAction {
    data class SetSearchActive(val active: Boolean) : CollectionAction
    data class UpdateSearchQuery(val query: String) : CollectionAction
    data class ApplyFiltersAndSort(val filters: CollectionFilters, val sort: SortOption) : CollectionAction
    data class ToggleSelectVn(val vnId: String) : CollectionAction
    data class SelectAllInStatus(val status: CollectionStatus) : CollectionAction
    data object ClearSelection : CollectionAction
    data class SaveEntry(val vn: VnSummary, val payload: CollectionEntryFormPayload) : CollectionAction
    data class RemoveEntry(val vnId: String) : CollectionAction
    data class BatchUpdateSelected(val status: CollectionStatus, val overrideVote: Int?) : CollectionAction
    data object BatchDeleteSelected : CollectionAction
    data object SyncNow : CollectionAction
    data object OpenAuthScreen : CollectionAction
    data class OpenVnDetail(val vn: VnSummary) : CollectionAction
    data class ToggleNsfwReveal(val vnId: String) : CollectionAction
}
