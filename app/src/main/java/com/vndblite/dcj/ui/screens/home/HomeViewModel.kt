package com.vndblite.dcj.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vndblite.dcj.AppContainer
import com.vndblite.dcj.core.AppResult
import com.vndblite.dcj.core.UiText
import com.vndblite.dcj.core.toUiText
import com.vndblite.dcj.domain.model.HomeSection
import com.vndblite.dcj.domain.model.Quote
import com.vndblite.dcj.domain.model.VnSummary
import com.vndblite.dcj.domain.repository.CollectionRepository
import com.vndblite.dcj.domain.repository.SettingsRepository
import com.vndblite.dcj.domain.repository.VnRepository
import com.vndblite.dcj.ui.components.SectionUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

private data class HomeTransientState(
    val isRefreshing: Boolean = false,
    val loadingSections: Set<HomeSection> = REMOTE_SECTIONS.toSet(),
    val sectionErrors: Map<HomeSection, UiText> = emptyMap(),
    val isLoadingQuotes: Boolean = true,
    val quotesError: UiText? = null,
    val quoteIndex: Int = 0,
    val isLoadingStats: Boolean = true,
    val statsError: UiText? = null
)

private val REMOTE_SECTIONS = listOf(
    HomeSection.TOP_RATED,
    HomeSection.TOP_VOTED,
    HomeSection.POPULAR_ONGOING,
    HomeSection.LATEST_RELEASES,
    HomeSection.UPCOMING_RELEASES
)

/**
 * ViewModel for the Home screen implementing stale-while-revalidate, per-section TTLs,
 * parallel section loading under the shared rate limiter, a single in-flight refresh guard,
 * deterministic collection ordering, and typed error states (A3, F2, F3, F5, F6, F10).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val vnRepository: VnRepository,
    private val collectionRepository: CollectionRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val refreshMutex = Mutex()
    private val transientFlow = MutableStateFlow(HomeTransientState())

    private val cachedSectionsFlow = combine(
        REMOTE_SECTIONS.map { section ->
            vnRepository.observeCachedHomeSection(section).map { section to it }
        }
    ) { pairs ->
        pairs.toMap()
    }

    private val recentCollectionFlow = settingsRepository.settingsFlow
        .map { it.maxPreviewItem }
        .flatMapLatest { limit -> collectionRepository.observeRecentCollection(limit) }

    private val collectionDataFlow = combine(
        recentCollectionFlow,
        collectionRepository.observeAllVisibleEntriesMap()
    ) { recentItems, entriesMap ->
        recentItems.map { it.vn } to entriesMap
    }

    private val cachedContentFlow = combine(
        cachedSectionsFlow,
        vnRepository.observeCachedQuotes(),
        vnRepository.observeCachedSiteStats()
    ) { sectionsMap, quotes, stats ->
        Triple(sectionsMap, quotes, stats)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        transientFlow,
        collectionDataFlow,
        cachedContentFlow,
        settingsRepository.settingsFlow,
        settingsRepository.nsfwRevealedIdsFlow
    ) { transient, (collectionCarousel, entriesMap), (cachedSections, cachedQuotes, cachedStats), settings, revealedIds ->
        val remoteSectionStates = REMOTE_SECTIONS.associateWith { section ->
            resolveSectionState(
                items = cachedSections[section].orEmpty().take(settings.maxPreviewItem),
                isLoading = section in transient.loadingSections,
                error = transient.sectionErrors[section]
            )
        }
        val quotesState = resolveQuotesState(
            quotes = cachedQuotes,
            isLoading = transient.isLoadingQuotes,
            error = transient.quotesError
        )
        val displayedQuote = if (cachedQuotes.isNotEmpty()) {
            cachedQuotes[transient.quoteIndex.mod(cachedQuotes.size)]
        } else {
            null
        }
        val statsState = when {
            cachedStats != null -> SectionUiState.Content(cachedStats)
            transient.isLoadingStats -> SectionUiState.Loading
            transient.statsError != null -> SectionUiState.Error(transient.statsError)
            else -> SectionUiState.Empty
        }

        HomeUiState(
            isRefreshing = transient.isRefreshing,
            collectionCarousel = collectionCarousel,
            collectionEntries = entriesMap,
            remoteSections = remoteSectionStates,
            quotesState = quotesState,
            displayedQuote = displayedQuote,
            statsState = statsState,
            settings = settings,
            nsfwRevealedIds = revealedIds
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    init {
        triggerRefreshAll(forceRefresh = false)
        hydrateUncachedCollectionItems()
    }

    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.RefreshAll -> triggerRefreshAll(forceRefresh = true)
            is HomeAction.RetrySection -> refreshSingleSection(action.section, forceRefresh = true)
            HomeAction.RetryQuotes -> refreshQuotes(forceRefresh = true)
            HomeAction.ShuffleQuote -> {
                transientFlow.update { it.copy(quoteIndex = it.quoteIndex + 1) }
            }
            HomeAction.RetryStats -> refreshStats(forceRefresh = true)
            is HomeAction.ToggleNsfwReveal -> settingsRepository.toggleNsfwReveal(action.vnId)
            is HomeAction.OpenVnDetail,
            is HomeAction.SeeMoreSection,
            HomeAction.GoToCollectionTab,
            is HomeAction.OpenTraitFinder,
            HomeAction.OpenRandomVn -> Unit
        }
    }

    private fun hydrateUncachedCollectionItems() {
        viewModelScope.launch {
            recentCollectionFlow.collect { items ->
                val ids = items.map { it.entry.vnId }
                if (ids.isNotEmpty()) {
                    collectionRepository.hydrateMissingSummaries(ids)
                }
            }
        }
    }

    private fun triggerRefreshAll(forceRefresh: Boolean) {
        viewModelScope.launch {
            if (!refreshMutex.tryLock()) return@launch
            try {
                transientFlow.update {
                    it.copy(
                        isRefreshing = forceRefresh,
                        loadingSections = REMOTE_SECTIONS.toSet(),
                        sectionErrors = emptyMap(),
                        isLoadingQuotes = true,
                        quotesError = null,
                        isLoadingStats = true,
                        statsError = null
                    )
                }
                val maxItems = settingsRepository.getSettingsSnapshot().maxPreviewItem
                coroutineScope {
                    val sectionDeferreds = REMOTE_SECTIONS.map { section ->
                        async { executeSectionRefresh(section, maxItems, forceRefresh) }
                    }
                    val quotesDeferred = async { executeQuotesRefresh(forceRefresh) }
                    val statsDeferred = async { executeStatsRefresh(forceRefresh) }
                    sectionDeferreds.awaitAll()
                    quotesDeferred.await()
                    statsDeferred.await()
                }
            } finally {
                transientFlow.update { it.copy(isRefreshing = false) }
                refreshMutex.unlock()
            }
        }
    }

    private fun refreshSingleSection(section: HomeSection, forceRefresh: Boolean) {
        viewModelScope.launch {
            val maxItems = settingsRepository.getSettingsSnapshot().maxPreviewItem
            executeSectionRefresh(section, maxItems, forceRefresh)
        }
    }

    private suspend fun executeSectionRefresh(
        section: HomeSection,
        maxItems: Int,
        forceRefresh: Boolean
    ) {
        transientFlow.update {
            it.copy(
                loadingSections = it.loadingSections + section,
                sectionErrors = it.sectionErrors - section
            )
        }
        when (val result = vnRepository.refreshHomeSection(section, maxItems, forceRefresh)) {
            is AppResult.Success -> {
                transientFlow.update {
                    it.copy(
                        loadingSections = it.loadingSections - section,
                        sectionErrors = it.sectionErrors - section
                    )
                }
            }
            is AppResult.Failure -> {
                transientFlow.update {
                    it.copy(
                        loadingSections = it.loadingSections - section,
                        sectionErrors = it.sectionErrors + (section to result.error.toUiText())
                    )
                }
            }
        }
    }

    private fun refreshQuotes(forceRefresh: Boolean) {
        viewModelScope.launch { executeQuotesRefresh(forceRefresh) }
    }

    private suspend fun executeQuotesRefresh(forceRefresh: Boolean) {
        transientFlow.update { it.copy(isLoadingQuotes = true, quotesError = null) }
        when (val result = vnRepository.refreshHomeQuotes(forceRefresh = forceRefresh)) {
            is AppResult.Success -> {
                transientFlow.update { it.copy(isLoadingQuotes = false, quotesError = null) }
            }
            is AppResult.Failure -> {
                transientFlow.update {
                    it.copy(isLoadingQuotes = false, quotesError = result.error.toUiText())
                }
            }
        }
    }

    private fun refreshStats(forceRefresh: Boolean) {
        viewModelScope.launch { executeStatsRefresh(forceRefresh) }
    }

    private suspend fun executeStatsRefresh(forceRefresh: Boolean) {
        transientFlow.update { it.copy(isLoadingStats = true, statsError = null) }
        when (val result = vnRepository.refreshSiteStats(forceRefresh = forceRefresh)) {
            is AppResult.Success -> {
                transientFlow.update { it.copy(isLoadingStats = false, statsError = null) }
            }
            is AppResult.Failure -> {
                transientFlow.update {
                    it.copy(isLoadingStats = false, statsError = result.error.toUiText())
                }
            }
        }
    }

    private fun resolveSectionState(
        items: List<VnSummary>,
        isLoading: Boolean,
        error: UiText?
    ): SectionUiState<List<VnSummary>> = when {
        items.isNotEmpty() -> SectionUiState.Content(items)
        isLoading -> SectionUiState.Loading
        error != null -> SectionUiState.Error(error)
        else -> SectionUiState.Empty
    }

    private fun resolveQuotesState(
        quotes: List<Quote>,
        isLoading: Boolean,
        error: UiText?
    ): SectionUiState<List<Quote>> = when {
        quotes.isNotEmpty() -> SectionUiState.Content(quotes)
        isLoading -> SectionUiState.Loading
        error != null -> SectionUiState.Error(error)
        else -> SectionUiState.Empty
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HomeViewModel(
                    vnRepository = container.vnRepository,
                    collectionRepository = container.collectionRepository,
                    settingsRepository = container.settingsRepository
                )
            }
        }
    }
}
