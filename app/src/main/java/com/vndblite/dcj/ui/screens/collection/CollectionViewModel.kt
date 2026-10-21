package com.vndblite.dcj.ui.screens.collection

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vndblite.dcj.AppContainer
import com.vndblite.dcj.domain.model.CollectionFilters
import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.domain.model.SortOption
import com.vndblite.dcj.domain.model.VnSummary
import com.vndblite.dcj.domain.repository.CollectionRepository
import com.vndblite.dcj.domain.repository.SettingsRepository
import com.vndblite.dcj.sync.SyncService
import com.vndblite.dcj.ui.components.CollectionEntryFormPayload
import com.vndblite.dcj.ui.format.toStatusUiText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionViewModel(
    private val collectionRepository: CollectionRepository,
    private val settingsRepository: SettingsRepository,
    private val syncService: SyncService,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val isSearchActiveFlow: StateFlow<Boolean> =
        savedStateHandle.getStateFlow(KEY_SEARCH_ACTIVE, false)

    private val selectedVnIdsFlow = MutableStateFlow<Set<String>>(
        savedStateHandle.get<ArrayList<String>>(KEY_SELECTED_IDS)?.toSet().orEmpty()
    )

    private val filterSortPairFlow = combine(
        settingsRepository.collectionFiltersFlow,
        settingsRepository.collectionSortFlow
    ) { filters, sort ->
        filters to sort
    }

    private val filteredItemsFlow = filterSortPairFlow.flatMapLatest { (filters, sort) ->
        collectionRepository.observeFilteredCollection(
            filters = filters.copy(status = null),
            sort = sort
        )
    }

    private val syncSnapshotFlow = combine(
        syncService.activeAccountFlow,
        syncService.isAccountSyncedFlow,
        syncService.progressFlow
    ) { account, isSynced, progress ->
        Triple(account, isSynced, progress)
    }

    private val collectionDataFlow = combine(
        filteredItemsFlow,
        collectionRepository.observeAllVisibleEntriesMap(),
        filterSortPairFlow
    ) { filteredItems, allEntries, (filters, sort) ->
        val grouped = CollectionStatus.entries.associateWith { status ->
            filteredItems.filter { it.entry.status == status }
        }
        Triple(grouped, allEntries, filters to sort)
    }

    val uiState: StateFlow<CollectionUiState> = combine(
        collectionDataFlow,
        isSearchActiveFlow,
        selectedVnIdsFlow,
        settingsRepository.settingsFlow,
        combine(syncSnapshotFlow, settingsRepository.nsfwRevealedIdsFlow) { s, n -> s to n }
    ) { (groupedByStatus, allEntries, filterAndSort), isSearchActive, selectedIds, settings, (syncSnap, revealedIds) ->
        val (account, isSynced, progress) = syncSnap
        CollectionUiState(
            itemsByStatus = groupedByStatus,
            allEntriesMap = allEntries,
            totalCount = allEntries.size,
            filters = filterAndSort.first,
            sort = filterAndSort.second,
            isSearchActive = isSearchActive,
            selectedVnIds = selectedIds.intersect(allEntries.keys),
            settings = settings,
            activeAccount = account,
            isAccountSynced = isSynced,
            isSyncRunning = progress.isRunning,
            syncStatusText = progress.toStatusUiText(),
            nsfwRevealedIds = revealedIds
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CollectionUiState()
    )

    init {
        hydrateUncachedCollectionSummaries()
    }

    fun onAction(action: CollectionAction) {
        when (action) {
            is CollectionAction.SetSearchActive -> setSearchActive(action.active)
            is CollectionAction.UpdateSearchQuery -> updateSearchQuery(action.query)
            is CollectionAction.ApplyFiltersAndSort -> applyFiltersAndSort(action.filters, action.sort)
            is CollectionAction.ToggleSelectVn -> toggleSelectVn(action.vnId)
            is CollectionAction.SelectAllInStatus -> selectAllInStatus(action.status)
            CollectionAction.ClearSelection -> updateSelectedIds(emptySet())
            is CollectionAction.SaveEntry -> saveEntry(action.vn, action.payload)
            is CollectionAction.RemoveEntry -> removeEntry(action.vnId)
            is CollectionAction.BatchUpdateSelected -> batchUpdate(action.status, action.overrideVote)
            CollectionAction.BatchDeleteSelected -> batchDeleteSelected()
            CollectionAction.SyncNow -> {
                viewModelScope.launch { syncService.syncCurrentAccount() }
            }
            is CollectionAction.ToggleNsfwReveal -> {
                settingsRepository.toggleNsfwReveal(action.vnId)
            }
            CollectionAction.OpenAuthScreen,
            is CollectionAction.OpenVnDetail -> Unit
        }
    }

    private fun hydrateUncachedCollectionSummaries() {
        viewModelScope.launch {
            collectionRepository.observeAllVisibleEntriesMap().collect { entries ->
                if (entries.isNotEmpty()) {
                    collectionRepository.hydrateMissingSummaries(entries.keys.toList())
                }
            }
        }
    }

    private fun setSearchActive(active: Boolean) {
        savedStateHandle[KEY_SEARCH_ACTIVE] = active
        if (!active) {
            updateSearchQuery("")
        }
    }

    private fun updateSearchQuery(query: String) {
        viewModelScope.launch {
            val current = uiState.value.filters
            settingsRepository.saveCollectionFilters(current.copy(searchQuery = query))
        }
    }

    private fun applyFiltersAndSort(filters: CollectionFilters, sort: SortOption) {
        viewModelScope.launch {
            settingsRepository.saveCollectionFilters(filters)
            settingsRepository.saveCollectionSort(sort)
        }
    }

    private fun toggleSelectVn(vnId: String) {
        val current = selectedVnIdsFlow.value
        val next = if (vnId in current) current - vnId else current + vnId
        updateSelectedIds(next)
    }

    private fun selectAllInStatus(status: CollectionStatus) {
        val inTabIds = uiState.value.itemsByStatus[status].orEmpty().map { it.entry.vnId }.toSet()
        updateSelectedIds(selectedVnIdsFlow.value + inTabIds)
    }

    private fun updateSelectedIds(next: Set<String>) {
        selectedVnIdsFlow.value = next
        savedStateHandle[KEY_SELECTED_IDS] = ArrayList(next)
    }

    private fun saveEntry(vn: VnSummary, payload: CollectionEntryFormPayload) {
        viewModelScope.launch {
            collectionRepository.saveOrUpdateEntry(
                vn = vn,
                status = payload.status,
                vote = payload.vote,
                notes = payload.notes,
                startedDate = payload.startedDate,
                finishedDate = payload.finishedDate
            )
        }
    }

    private fun removeEntry(vnId: String) {
        viewModelScope.launch {
            collectionRepository.removeEntry(vnId)
            updateSelectedIds(selectedVnIdsFlow.value - vnId)
        }
    }

    private fun batchUpdate(status: CollectionStatus, overrideVote: Int?) {
        val ids = selectedVnIdsFlow.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            collectionRepository.batchUpdateEntries(ids, status, overrideVote)
            updateSelectedIds(emptySet())
        }
    }

    private fun batchDeleteSelected() {
        val ids = selectedVnIdsFlow.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            collectionRepository.removeEntries(ids)
            updateSelectedIds(emptySet())
        }
    }

    companion object {
        private const val KEY_SEARCH_ACTIVE = "collection_search_active"
        private const val KEY_SELECTED_IDS = "collection_selected_ids"

        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                CollectionViewModel(
                    collectionRepository = container.collectionRepository,
                    settingsRepository = container.settingsRepository,
                    syncService = container.syncService,
                    savedStateHandle = createSavedStateHandle()
                )
            }
        }
    }
}
