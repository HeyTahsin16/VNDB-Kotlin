package com.vndblite.dcj.ui.screens.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vndblite.dcj.AppContainer
import com.vndblite.dcj.R
import com.vndblite.dcj.core.AppResult
import com.vndblite.dcj.core.UiText
import com.vndblite.dcj.core.toUiText
import com.vndblite.dcj.domain.model.Quote
import com.vndblite.dcj.domain.model.Review
import com.vndblite.dcj.domain.model.VnSummary
import com.vndblite.dcj.domain.repository.CollectionRepository
import com.vndblite.dcj.domain.repository.SettingsRepository
import com.vndblite.dcj.domain.repository.VnRepository
import com.vndblite.dcj.sync.SyncService
import com.vndblite.dcj.ui.components.CollectionEntryFormPayload
import com.vndblite.dcj.ui.components.SectionUiState
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Navigation-entry-scoped ViewModel for Visual Novel Details (A3, A5, F5, N2, N5, N6).
 *
 * Each section (`main`, `characters`, `quotes`, `reviews`) has an independent [SectionUiState]
 * and retry action so network failures never masquerade as empty lists or block retries (F5).
 */
class VnDetailViewModel(
    private val vnId: String,
    private val initialTitle: String,
    private val vnRepository: VnRepository,
    private val collectionRepository: CollectionRepository,
    private val settingsRepository: SettingsRepository,
    private val syncService: SyncService,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val selectedTabFlow = MutableStateFlow(
        VnDetailTab.entries.firstOrNull { it.name == savedStateHandle[KEY_SELECTED_TAB] }
            ?: VnDetailTab.GENERAL
    )
    private val tagSpoilerLevelFlow = MutableStateFlow(
        savedStateHandle[KEY_SPOILER_LEVEL] ?: 0
    )

    private val isRefreshingMainFlow = MutableStateFlow(false)
    private val mainErrorFlow = MutableStateFlow<UiText?>(null)

    private val charactersStateFlow =
        MutableStateFlow<SectionUiState<CharactersTabPayload>>(SectionUiState.Loading)
    private val quotesStateFlow =
        MutableStateFlow<SectionUiState<List<Quote>>>(SectionUiState.Loading)
    private val reviewsStateFlow =
        MutableStateFlow<SectionUiState<List<Review>>>(SectionUiState.Loading)

    private val cachedVnPairFlow = vnRepository.observeVnSummaryAndDetails(vnId)

    private val sectionsQuadFlow = combine(
        isRefreshingMainFlow,
        mainErrorFlow,
        charactersStateFlow,
        combine(quotesStateFlow, reviewsStateFlow) { q, r -> q to r }
    ) { refreshing, mainErr, chars, (quotes, reviews) ->
        SectionBundle(refreshing, mainErr, chars, quotes, reviews)
    }

    private data class SectionBundle(
        val isRefreshingMain: Boolean,
        val mainError: UiText?,
        val charactersState: SectionUiState<CharactersTabPayload>,
        val quotesState: SectionUiState<List<Quote>>,
        val reviewsState: SectionUiState<List<Review>>
    )

    private val userContextFlow = combine(
        collectionRepository.observeEntryById(vnId),
        collectionRepository.observeAllVisibleEntriesMap(),
        syncService.isAccountSyncedFlow,
        settingsRepository.settingsFlow,
        settingsRepository.nsfwRevealedIdsFlow
    ) { entry, allEntries, synced, settings, revealed ->
        UserContextBundle(entry, allEntries, synced, settings, revealed)
    }

    private data class UserContextBundle(
        val entry: com.vndblite.dcj.domain.model.CollectionEntry?,
        val allEntries: Map<String, com.vndblite.dcj.domain.model.CollectionEntry>,
        val isSynced: Boolean,
        val settings: com.vndblite.dcj.domain.model.AppSettings,
        val nsfwRevealedIds: Set<String>
    )

    val uiState: StateFlow<VnDetailUiState> = combine(
        cachedVnPairFlow,
        sectionsQuadFlow,
        userContextFlow,
        selectedTabFlow,
        tagSpoilerLevelFlow
    ) { (summary, details), sections, userCtx, tab, spoilerLevel ->
        VnDetailUiState(
            vnId = vnId,
            initialTitle = initialTitle,
            summary = summary,
            details = details,
            isRefreshingMain = sections.isRefreshingMain,
            mainError = sections.mainError,
            charactersState = sections.charactersState,
            quotesState = sections.quotesState,
            reviewsState = sections.reviewsState,
            collectionEntry = userCtx.entry,
            relatedCollectionEntries = userCtx.allEntries,
            isAccountSynced = userCtx.isSynced,
            settings = userCtx.settings,
            nsfwRevealedIds = userCtx.nsfwRevealedIds,
            selectedTab = tab,
            maxTagSpoilerLevel = spoilerLevel
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = VnDetailUiState(vnId = vnId, initialTitle = initialTitle)
    )

    init {
        loadAllSections()
    }

    fun onAction(action: VnDetailAction) {
        when (action) {
            is VnDetailAction.SelectTab -> {
                savedStateHandle[KEY_SELECTED_TAB] = action.tab.name
                selectedTabFlow.value = action.tab
            }
            is VnDetailAction.SetTagSpoilerLevel -> {
                savedStateHandle[KEY_SPOILER_LEVEL] = action.level
                tagSpoilerLevelFlow.value = action.level
            }
            VnDetailAction.RetryMainDetails -> viewModelScope.launch { refreshMainDetails() }
            VnDetailAction.RetryCharacters -> viewModelScope.launch { refreshCharacters() }
            VnDetailAction.RetryQuotes -> viewModelScope.launch { refreshQuotes() }
            VnDetailAction.RetryReviews -> viewModelScope.launch { refreshReviews() }
            is VnDetailAction.SaveCollectionEntry -> saveCollectionEntry(action.payload)
            VnDetailAction.RemoveCollectionEntry -> removeCollectionEntry()
            is VnDetailAction.ToggleNsfwReveal -> settingsRepository.toggleNsfwReveal(action.id)
            VnDetailAction.Back,
            is VnDetailAction.OpenRelatedVn,
            is VnDetailAction.FilterByTag,
            is VnDetailAction.FilterByTrait -> Unit
        }
    }

    private fun loadAllSections() {
        viewModelScope.launch {
            val mainDef = async { refreshMainDetails() }
            val charsDef = async { refreshCharacters() }
            val quotesDef = async { refreshQuotes() }
            val reviewsDef = async { refreshReviews() }
            mainDef.await()
            charsDef.await()
            quotesDef.await()
            reviewsDef.await()
        }
    }

    private suspend fun refreshMainDetails() {
        isRefreshingMainFlow.value = true
        mainErrorFlow.value = null
        when (val res = vnRepository.loadVnFullDetails(vnId, forceRefresh = false)) {
            is AppResult.Success -> mainErrorFlow.value = null
            is AppResult.Failure -> mainErrorFlow.value = res.error.toUiText()
        }
        isRefreshingMainFlow.value = false
    }

    private suspend fun refreshCharacters() {
        charactersStateFlow.value = SectionUiState.Loading
        when (val res = vnRepository.fetchCharactersForVn(vnId)) {
            is AppResult.Success -> {
                if (res.data.items.isEmpty()) {
                    charactersStateFlow.value = SectionUiState.Empty
                } else {
                    charactersStateFlow.value = SectionUiState.Content(
                        CharactersTabPayload(
                            characters = res.data.items,
                            isTruncated = res.data.isTruncated
                        )
                    )
                }
            }
            is AppResult.Failure -> {
                charactersStateFlow.value = SectionUiState.Error(res.error.toUiText())
            }
        }
    }

    private suspend fun refreshQuotes() {
        quotesStateFlow.value = SectionUiState.Loading
        when (val res = vnRepository.fetchQuotesForVn(vnId)) {
            is AppResult.Success -> {
                quotesStateFlow.value = if (res.data.isEmpty()) {
                    SectionUiState.Empty
                } else {
                    SectionUiState.Content(res.data)
                }
            }
            is AppResult.Failure -> {
                quotesStateFlow.value = SectionUiState.Error(res.error.toUiText())
            }
        }
    }

    private suspend fun refreshReviews() {
        reviewsStateFlow.value = SectionUiState.Loading
        when (val res = vnRepository.fetchReviewsForVn(vnId)) {
            is AppResult.Success -> {
                reviewsStateFlow.value = if (res.data.items.isEmpty()) {
                    SectionUiState.Empty
                } else {
                    SectionUiState.Content(res.data.items)
                }
            }
            is AppResult.Failure -> {
                reviewsStateFlow.value = SectionUiState.Error(
                    UiText.StringResource(R.string.detail_reviews_unavailable)
                )
            }
        }
    }

    private fun saveCollectionEntry(payload: CollectionEntryFormPayload) {
        viewModelScope.launch {
            val summary = uiState.value.summary ?: VnSummary(
                id = vnId,
                title = uiState.value.displayTitle
            )
            collectionRepository.saveOrUpdateEntry(
                vn = summary,
                status = payload.status,
                vote = payload.vote,
                notes = payload.notes,
                startedDate = payload.startedDate,
                finishedDate = payload.finishedDate
            )
        }
    }

    private fun removeCollectionEntry() {
        viewModelScope.launch {
            collectionRepository.removeEntry(vnId)
        }
    }

    companion object {
        private const val KEY_SELECTED_TAB = "detail_selected_tab"
        private const val KEY_SPOILER_LEVEL = "detail_spoiler_level"

        fun factory(
            vnId: String,
            initialTitle: String,
            container: AppContainer
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                VnDetailViewModel(
                    vnId = vnId,
                    initialTitle = initialTitle,
                    vnRepository = container.vnRepository,
                    collectionRepository = container.collectionRepository,
                    settingsRepository = container.settingsRepository,
                    syncService = container.syncService,
                    savedStateHandle = createSavedStateHandle()
                )
            }
        }
    }
}
