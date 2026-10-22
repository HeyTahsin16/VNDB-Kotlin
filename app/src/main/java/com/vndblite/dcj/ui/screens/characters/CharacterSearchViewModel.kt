package com.vndblite.dcj.ui.screens.characters

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.vndblite.dcj.core.AppResult
import com.vndblite.dcj.domain.model.CharacterSearchFilters
import com.vndblite.dcj.domain.model.Trait
import com.vndblite.dcj.domain.repository.SettingsRepository
import com.vndblite.dcj.domain.repository.VnRepository
import com.vndblite.dcj.ui.navigation.CharacterSearchRoute
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CharacterSearchViewModel(
    private val vnRepository: VnRepository,
    private val settingsRepository: SettingsRepository,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
    initialRoute: CharacterSearchRoute = CharacterSearchRoute(),
    private val filterDebounceMs: Long = 350L
) : ViewModel() {

    private val initialFilters = buildInitialFilters(initialRoute, savedStateHandle)

    private val _uiState = MutableStateFlow(
        CharacterSearchUiState(
            filters = initialFilters,
            revealedNsfwIds = savedStateHandle.get<ArrayList<String>>(KEY_REVEALED_NSFW)?.toSet().orEmpty()
        )
    )
    val uiState: StateFlow<CharacterSearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var debounceJob: Job? = null
    private var traitSuggestionJob: Job? = null

    init {
        observeSettings()
        executeSearch(page = 1, append = false)
    }

    fun onAction(action: CharacterSearchAction) {
        when (action) {
            is CharacterSearchAction.UpdateQuery -> {
                savedStateHandle[KEY_CHAR_QUERY] = action.query
                _uiState.update { it.copy(filters = it.filters.copy(searchQuery = action.query)) }
                scheduleDebouncedSearch()
            }
            CharacterSearchAction.SubmitSearch -> {
                debounceJob?.cancel()
                executeSearch(page = 1, append = false)
            }
            CharacterSearchAction.Retry -> executeSearch(page = 1, append = false)
            CharacterSearchAction.LoadNextPage -> loadNextPage()
            is CharacterSearchAction.ToggleTrait -> mutateFilters { f ->
                val updated = if (f.traits.any { it.id == action.trait.id }) {
                    f.traits.filterNot { it.id == action.trait.id }
                } else {
                    f.traits + action.trait
                }
                f.copy(traits = updated)
            }
            is CharacterSearchAction.ToggleRole -> mutateFilters { f ->
                val updated = if (action.role in f.roles) f.roles - action.role else f.roles + action.role
                f.copy(roles = updated)
            }
            is CharacterSearchAction.ToggleSex -> mutateFilters { f ->
                val updated = if (action.sex in f.sexes) f.sexes - action.sex else f.sexes + action.sex
                f.copy(sexes = updated)
            }
            is CharacterSearchAction.ToggleTraitAndOr -> mutateFilters { f ->
                f.copy(traitAndOr = action.andOr)
            }
            CharacterSearchAction.ResetFilters -> {
                val query = _uiState.value.filters.searchQuery
                _uiState.update { it.copy(filters = CharacterSearchFilters(searchQuery = query)) }
                debounceJob?.cancel()
                executeSearch(page = 1, append = false)
            }
            is CharacterSearchAction.QueryTraitSuggestions -> fetchTraitSuggestions(action.query)
            is CharacterSearchAction.SelectCharacter -> {
                _uiState.update { it.copy(selectedCharacter = action.character) }
            }
            CharacterSearchAction.DismissCharacterBio -> {
                _uiState.update { it.copy(selectedCharacter = null) }
            }
            is CharacterSearchAction.ToggleNsfwReveal -> toggleNsfwReveal(action.id)
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }

    private fun mutateFilters(transform: (CharacterSearchFilters) -> CharacterSearchFilters) {
        _uiState.update { it.copy(filters = transform(it.filters)) }
        scheduleDebouncedSearch()
    }

    private fun scheduleDebouncedSearch() {
        debounceJob?.cancel()
        debounceJob = viewModelScope.launch {
            if (filterDebounceMs > 0L) delay(filterDebounceMs)
            executeSearch(page = 1, append = false)
        }
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
            _uiState.update {
                if (append) {
                    it.copy(isAppending = true, error = null)
                } else {
                    it.copy(isLoading = true, isAppending = false, error = null)
                }
            }
            when (val result = vnRepository.searchCharacters(
                filters = snapshot.filters,
                page = page,
                pageSize = PAGE_SIZE
            )) {
                is AppResult.Success -> {
                    val paginated = result.data
                    _uiState.update { cur ->
                        val merged = if (append) {
                            (cur.results + paginated.items).distinctBy { it.id }
                        } else {
                            paginated.items
                        }
                        cur.copy(
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
                    _uiState.update { cur ->
                        cur.copy(
                            isLoading = false,
                            isAppending = false,
                            error = result.error
                        )
                    }
                }
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
            if (filterDebounceMs > 0L) delay(200L)
            when (val res = vnRepository.searchTraits(query.trim(), pageSize = 15)) {
                is AppResult.Success -> _uiState.update { it.copy(traitSuggestions = res.data) }
                is AppResult.Failure -> _uiState.update { it.copy(traitSuggestions = emptyList()) }
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
        private const val KEY_CHAR_QUERY = "char_search_query"
        private const val KEY_REVEALED_NSFW = "char_search_revealed_nsfw_ids"

        private fun buildInitialFilters(
            route: CharacterSearchRoute,
            savedStateHandle: SavedStateHandle
        ): CharacterSearchFilters {
            val savedQuery = savedStateHandle.get<String>(KEY_CHAR_QUERY)
            val query = savedQuery ?: route.initialQuery
            val initialTraits = if (!route.initialTraitId.isNullOrBlank()) {
                listOf(
                    Trait(
                        id = route.initialTraitId,
                        name = route.initialTraitName ?: route.initialTraitId,
                        groupName = route.initialTraitGroupName
                    )
                )
            } else {
                emptyList()
            }
            return CharacterSearchFilters(
                searchQuery = query,
                traits = initialTraits
            )
        }

        fun factory(
            vnRepository: VnRepository,
            settingsRepository: SettingsRepository,
            initialRoute: CharacterSearchRoute = CharacterSearchRoute()
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return CharacterSearchViewModel(
                    vnRepository = vnRepository,
                    settingsRepository = settingsRepository,
                    savedStateHandle = extras.createSavedStateHandle(),
                    initialRoute = initialRoute
                ) as T
            }
        }
    }
}
