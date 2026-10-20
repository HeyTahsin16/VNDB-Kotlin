package com.vndblite.dcj.data.local.db

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.vndblite.dcj.data.local.db.dao.CollectionDao
import com.vndblite.dcj.data.local.db.dao.CollectionSqlQueryBuilder
import com.vndblite.dcj.data.local.db.dao.VnCacheDao
import com.vndblite.dcj.data.local.db.entity.CollectionEntryEntity
import com.vndblite.dcj.data.local.db.entity.PendingRemoteDeletionEntity
import com.vndblite.dcj.data.local.db.entity.SyncMetadataEntity
import com.vndblite.dcj.data.local.db.entity.VnCacheEntity
import com.vndblite.dcj.domain.model.CollectionFilters
import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.domain.model.DevStatus
import com.vndblite.dcj.domain.model.Language
import com.vndblite.dcj.domain.model.Platform
import com.vndblite.dcj.domain.model.SortField
import com.vndblite.dcj.domain.model.SortOption
import com.vndblite.dcj.domain.model.SyncState
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CollectionDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var collectionDao: CollectionDao
    private lateinit var vnCacheDao: VnCacheDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = AppDatabase.buildInMemory(context)
        collectionDao = database.collectionDao()
        vnCacheDao = database.vnCacheDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `SQL filter and sort handle status language platform olang devstatus minAge and sort fields`() = runTest {
        seedSampleCollectionAndCache()

        // Filter by status = FINISHED
        val finishedQuery = CollectionSqlQueryBuilder.buildFilteredCollectionQuery(
            filters = CollectionFilters(status = CollectionStatus.FINISHED),
            sort = SortOption(field = SortField.RATING, reverse = true)
        )
        val finishedRows = collectionDao.getCollectionByQuery(finishedQuery)
        assertEquals(listOf("v92", "v17"), finishedRows.map { it.entry.vnId })

        // Filter by language=EN, platform=WIN, olang=JA, devStatus=FINISHED, minAge=18
        val metadataQuery = CollectionSqlQueryBuilder.buildFilteredCollectionQuery(
            filters = CollectionFilters(
                minAge = 18,
                languages = listOf(Language.EN),
                originalLanguages = listOf(Language.JA),
                devStatuses = listOf(DevStatus.FINISHED),
                platforms = listOf(Platform.WIN)
            ),
            sort = SortOption(field = SortField.VOTE_COUNT, reverse = true)
        )
        val metadataRows = collectionDao.getCollectionByQuery(metadataQuery)
        assertEquals(listOf("v92"), metadataRows.map { it.entry.vnId })

        // Search by alias in SQL
        val aliasQuery = CollectionSqlQueryBuilder.buildFilteredCollectionQuery(
            filters = CollectionFilters(searchQuery = "Muv-Luv Alternative"),
            sort = SortOption.DEFAULT_LOCAL
        )
        val aliasRows = collectionDao.getCollectionByQuery(aliasQuery)
        assertEquals(listOf("v92"), aliasRows.map { it.entry.vnId })

        // Sort by USER_VOTE descending (unvoted entries appear after voted ones)
        val voteQuery = CollectionSqlQueryBuilder.buildFilteredCollectionQuery(
            filters = CollectionFilters(),
            sort = SortOption(field = SortField.USER_VOTE, reverse = true)
        )
        val voteRows = collectionDao.getCollectionByQuery(voteQuery)
        assertEquals(listOf("v92", "v17", "v24"), voteRows.map { it.entry.vnId })

        // Sort by STARTED ascending (non-null dates come before null dates)
        val startedQuery = CollectionSqlQueryBuilder.buildFilteredCollectionQuery(
            filters = CollectionFilters(),
            sort = SortOption(field = SortField.STARTED, reverse = false)
        )
        val startedRows = collectionDao.getCollectionByQuery(startedQuery)
        assertEquals(listOf("v17", "v92", "v24"), startedRows.map { it.entry.vnId })
    }

    @Test
    fun `batchUpdateStatus and markEntriesPendingDelete operate transactionally and exclude PENDING_DELETE from UI flow`() = runTest {
        seedSampleCollectionAndCache()

        collectionDao.observeVisibleCollectionByLastModifiedDesc().test {
            val initial = awaitItem()
            // Ordered deterministically by lastModifiedEpochSeconds DESC (F3)
            assertEquals(listOf("v24", "v92", "v17"), initial.map { it.entry.vnId })

            // Batch update v17 (CLEAN) and v24 (NEW) to STALLED
            val updatedCount = collectionDao.batchUpdateStatus(
                vnIds = listOf("v17", "v24"),
                newStatusId = CollectionStatus.STALLED.labelId,
                modifiedEpochSeconds = 5_000L
            )
            assertEquals(2, updatedCount)

            val afterUpdate = awaitItem()
            val v17 = afterUpdate.first { it.entry.vnId == "v17" }.entry
            val v24 = afterUpdate.first { it.entry.vnId == "v24" }.entry
            assertEquals(CollectionStatus.STALLED.labelId, v17.statusId)
            // Previously CLEAN entry becomes DIRTY; NEW entry stays NEW
            assertEquals(SyncState.DIRTY.name, v17.syncState)
            assertEquals(SyncState.NEW.name, v24.syncState)

            // Mark v92 as PENDING_DELETE -> hidden from visible flow but kept for SyncEngine
            collectionDao.markEntriesPendingDelete(
                vnIds = listOf("v92"),
                modifiedEpochSeconds = 6_000L
            )
            val afterPendingDelete = awaitItem()
            assertEquals(listOf("v17", "v24"), afterPendingDelete.map { it.entry.vnId })
            assertNull(collectionDao.getVisibleEntryById("v92"))
            assertEquals(
                SyncState.PENDING_DELETE.name,
                collectionDao.getEntryByIdIncludingPendingDelete("v92")?.syncState
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearAccountSyncData detaches or deletes collection and clears account queues on logout`() = runTest {
        seedSampleCollectionAndCache()
        collectionDao.markEntriesPendingDelete(listOf("v24"), modifiedEpochSeconds = 4_000L)
        database.pendingRemoteDeletionDao().enqueue(
            PendingRemoteDeletionEntity(accountId = "u42", vnId = "v24", queuedAtEpochSeconds = 4_000L)
        )
        database.pendingRemoteDeletionDao().enqueue(
            PendingRemoteDeletionEntity(accountId = "u99", vnId = "v500", queuedAtEpochSeconds = 4_000L)
        )
        database.syncMetadataDao().upsert(
            SyncMetadataEntity(accountId = "u42", username = "reader", isSynced = true)
        )

        // Logout u42 keeping local collection
        database.clearAccountSyncData(accountId = "u42", clearLocalCollection = false)

        // u42 queue & metadata cleared; u99 queue untouched (Y6)
        assertTrue(database.pendingRemoteDeletionDao().getPendingForAccount("u42").isEmpty())
        assertEquals(1, database.pendingRemoteDeletionDao().getPendingForAccount("u99").size)
        assertNull(database.syncMetadataDao().getByAccountId("u42"))

        // PENDING_DELETE entries removed; remaining visible entries reset to NEW with null accountId
        val remaining = collectionDao.getAllEntriesIncludingPendingDelete()
        assertEquals(listOf("v17", "v92"), remaining.map { it.vnId })
        assertTrue(remaining.all { it.syncState == SyncState.NEW.name && it.accountId == null && it.remoteLastModEpochSeconds == null })
    }

    private suspend fun seedSampleCollectionAndCache() {
        vnCacheDao.upsertAll(
            listOf(
                VnCacheEntity(
                    vnId = "v17",
                    title = "Ever17",
                    rating = 86.4,
                    voteCount = 7500,
                    originalLanguage = "ja",
                    lengthBucketCode = 4,
                    lengthMinutes = 2100,
                    releasedRaw = "2002-08-29",
                    releasedSortKey = "2002-08-29",
                    devStatusCode = 0,
                    minAge = 12,
                    languagesPipe = "|en|ja|",
                    platformsPipe = "|win|psp|",
                    aliasesSearchText = "Ever17 -the out of infinity-",
                    summaryFetchedAtEpochSeconds = 1_000L,
                    lastAccessedAtEpochSeconds = 1_000L
                ),
                VnCacheEntity(
                    vnId = "v92",
                    title = "Muv-Luv",
                    rating = 91.2,
                    voteCount = 12500,
                    originalLanguage = "ja",
                    lengthBucketCode = 5,
                    lengthMinutes = 3400,
                    releasedRaw = "2006-02-24",
                    releasedSortKey = "2006-02-24",
                    devStatusCode = 0,
                    minAge = 18,
                    languagesPipe = "|en|ja|",
                    platformsPipe = "|win|",
                    aliasesSearchText = "Muv-Luv Alternative",
                    summaryFetchedAtEpochSeconds = 1_000L,
                    lastAccessedAtEpochSeconds = 1_000L
                ),
                VnCacheEntity(
                    vnId = "v24",
                    title = "Tsukihime Remake",
                    rating = 88.0,
                    voteCount = 4100,
                    originalLanguage = "ja",
                    lengthBucketCode = 4,
                    lengthMinutes = 2600,
                    releasedRaw = "2021-08-26",
                    releasedSortKey = "2021-08-26",
                    devStatusCode = 1,
                    minAge = 18,
                    languagesPipe = "|ja|",
                    platformsPipe = "|swi|ps4|",
                    aliasesSearchText = "Tsukihime -A piece of blue glass moon-",
                    summaryFetchedAtEpochSeconds = 1_000L,
                    lastAccessedAtEpochSeconds = 1_000L
                )
            )
        )

        collectionDao.upsertEntries(
            listOf(
                CollectionEntryEntity(
                    vnId = "v17",
                    title = "Ever17",
                    statusId = CollectionStatus.FINISHED.labelId,
                    vote = 85,
                    addedDate = "2024-01-10",
                    addedEpochSeconds = 1_704_844_800L,
                    startedDate = "2024-01-10",
                    finishedDate = "2024-01-20",
                    lastModifiedEpochSeconds = 1_000L,
                    remoteLastModEpochSeconds = 1_000L,
                    syncState = SyncState.CLEAN.name,
                    accountId = "u42"
                ),
                CollectionEntryEntity(
                    vnId = "v92",
                    title = "Muv-Luv",
                    statusId = CollectionStatus.FINISHED.labelId,
                    vote = 98,
                    addedDate = "2024-02-01",
                    addedEpochSeconds = 1_706_745_600L,
                    startedDate = "2024-02-05",
                    finishedDate = "2024-02-25",
                    lastModifiedEpochSeconds = 2_000L,
                    remoteLastModEpochSeconds = 2_000L,
                    syncState = SyncState.CLEAN.name,
                    accountId = "u42"
                ),
                CollectionEntryEntity(
                    vnId = "v24",
                    title = "Tsukihime Remake",
                    statusId = CollectionStatus.WISHLIST.labelId,
                    vote = null,
                    addedDate = "2024-03-01",
                    addedEpochSeconds = 1_709_251_200L,
                    startedDate = null,
                    finishedDate = null,
                    lastModifiedEpochSeconds = 3_000L,
                    remoteLastModEpochSeconds = null,
                    syncState = SyncState.NEW.name,
                    accountId = "u42"
                )
            )
        )
    }
}
