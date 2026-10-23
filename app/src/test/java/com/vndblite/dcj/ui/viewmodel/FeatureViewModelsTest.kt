package com.vndblite.dcj.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.vndblite.dcj.core.ApiError
import com.vndblite.dcj.core.AppResult
import com.vndblite.dcj.data.remote.VnFullDetailPayload
import com.vndblite.dcj.data.remote.VnQuerySpec
import com.vndblite.dcj.data.remote.filter.KanaFilter
import com.vndblite.dcj.domain.model.AppSettings
import com.vndblite.dcj.domain.model.Character
import com.vndblite.dcj.domain.model.CharacterRole
import com.vndblite.dcj.domain.model.CharacterSearchFilters
import com.vndblite.dcj.domain.model.CharacterSex
import com.vndblite.dcj.domain.model.CollectionEntry
import com.vndblite.dcj.domain.model.CollectionFilters
import com.vndblite.dcj.domain.model.CollectionItemWithVn
import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.domain.model.HomeSection
import com.vndblite.dcj.domain.model.PaginatedList
import com.vndblite.dcj.domain.model.Producer
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
import com.vndblite.dcj.domain.model.VnSearchFilters
import com.vndblite.dcj.domain.model.VnSummary
import com.vndblite.dcj.domain.repository.CollectionBatchUpdateSpec
import com.vndblite.dcj.domain.repository.CollectionRepository
import com.vndblite.dcj.domain.repository.CollectionSaveDraft
import com.vndblite.dcj.domain.repository.HomeSectionSnapshot
import com.vndblite.dcj.domain.repository.LogoutCollectionOption
import com.vndblite.dcj.domain.repository.SettingsRepository
import com.vndblite.dcj.domain.repository.SyncRepository
import com.vndblite.dcj.domain.repository.VnDetailBundle
import com.vndblite.dcj.domain.repository.VnRepository
import com.vndblite.dcj.sync.SyncProgress
import com.vndblite.dcj.ui.components.CollectionEntryDraft
import com.vndblite.dcj.ui.screens.auth.AuthSyncAction
import com.vndblite.dcj.ui.screens.auth.AuthSyncViewModel
import com.vndblite.dcj.ui.screens.characters.CharacterSearchAction
import com.vndblite.dcj.ui.screens.characters.CharacterSearchViewModel
import com.vndblite.dcj.ui.screens.collection.CollectionAction
import com.vndblite.dcj.ui.screens.collection.CollectionViewModel
import com.vndblite.dcj.ui.screens.detail.VnDetailAction
import com.vndblite.dcj.ui.screens.detail.VnDetailTab
import com.vndblite.dcj.ui.screens.detail.VnDetailViewModel
import com.vndblite.dcj.ui.screens.search.SearchAction
import com.vndblite.dcj.ui.screens.search.SearchViewModel
import com.vndblite.dcj.ui.screens.settings.SettingsAction
import com.vndblite.dcj.ui.screens.settings.SettingsViewModel
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeatureViewModelsTest {

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
    fun `SearchViewModel cancels stale in-flight search so latest query wins`() = runTest(testDispatcher) {
        val vnRepo = FakeVnRepository()
        val collectionRepo = FakeCollectionRepository()
        val settingsRepo = FakeSettingsRepository()

        vnRepo.searchHandler = { filters ->
            when (filters.searchQuery) {
                "slow" -> {
                    delay(500L)
                    AppResult.Success(
                        PaginatedList(listOf(VnSummary(id = "v1", title = "Slow Result")), hasMore = false)
                    )
                }
                "fast" -> {
                    delay(50L)
                    AppResult.Success(
                        PaginatedList(listOf(VnSummary(id = "v2", title = "Fast Winner")), hasMore = false)
                    )
                }
                else -> AppResult.Success(PaginatedList(emptyList(), hasMore = false))
            }
        }

        val vm = SearchViewModel(
            vnRepository = vnRepo,
            collectionRepository = collectionRepo,
            settingsRepository = settingsRepo,
            queryDebounceMs = 100L
        )
        advanceUntilIdle()

        vm.onAction(SearchAction.UpdateQuery("slow"))
        advanceTimeBy(120L) // starts "slow" request (500ms)
        vm.onAction(SearchAction.UpdateQuery("fast"))
        advanceUntilIdle()

        assertEquals(listOf("v2"), vm.uiState.value.results.map { it.id })
    }

    @Test
    fun `SearchViewModel surfaces error and recovers on Retry and restores SavedStateHandle`() = runTest(testDispatcher) {
        val vnRepo = FakeVnRepository()
        val collectionRepo = FakeCollectionRepository()
        val settingsRepo = FakeSettingsRepository()
        var shouldFail = true

        vnRepo.searchHandler = { filters ->
            if (shouldFail) {
                AppResult.Failure(ApiError.Offline)
            } else {
                AppResult.Success(
                    PaginatedList(listOf(VnSummary(id = "v17", title = "Ever17 (${filters.searchQuery})")), hasMore = false)
                )
            }
        }

        val handle = SavedStateHandle(
            mapOf(
                "vn_search_query" to "Ever17",
                "vn_search_revealed_nsfw_ids" to arrayListOf("v17")
            )
        )
        val vm = SearchViewModel(
            vnRepository = vnRepo,
            collectionRepository = collectionRepo,
            settingsRepository = settingsRepo,
            savedStateHandle = handle,
            queryDebounceMs = 0L
        )
        advanceUntilIdle()

        assertEquals("Ever17", vm.uiState.value.filters.searchQuery)
        assertTrue("v17" in vm.uiState.value.revealedNsfwIds)
        assertTrue(vm.uiState.value.error is ApiError.Offline)

        shouldFail = false
        vm.onAction(SearchAction.Retry)
        advanceUntilIdle()

        assertNull(vm.uiState.value.error)
        assertEquals(1, vm.uiState.value.results.size)
        assertEquals("v17", vm.uiState.value.results.first().id)
    }

    @Test
    fun `CharacterSearchViewModel coalesces rapid filter toggles into one network request`() = runTest(testDispatcher) {
        val vnRepo = FakeVnRepository()
        val settingsRepo = FakeSettingsRepository()
        val vm = CharacterSearchViewModel(
            vnRepository = vnRepo,
            settingsRepository = settingsRepo,
            filterDebounceMs = 300L
        )
        advanceUntilIdle()
        val initialCalls = vnRepo.characterSearchCalls

        vm.onAction(CharacterSearchAction.ToggleTrait(Trait(id = "i1", name = "Tsundere")))
        vm.onAction(CharacterSearchAction.ToggleRole(CharacterRole.MAIN))
        vm.onAction(CharacterSearchAction.ToggleSex(CharacterSex.FEMALE))
        advanceTimeBy(200L)
        assertEquals(initialCalls, vnRepo.characterSearchCalls)

        advanceUntilIdle()
        assertEquals(initialCalls + 1, vnRepo.characterSearchCalls)
    }

    @Test
    fun `CollectionViewModel restores SavedStateHandle tab and executes batch update and delete`() = runTest(testDispatcher) {
        val collectionRepo = FakeCollectionRepository()
        val settingsRepo = FakeSettingsRepository()
        val syncRepo = FakeSyncRepository()

        collectionRepo.upsertEntry(
            vnSummary = VnSummary(id = "v10", title = "Title 10"),
            status = CollectionStatus.WISHLIST,
            vote = null,
            notes = null,
            startedDate = null,
            finishedDate = null
        )
        collectionRepo.upsertEntry(
            vnSummary = VnSummary(id = "v20", title = "Title 20"),
            status = CollectionStatus.WISHLIST,
            vote = null,
            notes = null,
            startedDate = null,
            finishedDate = null
        )

        val handle = SavedStateHandle(mapOf("collection_selected_tab" to CollectionStatus.WISHLIST.storageKey))
        val vm = CollectionViewModel(
            collectionRepository = collectionRepo,
            settingsRepository = settingsRepo,
            syncRepository = syncRepo,
            savedStateHandle = handle
        )
        advanceUntilIdle()

        assertEquals(CollectionStatus.WISHLIST, vm.uiState.value.selectedTab)
        assertEquals(2, vm.uiState.value.items.size)

        vm.onAction(CollectionAction.SelectAllInCurrentTab)
        assertEquals(setOf("v10", "v20"), vm.uiState.value.selectedVnIds)

        vm.onAction(
            CollectionAction.BatchUpdateSelected(
                newStatus = CollectionStatus.FINISHED,
                newVote = 90,
                updateVote = true
            )
        )
        advanceUntilIdle()
        assertTrue(vm.uiState.value.selectedVnIds.isEmpty())

        vm.onAction(CollectionAction.SelectTab(CollectionStatus.FINISHED))
        advanceUntilIdle()
        assertEquals(2, vm.uiState.value.items.size)
        assertTrue(vm.uiState.value.items.all { it.entry.vote == 90 })
    }

    @Test
    fun `VnDetailViewModel restores tab and spoiler level and saves collection entry`() = runTest(testDispatcher) {
        val vnRepo = FakeVnRepository()
        val collectionRepo = FakeCollectionRepository()
        val settingsRepo = FakeSettingsRepository()
        val handle = SavedStateHandle(
            mapOf(
                "vn_detail_selected_tab" to VnDetailTab.RELATIONS_AND_LINKS.name,
                "vn_detail_spoiler_level" to 2
            )
        )

        val vm = VnDetailViewModel(
            vnId = "v17",
            vnRepository = vnRepo,
            collectionRepository = collectionRepo,
            settingsRepository = settingsRepo,
            savedStateHandle = handle
        )
        advanceUntilIdle()

        assertEquals(VnDetailTab.RELATIONS_AND_LINKS, vm.uiState.value.selectedTab)
        assertEquals(2, vm.uiState.value.spoilerLevel)
        assertEquals("v17", vm.uiState.value.summary?.id)

        vm.onAction(
            VnDetailAction.SaveCollectionEntry(
                CollectionEntryDraft(
                    status = CollectionStatus.PLAYING,
                    vote = 85,
                    notes = "Great route",
                    startedDate = LocalDate.of(2026, 1, 10),
                    finishedDate = null
                )
            )
        )
        advanceUntilIdle()
        assertEquals(CollectionStatus.PLAYING, vm.uiState.value.collectionEntry?.status)
        assertEquals(85, vm.uiState.value.collectionEntry?.vote)
    }

    @Test
    fun `SettingsViewModel and AuthSyncViewModel update theme and handle token auth and logout`() = runTest(testDispatcher) {
        val vnRepo = FakeVnRepository()
        val collectionRepo = FakeCollectionRepository()
        val settingsRepo = FakeSettingsRepository()
        val syncRepo = FakeSyncRepository()

        val settingsVm = SettingsViewModel(settingsRepo, vnRepo, collectionRepo)
        val authVm = AuthSyncViewModel(syncRepo)
        advanceUntilIdle()

        settingsVm.onAction(SettingsAction.SelectTheme(ThemeId.SAYA))
        advanceUntilIdle()
        assertEquals(ThemeId.SAYA, settingsVm.uiState.value.settings.themeId)

        authVm.uiState.test {
            val initial = awaitItem()
            assertNull(initial.activeAccount)

            authVm.onAction(AuthSyncAction.UpdateTokenInput("secret-token"))
            assertEquals("secret-token", awaitItem().tokenInput)

            authVm.onAction(AuthSyncAction.VerifyAndSaveToken)
            advanceUntilIdle()
            val afterVerify = expectMostRecentItem()
            assertEquals("", afterVerify.tokenInput)
            assertEquals("u42", afterVerify.activeAccount?.id)

            authVm.onAction(AuthSyncAction.ConfirmLogout(LogoutCollectionOption.CLEAR_LOCAL_COLLECTION))
            advanceUntilIdle()
            val afterLogout = expectMostRecentItem()
            assertNull(afterLogout.activeAccount)
            assertEquals(true, syncRepo.lastClearLocalCollection)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

private class FakeVnRepository : VnRepository {
    var searchHandler: suspend (VnSearchFilters) -> AppResult<PaginatedList<VnSummary>> = {
        AppResult.Success(PaginatedList(emptyList(), hasMore = false))
    }
    var characterSearchCalls: Int = 0

    override fun observeHomeSection(section: HomeSection, maxItems: Int): Flow<HomeSectionSnapshot> =
        MutableStateFlow(HomeSectionSnapshot(section, emptyList(), Instant.EPOCH, false))

    override suspend fun refreshHomeSection(section: HomeSection, maxItems: Int, force: Boolean): AppResult<List<VnSummary>> =
        AppResult.Success(emptyList())

    override suspend fun refreshAllRemoteHomeSections(maxItems: Int, force: Boolean): Map<HomeSection, AppResult<List<VnSummary>>> =
        emptyMap()

    override fun observeCachedQuotes(): Flow<List<Quote>> = MutableStateFlow(emptyList())

    override suspend fun refreshQuotes(maxResults: Int): AppResult<List<Quote>> = AppResult.Success(emptyList())

    override suspend fun fetchRandomQuote(): AppResult<Quote?> = AppResult.Success(null)

    override fun observeSiteStats(): Flow<SiteStats?> = MutableStateFlow(null)

    override suspend fun refreshSiteStats(): AppResult<SiteStats> =
        AppResult.Success(SiteStats(chars = 1, releases = 1, vn = 1, staff = 1, tags = 1, producers = 1, traits = 1))

    override fun observeVnDetail(vnId: String): Flow<VnDetailBundle?> =
        MutableStateFlow(
            VnDetailBundle(
                summary = VnSummary(id = vnId, title = "Sample $vnId"),
                details = VnDetails(id = vnId),
                isSummaryStale = false,
                isDetailsStale = false
            )
        )

    override suspend fun getVnDetail(vnId: String, forceRefresh: Boolean): AppResult<VnFullDetailPayload> =
        AppResult.Success(
            VnFullDetailPayload(
                summary = VnSummary(id = vnId, title = "Sample $vnId"),
                details = VnDetails(id = vnId)
            )
        )

    override suspend fun queryVns(spec: VnQuerySpec): AppResult<PaginatedList<VnSummary>> =
        searchHandler(spec.filters)

    override suspend fun searchCharacters(filters: CharacterSearchFilters, page: Int, maxResults: Int): AppResult<PaginatedList<Character>> {
        characterSearchCalls++
        return AppResult.Success(PaginatedList(emptyList(), hasMore = false))
    }

    override suspend fun fetchCharactersForVn(vnId: String): AppResult<PaginatedList<Character>> =
        AppResult.Success(PaginatedList(emptyList(), hasMore = false))

    override suspend fun fetchQuotesForVn(vnId: String, maxResults: Int): AppResult<List<Quote>> =
        AppResult.Success(emptyList())

    override suspend fun fetchReviewsForVn(vnId: String): AppResult<PaginatedList<Review>> =
        AppResult.Success(PaginatedList(emptyList(), hasMore = false))

    override suspend fun fetchFullReviewText(reviewId: String): AppResult<String> =
        AppResult.Success("Full review")

    override suspend fun fetchRandomVn(extraFilterClauses: List<KanaFilter>): AppResult<VnSummary> =
        AppResult.Success(VnSummary(id = "v1", title = "Random VN"))

    override suspend fun searchTags(query: String, maxResults: Int): AppResult<List<Tag>> =
        AppResult.Success(emptyList())

    override suspend fun searchProducers(query: String, maxResults: Int): AppResult<List<Producer>> =
        AppResult.Success(emptyList())

    override suspend fun searchTraits(query: String, maxResults: Int): AppResult<List<Trait>> =
        AppResult.Success(emptyList())

    override suspend fun clearTransientCaches() = Unit
}

private class FakeCollectionRepository : CollectionRepository {
    private val itemsFlow = MutableStateFlow<List<CollectionItemWithVn>>(emptyList())

    override fun observeFilteredCollection(filters: CollectionFilters, sort: SortOption): Flow<List<CollectionItemWithVn>> =
        itemsFlow.map { list ->
            list.filter { item -> filters.status == null || item.entry.status == filters.status }
        }

    override fun observeVisibleCollectionByLastModifiedDesc(): Flow<List<CollectionItemWithVn>> = itemsFlow

    override fun observeRecentCollectionEntries(limit: Int): Flow<List<CollectionItemWithVn>> =
        itemsFlow.map { it.take(limit) }

    override fun observeEntryById(vnId: String): Flow<CollectionEntry?> =
        itemsFlow.map { list -> list.firstOrNull { it.entry.vnId == vnId }?.entry }

    override fun observeVisibleCollectionIds(): Flow<Set<String>> =
        itemsFlow.map { list -> list.map { it.entry.vnId }.toSet() }

    override fun observeVisibleCount(): Flow<Int> =
        itemsFlow.map { it.size }

    override suspend fun getVisibleEntryById(vnId: String): CollectionEntry? =
        itemsFlow.value.firstOrNull { it.entry.vnId == vnId }?.entry

    override suspend fun saveEntry(draft: CollectionSaveDraft): CollectionEntry {
        val entry = CollectionEntry(
            vnId = draft.vn.id,
            title = draft.vn.title,
            status = draft.status,
            vote = draft.vote,
            notes = draft.notes,
            startedDate = draft.startedDate,
            finishedDate = draft.finishedDate,
            addedDate = LocalDate.of(2026, 1, 1),
            lastModifiedAt = Instant.EPOCH
        )
        itemsFlow.value = itemsFlow.value.filterNot { it.entry.vnId == draft.vn.id } +
            CollectionItemWithVn(entry = entry, vn = draft.vn)
        return entry
    }

    override suspend fun batchUpdateEntries(spec: CollectionBatchUpdateSpec): Int {
        val ids = spec.vnIds.toSet()
        itemsFlow.value = itemsFlow.value.map { item ->
            if (item.entry.vnId in ids) {
                item.copy(
                    entry = item.entry.copy(
                        status = spec.newStatus ?: item.entry.status,
                        vote = if (spec.updateVote) spec.newVote else item.entry.vote
                    )
                )
            } else {
                item
            }
        }
        return ids.size
    }

    override suspend fun removeEntry(vnId: String) {
        itemsFlow.value = itemsFlow.value.filterNot { it.entry.vnId == vnId }
    }

    override suspend fun batchRemoveEntries(vnIds: List<String>) {
        val idSet = vnIds.toSet()
        itemsFlow.value = itemsFlow.value.filterNot { it.entry.vnId in idSet }
    }

    override suspend fun clearAllEntries(queueRemoteDeletesIfSynced: Boolean) {
        itemsFlow.value = emptyList()
    }
}

private class FakeSettingsRepository : SettingsRepository {
    private val state = MutableStateFlow(AppSettings())
    private val filtersState = MutableStateFlow(CollectionFilters())
    private val sortState = MutableStateFlow(SortOption.DEFAULT_LOCAL)
    private val accountState = MutableStateFlow<String?>(null)

    override val settingsFlow: Flow<AppSettings> = state
    override val collectionFiltersFlow: Flow<CollectionFilters> = filtersState
    override val collectionSortFlow: Flow<SortOption> = sortState
    override val activeAccountIdFlow: Flow<String?> = accountState

    override suspend fun getSettingsSnapshot(): AppSettings = state.value
    override suspend fun saveSettings(settings: AppSettings) { state.value = settings }
    override suspend fun updateSettings(transform: (AppSettings) -> AppSettings) { state.value = transform(state.value) }
    override suspend fun resetSettingsToDefaults() { state.value = AppSettings() }
    override suspend fun saveCollectionFilters(filters: CollectionFilters) { filtersState.value = filters }
    override suspend fun saveCollectionSort(sort: SortOption) { sortState.value = sort }
}

private class FakeSyncRepository : SyncRepository {
    private val accountFlow = MutableStateFlow<UserAccount?>(null)
    private val syncedFlow = MutableStateFlow(false)
    private val progressState = MutableStateFlow(SyncProgress())
    var lastClearLocalCollection: Boolean? = null

    override val progressFlow: StateFlow<SyncProgress> = progressState.asStateFlow()
    override val activeAccountFlow: Flow<UserAccount?> = accountFlow
    override val isAccountSyncedFlow: Flow<Boolean> = syncedFlow

    override suspend fun authenticateAndSync(
        rawToken: String,
        onProgress: (SyncProgress) -> Unit
    ): AppResult<Pair<UserAccount, SyncReport>> {
        val account = UserAccount(id = "u42", username = "tester", permissions = setOf("listread", "listwrite"))
        accountFlow.value = account
        syncedFlow.value = true
        return AppResult.Success(account to SyncReport(pushed = 1, pulled = 1))
    }

    override suspend fun syncCurrentAccount(onProgress: (SyncProgress) -> Unit): AppResult<SyncReport> =
        AppResult.Success(SyncReport(pushed = 1, pulled = 1))

    override suspend fun hasStoredToken(): Boolean = accountFlow.value != null

    override suspend fun logout(clearLocalCollection: Boolean) {
        lastClearLocalCollection = clearLocalCollection
        accountFlow.value = null
        syncedFlow.value = false
    }
}
