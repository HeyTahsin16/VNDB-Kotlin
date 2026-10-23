package com.vndblite.dcj.ui.screens.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.vndblite.dcj.core.AppResult
import com.vndblite.dcj.domain.model.DevStatus
import com.vndblite.dcj.domain.model.ReleaseWindow
import com.vndblite.dcj.domain.model.SortField
import com.vndblite.dcj.domain.model.SortOption
import com.vndblite.dcj.domain.model.Tag
import com.vndblite.dcj.domain.model.VnSearchFilters
import com.vndblite.dcj.domain.repository.CollectionRepository
import com.vndblite.dcj.domain.repository.SettingsRepository
import com.vndblite.dcj.domain.repository.VnRepository
import com.vndblite.dcj.ui.navigation.SearchRoute
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SearchViewModel(
    private val vnRepository: VnRepository,
    private val collectionRepository: CollectionRepository,
    private val settingsRepository: SettingsRepository,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
    initialRoute: SearchRoute = SearchRoute(),
    private val queryDebounceMs: Long = 350L
) : ViewModel() {

    private val initialFilters: VnSearchFilters = buildInitialFilters(initialRoute, savedStateHandle)
    private val initialSort: SortOption = buildInitialSort(initialRoute)

    private val _uiState = MutableStateFlow(
        SearchUiState(
            filters = initialFilters,
            sort = initialSort,
            revealedNsfwIds = savedStateHandle.get<ArrayList<String>>(KEY_REVEALED_NSFW)?.toSet().orEmpty()
        )
    )
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var debounceJob: Job? = null
    private var tagSuggestionJob: Job? = null
    private var traitSuggestionJob: Job? = null
    private var producerSuggestionJob: Job? = null

    init {
        observeSettingsAndCollection()
        executeSearch(page = 1, append = false)
    }

    fun onAction(action: SearchAction) {
        when (action) {
            is SearchAction.UpdateQuery -> updateQuery(action.query)
            SearchAction.SubmitSearch -> {
                debounceJob?.cancel()
                executeSearch(page = 1, append = false)
            }
            SearchAction.Retry -> executeSearch(page = 1, append = false)
            SearchAction.LoadNextPage -> loadNextPage()
            is SearchAction.ApplyFiltersAndSort -> {
                savedStateHandle[KEY_SEARCH_QUERY] = action.filters.searchQuery
                _uiState.update { it.copy(filters = action.filters, sort = action.sort) }
                executeSearch(page = 1, append = false)
            }
            SearchAction.ResetFilters -> {
                val query = _uiState.value.filters.searchQuery
                _uiState.update {
                    it.copy(
                        filters = VnSearchFilters(searchQuery = query),
                        sort = SortOption.DEFAULT_REMOTE
                    )
                }
                executeSearch(page = 1, append = false)
            }
            is SearchAction.ToggleTag -> mutateFilters { f ->
                val updated = if (f.tags.any { it.id == action.tag.id }) {
                    f.tags.filterNot { it.id == action.tag.id }
                } else {
                    f.tags + action.tag
                }
                f.copy(tags = updated)
            }
            is SearchAction.ToggleTrait -> mutateFilters { f ->
                val updated = if (f.characterTraits.any { it.id == action.trait.id }) {
                    f.characterTraits.filterNot { it.id == action.trait.id }
                } else {
                    f.characterTraits + action.trait
                }
                f.copy(characterTraits = updated)
            }
            is SearchAction.ToggleProducer -> mutateFilters { f ->
                val updated = if (f.developers.any { it.id == action.producer.id }) {
                    f.developers.filterNot { it.id == action.producer.id }
                } else {
                    f.developers + action.producer
                }
                f.copy(developers = updated)
            }
            is SearchAction.ToggleLanguage -> mutateFilters { f ->
                val updated = if (action.language in f.languages) {
                    f.languages - action.language
                } else {
                    f.languages + action.language
                }
                f.copy(languages = updated)
            }
            is SearchAction.ToggleOriginalLanguage -> mutateFilters { f ->
                val updated = if (action.language in f.originalLanguages) {
                    f.originalLanguages - action.language
                } else {
                    f.originalLanguages + action.language
                }
                f.copy(originalLanguages = updated)
            }
            is SearchAction.TogglePlatform -> mutateFilters { f ->
                val updated = if (action.platform in f.platforms) {
                    f.platforms - action.platform
                } else {
                    f.platforms + action.platform
                }
                f.copy(platforms = updated)
            }
            is SearchAction.ToggleDevStatus -> mutateFilters { f ->
                val updated = if (action.devStatus in f.devStatuses) {
                    f.devStatuses - action.devStatus
                } else {
                    f.devStatuses + action.devStatus
                }
                f.copy(devStatuses = updated)
            }
            is SearchAction.ToggleCharacterRole -> mutateFilters { f ->
                val updated = if (action.role in f.characterRoles) {
                    f.characterRoles - action.role
                } else {
                    f.characterRoles + action.role
                }
                f.copy(characterRoles = updated)
            }
            is SearchAction.ToggleCharacterSex -> mutateFilters { f ->
                val updated = if (action.sex in f.characterSexes) {
                    f.characterSexes - action.sex
                } else {
                    f.characterSexes + action.sex
                }
                f.copy(characterSexes = updated)
            }
            is SearchAction.SelectReleaseWindow -> mutateFilters { f ->
                f.copy(releaseWindow = action.window)
            }
            is SearchAction.SelectSort -> {
                _uiState.update { it.copy(sort = SortOption(action.field, action.reverse)) }
                executeSearch(page = 1, append = false)
            }
            is SearchAction.ToggleFilterAndOr -> mutateFilters { f ->
                f.copy(andOr = action.andOr)
            }
            is SearchAction.QueryTagSuggestions -> fetchTagSuggestions(action.query)
            is SearchAction.QueryTraitSuggestions -> fetchTraitSuggestions(action.query)
            is SearchAction.QueryProducerSuggestions -> fetchProducerSuggestions(action.query)
            is SearchAction.ToggleNsfwReveal -> toggleNsfwReveal(action.id)
        }
    }

    private fun observeSettingsAndCollection() {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
        viewModelScope.launch {
            collectionRepository.observeAllWithVn().collect { items ->
                val byId = items.associate { it.entry.vnId to it.entry }
                _uiState.update { it.copy(collectionByVnId = byId) }
            }
        }
    }

    private fun updateQuery(query: String) {
        savedStateHandle[KEY_SEARCH_QUERY] = query
        _uiState.update { state ->
            val nextSort = if (query.isNotBlank() && state.sort.field == SortField.RATING) {
                SortOption(SortField.SEARCH_RANK, reverse = true)
            } else if (query.isBlank() && state.sort.field == SortField.SEARCH_RANK) {
                SortOption(SortField.RATING, reverse = true)
            } else {
                state.sort
            }
            state.copy(
                filters = state.filters.copy(searchQuery = query),
                sort = nextSort
            )
        }
        debounceJob?.cancel()
        debounceJob = viewModelScope.launch {
            if (queryDebounceMs > 0L) delay(queryDebounceMs)
            executeSearch(page = 1, append = false)
        }
    }

    private fun mutateFilters(transform: (VnSearchFilters) -> VnSearchFilters) {
        _uiState.update { it.copy(filters = transform(it.filters)) }
        executeSearch(page = 1, append = false)
    }

    private fun loadNextPage() {
        val state = _uiState.value
        if (!state.hasMore || state.isLoading || state.isAppending) return
        executeSearch(page = state.page + 1, append = true)
    }

    private fun executeSearch(page: Int, append: Boolean) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val snapshot = _uiState.value
            val effectiveSort = if (snapshot.filters.searchQuery.isBlank() &&
                snapshot.sort.field == SortField.SEARCH_RANK
            ) {
                SortOption(SortField.RATING, reverse = true)
            } else {
                snapshot.sort
            }

            _uiState.update {
                if (append) {
                    it.copy(isAppending = true, error = null)
                } else {
                    it.copy(isLoading = true, isAppending = false, error = null)
                }
            }

            when (val result = vnRepository.searchVn(
                filters = snapshot.filters,
                sort = effectiveSort,
                page = page,
                pageSize = PAGE_SIZE
            )) {
                is AppResult.Success -> {
                    val paginated = result.data
                    _uiState.update { current ->
                        val merged = if (append) {
                            (current.results + paginated.items).distinctBy { it.id }
                        } else {
                            paginated.items
                        }
                        current.copy(
                            results = merged,
                            page = page,
                            hasMore = paginated.hasMore,
                            isLoading = false,
                            isAppending = false,
                            error = null
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            isAppending = false,
                            error = result.error
                        )
                    }
                }
            }
        }
    }

    private fun fetchTagSuggestions(query: String) {
        tagSuggestionJob?.cancel()
        if (query.trim().length < 2) {
            _uiState.update { it.copy(tagSuggestions = emptyList()) }
            return
        }
        tagSuggestionJob = viewModelScope.launch {
            if (queryDebounceMs > 0L) delay(200L)
            when (val res = vnRepository.searchTags(query.trim(), maxResults = 15)) {
                is AppResult.Success -> _uiState.update { it.copy(tagSuggestions = res.data) }
                is AppResult.Failure -> _uiState.update { it.copy(tagSuggestions = emptyList()) }
            }
        }
    }

    private fun fetchTraitSuggestions(query: String) {
        traitSuggestionJob?.cancel()
        if (query.trim().length < 2) {
            _uiState.update { it.copy(traitSuggestions = emptyList()) }
            return
        }
        traitSuggestionJob = viewModelScope.launch {
            if (queryDebounceMs > 0L) delay(200L)
            when (val res = vnRepository.searchTraits(query.trim(), maxResults = 15)) {
                is AppResult.Success -> _uiState.update { it.copy(traitSuggestions = res.data) }
                is AppResult.Failure -> _uiState.update { it.copy(traitSuggestions = emptyList()) }
            }
        }
    }

    private fun fetchProducerSuggestions(query: String) {
        producerSuggestionJob?.cancel()
        if (query.trim().length < 2) {
            _uiState.update { it.copy(producerSuggestions = emptyList()) }
            return
        }
        producerSuggestionJob = viewModelScope.launch {
            if (queryDebounceMs > 0L) delay(200L)
            when (val res = vnRepository.searchProducers(query.trim(), maxResults = 15)) {
                is AppResult.Success -> _uiState.update { it.copy(producerSuggestions = res.data) }
                is AppResult.Failure -> _uiState.update { it.copy(producerSuggestions = emptyList()) }
            }
        }
    }

    private fun toggleNsfwReveal(id: String) {
        _uiState.update { state ->
            val updated = if (id in state.revealedNsfwIds) {
                state.revealedNsfwIds - id
            } else {
                state.revealedNsfwIds + id
            }
            savedStateHandle[KEY_REVEALED_NSFW] = ArrayList(updated)
            state.copy(revealedNsfwIds = updated)
        }
    }

    companion object {
        private const val PAGE_SIZE = 24
        private const val KEY_SEARCH_QUERY = "vn_search_query"
        private const val KEY_REVEALED_NSFW = "vn_search_revealed_nsfw_ids"

        private fun buildInitialFilters(
            route: SearchRoute,
            savedStateHandle: SavedStateHandle
        ): VnSearchFilters {
            val savedQuery = savedStateHandle.get<String>(KEY_SEARCH_QUERY)
            val query = savedQuery ?: route.initialQuery
            val initialTags = if (!route.initialTagId.isNullOrBlank()) {
                listOf(Tag(id = route.initialTagId, name = route.initialTagName ?: route.initialTagId))
            } else {
                emptyList()
            }
            val initialDevStatuses = DevStatus.fromApiCode(route.initialDevStatusCode)
                ?.let { listOf(it) }
                .orEmpty()
            val window = ReleaseWindow.fromStorageKey(route.initialReleaseWindowKey)
            return VnSearchFilters(
                searchQuery = query,
                tags = initialTags,
                devStatuses = initialDevStatuses,
                releaseWindow = window
            )
        }

        private fun buildInitialSort(route: SearchRoute): SortOption {
            val field = route.initialSortFieldCode?.let { SortField.fromCode(it, SortField.RATING) }
                ?: if (route.initialQuery.isNotBlank()) SortField.SEARCH_RANK else SortField.RATING
            return SortOption(field = field, reverse = route.initialReverseSort)
        }

        fun factory(
            vnRepository: VnRepository,
            collectionRepository: CollectionRepository,
            settingsRepository: SettingsRepository,
            initialRoute: SearchRoute = SearchRoute()
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return SearchViewModel(
                    vnRepository = vnRepository,
                    collectionRepository = collectionRepository,
                    settingsRepository = settingsRepository,
                    savedStateHandle = extras.createSavedStateHandle(),
                    initialRoute = initialRoute
                ) as T
            }
        }
    }
}
