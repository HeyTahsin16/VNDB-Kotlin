package com.vndblite.dcj.data.repository

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import com.vndblite.dcj.core.Clock
import com.vndblite.dcj.core.DispatcherProvider
import com.vndblite.dcj.data.local.db.AppDatabase
import com.vndblite.dcj.data.local.db.entity.CollectionEntryEntity
import com.vndblite.dcj.data.local.prefs.DataStoreSettingsStore
import com.vndblite.dcj.domain.model.CollectionEntry
import com.vndblite.dcj.domain.model.CollectionFilters
import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.domain.model.SortField
import com.vndblite.dcj.domain.model.SortOption
import com.vndblite.dcj.domain.model.SyncState
import com.vndblite.dcj.domain.model.VnSummary
import com.vndblite.dcj.domain.repository.CollectionBatchUpdateSpec
import com.vndblite.dcj.domain.repository.CollectionSaveDraft
import java.io.File
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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
class CollectionRepositoryImplTest {

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
    fun `saveEntry batchUpdateEntries and removeEntry manage DIRTY PENDING_DELETE and deterministic lastModifiedAt desc order`() = runTest {
        val tempFile = File.createTempFile("collection_repo_test_", ".preferences_pb").apply {
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
        val nowSeconds = AtomicLong(1_750_000_000L)
        val repo = CollectionRepositoryImpl(
            database = database,
            settingsStore = settingsStore,
            clock = Clock { nowSeconds.get() * 1_000L },
            dispatchers = dispatchers
        )

        settingsStore.setActiveAccountId("u42")

        // 1. Save a brand-new local entry -> SyncState.NEW
        val savedV17 = repo.saveEntry(
            CollectionSaveDraft(
                vn = VnSummary(id = "v17", title = "Ever17", rating = 86.5),
                status = CollectionStatus.PLAYING,
                vote = 90
            )
        )
        assertEquals(SyncState.NEW, savedV17.syncState)

        // 2. Seed a previously synced CLEAN entry (v11)
        nowSeconds.addAndGet(10L)
        database.collectionDao().upsertEntry(
            CollectionEntryEntity.fromDomain(
                CollectionEntry(
                    vnId = "v11",
                    title = "Fate/stay night",
                    status = CollectionStatus.PLAYING,
                    vote = 85,
                    lastModifiedAt = Instant.ofEpochSecond(nowSeconds.get()),
                    remoteLastMod = Instant.ofEpochSecond(nowSeconds.get()),
                    syncState = SyncState.CLEAN,
                    lastSyncedStatus = CollectionStatus.PLAYING,
                    lastSyncedVote = 85
                ),
                accountId = "u42"
            )
        )

        // Verify deterministic ordering by lastModifiedAt DESC (v11 is newer than v17)
        val ordered = repo.observeVisibleCollectionByLastModifiedDesc().first()
        assertEquals(listOf("v11", "v17"), ordered.map { it.entry.vnId })

        // 3. Batch update status + vote in one transaction -> NEW stays NEW, CLEAN becomes DIRTY
        nowSeconds.addAndGet(20L)
        val updatedCount = repo.batchUpdateEntries(
            CollectionBatchUpdateSpec(
                vnIds = listOf("v17", "v11"),
                newStatus = CollectionStatus.FINISHED,
                updateVote = true,
                newVote = 95
            )
        )
        assertEquals(2, updatedCount)
        assertEquals(SyncState.NEW, repo.getVisibleEntryById("v17")?.syncState)
        assertEquals(SyncState.DIRTY, repo.getVisibleEntryById("v11")?.syncState)
        assertEquals(95, repo.getVisibleEntryById("v11")?.vote)

        // 4. Filter by status and sort by title
        val filtered = repo.observeFilteredCollection(
            filters = CollectionFilters(status = CollectionStatus.FINISHED),
            sort = SortOption(field = SortField.TITLE, reverse = false)
        ).first()
        assertEquals(listOf("v17", "v11"), filtered.map { it.entry.vnId })

        // 5. Remove synced entry v11 -> marked PENDING_DELETE and queued in pending_remote_deletions
        //    Remove unsynced entry v17 -> hard-deleted locally
        repo.batchRemoveEntries(listOf("v11", "v17"))
        assertNull(repo.getVisibleEntryById("v11"))
        assertNull(repo.getVisibleEntryById("v17"))

        val rawV11 = database.collectionDao().getEntryByIdIncludingPendingDelete("v11")
        assertNotNull(rawV11)
        assertEquals(SyncState.PENDING_DELETE.name, rawV11.syncState)
        assertEquals(listOf("v11"), database.pendingRemoteDeletionDao().getPendingForAccount("u42").map { it.vnId })
        assertNull(database.collectionDao().getEntryByIdIncludingPendingDelete("v17"))
    }
}
