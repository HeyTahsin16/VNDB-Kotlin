package com.vndblite.dcj.ui.screens.characters

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vndblite.dcj.AppContainer
import com.vndblite.dcj.core.AppResult
import com.vndblite.dcj.core.toUiText
import com.vndblite.dcj.domain.model.Character
import com.vndblite.dcj.domain.model.CharacterRole
import com.vndblite.dcj.domain.model.CharacterSearchFilters
import com.vndblite.dcj.domain.model.CharacterSex
import com.vndblite.dcj.domain.model.PopularTraitPreset
import com.vndblite.dcj.domain.model.Trait
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
 * Dedicated ViewModel for Character Trait Explorer with a single debounced `collectLatest`
 * search pipeline so rapid chip taps or typing cancel stale in-flight requests (A3, A8, F4, N4).
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class CharacterSearchViewModel(
    private val vnRepository: VnRepository,
    private val settingsRepository: SettingsRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val filtersFlow = MutableStateFlow(
        CharacterSearchFilters(
            searchQuery = savedStateHandle[KEY_CHAR_QUERY] ?: "",
            traits = listOf(PopularTraitPreset.TSUNDERE.toTrait())
        )
    )
    private val traitQueryFlow = MutableStateFlow(savedStateHandle[KEY_TRAIT_QUERY] ?: "")
    private val traitSuggestionsFlow = MutableStateFlow<List<Trait>>(emptyList())
    private val isSearchingTraitsFlow = MutableStateFlow(false)

    private data class PagedResultState(
        val characters: List<Character> = emptyList(),
        val isLoading: Boolean = false,
        val isLoadingMore: Boolean = false,
        val canLoadMore: Boolean = false,
        val currentPage: Int = 1,
        val error: com.vndblite.dcj.core.UiText? = null
    )

    private val pagedState = MutableStateFlow(PagedResultState())
    private val searchEpoch = AtomicLong(0L)
    private var activeSearchJob: Job? = null
    private var paginationJob: Job? = null

    val uiState: StateFlow<CharacterSearchUiState> = combine(
        filtersFlow,
        traitQueryFlow,
        combine(traitSuggestionsFlow, isSearchingTraitsFlow) { list, loading -> list to loading },
        pagedState,
        combine(settingsRepository.settingsFlow, settingsRepository.nsfwRevealedIdsFlow) { s, r -> s to r }
    ) { filters, traitQuery, (suggestions, searchingTraits), paged, (settings, revealedIds) ->
        CharacterSearchUiState(
            filters = filters,
            traitQuery = traitQuery,
            traitSuggestions = suggestions,
            isSearchingTraits = searchingTraits,
            characters = paged.characters,
            isLoading = paged.isLoading,
            isLoadingMore = paged.isLoadingMore,
            canLoadMore = paged.canLoadMore,
            currentPage = paged.currentPage,
            error = paged.error,
            showCoverCensor = settings.showCoverCensor,
            nsfwRevealedIds = revealedIds
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CharacterSearchUiState()
    )

    init {
        observeDebouncedCharacterFilters()
        observeDebouncedTraitAutocomplete()
    }

    fun applyPresetTrait(trait: Trait?) {
        if (trait != null) {
            filtersFlow.update { current ->
                current.copy(traits = listOf(trait))
            }
        }
    }

    fun onAction(action: CharacterSearchAction) {
        when (action) {
            is CharacterSearchAction.UpdateNameQuery -> {
                savedStateHandle[KEY_CHAR_QUERY] = action.query
                filtersFlow.update { it.copy(searchQuery = action.query) }
            }
            is CharacterSearchAction.UpdateTraitAutocompleteQuery -> {
                savedStateHandle[KEY_TRAIT_QUERY] = action.query
                traitQueryFlow.value = action.query
            }
            is CharacterSearchAction.ToggleTrait -> toggleTrait(action.trait)
            is CharacterSearchAction.RemoveTrait -> removeTrait(action.traitId)
            CharacterSearchAction.ToggleTraitAndOr -> {
                filtersFlow.update { it.copy(traitAndOr = it.traitAndOr.toggled()) }
            }
            is CharacterSearchAction.ToggleRole -> toggleRole(action.role)
            CharacterSearchAction.ToggleRoleAndOr -> {
                filtersFlow.update { it.copy(roleAndOr = it.roleAndOr.toggled()) }
            }
            is CharacterSearchAction.ToggleSex -> toggleSex(action.sex)
            CharacterSearchAction.ToggleSexAndOr -> {
                filtersFlow.update { it.copy(sexAndOr = it.sexAndOr.toggled()) }
            }
            CharacterSearchAction.ClearAllFilters -> clearFilters()
            CharacterSearchAction.ExecuteSearchNow,
            CharacterSearchAction.Retry -> executeFreshSearch(filtersFlow.value)
            CharacterSearchAction.LoadNextPage -> loadNextPage()
            is CharacterSearchAction.ToggleNsfwReveal -> {
                settingsRepository.toggleNsfwReveal(action.id)
            }
            is CharacterSearchAction.FilterVnsByTrait,
            is CharacterSearchAction.OpenVnDetail -> Unit
        }
    }

    private fun observeDebouncedCharacterFilters() {
        viewModelScope.launch {
            filtersFlow
                .debounce(SEARCH_DEBOUNCE_MS)
                .distinctUntilChanged()
                .collectLatest { filters ->
                    executeFreshSearchSuspend(filters)
                }
        }
    }

    private fun observeDebouncedTraitAutocomplete() {
        viewModelScope.launch {
            traitQueryFlow
                .debounce(AUTOCOMPLETE_DEBOUNCE_MS)
                .distinctUntilChanged()
                .collectLatest { rawQuery ->
                    val trimmed = rawQuery.trim()
                    if (trimmed.length < 2) {
                        traitSuggestionsFlow.value = emptyList()
                        isSearchingTraitsFlow.value = false
                        return@collectLatest
                    }
                    isSearchingTraitsFlow.value = true
                    when (val res = vnRepository.searchTraits(trimmed)) {
                        is AppResult.Success -> traitSuggestionsFlow.value = res.data
                        is AppResult.Failure -> traitSuggestionsFlow.value = emptyList()
                    }
                    isSearchingTraitsFlow.value = false
                }
        }
    }

    private fun executeFreshSearch(filters: CharacterSearchFilters) {
        activeSearchJob?.cancel()
        activeSearchJob = viewModelScope.launch {
            executeFreshSearchSuspend(filters)
        }
    }

    private suspend fun executeFreshSearchSuspend(filters: CharacterSearchFilters) {
        paginationJob?.cancel()
        val epoch = searchEpoch.incrementAndGet()
        pagedState.update {
            it.copy(isLoading = true, isLoadingMore = false, error = null, currentPage = 1)
        }
        when (val result = vnRepository.searchCharacters(filters = filters, page = 1, maxResults = PAGE_SIZE)) {
            is AppResult.Success -> {
                if (searchEpoch.get() == epoch) {
                    pagedState.value = PagedResultState(
                        characters = result.data.items,
                        isLoading = false,
                        isLoadingMore = false,
                        canLoadMore = result.data.hasMore,
                        currentPage = 1,
                        error = null
                    )
                }
            }
            is AppResult.Failure -> {
                if (searchEpoch.get() == epoch) {
                    pagedState.update {
                        it.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            error = result.error.toUiText()
                        )
                    }
                }
            }
        }
    }

    private fun loadNextPage() {
        val snapshot = pagedState.value
        if (snapshot.isLoading || snapshot.isLoadingMore || !snapshot.canLoadMore) return
        val expectedEpoch = searchEpoch.get()
        val nextPage = snapshot.currentPage + 1
        val currentFilters = filtersFlow.value

        paginationJob?.cancel()
        paginationJob = viewModelScope.launch {
            pagedState.update { it.copy(isLoadingMore = true, error = null) }
            when (
                val res = vnRepository.searchCharacters(
                    filters = currentFilters,
                    page = nextPage,
                    maxResults = PAGE_SIZE
                )
            ) {
                is AppResult.Success -> {
                    if (searchEpoch.get() == expectedEpoch) {
                        pagedState.update { prev ->
                            val combined = (prev.characters + res.data.items).distinctBy { it.id }
                            prev.copy(
                                characters = combined,
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

    private fun toggleTrait(trait: Trait) {
        filtersFlow.update { current ->
            val exists = current.traits.any { it.id == trait.id }
            val next = if (exists) {
                current.traits.filterNot { it.id == trait.id }
            } else {
                current.traits + trait
            }
            current.copy(traits = next)
        }
        traitQueryFlow.value = ""
        traitSuggestionsFlow.value = emptyList()
    }

    private fun removeTrait(traitId: String) {
        filtersFlow.update { current ->
            current.copy(traits = current.traits.filterNot { it.id == traitId })
        }
    }

    private fun toggleRole(role: CharacterRole) {
        filtersFlow.update { current ->
            val next = if (role in current.roles) current.roles - role else current.roles + role
            current.copy(roles = next)
        }
    }

    private fun toggleSex(sex: CharacterSex) {
        filtersFlow.update { current ->
            val next = if (sex in current.sexes) current.sexes - sex else current.sexes + sex
            current.copy(sexes = next)
        }
    }

    private fun clearFilters() {
        savedStateHandle[KEY_CHAR_QUERY] = ""
        savedStateHandle[KEY_TRAIT_QUERY] = ""
        traitQueryFlow.value = ""
        traitSuggestionsFlow.value = emptyList()
        filtersFlow.value = CharacterSearchFilters()
    }

    companion object {
        private const val KEY_CHAR_QUERY = "char_search_query"
        private const val KEY_TRAIT_QUERY = "char_trait_query"
        private const val PAGE_SIZE = 24
        const val SEARCH_DEBOUNCE_MS = 320L
        private const val AUTOCOMPLETE_DEBOUNCE_MS = 280L

        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                CharacterSearchViewModel(
                    vnRepository = container.vnRepository,
                    settingsRepository = container.settingsRepository,
                    savedStateHandle = createSavedStateHandle()
                )
            }
        }
    }
}
