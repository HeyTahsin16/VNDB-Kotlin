package com.vndblite.dcj.data.repository

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import com.vndblite.dcj.core.AppResult
import com.vndblite.dcj.core.Clock
import com.vndblite.dcj.core.DispatcherProvider
import com.vndblite.dcj.data.local.db.AppDatabase
import com.vndblite.dcj.data.local.db.entity.CollectionEntryEntity
import com.vndblite.dcj.data.local.db.entity.SyncMetadataEntity
import com.vndblite.dcj.data.local.prefs.DataStoreSettingsStore
import com.vndblite.dcj.data.local.secure.EncryptedTokenStore
import com.vndblite.dcj.data.local.secure.JvmAesGcmTokenCipher
import com.vndblite.dcj.data.remote.VnFullDetailPayload
import com.vndblite.dcj.data.remote.VnQuerySpec
import com.vndblite.dcj.data.remote.VndbApi
import com.vndblite.dcj.data.remote.filter.KanaFilter
import com.vndblite.dcj.data.remote.scraper.ReviewSource
import com.vndblite.dcj.domain.model.Character
import com.vndblite.dcj.domain.model.CharacterSearchFilters
import com.vndblite.dcj.domain.model.CollectionFilters
import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.domain.model.DevStatus
import com.vndblite.dcj.domain.model.HomeSection
import com.vndblite.dcj.domain.model.Language
import com.vndblite.dcj.domain.model.PaginatedList
import com.vndblite.dcj.domain.model.Platform
import com.vndblite.dcj.domain.model.Producer
import com.vndblite.dcj.domain.model.Quote
import com.vndblite.dcj.domain.model.RemoteUserListItem
import com.vndblite.dcj.domain.model.Review
import com.vndblite.dcj.domain.model.SiteStats
import com.vndblite.dcj.domain.model.SortField
import com.vndblite.dcj.domain.model.SortOption
import com.vndblite.dcj.domain.model.SyncState
import com.vndblite.dcj.domain.model.Tag
import com.vndblite.dcj.domain.model.ThemeId
import com.vndblite.dcj.domain.model.Trait
import com.vndblite.dcj.domain.model.UserAccount
import com.vndblite.dcj.domain.model.UserListPatch
import com.vndblite.dcj.domain.model.VnDetails
import com.vndblite.dcj.domain.model.VnRelation
import com.vndblite.dcj.domain.model.VnRelationType
import com.vndblite.dcj.domain.model.VnSummary
import com.vndblite.dcj.sync.SyncEngine
import java.io.File
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RepositoriesTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = AppDatabase.buildInMemory(context)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `CollectionRepositoryImpl saves, batch-updates, filters by metadata, and queues synced removals`() = runTest {
        val tempFile = File.createTempFile("repo_collection_", ".preferences_pb").apply {
            deleteOnExit()
            delete()
        }
        val settingsStore = DataStoreSettingsStore(
            PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = { tempFile })
        )
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val dispatchers = object : DispatcherProvider {
            override val main: CoroutineDispatcher = testDispatcher
            override val io: CoroutineDispatcher = testDispatcher
            override val default: CoroutineDispatcher = testDispatcher
        }
        var currentMillis = 1_750_000_000_000L
        val clock = Clock { currentMillis }
        val fakeApi = FakeRepoVndbApi()
        val tokenPrefs = context.getSharedPreferences("test_repo_tokens", Context.MODE_PRIVATE).apply {
            edit().clear().commit()
        }
        val tokenStore = EncryptedTokenStore(tokenPrefs, JvmAesGcmTokenCipher(), dispatchers)
        val syncEngine = SyncEngine(
            api = fakeApi,
            database = database,
            settingsStore = settingsStore,
            tokenStore = tokenStore,
            clock = clock,
            dispatchers = dispatchers
        )
        val collectionRepo = CollectionRepositoryImpl(
            database = database,
            api = fakeApi,
            settingsStore = settingsStore,
            syncService = syncEngine,
            clock = clock,
            dispatchers = dispatchers
        )

        val vn17 = VnSummary(
            id = "v17",
            title = "Ever17 -the out of infinity-",
            rating = 86.4,
            voteCount = 7200,
            originalLanguage = "ja"
        )
        val vn2 = VnSummary(
            id = "v2",
            title = "Kanon",
            rating = 82.0,
            voteCount = 5400,
            originalLanguage = "ja"
        )

        collectionRepo.saveOrUpdateEntry(
            vn = vn17,
            status = CollectionStatus.PLAYING,
            vote = 90,
            notes = "Route 1",
            startedDate = LocalDate.of(2025, 1, 10),
            finishedDate = null
        )
        currentMillis += 5_000L
        collectionRepo.saveOrUpdateEntry(
            vn = vn2,
            status = CollectionStatus.WISHLIST,
            vote = null,
            notes = null,
            startedDate = null,
            finishedDate = null
        )

        val visibleMap = collectionRepo.observeAllVisibleEntriesMap().first()
        assertEquals(2, visibleMap.size)
        assertEquals(SyncState.NEW, visibleMap["v17"]?.syncState)

        // Batch update both entries to FINISHED with overrideVote = 95
        currentMillis += 5_000L
        collectionRepo.batchUpdateEntries(
            vnIds = listOf("v17", "v2"),
            status = CollectionStatus.FINISHED,
            overrideVote = 95
        )

        val updatedMap = collectionRepo.observeAllVisibleEntriesMap().first()
        assertEquals(CollectionStatus.FINISHED, updatedMap["v17"]?.status)
        assertEquals(95, updatedMap["v17"]?.vote)
        assertEquals(CollectionStatus.FINISHED, updatedMap["v2"]?.status)
        assertEquals(95, updatedMap["v2"]?.vote)

        // Mark account synced and entry CLEAN, then remove v17 -> should queue in pending_remote_deletions
        settingsStore.setActiveAccountId("u42")
        database.syncMetadataDao().upsert(
            SyncMetadataEntity.fromUserAccount(
                account = UserAccount(id = "u42", username = "tester", permissions = setOf("listread", "listwrite")),
                isSynced = true,
                lastSyncedAtEpochSeconds = currentMillis / 1000L
            )
        )
        database.collectionDao().upsertEntry(
            CollectionEntryEntity.fromDomain(
                entry = updatedMap.getValue("v17").copy(
                    syncState = SyncState.CLEAN,
                    remoteLastMod = Instant.ofEpochSecond(currentMillis / 1000L)
                ),
                accountId = "u42"
            )
        )
        collectionRepo.removeEntry("v17")

        val afterRemoveVisible = collectionRepo.observeAllVisibleEntriesMap().first()
        assertNull(afterRemoveVisible["v17"])
        assertNotNull(afterRemoveVisible["v2"])

        val pendingDeletes = database.pendingRemoteDeletionDao().getPendingForAccount("u42")
        assertEquals(1, pendingDeletes.size)
        assertEquals("v17", pendingDeletes.first().vnId)
    }

    @Test
    fun `VnRepositoryImpl caches home sections with TTL and hydrates related VNs on detail load`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val dispatchers = object : DispatcherProvider {
            override val main: CoroutineDispatcher = testDispatcher
            override val io: CoroutineDispatcher = testDispatcher
            override val default: CoroutineDispatcher = testDispatcher
        }
        var currentMillis = 1_750_000_000_000L
        val clock = Clock { currentMillis }
        val fakeApi = FakeRepoVndbApi()
        val fakeReviews = FakeRepoReviewSource()

        val vnRepo = VnRepositoryImpl(
            api = fakeApi,
            reviewSource = fakeReviews,
            database = database,
            clock = clock,
            dispatchers = dispatchers
        )

        val firstRefresh = vnRepo.refreshHomeSection(HomeSection.TOP_RATED, maxItems = 10, forceRefresh = false)
        assertTrue(firstRefresh is AppResult.Success)
        assertEquals(1, fakeApi.queryVnsCalls)

        // Second refresh within TTL should return cached summaries without calling API
        currentMillis += 60_000L
        val secondRefresh = vnRepo.refreshHomeSection(HomeSection.TOP_RATED, maxItems = 10, forceRefresh = false)
        assertTrue(secondRefresh is AppResult.Success)
        assertEquals(1, fakeApi.queryVnsCalls)

        // Load full details and verify related VN summary is hydrated and persisted
        val detailResult = vnRepo.loadVnFullDetails("v17", forceRefresh = true)
        assertTrue(detailResult is AppResult.Success)
        val cachedPair = vnRepo.observeVnSummaryAndDetails("v17").first()
        assertEquals("Ever17", cachedPair.first?.title)
        assertEquals(DevStatus.FINISHED, cachedPair.second?.devStatus)
        assertEquals("Remember11", cachedPair.second?.relations?.firstOrNull()?.relatedVn?.title)
    }

    @Test
    fun `SettingsRepositoryImpl toggles session NSFW reveal set and persists settings`() = runTest {
        val tempFile = File.createTempFile("repo_settings_", ".preferences_pb").apply {
            deleteOnExit()
            delete()
        }
        val settingsStore = DataStoreSettingsStore(
            PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = { tempFile })
        )
        val settingsRepo = SettingsRepositoryImpl(settingsStore)

        settingsRepo.toggleNsfwReveal("v17")
        assertTrue("v17" in settingsRepo.nsfwRevealedIdsFlow.value)
        settingsRepo.toggleNsfwReveal("v17")
        assertTrue("v17" !in settingsRepo.nsfwRevealedIdsFlow.value)

        settingsRepo.updateSettings { it.copy(themeId = ThemeId.FATE, showCoverCensor = false) }
        val snapshot = settingsRepo.getSettingsSnapshot()
        assertEquals(ThemeId.FATE, snapshot.themeId)
        assertEquals(false, snapshot.showCoverCensor)
    }
}

private class FakeRepoVndbApi : VndbApi {
    var queryVnsCalls = 0

    override suspend fun queryVns(spec: VnQuerySpec): AppResult<PaginatedList<VnSummary>> {
        queryVnsCalls++
        return AppResult.Success(
            PaginatedList(
                items = listOf(VnSummary(id = "v17", title = "Ever17", rating = 86.4, voteCount = 7200)),
                hasMore = false,
                page = 1
            )
        )
    }

    override suspend fun fetchVnSummariesByIds(
        ids: List<String>,
        includeDescription: Boolean
    ): AppResult<List<VnSummary>> {
        return AppResult.Success(
            ids.map { id ->
                VnSummary(
                    id = id,
                    title = if (id == "v18") "Remember11" else "Title $id",
                    rating = 84.0,
                    voteCount = 3000
                )
            }
        )
    }

    override suspend fun fetchVnFullDetails(vnId: String): AppResult<VnFullDetailPayload> {
        return AppResult.Success(
            VnFullDetailPayload(
                summary = VnSummary(
                    id = vnId,
                    title = "Ever17",
                    rating = 86.4,
                    voteCount = 7200,
                    description = "Sci-fi mystery"
                ),
                details = VnDetails(
                    id = vnId,
                    devStatus = DevStatus.FINISHED,
                    languages = listOf(Language.JA.code, Language.EN.code),
                    platforms = listOf(Platform.WIN.code),
                    relations = listOf(
                        VnRelation(
                            id = "v18",
                            relationType = VnRelationType.SAME_SERIES,
                            rawRelationCode = "ser",
                            isOfficial = true
                        )
                    )
                )
            )
        )
    }

    override suspend fun fetchRandomVn(extraFilterClauses: List<KanaFilter>): AppResult<VnSummary> =
        AppResult.Success(VnSummary(id = "v17", title = "Ever17"))

    override suspend fun fetchCharactersForVn(vnId: String): AppResult<PaginatedList<Character>> =
        AppResult.Success(PaginatedList(emptyList(), hasMore = false))

    override suspend fun searchCharacters(
        filters: CharacterSearchFilters,
        page: Int,
        maxResults: Int
    ): AppResult<PaginatedList<Character>> =
        AppResult.Success(PaginatedList(emptyList(), hasMore = false))

    override suspend fun fetchTopQuotes(maxResults: Int): AppResult<List<Quote>> =
        AppResult.Success(emptyList())

    override suspend fun fetchRandomQuote(): AppResult<Quote?> = AppResult.Success(null)

    override suspend fun fetchQuotesForVn(vnId: String, maxResults: Int): AppResult<List<Quote>> =
        AppResult.Success(emptyList())

    override suspend fun searchTags(query: String, maxResults: Int): AppResult<List<Tag>> =
        AppResult.Success(emptyList())

    override suspend fun searchProducers(query: String, maxResults: Int): AppResult<List<Producer>> =
        AppResult.Success(emptyList())

    override suspend fun searchTraits(query: String, maxResults: Int): AppResult<List<Trait>> =
        AppResult.Success(emptyList())

    override suspend fun fetchStats(): AppResult<SiteStats> =
        AppResult.Success(SiteStats(chars = 100, producers = 10, releases = 50, staff = 20, tags = 30, traits = 40, vn = 25))

    override suspend fun authenticate(token: String): AppResult<UserAccount> =
        AppResult.Success(UserAccount(id = "u42", username = "tester", permissions = setOf("listread", "listwrite")))

    override suspend fun fetchUserList(userId: String, token: String): AppResult<List<RemoteUserListItem>> =
        AppResult.Success(emptyList())

    override suspend fun patchUserListEntry(token: String, patch: UserListPatch): AppResult<Unit> =
        AppResult.Success(Unit)

    override suspend fun deleteUserListEntry(token: String, vnId: String): AppResult<Unit> =
        AppResult.Success(Unit)
}

private class FakeRepoReviewSource : ReviewSource {
    override suspend fun fetchReviewsForVn(vnId: String): AppResult<PaginatedList<Review>> =
        AppResult.Success(
            PaginatedList(
                items = listOf(Review(id = "w1", vnId = vnId, username = "reviewer", text = "Great VN!")),
                hasMore = false
            )
        )

    override suspend fun fetchFullReviewText(reviewId: String): AppResult<String> =
        AppResult.Success("Full review text")
}
