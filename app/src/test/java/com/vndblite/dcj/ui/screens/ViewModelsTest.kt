package com.vndblite.dcj.ui.screens

import androidx.lifecycle.SavedStateHandle
import com.vndblite.dcj.core.ApiError
import com.vndblite.dcj.core.AppResult
import com.vndblite.dcj.data.remote.VnFullDetailPayload
import com.vndblite.dcj.data.remote.VnQuerySpec
import com.vndblite.dcj.domain.model.AppSettings
import com.vndblite.dcj.domain.model.Character
import com.vndblite.dcj.domain.model.CharacterSearchFilters
import com.vndblite.dcj.domain.model.CollectionEntry
import com.vndblite.dcj.domain.model.CollectionFilters
import com.vndblite.dcj.domain.model.CollectionItemWithVn
import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.domain.model.DevStatus
import com.vndblite.dcj.domain.model.HomeSection
import com.vndblite.dcj.domain.model.PaginatedList
import com.vndblite.dcj.domain.model.Producer
import com.vndblite.dcj.domain.model.QuickGenrePreset
import com.vndblite.dcj.domain.model.Quote
import com.vndblite.dcj.domain.model.Review
import com.vndblite.dcj.domain.model.SiteStats
import com.vndblite.dcj.domain.model.SortOption
import com.vndblite.dcj.domain.model.SyncReport
import com.vndblite.dcj.domain.model.Tag
import com.vndblite.dcj.domain.model.ThemeId
import com.vndblite.dcj.domain.model.Trait
import com.vndblite.dcj.domain.model.UserAccount
import com.vndblite.dcj.domain.model.VnDetails
import com.vndblite.dcj.domain.model.VnSummary
import com.vndblite.dcj.domain.repository.CollectionRepository
import com.vndblite.dcj.domain.repository.SettingsRepository
import com.vndblite.dcj.domain.repository.VnRepository
import com.vndblite.dcj.sync.SyncProgress
import com.vndblite.dcj.sync.SyncService
import com.vndblite.dcj.ui.components.SectionUiState
import com.vndblite.dcj.ui.screens.auth.AuthAction
import com.vndblite.dcj.ui.screens.auth.AuthViewModel
import com.vndblite.dcj.ui.screens.characters.CharacterSearchAction
import com.vndblite.dcj.ui.screens.characters.CharacterSearchViewModel
import com.vndblite.dcj.ui.screens.collection.CollectionAction
import com.vndblite.dcj.ui.screens.collection.CollectionViewModel
import com.vndblite.dcj.ui.screens.detail.VnDetailAction
import com.vndblite.dcj.ui.screens.detail.VnDetailTab
import com.vndblite.dcj.ui.screens.detail.VnDetailViewModel
import com.vndblite.dcj.ui.screens.home.HomeAction
import com.vndblite.dcj.ui.screens.home.HomeViewModel
import com.vndblite.dcj.ui.screens.random.RandomVnAction
import com.vndblite.dcj.ui.screens.random.RandomVnViewModel
import com.vndblite.dcj.ui.screens.search.ActiveFilterChip
import com.vndblite.dcj.ui.screens.search.SearchAction
import com.vndblite.dcj.ui.screens.search.SearchViewModel
import com.vndblite.dcj.ui.screens.settings.SettingsAction
import com.vndblite.dcj.ui.screens.settings.SettingsViewModel
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelsTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `HomeViewModel surfaces per-section Error when refresh fails and recovers on RetrySection (F5)`() = runTest {
        val vnRepo = FakeVmVnRepository(initialFailSections = setOf(HomeSection.TOP_RATED))
        val colRepo = FakeVmCollectionRepository()
        val settingsRepo = FakeVmSettingsRepository()

        val vm = HomeViewModel(vnRepo, colRepo, settingsRepo)
        val collectJob = backgroundScope.launch { vm.uiState.collect() }
        advanceUntilIdle()

        val initialTopRated = vm.uiState.value.remoteSections[HomeSection.TOP_RATED]
        assertIs<SectionUiState.Error>(initialTopRated)

        // Fix section and retry
        vnRepo.failSections.remove(HomeSection.TOP_RATED)
        vm.onAction(HomeAction.RetrySection(HomeSection.TOP_RATED))
        advanceUntilIdle()

        val recoveredTopRated = vm.uiState.value.remoteSections[HomeSection.TOP_RATED]
        assertIs<SectionUiState.Content<List<VnSummary>>>(recoveredTopRated)
        assertEquals("Ever17", recoveredTopRated.data.first().title)
        collectJob.cancel()
    }

    @Test
    fun `SearchViewModel debounces rapid query updates and supports chip removal and pagination (F4)`() = runTest {
        val vnRepo = FakeVmVnRepository()
        val colRepo = FakeVmCollectionRepository()
        val settingsRepo = FakeVmSettingsRepository()
        val vm = SearchViewModel(vnRepo, colRepo, settingsRepo, SavedStateHandle())
        val collectJob = backgroundScope.launch { vm.uiState.collect() }

        // Initial debounced load
        advanceTimeBy(SearchViewModel.SEARCH_DEBOUNCE_MS + 50L)
        advanceUntilIdle()
        val baselineCalls = vnRepo.searchVnsCalls

        // Rapid typing should coalesce into a single API call
        vm.onAction(SearchAction.UpdateQuery("S"))
        advanceTimeBy(100L)
        vm.onAction(SearchAction.UpdateQuery("St"))
        advanceTimeBy(100L)
        vm.onAction(SearchAction.UpdateQuery("Steins"))
        advanceTimeBy(SearchViewModel.SEARCH_DEBOUNCE_MS + 50L)
        advanceUntilIdle()

        assertEquals(baselineCalls + 1, vnRepo.searchVnsCalls)
        assertEquals("Steins", vm.uiState.value.filters.searchQuery)

        // Toggle quick genre and remove chip
        vm.onAction(SearchAction.ToggleQuickGenre(QuickGenrePreset.SCIFI))
        advanceTimeBy(SearchViewModel.SEARCH_DEBOUNCE_MS + 50L)
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.filters.tags.size)

        val tagChip = vm.uiState.value.activeFilterChips.filterIsInstance<ActiveFilterChip.TagChip>().first()
        vm.onAction(SearchAction.RemoveChip(tagChip))
        advanceTimeBy(SearchViewModel.SEARCH_DEBOUNCE_MS + 50L)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.filters.tags.isEmpty())

        collectJob.cancel()
    }

    @Test
    fun `CharacterSearchViewModel debounces trait changes and toggles AND-OR logic`() = runTest {
        val vnRepo = FakeVmVnRepository()
        val settingsRepo = FakeVmSettingsRepository()
        val vm = CharacterSearchViewModel(vnRepo, settingsRepo, SavedStateHandle())
        val collectJob = backgroundScope.launch { vm.uiState.collect() }

        advanceTimeBy(CharacterSearchViewModel.SEARCH_DEBOUNCE_MS + 50L)
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.characters.size)

        vm.onAction(CharacterSearchAction.ToggleTraitAndOr)
        advanceTimeBy(CharacterSearchViewModel.SEARCH_DEBOUNCE_MS + 50L)
        advanceUntilIdle()
        assertEquals("or", vm.uiState.value.filters.traitAndOr.apiToken)

        collectJob.cancel()
    }

    @Test
    fun `CollectionViewModel supports multi-selection, batch update, and batch delete`() = runTest {
        val colRepo = FakeVmCollectionRepository()
        val settingsRepo = FakeVmSettingsRepository()
        val syncService = FakeVmSyncService()

        colRepo.saveOrUpdateEntry(
            vn = VnSummary(id = "v17", title = "Ever17"),
            status = CollectionStatus.PLAYING,
            vote = 80,
            notes = null,
            startedDate = null,
            finishedDate = null
        )
        colRepo.saveOrUpdateEntry(
            vn = VnSummary(id = "v2", title = "Kanon"),
            status = CollectionStatus.PLAYING,
            vote = 70,
            notes = null,
            startedDate = null,
            finishedDate = null
        )

        val vm = CollectionViewModel(colRepo, settingsRepo, syncService, SavedStateHandle())
        val collectJob = backgroundScope.launch { vm.uiState.collect() }
        advanceUntilIdle()

        vm.onAction(CollectionAction.SelectAllInStatus(CollectionStatus.PLAYING))
        advanceUntilIdle()
        assertEquals(setOf("v17", "v2"), vm.uiState.value.selectedVnIds)

        vm.onAction(CollectionAction.BatchUpdateSelected(CollectionStatus.FINISHED, overrideVote = 90))
        advanceUntilIdle()
        assertTrue(vm.uiState.value.selectedVnIds.isEmpty())
        assertEquals(CollectionStatus.FINISHED, colRepo.entriesFlow.value["v17"]?.status)
        assertEquals(90, colRepo.entriesFlow.value["v17"]?.vote)

        collectJob.cancel()
    }

    @Test
    fun `VnDetailViewModel loads tabs independently and supports section retry (F5, N5)`() = runTest {
        val vnRepo = FakeVmVnRepository().apply {
            failReviews = true
        }
        val colRepo = FakeVmCollectionRepository()
        val settingsRepo = FakeVmSettingsRepository()
        val syncService = FakeVmSyncService()

        val vm = VnDetailViewModel(
            vnId = "v17",
            initialTitle = "Ever17",
            vnRepository = vnRepo,
            collectionRepository = colRepo,
            settingsRepository = settingsRepo,
            syncService = syncService,
            savedStateHandle = SavedStateHandle()
        )
        val collectJob = backgroundScope.launch { vm.uiState.collect() }
        advanceUntilIdle()

        // Reviews failed while Characters and Quotes succeeded
        assertIs<SectionUiState.Error>(vm.uiState.value.reviewsState)
        assertIs<SectionUiState.Content<*>>(vm.uiState.value.charactersState)
        assertIs<SectionUiState.Content<*>>(vm.uiState.value.quotesState)

        // Retry reviews after network recovery
        vnRepo.failReviews = false
        vm.onAction(VnDetailAction.SelectTab(VnDetailTab.REVIEWS))
        vm.onAction(VnDetailAction.RetryReviews)
        advanceUntilIdle()

        assertEquals(VnDetailTab.REVIEWS, vm.uiState.value.selectedTab)
        assertIs<SectionUiState.Content<List<Review>>>(vm.uiState.value.reviewsState)
        collectJob.cancel()
    }

    @Test
    fun `SettingsViewModel, AuthViewModel, and RandomVnViewModel handle user actions`() = runTest {
        val vnRepo = FakeVmVnRepository()
        val colRepo = FakeVmCollectionRepository()
        val settingsRepo = FakeVmSettingsRepository()
        val syncService = FakeVmSyncService()

        val settingsVm = SettingsViewModel(settingsRepo, vnRepo, colRepo)
        val authVm = AuthViewModel(syncService)
        val randomVm = RandomVnViewModel(vnRepo, colRepo, settingsRepo)

        val jobs = listOf(
            backgroundScope.launch { settingsVm.uiState.collect() },
            backgroundScope.launch { authVm.uiState.collect() },
            backgroundScope.launch { randomVm.uiState.collect() }
        )

        settingsVm.onAction(SettingsAction.SelectTheme(ThemeId.SAYA))
        settingsVm.onAction(SettingsAction.MoveStatusTab(CollectionStatus.PLAYING, direction = 1))
        advanceUntilIdle()
        assertEquals(ThemeId.SAYA, settingsVm.uiState.value.settings.themeId)
        assertEquals(CollectionStatus.PLAYING, settingsVm.uiState.value.settings.collectionStatusTabArrangement[1])

        authVm.onAction(AuthAction.AuthenticateAndSync("tok_123"))
        advanceUntilIdle()
        assertNotNull(authVm.uiState.value.activeAccount)
        assertTrue(authVm.uiState.value.isAccountSynced)

        randomVm.onAction(RandomVnAction.OpenAndRoll)
        advanceUntilIdle()
        assertTrue(randomVm.uiState.value.isVisible)
        assertEquals("Ever17", randomVm.uiState.value.vn?.title)
        randomVm.onAction(RandomVnAction.Dismiss)
        advanceUntilIdle()
        assertFalse(randomVm.uiState.value.isVisible)

        jobs.forEach { it.cancel() }
    }
}

private class FakeVmVnRepository(
    initialFailSections: Set<HomeSection> = emptySet()
) : VnRepository {
    val failSections = initialFailSections.toMutableSet()
    var failReviews = false
    var searchVnsCalls = 0

    private val homeSectionFlows = HomeSection.entries.associateWith { section ->
        MutableStateFlow(
            if (section in initialFailSections) emptyList()
            else listOf(VnSummary(id = "v17", title = "Ever17", rating = 86.4, voteCount = 7200))
        )
    }
    private val quotesFlow = MutableStateFlow(
        listOf(Quote(id = "q1", text = "To the other side.", vn = VnSummary(id = "v17", title = "Ever17")))
    )
    private val statsFlow = MutableStateFlow<SiteStats?>(
        SiteStats(chars = 100, producers = 10, releases = 50, staff = 20, tags = 30, traits = 40, vn = 25)
    )
    private val detailPairFlow = MutableStateFlow<Pair<VnSummary?, VnDetails?>>(
        VnSummary(id = "v17", title = "Ever17", rating = 86.4, voteCount = 7200) to
            VnDetails(id = "v17", devStatus = DevStatus.FINISHED)
    )

    override fun observeCachedHomeSection(section: HomeSection): Flow<List<VnSummary>> =
        homeSectionFlows.getValue(section)

    override suspend fun refreshHomeSection(
        section: HomeSection,
        maxItems: Int,
        forceRefresh: Boolean
    ): AppResult<List<VnSummary>> {
        if (section in failSections) {
            homeSectionFlows.getValue(section).value = emptyList()
            return AppResult.Failure(ApiError.Offline)
        }
        val list = listOf(VnSummary(id = "v17", title = "Ever17", rating = 86.4, voteCount = 7200))
        homeSectionFlows.getValue(section).value = list
        return AppResult.Success(list)
    }

    override fun observeCachedQuotes(): Flow<List<Quote>> = quotesFlow

    override suspend fun refreshHomeQuotes(maxResults: Int, forceRefresh: Boolean): AppResult<List<Quote>> =
        AppResult.Success(quotesFlow.value)

    override fun observeCachedSiteStats(): Flow<SiteStats?> = statsFlow

    override suspend fun refreshSiteStats(forceRefresh: Boolean): AppResult<SiteStats> =
        AppResult.Success(checkNotNull(statsFlow.value))

    override suspend fun searchVns(spec: VnQuerySpec): AppResult<PaginatedList<VnSummary>> {
        searchVnsCalls++
        return AppResult.Success(
            PaginatedList(
                items = listOf(VnSummary(id = "v17", title = "Ever17", rating = 86.4, voteCount = 7200)),
                hasMore = true,
                page = spec.page
            )
        )
    }

    override suspend fun searchCharacters(
        filters: CharacterSearchFilters,
        page: Int,
        maxResults: Int
    ): AppResult<PaginatedList<Character>> =
        AppResult.Success(
            PaginatedList(
                items = listOf(Character(id = "c1", name = "Tsugumi Komachi")),
                hasMore = false,
                page = page
            )
        )

    override suspend fun searchTags(query: String): AppResult<List<Tag>> =
        AppResult.Success(listOf(Tag(id = "g7", name = "Sci-fi")))

    override suspend fun searchProducers(query: String): AppResult<List<Producer>> =
        AppResult.Success(listOf(Producer(id = "p1", name = "KID")))

    override suspend fun searchTraits(query: String): AppResult<List<Trait>> =
        AppResult.Success(listOf(Trait(id = "i1", name = "Tsundere", groupName = "Personality")))

    override suspend fun fetchRandomVn(): AppResult<VnSummary> =
        AppResult.Success(VnSummary(id = "v17", title = "Ever17", rating = 86.4, voteCount = 7200))

    override fun observeVnSummaryAndDetails(vnId: String): Flow<Pair<VnSummary?, VnDetails?>> =
        detailPairFlow

    override suspend fun getCachedVnSummary(vnId: String): VnSummary? = detailPairFlow.value.first

    override suspend fun cacheVnSummary(summary: VnSummary) {
        detailPairFlow.value = summary to detailPairFlow.value.second
    }

    override suspend fun loadVnFullDetails(
        vnId: String,
        forceRefresh: Boolean
    ): AppResult<VnFullDetailPayload> {
        val summary = VnSummary(id = vnId, title = "Ever17", rating = 86.4, voteCount = 7200)
        val details = VnDetails(id = vnId, devStatus = DevStatus.FINISHED)
        detailPairFlow.value = summary to details
        return AppResult.Success(VnFullDetailPayload(summary, details))
    }

    override suspend fun fetchRelatedVnSummaries(vnIds: List<String>): AppResult<List<VnSummary>> =
        AppResult.Success(emptyList())

    override suspend fun fetchCharactersForVn(vnId: String): AppResult<PaginatedList<Character>> =
        AppResult.Success(
            PaginatedList(
                items = listOf(Character(id = "c1", name = "Tsugumi Komachi")),
                hasMore = false
            )
        )

    override suspend fun fetchQuotesForVn(vnId: String): AppResult<List<Quote>> =
        AppResult.Success(quotesFlow.value)

    override suspend fun fetchReviewsForVn(vnId: String): AppResult<PaginatedList<Review>> {
        if (failReviews) return AppResult.Failure(ApiError.Timeout)
        return AppResult.Success(
            PaginatedList(
                items = listOf(Review(id = "w1", vnId = vnId, username = "reader", text = "Masterpiece!")),
                hasMore = false
            )
        )
    }

    override suspend fun fetchFullReviewText(reviewId: String): AppResult<String> =
        AppResult.Success("Full text")

    override suspend fun clearTransientCaches() = Unit
}

private class FakeVmCollectionRepository : CollectionRepository {
    val entriesFlow = MutableStateFlow<Map<String, CollectionEntry>>(emptyMap())

    override fun observeRecentCollection(limit: Int): Flow<List<CollectionItemWithVn>> =
        entriesFlow.map { map ->
            map.values.take(limit).map { entry ->
                CollectionItemWithVn(
                    entry = entry,
                    vn = VnSummary(id = entry.vnId, title = entry.title),
                    details = null
                )
            }
        }

    override fun observeAllVisibleEntriesMap(): Flow<Map<String, CollectionEntry>> = entriesFlow

    override fun observeEntryById(vnId: String): Flow<CollectionEntry?> =
        entriesFlow.map { it[vnId] }

    override fun observeFilteredCollection(
        filters: CollectionFilters,
        sort: SortOption
    ): Flow<List<CollectionItemWithVn>> =
        entriesFlow.map { map ->
            map.values.map { entry ->
                CollectionItemWithVn(
                    entry = entry,
                    vn = VnSummary(id = entry.vnId, title = entry.title),
                    details = null
                )
            }
        }

    override fun observeVisibleCount(): Flow<Int> = entriesFlow.map { it.size }

    override suspend fun saveOrUpdateEntry(
        vn: VnSummary,
        status: CollectionStatus,
        vote: Int?,
        notes: String?,
        startedDate: LocalDate?,
        finishedDate: LocalDate?
    ) {
        entriesFlow.update { current ->
            current + (
                vn.id to CollectionEntry(
                    vnId = vn.id,
                    title = vn.title,
                    status = status,
                    vote = vote,
                    notes = notes,
                    startedDate = startedDate,
                    finishedDate = finishedDate,
                    lastModifiedAt = Instant.EPOCH
                )
                )
        }
    }

    override suspend fun removeEntry(vnId: String) {
        entriesFlow.update { it - vnId }
    }

    override suspend fun batchUpdateEntries(
        vnIds: List<String>,
        status: CollectionStatus,
        overrideVote: Int?
    ) {
        entriesFlow.update { current ->
            val mutable = current.toMutableMap()
            vnIds.forEach { id ->
                mutable[id]?.let { existing ->
                    mutable[id] = existing.copy(
                        status = status,
                        vote = overrideVote ?: existing.vote
                    )
                }
            }
            mutable
        }
    }

    override suspend fun removeEntries(vnIds: List<String>) {
        entriesFlow.update { it - vnIds.toSet() }
    }

    override suspend fun clearLocalCollection(queueForRemoteDelete: Boolean) {
        entriesFlow.value = emptyMap()
    }

    override suspend fun hydrateMissingSummaries(vnIds: List<String>) = Unit
}

private class FakeVmSettingsRepository : SettingsRepository {
    private val _settings = MutableStateFlow(AppSettings())
    private val _filters = MutableStateFlow(CollectionFilters())
    private val _sort = MutableStateFlow(SortOption.DEFAULT_LOCAL)
    private val _revealed = MutableStateFlow<Set<String>>(emptySet())

    override val settingsFlow: Flow<AppSettings> = _settings
    override val collectionFiltersFlow: Flow<CollectionFilters> = _filters
    override val collectionSortFlow: Flow<SortOption> = _sort
    override val nsfwRevealedIdsFlow: StateFlow<Set<String>> = _revealed.asStateFlow()

    override fun toggleNsfwReveal(id: String) {
        _revealed.update { if (id in it) it - id else it + id }
    }

    override suspend fun getSettingsSnapshot(): AppSettings = _settings.value

    override suspend fun saveSettings(settings: AppSettings) {
        _settings.value = settings
    }

    override suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        _settings.update(transform)
    }

    override suspend fun resetSettingsToDefaults() {
        _settings.value = AppSettings()
    }

    override suspend fun saveCollectionFilters(filters: CollectionFilters) {
        _filters.value = filters
    }

    override suspend fun saveCollectionSort(sort: SortOption) {
        _sort.value = sort
    }
}

private class FakeVmSyncService : SyncService {
    private val _progress = MutableStateFlow(SyncProgress())
    private val _account = MutableStateFlow<UserAccount?>(null)
    private val _synced = MutableStateFlow(false)

    override val progressFlow: StateFlow<SyncProgress> = _progress.asStateFlow()
    override val activeAccountFlow: Flow<UserAccount?> = _account
    override val isAccountSyncedFlow: Flow<Boolean> = _synced

    override suspend fun authenticateAndSync(
        rawToken: String,
        onProgress: (SyncProgress) -> Unit
    ): AppResult<Pair<UserAccount, SyncReport>> {
        val account = UserAccount("u42", "tester", setOf("listread", "listwrite"))
        _account.value = account
        _synced.value = true
        return AppResult.Success(account to SyncReport(pushed = 1, pulled = 2, deletedLocal = 0, deletedRemote = 0))
    }

    override suspend fun syncCurrentAccount(
        onProgress: (SyncProgress) -> Unit
    ): AppResult<SyncReport> =
        AppResult.Success(SyncReport(pushed = 0, pulled = 0, deletedLocal = 0, deletedRemote = 0))

    override suspend fun queueLocalDeletions(vnIds: List<String>) = Unit

    override suspend fun logout(clearLocalCollection: Boolean) {
        _account.value = null
        _synced.value = false
    }
}
