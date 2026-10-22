package com.vndblite.dcj.sync

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import com.vndblite.dcj.core.ApiError
import com.vndblite.dcj.core.AppResult
import com.vndblite.dcj.core.Clock
import com.vndblite.dcj.core.DispatcherProvider
import com.vndblite.dcj.data.local.db.AppDatabase
import com.vndblite.dcj.data.local.db.entity.CollectionEntryEntity
import com.vndblite.dcj.data.local.prefs.DataStoreSettingsStore
import com.vndblite.dcj.data.local.prefs.SettingsStore
import com.vndblite.dcj.data.local.secure.EncryptedTokenStore
import com.vndblite.dcj.data.local.secure.JvmAesGcmTokenCipher
import com.vndblite.dcj.data.local.secure.TokenStore
import com.vndblite.dcj.data.remote.VnFullDetailPayload
import com.vndblite.dcj.data.remote.VnQuerySpec
import com.vndblite.dcj.data.remote.VndbApi
import com.vndblite.dcj.data.remote.VndbApiImpl
import com.vndblite.dcj.data.remote.filter.KanaFilter
import com.vndblite.dcj.data.remote.http.HttpClientFactory
import com.vndblite.dcj.data.remote.http.RateLimiter
import com.vndblite.dcj.domain.model.Character
import com.vndblite.dcj.domain.model.CharacterSearchFilters
import com.vndblite.dcj.domain.model.CollectionEntry
import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.domain.model.PaginatedList
import com.vndblite.dcj.domain.model.Producer
import com.vndblite.dcj.domain.model.Quote
import com.vndblite.dcj.domain.model.RemoteUserListItem
import com.vndblite.dcj.domain.model.SiteStats
import com.vndblite.dcj.domain.model.SyncReport
import com.vndblite.dcj.domain.model.SyncState
import com.vndblite.dcj.domain.model.Tag
import com.vndblite.dcj.domain.model.Trait
import com.vndblite.dcj.domain.model.UserAccount
import com.vndblite.dcj.domain.model.UserListPatch
import com.vndblite.dcj.domain.model.VnSummary
import java.io.File
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SyncEngineTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var tokenPrefs: SharedPreferences

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = AppDatabase.buildInMemory(context)
        tokenPrefs = context.getSharedPreferences("test_sync_engine_tokens", Context.MODE_PRIVATE)
        tokenPrefs.edit().clear().commit()
    }

    @After
    fun tearDown() {
        database.close()
        tokenPrefs.edit().clear().commit()
    }

    private fun TestScope.createHarness(
        api: VndbApi = FakeSyncVndbApi(),
        nowMillis: Long = 1_750_000_000_000L
    ): SyncTestHarness {
        val tempFile = File.createTempFile("sync_engine_test_", ".preferences_pb").apply {
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
        val tokenStore = EncryptedTokenStore(tokenPrefs, JvmAesGcmTokenCipher(), dispatchers)
        val clock = Clock { nowMillis }
        val engine = SyncEngine(
            api = api,
            database = database,
            settingsStore = settingsStore,
            tokenStore = tokenStore,
            clock = clock,
            dispatchers = dispatchers
        )
        return SyncTestHarness(engine, settingsStore, tokenStore)
    }

    @Test
    fun `1 - partial push failure keeps failed entries DIRTY and preserves pending delete queue until confirmed`() = runTest {
        val fakeApi = FakeSyncVndbApi()
        val harness = createHarness(api = fakeApi)

        database.collectionDao().upsertEntries(
            listOf(
                CollectionEntryEntity.fromDomain(
                    CollectionEntry(
                        vnId = "v17",
                        title = "Ever17",
                        status = CollectionStatus.PLAYING,
                        vote = 90,
                        lastModifiedAt = Instant.ofEpochSecond(1_750_000_000L),
                        syncState = SyncState.NEW
                    ),
                    accountId = "u42"
                ),
                CollectionEntryEntity.fromDomain(
                    CollectionEntry(
                        vnId = "v11",
                        title = "Fate/stay night",
                        status = CollectionStatus.FINISHED,
                        vote = 95,
                        lastModifiedAt = Instant.ofEpochSecond(1_750_000_000L),
                        syncState = SyncState.NEW
                    ),
                    accountId = "u42"
                ),
                CollectionEntryEntity.fromDomain(
                    CollectionEntry(
                        vnId = "v50",
                        title = "To Delete",
                        status = CollectionStatus.DROPPED,
                        lastModifiedAt = Instant.ofEpochSecond(1_740_000_000L),
                        remoteLastMod = Instant.ofEpochSecond(1_740_000_000L),
                        syncState = SyncState.CLEAN
                    ),
                    accountId = "u42"
                )
            )
        )

        harness.settingsStore.setActiveAccountId("u42")
        harness.tokenStore.saveToken("u42", "valid_token")
        harness.engine.queueLocalDeletions(listOf("v50"))

        fakeApi.remoteList = mutableListOf(
            RemoteUserListItem(
                vnId = "v50",
                lastMod = Instant.ofEpochSecond(1_740_000_000L),
                labelIds = setOf(CollectionStatus.DROPPED.labelId)
            )
        )
        fakeApi.patchResults["v11"] = AppResult.Success(Unit)
        fakeApi.patchResults["v17"] = AppResult.Failure(ApiError.Server(502))
        fakeApi.deleteResults["v50"] = AppResult.Failure(ApiError.Offline)

        val firstSync = harness.engine.syncCurrentAccount()
        val firstReport = assertIs<AppResult.Success<SyncReport>>(firstSync).data
        assertFalse(firstReport.isCompleteSuccess)
        assertTrue(firstReport.hasPartialFailures)
        assertEquals(1, firstReport.pushed)
        assertEquals(0, firstReport.deletedRemote)
        assertEquals(2, firstReport.failed.size)

        assertEquals(SyncState.CLEAN.name, database.collectionDao().getVisibleEntryById("v11")?.syncState)
        assertEquals(SyncState.DIRTY.name, database.collectionDao().getVisibleEntryById("v17")?.syncState)
        assertNull(database.collectionDao().getVisibleEntryById("v50"))
        assertEquals(listOf("v50"), database.pendingRemoteDeletionDao().getPendingForAccount("u42").map { it.vnId })

        fakeApi.patchResults["v17"] = AppResult.Success(Unit)
        fakeApi.deleteResults["v50"] = AppResult.Failure(ApiError.NotFound(404))

        val secondSync = harness.engine.syncCurrentAccount()
        val secondReport = assertIs<AppResult.Success<SyncReport>>(secondSync).data
        assertTrue(secondReport.isCompleteSuccess)
        assertEquals(1, secondReport.pushed)
        assertEquals(1, secondReport.deletedRemote)
        assertTrue(secondReport.failed.isEmpty())
        assertEquals(SyncState.CLEAN.name, database.collectionDao().getVisibleEntryById("v17")?.syncState)
        assertTrue(database.pendingRemoteDeletionDao().getPendingForAccount("u42").isEmpty())
    }

    @Test
    fun `2 - truncated remote list with hasMore true refuses to delete local CLEAN entries`() = runTest {
        val fakeApi = FakeSyncVndbApi().apply {
            remoteHasMore = true
            remoteList = mutableListOf(
                RemoteUserListItem(
                    vnId = "v2",
                    lastMod = Instant.ofEpochSecond(1_745_000_000L),
                    labelIds = setOf(CollectionStatus.PLAYING.labelId),
                    vnSummary = VnSummary(id = "v2", title = "Kanon")
                )
            )
        }
        val harness = createHarness(api = fakeApi)
        harness.settingsStore.setActiveAccountId("u42")
        harness.tokenStore.saveToken("u42", "valid_token")

        // v17 is CLEAN locally and absent from the first truncated page of remote results
        database.collectionDao().upsertEntry(
            CollectionEntryEntity.fromDomain(
                CollectionEntry(
                    vnId = "v17",
                    title = "Ever17",
                    status = CollectionStatus.FINISHED,
                    vote = 90,
                    lastModifiedAt = Instant.ofEpochSecond(1_740_000_000L),
                    remoteLastMod = Instant.ofEpochSecond(1_740_000_000L),
                    syncState = SyncState.CLEAN
                ),
                accountId = "u42"
            )
        )

        val result = harness.engine.syncCurrentAccount()
        val report = assertIs<AppResult.Success<SyncReport>>(result).data
        assertEquals(0, report.deletedLocal)
        assertEquals(1, report.pulled)
        assertNotNull(database.collectionDao().getVisibleEntryById("v17"))
        assertNotNull(database.collectionDao().getVisibleEntryById("v2"))
    }

    @Test
    fun `3 - account switch strips foreign sync state and prevents replaying foreign pending deletions`() = runTest {
        val fakeApi = FakeSyncVndbApi()
        val harness = createHarness(api = fakeApi)

        fakeApi.accountToReturn = UserAccount(id = "u1", username = "Alice", permissions = setOf("listread", "listwrite"))
        fakeApi.remoteList = mutableListOf(
            RemoteUserListItem(
                vnId = "v17",
                lastMod = Instant.ofEpochSecond(1_740_000_000L),
                vote = 85,
                labelIds = setOf(CollectionStatus.FINISHED.labelId),
                vnSummary = VnSummary(id = "v17", title = "Ever17")
            )
        )
        assertTrue(harness.engine.authenticateAndSync("token_u1").isSuccess)
        harness.engine.queueLocalDeletions(listOf("v999"))
        assertEquals(listOf("v999"), database.pendingRemoteDeletionDao().getPendingForAccount("u1").map { it.vnId })

        // Directly authenticate as u2 without calling logout first (account switch)
        fakeApi.deletedIds.clear()
        fakeApi.patchedIds.clear()
        fakeApi.accountToReturn = UserAccount(id = "u2", username = "Bob", permissions = setOf("listread", "listwrite"))
        fakeApi.remoteList = mutableListOf()

        val switchRes = harness.engine.authenticateAndSync("token_u2")
        assertTrue(switchRes.isSuccess)
        assertTrue(fakeApi.deletedIds.isEmpty())
        assertEquals(listOf("v17"), fakeApi.patchedIds)
        assertEquals("u2", database.collectionDao().getVisibleEntryById("v17")?.accountId)
    }

    @Test
    fun `4 - HTTP 429 rate limit backs off via RateLimiter and completes sync`() = runTest {
        val server = MockWebServer()
        server.start()
        try {
            val recordedBackoffs = mutableListOf<Long>()
            val rateLimiter = RateLimiter(
                initialBackoffMillis = 500L,
                sleeper = { ms -> recordedBackoffs += ms }
            )
            val realApi = VndbApiImpl(
                httpClient = HttpClientFactory.create(),
                rateLimiter = rateLimiter,
                baseKanaUrl = server.url("/kana").toString()
            )
            val harness = createHarness(api = realApi)
            harness.settingsStore.setActiveAccountId("u42")
            harness.tokenStore.saveToken("u42", "token_42")

            // 1. /authinfo succeeds
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("""{"id":"u42","username":"TestUser","permissions":["listread","listwrite"]}""")
            )
            // 2. /ulist returns 429 with Retry-After: 2, then 200 on retry
            server.enqueue(
                MockResponse()
                    .setResponseCode(429)
                    .addHeader("Retry-After", "2")
            )
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("""{"more":false,"results":[]}""")
            )

            val result = harness.engine.syncCurrentAccount()
            assertIs<AppResult.Success<SyncReport>>(result)
            assertEquals(listOf(2_000L), recordedBackoffs)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `5 - authenticateAndSync rejects token missing listwrite permission`() = runTest {
        val fakeApi = FakeSyncVndbApi().apply {
            authResultOverride = AppResult.Failure(ApiError.MissingPermission(setOf("listwrite")))
        }
        val harness = createHarness(api = fakeApi)

        val result = harness.engine.authenticateAndSync("read_only_token")
        val failure = assertIs<AppResult.Failure>(result)
        assertEquals(ApiError.MissingPermission(setOf("listwrite")), failure.error)
        assertNull(harness.settingsStore.getActiveAccountId())
        assertFalse(harness.tokenStore.hasToken("u42"))
        assertEquals(SyncStage.FAILED, harness.engine.progressFlow.value.stage)
    }

    @Test
    fun `6 - overlapping sync request is rejected with SyncInProgress without clobbering active sync`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val enteredFetch = CompletableDeferred<Unit>()
        val fakeApi = FakeSyncVndbApi().apply {
            onFetchUserList = {
                enteredFetch.complete(Unit)
                gate.await()
            }
        }
        val harness = createHarness(api = fakeApi)
        harness.settingsStore.setActiveAccountId("u42")
        harness.tokenStore.saveToken("u42", "valid_token")

        val firstJob = async { harness.engine.syncCurrentAccount() }
        runCurrent()
        enteredFetch.await()
        assertTrue(harness.engine.progressFlow.value.isRunning)

        val secondResult = harness.engine.syncCurrentAccount()
        val failure = assertIs<AppResult.Failure>(secondResult)
        assertEquals(ApiError.SyncInProgress, failure.error)
        // Active sync must still be marked running
        assertTrue(harness.engine.progressFlow.value.isRunning)

        gate.complete(Unit)
        val firstResult = firstJob.await()
        assertIs<AppResult.Success<SyncReport>>(firstResult)
        assertFalse(harness.engine.progressFlow.value.isRunning)
    }

    @Test
    fun `7 - logout with clearLocalCollection false keeps local entries as NEW and clears token`() = runTest {
        val fakeApi = FakeSyncVndbApi().apply {
            remoteList = mutableListOf(
                RemoteUserListItem(
                    vnId = "v17",
                    lastMod = Instant.ofEpochSecond(1_740_000_000L),
                    vote = 85,
                    labelIds = setOf(CollectionStatus.FINISHED.labelId),
                    vnSummary = VnSummary(id = "v17", title = "Ever17")
                )
            )
        }
        val harness = createHarness(api = fakeApi)
        assertTrue(harness.engine.authenticateAndSync("token_u42").isSuccess)

        harness.engine.logout(clearLocalCollection = false)

        assertNull(harness.settingsStore.getActiveAccountId())
        assertFalse(harness.tokenStore.hasToken("u42"))
        val kept = database.collectionDao().getVisibleEntryById("v17")
        assertNotNull(kept)
        assertEquals(SyncState.NEW.name, kept.syncState)
        assertNull(kept.accountId)
        assertNull(kept.remoteLastModEpochSeconds)
    }

    @Test
    fun `8 - logout with clearLocalCollection true deletes all local collection entries and sync queues`() = runTest {
        val fakeApi = FakeSyncVndbApi().apply {
            remoteList = mutableListOf(
                RemoteUserListItem(
                    vnId = "v17",
                    lastMod = Instant.ofEpochSecond(1_740_000_000L),
                    vote = 85,
                    labelIds = setOf(CollectionStatus.FINISHED.labelId),
                    vnSummary = VnSummary(id = "v17", title = "Ever17")
                )
            )
        }
        val harness = createHarness(api = fakeApi)
        assertTrue(harness.engine.authenticateAndSync("token_u42").isSuccess)
        harness.engine.queueLocalDeletions(listOf("v99"))
        assertEquals(1, database.collectionDao().getAllVisibleEntries().size)

        harness.engine.logout(clearLocalCollection = true)

        assertNull(harness.settingsStore.getActiveAccountId())
        assertFalse(harness.tokenStore.hasToken("u42"))
        assertTrue(database.collectionDao().getAllEntriesIncludingPendingDelete().isEmpty())
        assertTrue(database.pendingRemoteDeletionDao().getPendingForAccount("u42").isEmpty())
        assertNull(database.syncMetadataDao().getByAccountId("u42"))
    }

    private data class SyncTestHarness(
        val engine: SyncEngine,
        val settingsStore: SettingsStore,
        val tokenStore: TokenStore
    )
}

private class FakeSyncVndbApi : VndbApi {
    var accountToReturn = UserAccount(
        id = "u42",
        username = "TestUser",
        permissions = setOf("listread", "listwrite")
    )
    var authResultOverride: AppResult<UserAccount>? = null
    var remoteList = mutableListOf<RemoteUserListItem>()
    var remoteHasMore: Boolean = false
    var onFetchUserList: (suspend () -> Unit)? = null
    val patchResults = mutableMapOf<String, AppResult<Unit>>()
    val deleteResults = mutableMapOf<String, AppResult<Unit>>()
    val patchedIds = mutableListOf<String>()
    val deletedIds = mutableListOf<String>()

    override suspend fun authenticate(token: String): AppResult<UserAccount> =
        authResultOverride ?: AppResult.Success(accountToReturn)

    override suspend fun fetchUserList(
        userId: String,
        token: String
    ): AppResult<PaginatedList<RemoteUserListItem>> {
        onFetchUserList?.invoke()
        return AppResult.Success(
            PaginatedList(
                items = remoteList.toList(),
                hasMore = remoteHasMore,
                page = 1
            )
        )
    }

    override suspend fun patchUserListEntry(token: String, patch: UserListPatch): AppResult<Unit> {
        patchedIds += patch.vnId
        return patchResults[patch.vnId] ?: AppResult.Success(Unit)
    }

    override suspend fun deleteUserListEntry(token: String, vnId: String): AppResult<Unit> {
        deletedIds += vnId
        return deleteResults[vnId] ?: AppResult.Success(Unit)
    }

    override suspend fun fetchVnSummariesByIds(
        ids: List<String>,
        includeDescription: Boolean
    ): AppResult<List<VnSummary>> =
        AppResult.Success(ids.map { VnSummary(id = it, title = "Title $it") })

    override suspend fun queryVns(spec: VnQuerySpec): AppResult<PaginatedList<VnSummary>> =
        AppResult.Success(PaginatedList(emptyList(), false, 1))

    override suspend fun fetchVnFullDetails(vnId: String): AppResult<VnFullDetailPayload> =
        AppResult.Failure(ApiError.NotFound())

    override suspend fun fetchRandomVn(extraFilterClauses: List<KanaFilter>): AppResult<VnSummary> =
        AppResult.Failure(ApiError.NotFound())

    override suspend fun fetchCharactersForVn(vnId: String): AppResult<PaginatedList<Character>> =
        AppResult.Success(PaginatedList(emptyList(), false, 1))

    override suspend fun searchCharacters(
        filters: CharacterSearchFilters,
        page: Int,
        maxResults: Int
    ): AppResult<PaginatedList<Character>> =
        AppResult.Success(PaginatedList(emptyList(), false, 1))

    override suspend fun fetchTopQuotes(maxResults: Int): AppResult<List<Quote>> =
        AppResult.Success(emptyList())

    override suspend fun fetchRandomQuote(): AppResult<Quote?> =
        AppResult.Success(null)

    override suspend fun fetchQuotesForVn(vnId: String, maxResults: Int): AppResult<List<Quote>> =
        AppResult.Success(emptyList())

    override suspend fun searchTags(query: String, maxResults: Int): AppResult<List<Tag>> =
        AppResult.Success(emptyList())

    override suspend fun searchProducers(query: String, maxResults: Int): AppResult<List<Producer>> =
        AppResult.Success(emptyList())

    override suspend fun searchTraits(query: String, maxResults: Int): AppResult<List<Trait>> =
        AppResult.Success(emptyList())

    override suspend fun fetchStats(): AppResult<SiteStats> =
        AppResult.Failure(ApiError.NotFound())
}
