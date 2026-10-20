package com.vndblite.dcj.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vndblite.dcj.data.local.LocalStorageManager
import com.vndblite.dcj.data.model.*
import com.vndblite.dcj.data.remote.VndbApiService
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HomeUiState(
    val sections: Map<HomeSectionsCode, List<VnDataPhase01>> = emptyMap(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val stats: List<ChartStatItem> = DefaultChartStats.items(),
    val quotes: List<VnQuote> = emptyList(),
    val isRefreshingQuotes: Boolean = false
)

enum class SearchTabMode {
    VISUAL_NOVELS,
    CHARACTERS_AND_TRAITS
}

data class SearchUiState(
    val tabMode: SearchTabMode = SearchTabMode.VISUAL_NOVELS,
    val hasInitiatedVnSearch: Boolean = false,
    val filterData: FilterData = FilterData.DEFAULT_REMOTE,
    val sortData: SortData = SortData.DEFAULT_REMOTE,
    val vnResults: List<VnDataPhase01> = emptyList(),
    val vnPage: Int = 1,
    val vnHasMore: Boolean = false,
    val isVnLoading: Boolean = false,
    val vnError: String? = null,

    // Live suggestions inside filter sheet
    val tagSearchResults: List<VnTag> = emptyList(),
    val isSearchingTags: Boolean = false,
    val devSearchResults: List<Developer> = emptyList(),
    val isSearchingDevs: Boolean = false,
    val traitSearchResults: List<VnTrait> = emptyList(),
    val isSearchingTraits: Boolean = false,

    // Character & Trait Explorer state
    val charQuery: String = "",
    val selectedCharTraits: List<VnTrait> = emptyList(),
    val charTraitAndOr: String = "and",
    val selectedCharRoles: List<String> = emptyList(),
    val charRoleAndOr: String = "or",
    val selectedCharSexes: List<String> = emptyList(),
    val charSexAndOr: String = "or",
    val charOverallAndOr: String = "and",
    val charResults: List<VnCharacter> = emptyList(),
    val charPage: Int = 1,
    val charHasMore: Boolean = false,
    val isCharLoading: Boolean = false,
    val charError: String? = null,
    val hasInitiatedCharSearch: Boolean = false
)

data class CollectionUiState(
    val recordsByStatus: Map<String, List<VnDataPhase01>> = emptyMap(),
    val rawRecords: Map<String, VnRecord> = emptyMap(),
    val p2Cache: Map<String, VnDataPhase02> = emptyMap(),
    val localFilter: FilterData = FilterData.DEFAULT_LOCAL,
    val localSort: SortData = SortData.DEFAULT_LOCAL,
    val isSearchActive: Boolean = false,
    val selectedVnIds: List<String> = emptyList(),
    val isLoading: Boolean = false
)

data class SyncUiState(
    val userIdentity: UserIdentity? = null,
    val isSynced: Boolean = false,
    val isSyncing: Boolean = false,
    val statusMessage: String? = null,
    val isError: Boolean = false
)

data class VnDetailUiState(
    val vnId: String = "",
    val p1: VnDataPhase01? = null,
    val p2: VnDataPhase02? = null,
    val p3: VnDataPhase03? = null,
    val relatedVns: List<VnDataPhase01> = emptyList(),
    val characters: List<VnCharacter> = emptyList(),
    val quotes: List<VnQuote> = emptyList(),
    val reviews: List<VnReview> = emptyList(),
    val expandingReviewIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val isLoadingCharacters: Boolean = false,
    val isLoadingReviews: Boolean = false,
    val error: String? = null
)

data class RandomVnUiState(
    val vn: VnDataPhase01? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class VndbViewModel(application: Application) : AndroidViewModel(application) {

    val storage = LocalStorageManager(application.applicationContext)
    val api = VndbApiService()

    private val _settings = MutableStateFlow(storage.getSettings())
    val settings: StateFlow<SettingsState> = _settings.asStateFlow()

    private val _homeState = MutableStateFlow(HomeUiState())
    val homeState: StateFlow<HomeUiState> = _homeState.asStateFlow()

    private val _searchState = MutableStateFlow(SearchUiState())
    val searchState: StateFlow<SearchUiState> = _searchState.asStateFlow()

    private val _collectionState = MutableStateFlow(
        CollectionUiState(
            localFilter = storage.getLocalFilter(),
            localSort = storage.getLocalSort()
        )
    )
    val collectionState: StateFlow<CollectionUiState> = _collectionState.asStateFlow()

    private val _syncState = MutableStateFlow(
        SyncUiState(
            userIdentity = storage.getUserIdentity(),
            isSynced = storage.isUserSynced
        )
    )
    val syncState: StateFlow<SyncUiState> = _syncState.asStateFlow()

    private val _detailMap = MutableStateFlow<Map<String, VnDetailUiState>>(emptyMap())
    val detailMap: StateFlow<Map<String, VnDetailUiState>> = _detailMap.asStateFlow()

    private val _randomState = MutableStateFlow(RandomVnUiState())
    val randomState: StateFlow<RandomVnUiState> = _randomState.asStateFlow()

    private var tagSearchJob: Job? = null
    private var devSearchJob: Job? = null
    private var traitSearchJob: Job? = null

    init {
        loadHomeData(forceRefresh = false)
        refreshCollectionUi()
    }

    // --- Settings ---
    fun updateSettings(transform: (SettingsState) -> SettingsState) {
        val updated = transform(_settings.value)
        _settings.value = updated
        storage.saveSettings(updated)
    }

    fun resetSettingsToDefault() {
        val defaultSettings = SettingsState()
        _settings.value = defaultSettings
        storage.saveSettings(defaultSettings)
    }

    // --- Home Data, Quotes & Chart Stats ---
    fun loadHomeData(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _homeState.update { it.copy(isLoading = true, error = null) }
            try {
                val cachedStats = storage.getStats()
                if (cachedStats.isNotEmpty()) {
                    _homeState.update { it.copy(stats = DefaultChartStats.items(cachedStats)) }
                }
                val cachedQuotes = storage.getQuotes()
                if (cachedQuotes.isNotEmpty()) {
                    _homeState.update { it.copy(quotes = cachedQuotes) }
                }

                // Launch stats & quotes fetch in parallel
                val statsDeferred = async {
                    runCatching {
                        val fresh = api.fetchStats()
                        if (fresh.isNotEmpty()) {
                            storage.saveStats(fresh)
                            _homeState.update { it.copy(stats = DefaultChartStats.items(fresh)) }
                        }
                    }
                }
                val quotesDeferred = async {
                    runCatching {
                        val freshQuotes = api.fetchHomeQuotes(randomCount = 3, topCount = 6)
                        if (freshQuotes.isNotEmpty()) {
                            freshQuotes.mapNotNull { it.vn }.forEach { storage.saveP1(it) }
                            storage.saveQuotes(freshQuotes)
                            _homeState.update { it.copy(quotes = freshQuotes) }
                        }
                    }
                }

                val maxItems = _settings.value.maxPreviewItem
                val sectionMap = mutableMapOf<HomeSectionsCode, List<VnDataPhase01>>()

                for (section in HomeSectionsCode.entries) {
                    if (section == HomeSectionsCode.COLLECTION) {
                        val records = storage.getAllRecords()
                        val p1List = records.take(maxItems).mapNotNull { rec ->
                            storage.getP1(rec.id) ?: runCatching {
                                api.fetchP1ById(rec.id)?.also { storage.saveP1(it) }
                            }.getOrNull()
                        }
                        sectionMap[section] = p1List
                    } else {
                        val cachedIds = if (!forceRefresh) storage.getHomePreviewIds(section) else emptyList()
                        val cachedItems = cachedIds.mapNotNull { storage.getP1(it) }
                        if (cachedItems.isNotEmpty() && !forceRefresh) {
                            sectionMap[section] = cachedItems.take(maxItems)
                        } else {
                            val response = runCatching {
                                api.searchVn(
                                    filterData = section.filter,
                                    sortData = SortData(
                                        sort = section.labelCode.code,
                                        reverse = section.reverseSort
                                    ),
                                    page = 1,
                                    results = maxItems
                                )
                            }.getOrNull()
                            if (response != null && response.items.isNotEmpty()) {
                                response.items.forEach { storage.saveP1(it) }
                                storage.saveHomePreviewIds(section, response.items.map { it.id })
                                sectionMap[section] = response.items
                            } else {
                                sectionMap[section] = cachedItems
                            }
                        }
                    }
                }

                statsDeferred.await()
                quotesDeferred.await()
                _homeState.update {
                    it.copy(
                        sections = sectionMap,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _homeState.update {
                    it.copy(
                        isLoading = false,
                        error = e.localizedMessage ?: "Failed to load home previews."
                    )
                }
            }
        }
    }

    fun shuffleHomeQuotes() {
        if (_homeState.value.isRefreshingQuotes) return
        viewModelScope.launch {
            _homeState.update { it.copy(isRefreshingQuotes = true) }
            try {
                val freshRandom = coroutineScope {
                    (1..4).map {
                        async { runCatching { api.fetchRandomQuote() }.getOrNull() }
                    }.awaitAll().filterNotNull()
                }
                if (freshRandom.isNotEmpty()) {
                    freshRandom.mapNotNull { it.vn }.forEach { storage.saveP1(it) }
                    val merged = (freshRandom + _homeState.value.quotes).distinctBy { it.id }.take(10)
                    storage.saveQuotes(merged)
                    _homeState.update { it.copy(quotes = merged, isRefreshingQuotes = false) }
                } else {
                    _homeState.update { it.copy(isRefreshingQuotes = false) }
                }
            } catch (_: Exception) {
                _homeState.update { it.copy(isRefreshingQuotes = false) }
            }
        }
    }

    // --- Remote Search (VNs + Character Traits) ---
    fun setSearchTabMode(mode: SearchTabMode) {
        _searchState.update { it.copy(tabMode = mode) }
    }

    fun updateSearchQuery(query: String) {
        _searchState.update {
            it.copy(filterData = it.filterData.copy(search = query))
        }
    }

    fun updateRemoteFilterAndSort(newFilter: FilterData, newSort: SortData, triggerSearch: Boolean = true) {
        _searchState.update {
            it.copy(filterData = newFilter, sortData = newSort)
        }
        if (triggerSearch) {
            performVnSearch(resetPage = true)
        }
    }

    fun applyQuickGenreSearch(tag: VnTag) {
        val newFilter = FilterData.DEFAULT_REMOTE.copy(tag = listOf(tag))
        _searchState.update {
            it.copy(
                tabMode = SearchTabMode.VISUAL_NOVELS,
                filterData = newFilter,
                sortData = SortData(sort = SortableCode.RATING.code, reverse = true)
            )
        }
        performVnSearch(resetPage = true)
    }

    fun applyQuickTraitVnSearch(trait: VnTrait) {
        val currentTraits = _searchState.value.filterData.trait
        val updatedTraits = if (currentTraits.any { it.id == trait.id }) {
            currentTraits
        } else {
            currentTraits + trait
        }
        val newFilter = _searchState.value.filterData.copy(trait = updatedTraits)
        _searchState.update {
            it.copy(
                tabMode = SearchTabMode.VISUAL_NOVELS,
                filterData = newFilter,
                sortData = if (it.sortData.sort == SortableCode.SEARCHRANK.code && newFilter.search.isBlank()) {
                    SortData(sort = SortableCode.RATING.code, reverse = true)
                } else {
                    it.sortData
                }
            )
        }
        performVnSearch(resetPage = true)
    }

    fun applyHomeSeeMore(section: HomeSectionsCode) {
        val sort = SortData(
            sort = if (section.labelCode == SortableCode.COLLECTION) SortableCode.RATING.code else section.labelCode.code,
            reverse = section.reverseSort
        )
        _searchState.update {
            it.copy(
                tabMode = SearchTabMode.VISUAL_NOVELS,
                filterData = section.filter,
                sortData = sort
            )
        }
        performVnSearch(resetPage = true)
    }

    fun removeSingleFilterChip(
        tagToRemove: VnTag? = null,
        traitToRemove: VnTrait? = null,
        devToRemove: Developer? = null,
        langToRemove: String? = null,
        olangToRemove: String? = null,
        platfToRemove: String? = null,
        devStatusToRemove: Int? = null,
        charRoleToRemove: String? = null,
        charSexToRemove: String? = null,
        clearReleaseWindow: Boolean = false
    ) {
        val cur = _searchState.value.filterData
        val next = cur.copy(
            tag = if (tagToRemove != null) cur.tag.filterNot { it.id == tagToRemove.id } else cur.tag,
            trait = if (traitToRemove != null) cur.trait.filterNot { it.id == traitToRemove.id } else cur.trait,
            dev = if (devToRemove != null) cur.dev.filterNot { it.id == devToRemove.id } else cur.dev,
            lang = if (langToRemove != null) cur.lang - langToRemove else cur.lang,
            olang = if (olangToRemove != null) cur.olang - olangToRemove else cur.olang,
            platform = if (platfToRemove != null) cur.platform - platfToRemove else cur.platform,
            devstatus = if (devStatusToRemove != null) cur.devstatus - devStatusToRemove else cur.devstatus,
            charRoles = if (charRoleToRemove != null) cur.charRoles - charRoleToRemove else cur.charRoles,
            charSexes = if (charSexToRemove != null) cur.charSexes - charSexToRemove else cur.charSexes,
            releaseWindow = if (clearReleaseWindow) "" else cur.releaseWindow
        )
        _searchState.update { it.copy(filterData = next) }
        performVnSearch(resetPage = true)
    }

    fun resetRemoteFilters() {
        val currentQuery = _searchState.value.filterData.search
        _searchState.update {
            it.copy(
                filterData = FilterData.DEFAULT_REMOTE.copy(search = currentQuery),
                sortData = SortData.DEFAULT_REMOTE
            )
        }
    }

    fun performVnSearch(resetPage: Boolean = true) {
        val state = _searchState.value
        if (state.isVnLoading && !resetPage) return

        val nextPage = if (resetPage) 1 else state.vnPage + 1
        viewModelScope.launch {
            _searchState.update {
                it.copy(
                    hasInitiatedVnSearch = true,
                    isVnLoading = true,
                    vnError = null,
                    vnPage = nextPage,
                    vnResults = if (resetPage) emptyList() else it.vnResults
                )
            }
            try {
                // Ensure valid sort field when search query is blank
                val effectiveSort = if (
                    state.filterData.search.isBlank() &&
                    state.sortData.sort == SortableCode.SEARCHRANK.code
                ) {
                    state.sortData.copy(sort = SortableCode.RATING.code)
                } else {
                    state.sortData
                }

                val res = api.searchVn(
                    filterData = state.filterData,
                    sortData = effectiveSort,
                    page = nextPage,
                    results = 24
                )
                _searchState.update { cur ->
                    val combined = if (resetPage) res.items else (cur.vnResults + res.items).distinctBy { it.id }
                    cur.copy(
                        vnResults = combined,
                        vnHasMore = res.more,
                        isVnLoading = false,
                        vnError = null
                    )
                }
            } catch (e: Exception) {
                _searchState.update {
                    it.copy(
                        isVnLoading = false,
                        vnError = e.localizedMessage ?: "Search request failed."
                    )
                }
            }
        }
    }

    // --- Live Tag / Developer / Character Trait Autocomplete ---
    fun queryTagsLive(query: String) {
        tagSearchJob?.cancel()
        if (query.isBlank()) {
            _searchState.update { it.copy(tagSearchResults = emptyList(), isSearchingTags = false) }
            return
        }
        tagSearchJob = viewModelScope.launch {
            _searchState.update { it.copy(isSearchingTags = true) }
            delay(350)
            val tags = runCatching { api.searchTags(query) }.getOrDefault(emptyList())
            _searchState.update { it.copy(tagSearchResults = tags, isSearchingTags = false) }
        }
    }

    fun queryDevelopersLive(query: String) {
        devSearchJob?.cancel()
        if (query.isBlank()) {
            _searchState.update { it.copy(devSearchResults = emptyList(), isSearchingDevs = false) }
            return
        }
        devSearchJob = viewModelScope.launch {
            _searchState.update { it.copy(isSearchingDevs = true) }
            delay(350)
            val devs = runCatching { api.searchDevelopers(query) }.getOrDefault(emptyList())
            _searchState.update { it.copy(devSearchResults = devs, isSearchingDevs = false) }
        }
    }

    fun queryTraitsLive(query: String) {
        traitSearchJob?.cancel()
        if (query.isBlank()) {
            _searchState.update { it.copy(traitSearchResults = emptyList(), isSearchingTraits = false) }
            return
        }
        traitSearchJob = viewModelScope.launch {
            _searchState.update { it.copy(isSearchingTraits = true) }
            delay(350)
            val traits = runCatching { api.searchTraits(query) }.getOrDefault(emptyList())
            _searchState.update { it.copy(traitSearchResults = traits, isSearchingTraits = false) }
        }
    }

    // --- Character & Trait Explorer ---
    fun updateCharSearchQuery(query: String) {
        _searchState.update { it.copy(charQuery = query) }
    }

    fun toggleExplorerTrait(trait: VnTrait) {
        _searchState.update { cur ->
            val exists = cur.selectedCharTraits.any { it.id == trait.id }
            val updated = if (exists) {
                cur.selectedCharTraits.filterNot { it.id == trait.id }
            } else {
                cur.selectedCharTraits + trait
            }
            cur.copy(selectedCharTraits = updated)
        }
        performCharacterSearch(resetPage = true)
    }

    fun toggleCharTraitAndOr() {
        _searchState.update { cur ->
            val next = if (cur.charTraitAndOr == "and") "or" else "and"
            cur.copy(charTraitAndOr = next)
        }
        if (_searchState.value.hasInitiatedCharSearch) {
            performCharacterSearch(resetPage = true)
        }
    }

    fun toggleExplorerRole(roleCode: String) {
        _searchState.update { cur ->
            val updated = if (cur.selectedCharRoles.contains(roleCode)) {
                cur.selectedCharRoles - roleCode
            } else {
                cur.selectedCharRoles + roleCode
            }
            cur.copy(selectedCharRoles = updated)
        }
        performCharacterSearch(resetPage = true)
    }

    fun toggleCharRoleAndOr() {
        _searchState.update { cur ->
            val next = if (cur.charRoleAndOr == "and") "or" else "and"
            cur.copy(charRoleAndOr = next)
        }
        if (_searchState.value.hasInitiatedCharSearch) {
            performCharacterSearch(resetPage = true)
        }
    }

    fun toggleExplorerSex(sexCode: String) {
        _searchState.update { cur ->
            val updated = if (cur.selectedCharSexes.contains(sexCode)) {
                cur.selectedCharSexes - sexCode
            } else {
                cur.selectedCharSexes + sexCode
            }
            cur.copy(selectedCharSexes = updated)
        }
        performCharacterSearch(resetPage = true)
    }

    fun toggleCharSexAndOr() {
        _searchState.update { cur ->
            val next = if (cur.charSexAndOr == "and") "or" else "and"
            cur.copy(charSexAndOr = next)
        }
        if (_searchState.value.hasInitiatedCharSearch) {
            performCharacterSearch(resetPage = true)
        }
    }

    fun toggleCharOverallAndOr() {
        _searchState.update { cur ->
            val next = if (cur.charOverallAndOr == "and") "or" else "and"
            cur.copy(charOverallAndOr = next)
        }
        if (_searchState.value.hasInitiatedCharSearch) {
            performCharacterSearch(resetPage = true)
        }
    }

    fun clearExplorerTraits() {
        _searchState.update {
            it.copy(
                charQuery = "",
                selectedCharTraits = emptyList(),
                selectedCharRoles = emptyList(),
                selectedCharSexes = emptyList(),
                hasInitiatedCharSearch = false,
                charResults = emptyList()
            )
        }
    }

    fun performCharacterSearch(resetPage: Boolean = true) {
        val state = _searchState.value
        if (state.isCharLoading && !resetPage) return

        val nextPage = if (resetPage) 1 else state.charPage + 1
        viewModelScope.launch {
            _searchState.update {
                it.copy(
                    hasInitiatedCharSearch = true,
                    isCharLoading = true,
                    charError = null,
                    charPage = nextPage,
                    charResults = if (resetPage) emptyList() else it.charResults
                )
            }
            try {
                val res = api.searchCharacters(
                    query = state.charQuery,
                    traits = state.selectedCharTraits,
                    traitAndOr = state.charTraitAndOr,
                    roles = state.selectedCharRoles,
                    roleAndOr = state.charRoleAndOr,
                    sexes = state.selectedCharSexes,
                    sexAndOr = state.charSexAndOr,
                    overallAndOr = state.charOverallAndOr,
                    page = nextPage,
                    results = 24
                )
                _searchState.update { cur ->
                    val combined = if (resetPage) res.items else (cur.charResults + res.items).distinctBy { it.id }
                    cur.copy(
                        charResults = combined,
                        charHasMore = res.more,
                        isCharLoading = false,
                        charError = null
                    )
                }
            } catch (e: Exception) {
                _searchState.update {
                    it.copy(
                        isCharLoading = false,
                        charError = e.localizedMessage ?: "Failed to search characters."
                    )
                }
            }
        }
    }

    // --- Collection Management ---
    fun refreshCollectionUi() {
        viewModelScope.launch {
            val allRecords = storage.getAllRecords()
            val rawMap = allRecords.associateBy { it.id }
            val p1Map = mutableMapOf<String, VnDataPhase01>()
            val p2Map = mutableMapOf<String, VnDataPhase02>()

            for (record in allRecords) {
                val p1 = storage.getP1(record.id) ?: VnDataPhase01(
                    id = record.id,
                    title = record.title
                )
                p1Map[record.id] = p1
                storage.getP2(record.id)?.let { p2Map[record.id] = it }
            }

            val filter = _collectionState.value.localFilter
            val sort = _collectionState.value.localSort

            // Filter records locally
            val filteredRecords = allRecords.filter { rec ->
                val p1 = p1Map[rec.id]
                val p2 = p2Map[rec.id]

                val matchesSearch = filter.search.isBlank() ||
                    rec.title.contains(filter.search.trim(), ignoreCase = true) ||
                    (p1?.title?.contains(filter.search.trim(), ignoreCase = true) == true) ||
                    (p2?.aliases?.any { it.contains(filter.search.trim(), ignoreCase = true) } == true)

                val matchesOlang = filter.olang.isEmpty() ||
                    (p1 != null && filter.olang.contains(p1.olang))

                val matchesDevstatus = filter.devstatus.isEmpty() ||
                    (p2 != null && filter.devstatus.contains(p2.devstatus))

                val matchesLang = filter.lang.isEmpty() ||
                    (p2 != null && filter.lang.any { p2.languages.contains(it) })

                val matchesPlatf = filter.platform.isEmpty() ||
                    (p2 != null && filter.platform.any { p2.platforms.contains(it) })

                val matchesMinAge = filter.minage <= 0 ||
                    (p2 != null && p2.minage.any { it >= filter.minage })

                matchesSearch && matchesOlang && matchesDevstatus && matchesLang && matchesPlatf && matchesMinAge
            }

            // Sort records locally
            val sortedRecords = filteredRecords.sortedWith { a, b ->
                val p1A = p1Map[a.id]
                val p1B = p1Map[b.id]
                val p2A = p2Map[a.id]
                val p2B = p2Map[b.id]

                val cmp = when (sort.sort) {
                    SortableCode.TITLE.code -> (p1A?.title ?: a.title).compareTo(p1B?.title ?: b.title, ignoreCase = true)
                    SortableCode.ADDED.code -> (a.added ?: "").compareTo(b.added ?: "")
                    SortableCode.STARTED.code -> (a.started ?: "").compareTo(b.started ?: "")
                    SortableCode.LENGTH_MINUTES.code -> (p2A?.lengthMinutes ?: 0).compareTo(p2B?.lengthMinutes ?: 0)
                    SortableCode.RATING.code -> (p1A?.rating ?: 0.0).compareTo(p1B?.rating ?: 0.0)
                    SortableCode.RELEASED.code -> (p1A?.released ?: "").compareTo(p1B?.released ?: "")
                    SortableCode.VOTECOUNT.code -> (p1A?.votecount ?: 0).compareTo(p1B?.votecount ?: 0)
                    SortableCode.VOTE.code -> a.vote.compareTo(b.vote)
                    else -> (p1A?.title ?: a.title).compareTo(p1B?.title ?: b.title, ignoreCase = true)
                }
                if (sort.reverse) -cmp else cmp
            }

            val grouped = mutableMapOf<String, List<VnDataPhase01>>()
            for (status in CollectionStatusCode.entries) {
                grouped[status.statusName] = sortedRecords
                    .filter { it.status.equals(status.statusName, ignoreCase = true) }
                    .mapNotNull { p1Map[it.id] }
            }

            _collectionState.update {
                it.copy(
                    recordsByStatus = grouped,
                    rawRecords = rawMap,
                    p2Cache = p2Map
                )
            }

            // Also update home collection preview
            val maxItems = _settings.value.maxPreviewItem
            val homePreview = allRecords.take(maxItems).mapNotNull { p1Map[it.id] }
            _homeState.update { cur ->
                val updatedMap = cur.sections.toMutableMap()
                updatedMap[HomeSectionsCode.COLLECTION] = homePreview
                cur.copy(sections = updatedMap)
            }
        }
    }

    fun updateLocalFilterAndSort(filter: FilterData, sort: SortData) {
        storage.saveLocalFilter(filter)
        storage.saveLocalSort(sort)
        _collectionState.update {
            it.copy(localFilter = filter, localSort = sort)
        }
        refreshCollectionUi()
    }

    fun setCollectionSearchActive(active: Boolean) {
        _collectionState.update { cur ->
            val nextFilter = if (!active) cur.localFilter.copy(search = "") else cur.localFilter
            if (!active) storage.saveLocalFilter(nextFilter)
            cur.copy(isSearchActive = active, localFilter = nextFilter)
        }
        refreshCollectionUi()
    }

    fun updateCollectionSearchQuery(query: String) {
        val next = _collectionState.value.localFilter.copy(search = query)
        storage.saveLocalFilter(next)
        _collectionState.update { it.copy(localFilter = next) }
        refreshCollectionUi()
    }

    fun toggleRecordSelection(vnId: String) {
        _collectionState.update { cur ->
            val updated = if (cur.selectedVnIds.contains(vnId)) {
                cur.selectedVnIds - vnId
            } else {
                cur.selectedVnIds + vnId
            }
            cur.copy(selectedVnIds = updated)
        }
    }

    fun selectAllInStatus(statusName: String) {
        val allInTab = _collectionState.value.recordsByStatus[statusName]?.map { it.id } ?: emptyList()
        _collectionState.update { it.copy(selectedVnIds = allInTab) }
    }

    fun clearRecordSelection() {
        _collectionState.update { it.copy(selectedVnIds = emptyList()) }
    }

    fun saveOrUpdateCollectionRecord(
        p1: VnDataPhase01,
        status: String,
        vote: Int,
        notes: String?,
        started: String?,
        finished: String?
    ) {
        val nowStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val existing = storage.getRecordById(p1.id)
        val record = VnRecord(
            id = p1.id,
            title = p1.title,
            status = status.lowercase(),
            vote = vote,
            added = existing?.added ?: nowStr,
            started = started?.takeIf { it.isNotBlank() && it != "null" },
            finished = finished?.takeIf { it.isNotBlank() && it != "null" },
            lastmod = nowStr,
            notes = notes?.takeIf { it.isNotBlank() }
        )
        storage.saveP1(p1)
        storage.saveRecord(record)
        storage.addAddedViaAppNotBySync(p1.id)
        refreshCollectionUi()

        // Ensure P2 & P3 are downloaded and saved locally for offline collection filtering/viewing
        viewModelScope.launch {
            if (!storage.hasP2(p1.id)) {
                runCatching { api.fetchP2ById(p1.id) }.getOrNull()?.let {
                    storage.saveP2(it)
                    refreshCollectionUi()
                }
            }
            if (!storage.hasP3(p1.id)) {
                runCatching { api.fetchP3ById(p1.id) }.getOrNull()?.let { (p3, _) ->
                    storage.saveP3(p3)
                }
            }
        }
    }

    fun batchUpdateSelectedRecords(status: String, vote: Int?) {
        val nowStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val ids = _collectionState.value.selectedVnIds
        for (id in ids) {
            val existing = storage.getRecordById(id) ?: continue
            val updated = existing.copy(
                status = status.lowercase(),
                vote = vote ?: existing.vote,
                lastmod = nowStr
            )
            storage.saveRecord(updated)
            storage.addAddedViaAppNotBySync(id)
        }
        clearRecordSelection()
        refreshCollectionUi()
    }

    fun removeRecordFromCollection(vnId: String) {
        val wasAddedLocallyOnly = storage.getAddedViaAppNotBySync().contains(vnId)
        if (!wasAddedLocallyOnly && _syncState.value.isSynced) {
            storage.addVnToBeRemovedWhenSync(vnId)
        }
        storage.removeAddedViaAppNotBySync(vnId)
        storage.removeRecord(vnId)
        clearRecordSelection()
        refreshCollectionUi()
    }

    fun removeSelectedRecordsFromCollection() {
        val ids = _collectionState.value.selectedVnIds
        for (id in ids) {
            val wasAddedLocallyOnly = storage.getAddedViaAppNotBySync().contains(id)
            if (!wasAddedLocallyOnly && _syncState.value.isSynced) {
                storage.addVnToBeRemovedWhenSync(id)
            }
            storage.removeAddedViaAppNotBySync(id)
            storage.removeRecord(id)
        }
        clearRecordSelection()
        refreshCollectionUi()
    }

    fun clearEntireLocalCollection(keepSyncDeletions: Boolean) {
        storage.clearAllRecords(markForSyncRemoval = keepSyncDeletions)
        refreshCollectionUi()
    }

    // --- VNDB Cloud Sync ---
    fun authenticateAndSync(tokenInput: String, onSuccess: () -> Unit = {}) {
        if (_syncState.value.isSyncing) return
        viewModelScope.launch {
            _syncState.update {
                it.copy(isSyncing = true, statusMessage = "Verifying personal access token...", isError = false)
            }
            try {
                val identity = api.authenticate(tokenInput)
                storage.saveUserIdentity(identity)
                _syncState.update {
                    it.copy(
                        userIdentity = identity,
                        statusMessage = "Authenticated as ${identity.username}! Syncing collection..."
                    )
                }
                performSyncInternal(identity)
                onSuccess()
            } catch (e: Exception) {
                _syncState.update {
                    it.copy(
                        isSyncing = false,
                        statusMessage = "Authentication failed: ${e.localizedMessage ?: "Invalid token"}",
                        isError = true
                    )
                }
            }
        }
    }

    fun syncCurrentAccount() {
        val identity = _syncState.value.userIdentity ?: return
        if (_syncState.value.isSyncing) return
        viewModelScope.launch {
            performSyncInternal(identity)
        }
    }

    fun logoutVnAccount() {
        storage.saveUserIdentity(null)
        _syncState.update {
            SyncUiState(userIdentity = null, isSynced = false, statusMessage = "Signed out of VNDB account.")
        }
    }

    private suspend fun performSyncInternal(identity: UserIdentity) {
        _syncState.update {
            it.copy(isSyncing = true, statusMessage = "Synchronizing with VNDB cloud...", isError = false)
        }
        try {
            // 1. Delete cloud records marked for removal
            val toDelete = storage.getVnToBeRemovedWhenSync()
            for (vnId in toDelete) {
                runCatching { api.deleteCloudRecord(vnId, identity.authToken) }
            }
            storage.cleanVnToBeRemovedWhenSync()

            // 2. Fetch cloud ulist
            val cloudList = api.fetchAllCloudUlist(identity.id)
            val cloudVnIds = mutableListOf<String>()
            val localRecords = storage.getAllRecords().associateBy { it.id }

            // 3. Reconcile cloud & local records
            for (cloudObj in cloudList) {
                val vnId = cloudObj.optString("id", "")
                if (vnId.isBlank()) continue
                cloudVnIds.add(vnId)

                val localRec = localRecords[vnId]
                val cloudLastMod = cloudObj.optInt("lastmod", 0) * 1000L

                if (localRec != null) {
                    // Compare timestamps
                    val localLastMod = runCatching {
                        SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(localRec.lastmod ?: "")?.time ?: 0L
                    }.getOrDefault(0L)

                    if (localLastMod > cloudLastMod) {
                        val statusCode = CollectionStatusCode.fromName(localRec.status)
                        runCatching { api.patchCloudRecord(localRec, statusCode.id, identity.authToken) }
                        continue
                    }
                }

                // Save cloud record to local
                val record = parseCloudUlistRecord(cloudObj)
                storage.saveRecord(record)
            }

            // 4. Upload purely local records not yet in cloud
            for ((vnId, localRec) in localRecords) {
                if (!cloudVnIds.contains(vnId)) {
                    val statusCode = CollectionStatusCode.fromName(localRec.status)
                    runCatching { api.patchCloudRecord(localRec, statusCode.id, identity.authToken) }
                }
            }

            storage.cleanAddedViaAppNotBySync()
            storage.isUserSynced = true

            // 5. Ensure P1 data is cached for all synced records
            val updatedRecords = storage.getAllRecords()
            coroutineScope {
                updatedRecords.map { rec ->
                    async {
                        if (!storage.hasP1(rec.id)) {
                            runCatching { api.fetchP1ById(rec.id) }.getOrNull()?.let { storage.saveP1(it) }
                        }
                        if (!storage.hasP2(rec.id)) {
                            runCatching { api.fetchP2ById(rec.id) }.getOrNull()?.let { storage.saveP2(it) }
                        }
                    }
                }.awaitAll()
            }

            refreshCollectionUi()
            _syncState.update {
                it.copy(
                    isSynced = true,
                    isSyncing = false,
                    statusMessage = "Collection successfully synced (${updatedRecords.size} VNs)!",
                    isError = false
                )
            }
        } catch (e: Exception) {
            _syncState.update {
                it.copy(
                    isSyncing = false,
                    statusMessage = "Sync error: ${e.localizedMessage ?: "Check connection"}",
                    isError = true
                )
            }
        }
    }

    private fun parseCloudUlistRecord(obj: JSONObject): VnRecord {
        val vnId = obj.optString("id", "")
        val vnTitle = obj.optJSONObject("vn")?.optString("title", vnId) ?: vnId
        val rawVote = if (obj.isNull("vote")) 0 else (obj.optInt("vote", 0) / 10)
        val labelsArr = obj.optJSONArray("labels")
        var status = CollectionStatusCode.WISHLIST.statusName
        if (labelsArr != null) {
            for (i in 0 until labelsArr.length()) {
                val lbl = labelsArr.optJSONObject(i)?.optString("label", "")?.lowercase() ?: ""
                if (CollectionStatusCode.entries.any { it.statusName == lbl }) {
                    status = lbl
                    break
                }
            }
        }
        fun epochToDate(sec: Long): String? {
            if (sec <= 0L) return null
            return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(sec * 1000L))
        }
        return VnRecord(
            id = vnId,
            title = vnTitle,
            status = status,
            vote = rawVote,
            added = epochToDate(obj.optLong("added", 0L)),
            started = obj.optString("started").takeIf { it.isNotBlank() && it != "null" },
            finished = obj.optString("finished").takeIf { it.isNotBlank() && it != "null" },
            lastmod = epochToDate(obj.optLong("lastmod", 0L)),
            notes = obj.optString("notes").takeIf { it.isNotBlank() && it != "null" }
        )
    }

    // --- VN Detail Loading (Phases 1, 2, 3 + Characters, Quotes & Reviews) ---
    fun loadVnDetail(vnId: String, initialP1: VnDataPhase01? = null) {
        val existing = _detailMap.value[vnId]
        if (existing != null && existing.p2 != null && existing.p3 != null && !existing.isLoading) {
            return
        }

        viewModelScope.launch {
            val cachedP1 = initialP1 ?: storage.getP1(vnId)
            val cachedP2 = storage.getP2(vnId)
            val cachedP3 = storage.getP3(vnId)

            _detailMap.update { map ->
                map + (vnId to VnDetailUiState(
                    vnId = vnId,
                    p1 = cachedP1,
                    p2 = cachedP2,
                    p3 = cachedP3,
                    isLoading = cachedP2 == null || cachedP3 == null,
                    isLoadingCharacters = true,
                    isLoadingReviews = true
                ))
            }

            val inCollection = storage.getRecordById(vnId) != null

            val p1Deferred = async {
                cachedP1 ?: runCatching { api.fetchP1ById(vnId) }.getOrNull()?.also {
                    if (inCollection) storage.saveP1(it)
                }
            }
            val p2Deferred = async {
                cachedP2 ?: runCatching { api.fetchP2ById(vnId) }.getOrNull()?.also {
                    if (inCollection) storage.saveP2(it)
                }
            }
            val p3Deferred = async {
                runCatching { api.fetchP3ById(vnId) }.getOrNull()?.also { (p3, _) ->
                    if (inCollection) storage.saveP3(p3)
                }
            }
            val charsDeferred = async {
                runCatching { api.fetchCharactersForVn(vnId) }.getOrDefault(emptyList())
            }
            val quotesDeferred = async {
                runCatching { api.fetchQuotesForVn(vnId, results = 8) }.getOrDefault(emptyList())
            }
            val reviewsDeferred = async {
                runCatching { api.fetchReviewsForVn(vnId) }.getOrDefault(emptyList())
            }

            val finalP1 = p1Deferred.await()
            val finalP2 = p2Deferred.await()
            val p3Pair = p3Deferred.await()
            val finalP3 = p3Pair?.first ?: cachedP3
            val relatedVns = p3Pair?.second ?: emptyList()
            val chars = charsDeferred.await()
            val vnQuotes = quotesDeferred.await()
            val vnReviews = reviewsDeferred.await()

            _detailMap.update { map ->
                map + (vnId to VnDetailUiState(
                    vnId = vnId,
                    p1 = finalP1,
                    p2 = finalP2,
                    p3 = finalP3,
                    relatedVns = relatedVns,
                    characters = chars,
                    quotes = vnQuotes,
                    reviews = vnReviews,
                    isLoading = false,
                    isLoadingCharacters = false,
                    isLoadingReviews = false,
                    error = if (finalP1 == null) "Could not load visual novel details." else null
                ))
            }
        }
    }

    fun expandFullReview(vnId: String, reviewId: String) {
        val current = _detailMap.value[vnId] ?: return
        if (current.expandingReviewIds.contains(reviewId)) return

        viewModelScope.launch {
            _detailMap.update { map ->
                val st = map[vnId] ?: return@update map
                map + (vnId to st.copy(expandingReviewIds = st.expandingReviewIds + reviewId))
            }
            val fullText = runCatching { api.fetchFullReviewText(reviewId) }.getOrNull()
            _detailMap.update { map ->
                val st = map[vnId] ?: return@update map
                val updatedReviews = if (!fullText.isNullOrBlank()) {
                    st.reviews.map { rev ->
                        if (rev.id == reviewId) rev.copy(text = fullText, hasReadMore = false) else rev
                    }
                } else {
                    st.reviews.map { rev ->
                        if (rev.id == reviewId) rev.copy(hasReadMore = false) else rev
                    }
                }
                map + (vnId to st.copy(
                    reviews = updatedReviews,
                    expandingReviewIds = st.expandingReviewIds - reviewId
                ))
            }
        }
    }

    // --- Random VN ---
    fun loadRandomVn() {
        viewModelScope.launch {
            _randomState.update { it.copy(isLoading = true, error = null) }
            try {
                val randomVn = api.fetchRandomHighRatedVn()
                storage.saveP1(randomVn)
                _randomState.update {
                    it.copy(vn = randomVn, isLoading = false, error = null)
                }
            } catch (e: Exception) {
                _randomState.update {
                    it.copy(
                        isLoading = false,
                        error = e.localizedMessage ?: "Failed to fetch random VN."
                    )
                }
            }
        }
    }
}
