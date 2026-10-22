package com.vndblite.dcj.ui.screens.collection

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.vndblite.dcj.core.AppResult
import com.vndblite.dcj.domain.model.CollectionFilters
import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.domain.model.SortOption
import com.vndblite.dcj.domain.repository.CollectionRepository
import com.vndblite.dcj.domain.repository.SettingsRepository
import com.vndblite.dcj.domain.repository.SyncRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CollectionViewModel(
    private val collectionRepository: CollectionRepository,
    private val settingsRepository: SettingsRepository,
    private val syncRepository: SyncRepository,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle()
) : ViewModel() {

    private val initialTab = CollectionStatus.fromStorageKeyOrDefault(
        savedStateHandle.get<String>(KEY_SELECTED_TAB) ?: CollectionStatus.PLAYING.storageKey
    )
    private val initialQuery = savedStateHandle.get<String>(KEY_SEARCH_QUERY).orEmpty()

    private val _uiState = MutableStateFlow(
        CollectionUiState(
            selectedTab = initialTab,
            isSearchVisible = initialQuery.isNotEmpty(),
            filters = CollectionFilters(
                searchQuery = initialQuery,
                status = initialTab
            ),
            selectedVnIds = savedStateHandle.get<ArrayList<String>>(KEY_SELECTED_VN_IDS)?.toSet().orEmpty(),
            revealedNsfwIds = savedStateHandle.get<ArrayList<String>>(KEY_REVEALED_NSFW)?.toSet().orEmpty()
        )
    )
    val uiState: StateFlow<CollectionUiState> = _uiState.asStateFlow()

    private var queryObserverJob: Job? = null

    init {
        observeSettings()
        observeStatusCounts()
        observeSyncAndAccount()
        restartFilteredQueryObserver()
    }

    fun onAction(action: CollectionAction) {
        when (action) {
            is CollectionAction.SelectTab -> {
                savedStateHandle[KEY_SELECTED_TAB] = action.status.storageKey
                _uiState.update {
                    it.copy(
                        selectedTab = action.status,
                        filters = it.filters.copy(status = action.status),
                        selectedVnIds = emptySet()
                    )
                }
                savedStateHandle[KEY_SELECTED_VN_IDS] = ArrayList<String>()
                restartFilteredQueryObserver()
            }
            CollectionAction.ToggleSearchVisibility -> {
                val nextVisible = !_uiState.value.isSearchVisible
                _uiState.update {
                    val nextQuery = if (nextVisible) it.filters.searchQuery else ""
                    savedStateHandle[KEY_SEARCH_QUERY] = nextQuery
                    it.copy(
                        isSearchVisible = nextVisible,
                        filters = it.filters.copy(searchQuery = nextQuery)
                    )
                }
                restartFilteredQueryObserver()
            }
            is CollectionAction.UpdateSearchQuery -> {
                savedStateHandle[KEY_SEARCH_QUERY] = action.query
                _uiState.update {
                    it.copy(filters = it.filters.copy(searchQuery = action.query))
                }
                restartFilteredQueryObserver()
            }
            is CollectionAction.SelectSort -> {
                _uiState.update { it.copy(sort = SortOption(action.field, action.reverse)) }
                restartFilteredQueryObserver()
            }
            is CollectionAction.SelectMinAge -> mutateFilters { f -> f.copy(minAge = action.minAge) }
            is CollectionAction.ToggleLanguage -> mutateFilters { f ->
                val next = if (action.language in f.languages) f.languages - action.language else f.languages + action.language
                f.copy(languages = next)
            }
            is CollectionAction.ToggleOriginalLanguage -> mutateFilters { f ->
                val next = if (action.language in f.originalLanguages) f.originalLanguages - action.language else f.originalLanguages + action.language
                f.copy(originalLanguages = next)
            }
            is CollectionAction.TogglePlatform -> mutateFilters { f ->
                val next = if (action.platform in f.platforms) f.platforms - action.platform else f.platforms + action.platform
                f.copy(platforms = next)
            }
            is CollectionAction.ToggleDevStatus -> mutateFilters { f ->
                val next = if (action.devStatus in f.devStatuses) f.devStatuses - action.devStatus else f.devStatuses + action.devStatus
                f.copy(devStatuses = next)
            }
            CollectionAction.ResetFilters -> {
                _uiState.update { state ->
                    state.copy(
                        filters = CollectionFilters(
                            searchQuery = state.filters.searchQuery,
                            status = state.selectedTab
                        ),
                        sort = SortOption.DEFAULT_LOCAL
                    )
                }
                restartFilteredQueryObserver()
            }
            is CollectionAction.ToggleSelectVn -> {
                _uiState.update { state ->
                    val next = if (action.vnId in state.selectedVnIds) {
                        state.selectedVnIds - action.vnId
                    } else {
                        state.selectedVnIds + action.vnId
                    }
                    savedStateHandle[KEY_SELECTED_VN_IDS] = ArrayList(next)
                    state.copy(selectedVnIds = next)
                }
            }
            CollectionAction.SelectAllInCurrentTab -> {
                _uiState.update { state ->
                    val allIds = state.items.map { it.entry.vnId }.toSet()
                    savedStateHandle[KEY_SELECTED_VN_IDS] = ArrayList(allIds)
                    state.copy(selectedVnIds = allIds)
                }
            }
            CollectionAction.ClearSelection -> {
                savedStateHandle[KEY_SELECTED_VN_IDS] = ArrayList<String>()
                _uiState.update { it.copy(selectedVnIds = emptySet()) }
            }
            is CollectionAction.BatchUpdateSelected -> {
                val ids = _uiState.value.selectedVnIds
                if (ids.isEmpty()) return
                viewModelScope.launch {
                    collectionRepository.batchUpdate(
                        vnIds = ids,
                        newStatus = action.newStatus,
                        newVote = action.newVote,
                        updateVote = action.updateVote
                    )
                    savedStateHandle[KEY_SELECTED_VN_IDS] = ArrayList<String>()
                    _uiState.update { it.copy(selectedVnIds = emptySet()) }
                }
            }
            CollectionAction.BatchDeleteSelected -> {
                val ids = _uiState.value.selectedVnIds
                if (ids.isEmpty()) return
                viewModelScope.launch {
                    collectionRepository.removeEntries(ids)
                    savedStateHandle[KEY_SELECTED_VN_IDS] = ArrayList<String>()
                    _uiState.update { it.copy(selectedVnIds = emptySet()) }
                }
            }
            is CollectionAction.OpenEditDialog -> {
                _uiState.update { it.copy(editingItem = action.item) }
            }
            CollectionAction.DismissEditDialog -> {
                _uiState.update { it.copy(editingItem = null) }
            }
            is CollectionAction.SaveEditedEntry -> {
                val editing = _uiState.value.editingItem ?: return
                viewModelScope.launch {
                    collectionRepository.upsertEntry(
                        vnSummary = editing.vn,
                        status = action.draft.status,
                        vote = action.draft.vote,
                        notes = action.draft.notes,
                        startedDate = action.draft.startedDate,
                        finishedDate = action.draft.finishedDate
                    )
                    _uiState.update { it.copy(editingItem = null) }
                }
            }
            is CollectionAction.RemoveEditedEntry -> {
                viewModelScope.launch {
                    collectionRepository.removeEntry(action.vnId)
                    _uiState.update { it.copy(editingItem = null) }
                }
            }
            CollectionAction.TriggerSync -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(syncError = null, lastSyncReport = null) }
                    when (val result = syncRepository.syncCollection()) {
                        is AppResult.Success -> _uiState.update { it.copy(lastSyncReport = result.data) }
                        is AppResult.Failure -> _uiState.update { it.copy(syncError = result.error) }
                    }
                }
            }
            CollectionAction.DismissSyncFeedback -> {
                _uiState.update { it.copy(lastSyncReport = null, syncError = null) }
            }
            is CollectionAction.ToggleNsfwReveal -> {
                _uiState.update { state ->
                    val updated = if (action.id in state.revealedNsfwIds) {
                        state.revealedNsfwIds - action.id
                    } else {
                        state.revealedNsfwIds + action.id
                    }
                    savedStateHandle[KEY_REVEALED_NSFW] = ArrayList(updated)
                    state.copy(revealedNsfwIds = updated)
                }
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                _uiState.update { state ->
                    val orderedTabs = settings.collectionStatusTabArrangement.ifEmpty {
                        CollectionStatus.entries
                    }
                    val validTab = if (state.selectedTab in orderedTabs) {
                        state.selectedTab
                    } else {
                        orderedTabs.first()
                    }
                    state.copy(
                        settings = settings,
                        selectedTab = validTab,
                        filters = state.filters.copy(status = validTab)
                    )
                }
            }
        }
    }

    private fun observeStatusCounts() {
        viewModelScope.launch {
            collectionRepository.observeStatusCounts().collect { counts ->
                _uiState.update { it.copy(statusCounts = counts) }
            }
        }
    }

    private fun observeSyncAndAccount() {
        viewModelScope.launch {
            syncRepository.observeActiveAccount().collect { account ->
                _uiState.update { it.copy(activeAccount = account) }
            }
        }
        viewModelScope.launch {
            syncRepository.syncProgress.collect { progress ->
                _uiState.update { it.copy(syncProgress = progress) }
            }
        }
    }

    private fun mutateFilters(transform: (CollectionFilters) -> CollectionFilters) {
        _uiState.update { it.copy(filters = transform(it.filters)) }
        restartFilteredQueryObserver()
    }

    private fun restartFilteredQueryObserver() {
        queryObserverJob?.cancel()
        queryObserverJob = viewModelScope.launch {
            val snapshot = _uiState.value
            collectionRepository.observeFilteredWithVn(
                filters = snapshot.filters,
                sort = snapshot.sort
            ).collect { items ->
                _uiState.update { it.copy(items = items) }
            }
        }
    }

    companion object {
        private const val KEY_SELECTED_TAB = "collection_selected_tab"
        private const val KEY_SEARCH_QUERY = "collection_search_query"
        private const val KEY_SELECTED_VN_IDS = "collection_selected_vn_ids"
        private const val KEY_REVEALED_NSFW = "collection_revealed_nsfw_ids"

        fun factory(
            collectionRepository: CollectionRepository,
            settingsRepository: SettingsRepository,
            syncRepository: SyncRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return CollectionViewModel(
                    collectionRepository = collectionRepository,
                    settingsRepository = settingsRepository,
                    syncRepository = syncRepository,
                    savedStateHandle = extras.createSavedStateHandle()
                ) as T
            }
        }
    }
}
