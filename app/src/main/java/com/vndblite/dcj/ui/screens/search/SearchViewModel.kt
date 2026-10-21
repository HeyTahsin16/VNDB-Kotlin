package com.vndblite.dcj.ui.screens.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vndblite.dcj.AppContainer
import com.vndblite.dcj.core.AppResult
import com.vndblite.dcj.core.UiText
import com.vndblite.dcj.core.toUiText
import com.vndblite.dcj.data.remote.VnQuerySpec
import com.vndblite.dcj.domain.model.HomeSection
import com.vndblite.dcj.domain.model.SortOption
import com.vndblite.dcj.domain.model.VnSearchFilters
import com.vndblite.dcj.domain.model.VnSummary
import com.vndblite.dcj.domain.repository.CollectionRepository
import com.vndblite.dcj.domain.repository.SettingsRepository
import com.vndblite.dcj.domain.repository.VnRepository
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Dedicated ViewModel for remote Visual Novel search with:
 * - Single debounced `collectLatest` pipeline so rapid typing or filter edits cancel stale requests (F4, N4)
 * - Generic debounced autocomplete helper replacing the 3 copy-pasted `query*Live` functions (A9)
 * - Sealed [ActiveFilterChip] removal replacing the 10-arg `removeSingleFilterChip` (A4)
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val vnRepository: VnRepository,
    private val collectionRepository: CollectionRepository,
    private val settingsRepository: SettingsRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val modeFlow = MutableStateFlow(
        SearchMode.entries.firstOrNull { it.name == savedStateHandle[KEY_SEARCH_MODE] }
            ?: SearchMode.VISUAL_NOVELS
    )

    private val filtersAndSortFlow = MutableStateFlow(
        VnSearchFilters(searchQuery = savedStateHandle[KEY_SEARCH_QUERY] ?: "") to SortOption.DEFAULT_REMOTE
    )

    private data class PagedVnState(
        val items: List<VnSummary> = emptyList(),
        val isLoading: Boolean = false,
        val isLoadingMore: Boolean = false,
        val canLoadMore: Boolean = false,
        val currentPage: Int = 1,
        val error: UiText? = null
    )

    private val pagedState = MutableStateFlow(PagedVnState())
    private val autocompleteFlow = MutableStateFlow(FilterSheetAutocompleteState())

    private val tagQueryFlow = MutableStateFlow("")
    private val traitQueryFlow = MutableStateFlow("")
    private val developerQueryFlow = MutableStateFlow("")

    private val searchEpoch = AtomicLong(0L)
    private var activeSearchJob: Job? = null
    private var paginationJob: Job? = null

    val uiState: StateFlow<SearchUiState> = combine(
        modeFlow,
        filtersAndSortFlow,
        pagedState,
        autocompleteFlow,
        combine(
            collectionRepository.observeAllVisibleEntriesMap(),
            settingsRepository.settingsFlow,
            settingsRepository.nsfwRevealedIdsFlow
        ) { entries, settings, revealed -> Triple(entries, settings, revealed) }
    ) { mode, (filters, sort), paged, autocomplete, (entries, settings, revealed) ->
        SearchUiState(
            mode = mode,
            filters = filters,
            sort = sort,
            results = paged.items,
            isLoading = paged.isLoading,
            isLoadingMore = paged.isLoadingMore,
            canLoadMore = paged.canLoadMore,
            currentPage = paged.currentPage,
            error = paged.error,
            autocomplete = autocomplete,
            collectionEntries = entries,
            settings = settings,
            nsfwRevealedIds = revealed
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SearchUiState()
    )

    init {
        observeDebouncedSearchPipeline()
        observeAutocompleteStream(
            queryFlow = tagQueryFlow,
            fetcher = vnRepository::searchTags,
            onLoading = { loading ->
                autocompleteFlow.update { it.copy(isSearchingTags = loading) }
            },
            onResults = { list ->
                autocompleteFlow.update { it.copy(tagSuggestions = list, isSearchingTags = false) }
            }
        )
        observeAutocompleteStream(
            queryFlow = traitQueryFlow,
            fetcher = vnRepository::searchTraits,
            onLoading = { loading ->
                autocompleteFlow.update { it.copy(isSearchingTraits = loading) }
            },
            onResults = { list ->
                autocompleteFlow.update { it.copy(traitSuggestions = list, isSearchingTraits = false) }
            }
        )
        observeAutocompleteStream(
            queryFlow = developerQueryFlow,
            fetcher = vnRepository::searchProducers,
            onLoading = { loading ->
                autocompleteFlow.update { it.copy(isSearchingDevelopers = loading) }
            },
            onResults = { list ->
                autocompleteFlow.update { it.copy(developerSuggestions = list, isSearchingDevelopers = false) }
            }
        )
    }

    fun onAction(action: SearchAction) {
        when (action) {
            is SearchAction.SelectMode -> {
                savedStateHandle[KEY_SEARCH_MODE] = action.mode.name
                modeFlow.value = action.mode
            }
            is SearchAction.UpdateQuery -> {
                savedStateHandle[KEY_SEARCH_QUERY] = action.query
                filtersAndSortFlow.update { (f, s) -> f.copy(searchQuery = action.query) to s }
            }
            SearchAction.ExecuteSearchNow,
            SearchAction.Retry -> {
                val (f, s) = filtersAndSortFlow.value
                executeFreshSearchNow(f, s)
            }
            is SearchAction.ToggleQuickGenre -> {
                filtersAndSortFlow.update { (current, sort) ->
                    SearchFilterMutations.toggleQuickGenre(current, action.genre) to sort
                }
            }
            is SearchAction.ApplyFiltersAndSort -> {
                savedStateHandle[KEY_SEARCH_QUERY] = action.filters.searchQuery
                filtersAndSortFlow.value = action.filters to action.sort
            }
            is SearchAction.RemoveChip -> {
                filtersAndSortFlow.update { (current, sort) ->
                    SearchFilterMutations.removeChip(current, action.chip) to sort
                }
            }
            SearchAction.ClearAllFilters -> {
                savedStateHandle[KEY_SEARCH_QUERY] = ""
                filtersAndSortFlow.value = VnSearchFilters() to SortOption.DEFAULT_REMOTE
            }
            is SearchAction.ApplyHomeSectionPreset -> applyHomeSection(action.section)
            is SearchAction.ApplySingleTagFilter -> {
                savedStateHandle[KEY_SEARCH_MODE] = SearchMode.VISUAL_NOVELS.name
                modeFlow.value = SearchMode.VISUAL_NOVELS
                filtersAndSortFlow.value = VnSearchFilters(tags = listOf(action.tag)) to SortOption.DEFAULT_REMOTE
            }
            is SearchAction.ApplySingleTraitFilter -> {
                savedStateHandle[KEY_SEARCH_MODE] = SearchMode.VISUAL_NOVELS.name
                modeFlow.value = SearchMode.VISUAL_NOVELS
                filtersAndSortFlow.value = VnSearchFilters(characterTraits = listOf(action.trait)) to SortOption.DEFAULT_REMOTE
            }
            is SearchAction.UpdateTagAutocompleteQuery -> {
                tagQueryFlow.value = action.query
                autocompleteFlow.update { it.copy(tagQuery = action.query) }
            }
            is SearchAction.UpdateTraitAutocompleteQuery -> {
                traitQueryFlow.value = action.query
                autocompleteFlow.update { it.copy(traitQuery = action.query) }
            }
            is SearchAction.UpdateDeveloperAutocompleteQuery -> {
                developerQueryFlow.value = action.query
                autocompleteFlow.update { it.copy(developerQuery = action.query) }
            }
            SearchAction.LoadNextPage -> loadNextPage()
            is SearchAction.ToggleNsfwReveal -> settingsRepository.toggleNsfwReveal(action.vnId)
            is SearchAction.OpenVnDetail -> Unit
        }
    }

    private fun observeDebouncedSearchPipeline() {
        viewModelScope.launch {
            filtersAndSortFlow
                .debounce(SEARCH_DEBOUNCE_MS)
                .distinctUntilChanged()
                .collectLatest { (filters, sort) ->
                    executeFreshSearchSuspend(filters, sort)
                }
        }
    }

    private fun <T> observeAutocompleteStream(
        queryFlow: MutableStateFlow<String>,
        fetcher: suspend (String) -> AppResult<List<T>>,
        onLoading: (Boolean) -> Unit,
        onResults: (List<T>) -> Unit
    ) {
        viewModelScope.launch {
            queryFlow
                .debounce(AUTOCOMPLETE_DEBOUNCE_MS)
                .distinctUntilChanged()
                .collectLatest { raw ->
                    val trimmed = raw.trim()
                    if (trimmed.length < 2) {
                        onResults(emptyList())
                        return@collectLatest
                    }
                    onLoading(true)
                    when (val res = fetcher(trimmed)) {
                        is AppResult.Success -> onResults(res.data)
                        is AppResult.Failure -> onResults(emptyList())
                    }
                }
        }
    }

    private fun executeFreshSearchNow(filters: VnSearchFilters, sort: SortOption) {
        activeSearchJob?.cancel()
        activeSearchJob = viewModelScope.launch {
            executeFreshSearchSuspend(filters, sort)
        }
    }

    private suspend fun executeFreshSearchSuspend(filters: VnSearchFilters, sort: SortOption) {
        paginationJob?.cancel()
        val epoch = searchEpoch.incrementAndGet()
        pagedState.update {
            it.copy(isLoading = true, isLoadingMore = false, error = null, currentPage = 1)
        }
        when (
            val res = vnRepository.searchVns(
                VnQuerySpec(
                    filters = filters,
                    sort = sort,
                    page = 1,
                    maxResults = PAGE_SIZE
                )
            )
        ) {
            is AppResult.Success -> {
                if (searchEpoch.get() == epoch) {
                    pagedState.value = PagedVnState(
                        items = res.data.items,
                        isLoading = false,
                        isLoadingMore = false,
                        canLoadMore = res.data.hasMore,
                        currentPage = 1,
                        error = null
                    )
                }
            }
            is AppResult.Failure -> {
                if (searchEpoch.get() == epoch) {
                    pagedState.update {
                        it.copy(isLoading = false, isLoadingMore = false, error = res.error.toUiText())
                    }
                }
            }
        }
    }

    private fun loadNextPage() {
        val snap = pagedState.value
        if (snap.isLoading || snap.isLoadingMore || !snap.canLoadMore) return
        val expectedEpoch = searchEpoch.get()
        val nextPage = snap.currentPage + 1
        val (currentFilters, currentSort) = filtersAndSortFlow.value

        paginationJob?.cancel()
        paginationJob = viewModelScope.launch {
            pagedState.update { it.copy(isLoadingMore = true, error = null) }
            when (
                val res = vnRepository.searchVns(
                    VnQuerySpec(
                        filters = currentFilters,
                        sort = currentSort,
                        page = nextPage,
                        maxResults = PAGE_SIZE
                    )
                )
            ) {
                is AppResult.Success -> {
                    if (searchEpoch.get() == expectedEpoch) {
                        pagedState.update { prev ->
                            val combined = (prev.items + res.data.items).distinctBy { it.id }
                            prev.copy(
                                items = combined,
                                isLoadingMore = false,
                                canLoadMore = res.data.hasMore,
                                currentPage = nextPage
                            )
                        }
                    }
                }
                is AppResult.Failure -> {
                    if (searchEpoch.get() == expectedEpoch) {
                        pagedState.update {
                            it.copy(isLoadingMore = false, error = res.error.toUiText())
                        }
                    }
                }
            }
        }
    }

    private fun applyHomeSection(section: HomeSection) {
        savedStateHandle[KEY_SEARCH_MODE] = SearchMode.VISUAL_NOVELS.name
        modeFlow.value = SearchMode.VISUAL_NOVELS
        filtersAndSortFlow.value = SearchFilterMutations.filtersAndSortForHomeSection(section)
    }

    companion object {
        private const val KEY_SEARCH_MODE = "search_mode"
        private const val KEY_SEARCH_QUERY = "search_query"
        private const val PAGE_SIZE = 24
        const val SEARCH_DEBOUNCE_MS = 320L
        private const val AUTOCOMPLETE_DEBOUNCE_MS = 280L

        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SearchViewModel(
                    vnRepository = container.vnRepository,
                    collectionRepository = container.collectionRepository,
                    settingsRepository = container.settingsRepository,
                    savedStateHandle = createSavedStateHandle()
                )
            }
        }
    }
}
