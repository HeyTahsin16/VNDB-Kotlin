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
import com.vndblite.dcj.data.local.secure.EncryptedTokenStore
import com.vndblite.dcj.data.local.secure.JvmAesGcmTokenCipher
import com.vndblite.dcj.data.remote.VnFullDetailPayload
import com.vndblite.dcj.data.remote.VnQuerySpec
import com.vndblite.dcj.data.remote.VndbApi
import com.vndblite.dcj.data.remote.filter.KanaFilter
import com.vndblite.dcj.domain.model.Character
import com.vndblite.dcj.domain.model.CharacterSearchFilters
import com.vndblite.dcj.domain.model.CollectionEntry
import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.domain.model.PaginatedList
import com.vndblite.dcj.domain.model.Producer
import com.vndblite.dcj.domain.model.Quote
import com.vndblite.dcj.domain.model.RemoteUserListItem
import com.vndblite.dcj.domain.model.SiteStats
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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    @Test
    fun `failed push stays DIRTY and retries on next sync while pending delete only dequeues after 2xx or 404 (Y4)`() = runTest {
        val tempFile = File.createTempFile("sync_engine_y4_", ".preferences_pb").apply {
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
        val fakeApi = FakeSyncVndbApi()
        val clock = Clock { 1_750_000_000_000L }

        val engine = SyncEngine(
            api = fakeApi,
            database = database,
            settingsStore = settingsStore,
            tokenStore = tokenStore,
            clock = clock,
            dispatchers = dispatchers
        )

        // Seed a local NEW entry (v17) and a synced entry (v50) that we then queue for deletion
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

        settingsStore.setActiveAccountId("u42")
        tokenStore.saveToken("u42", "valid_token")
        engine.queueLocalDeletions(listOf("v50"))

        // Remote still has v50, and both PATCH(v17) and DELETE(v50) fail on first sync
        fakeApi.remoteList = mutableListOf(
            RemoteUserListItem(
                vnId = "v50",
                lastMod = Instant.ofEpochSecond(1_740_000_000L),
                labelIds = setOf(CollectionStatus.DROPPED.labelId)
            )
        )
        fakeApi.patchResults["v17"] = AppResult.Failure(ApiError.Server(502))
        fakeApi.deleteResults["v50"] = AppResult.Failure(ApiError.Offline)

        val firstSync = engine.syncCurrentAccount()
        val firstReport = assertIs<AppResult.Success<*>>(firstSync).data as com.vndblite.dcj.domain.model.SyncReport
        assertFalse(firstReport.isCompleteSuccess)
        assertEquals(0, firstReport.pushed)
        assertEquals(0, firstReport.deletedRemote)
        assertEquals(2, firstReport.failed.size)

        // 1. Failed push for v17 is marked DIRTY (not CLEAN) so it will retry next sync
        val v17AfterFail = database.collectionDao().getVisibleEntryById("v17")
        assertNotNull(v17AfterFail)
        assertEquals(SyncState.DIRTY.name, v17AfterFail.syncState)

        // 2. Failed delete for v50 was NOT forgotten and v50 was NOT re-imported into visible collection!
        assertNull(database.collectionDao().getVisibleEntryById("v50"))
        assertEquals(listOf("v50"), database.pendingRemoteDeletionDao().getPendingForAccount("u42").map { it.vnId })

        // Now server recovers: PATCH(v17) succeeds (204) and DELETE(v50) returns 404 NotFound (already gone)
        fakeApi.patchResults["v17"] = AppResult.Success(Unit)
        fakeApi.deleteResults["v50"] = AppResult.Failure(ApiError.NotFound(404))

        val secondSync = engine.syncCurrentAccount()
        val secondReport = assertIs<AppResult.Success<*>>(secondSync).data as com.vndblite.dcj.domain.model.SyncReport
        assertTrue(secondReport.isCompleteSuccess)
        assertEquals(1, secondReport.pushed)
        assertEquals(1, secondReport.deletedRemote)
        assertTrue(secondReport.failed.isEmpty())

        // v17 is now CLEAN and v50 is removed from pending_remote_deletions
        val v17AfterRetry = database.collectionDao().getVisibleEntryById("v17")
        assertNotNull(v17AfterRetry)
        assertEquals(SyncState.CLEAN.name, v17AfterRetry.syncState)
        assertTrue(database.pendingRemoteDeletionDao().getPendingForAccount("u42").isEmpty())
    }

    @Test
    fun `logout and account switch isolate pending deletions and sync state per account (Y6, N1)`() = runTest {
        val tempFile = File.createTempFile("sync_engine_y6_", ".preferences_pb").apply {
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
        val fakeApi = FakeSyncVndbApi()
        val engine = SyncEngine(
            api = fakeApi,
            database = database,
            settingsStore = settingsStore,
            tokenStore = tokenStore,
            clock = Clock { 1_750_000_000_000L },
            dispatchers = dispatchers
        )

        // Authenticate as u1, with remote entry v17
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
        val u1Res = engine.authenticateAndSync("token_u1")
        assertTrue(u1Res.isSuccess)
        assertEquals(1, database.collectionDao().getAllVisibleEntries().size)

        // u1 queues deletion of v999 before logging out
        engine.queueLocalDeletions(listOf("v999"))
        assertEquals(listOf("v999"), database.pendingRemoteDeletionDao().getPendingForAccount("u1").map { it.vnId })

        // u1 logs out and chooses to KEEP the local collection (clearLocalCollection = false)
        engine.logout(clearLocalCollection = false)
        assertTrue(database.pendingRemoteDeletionDao().getPendingForAccount("u1").isEmpty())
        assertFalse(tokenStore.hasToken("u1"))

        val keptV17 = database.collectionDao().getVisibleEntryById("v17")
        assertNotNull(keptV17)
        assertEquals(SyncState.NEW.name, keptV17.syncState)
        assertNull(keptV17.accountId)

        // Now u2 signs in with an empty cloud userlist
        fakeApi.deletedIds.clear()
        fakeApi.patchedIds.clear()
        fakeApi.accountToReturn = UserAccount(id = "u2", username = "Bob", permissions = setOf("listread", "listwrite"))
        fakeApi.remoteList = mutableListOf()

        val u2Res = engine.authenticateAndSync("token_u2")
        assertTrue(u2Res.isSuccess)

        // u1's deletion of v999 was NEVER replayed on u2, and kept v17 was pushed to u2!
        assertTrue(fakeApi.deletedIds.isEmpty())
        assertEquals(listOf("v17"), fakeApi.patchedIds)
    }
}

private class FakeSyncVndbApi : VndbApi {
    var accountToReturn = UserAccount(
        id = "u42",
        username = "TestUser",
        permissions = setOf("listread", "listwrite")
    )
    var remoteList = mutableListOf<RemoteUserListItem>()
    val patchResults = mutableMapOf<String, AppResult<Unit>>()
    val deleteResults = mutableMapOf<String, AppResult<Unit>>()
    val patchedIds = mutableListOf<String>()
    val deletedIds = mutableListOf<String>()

    override suspend fun authenticate(token: String): AppResult<UserAccount> =
        AppResult.Success(accountToReturn)

    override suspend fun fetchUserList(userId: String, token: String): AppResult<List<RemoteUserListItem>> =
        AppResult.Success(remoteList.toList())

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
