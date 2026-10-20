package com.vndblite.dcj.data.local.db

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.vndblite.dcj.data.local.db.entity.CollectionEntryEntity
import com.vndblite.dcj.data.local.db.entity.HomeSectionCacheEntity
import com.vndblite.dcj.data.local.db.entity.QuoteCacheEntity
import com.vndblite.dcj.data.local.db.entity.SiteStatsCacheEntity
import com.vndblite.dcj.data.local.db.entity.VnCacheEntity
import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.domain.model.SyncState
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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
class VnCacheDaoAndAuxiliaryTest {

    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = AppDatabase.buildInMemory(context)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `TTL and LRU eviction remove stale and overflow non-collection VNs but NEVER evict collection VNs`() = runTest {
        val collectionDao = database.collectionDao()
        val vnCacheDao = database.vnCacheDao()

        // Put v1 in collection with the OLDEST timestamps
        collectionDao.upsertEntry(
            CollectionEntryEntity(
                vnId = "v1",
                title = "Collection VN",
                statusId = CollectionStatus.PLAYING.labelId,
                syncState = SyncState.CLEAN.name
            )
        )
        vnCacheDao.upsertAll(
            listOf(
                VnCacheEntity(
                    vnId = "v1",
                    title = "Collection VN",
                    summaryFetchedAtEpochSeconds = 10L,
                    lastAccessedAtEpochSeconds = 10L
                ),
                VnCacheEntity(
                    vnId = "v2",
                    title = "Expired Non-Collection VN",
                    summaryFetchedAtEpochSeconds = 50L,
                    lastAccessedAtEpochSeconds = 50L
                ),
                VnCacheEntity(
                    vnId = "v3",
                    title = "Fresh Older LRU Non-Collection VN",
                    summaryFetchedAtEpochSeconds = 200L,
                    lastAccessedAtEpochSeconds = 200L
                ),
                VnCacheEntity(
                    vnId = "v4",
                    title = "Fresh Newer LRU Non-Collection VN",
                    summaryFetchedAtEpochSeconds = 210L,
                    lastAccessedAtEpochSeconds = 300L
                ),
                VnCacheEntity(
                    vnId = "v5",
                    title = "Fresh Newest LRU Non-Collection VN",
                    summaryFetchedAtEpochSeconds = 220L,
                    lastAccessedAtEpochSeconds = 400L
                )
            )
        )

        // Prune with minFetchedAt=100 (expires v2) and LRU cap = 2 non-collection entries (evicts v3, keeps v4 & v5)
        val removed = vnCacheDao.pruneNonCollectionCache(
            minFetchedAtEpochSeconds = 100L,
            maxNonCollectionEntries = 2
        )
        assertEquals(2, removed)

        // Collection entry v1 MUST still be cached despite having timestamp 10L!
        assertNotNull(vnCacheDao.getById("v1"))
        assertNull(vnCacheDao.getById("v2"))
        assertNull(vnCacheDao.getById("v3"))
        assertNotNull(vnCacheDao.getById("v4"))
        assertNotNull(vnCacheDao.getById("v5"))
    }

    @Test
    fun `clearTransientCaches removes home sections quotes and non-collection VNs while keeping collection VNs`() = runTest {
        database.collectionDao().upsertEntry(
            CollectionEntryEntity(vnId = "v17", title = "Ever17", statusId = CollectionStatus.FINISHED.labelId)
        )
        database.vnCacheDao().upsertAll(
            listOf(
                VnCacheEntity(vnId = "v17", title = "Ever17"),
                VnCacheEntity(vnId = "v99", title = "Transient Browse VN")
            )
        )
        database.homeSectionCacheDao().upsertSection(
            HomeSectionCacheEntity.create("RATING", listOf("v17", "v99"), fetchedAtEpochSeconds = 1_000L)
        )
        database.quoteCacheDao().replaceAll(
            listOf(QuoteCacheEntity(quoteId = "q1", text = "Test quote", displayOrder = 0))
        )
        database.siteStatsCacheDao().upsertStats(
            SiteStatsCacheEntity(
                chars = 140_000,
                releases = 120_000,
                vn = 50_000,
                staff = 39_000,
                tags = 3_100,
                producers = 22_000,
                traits = 3_300,
                fetchedAtEpochSeconds = 1_000L
            )
        )

        database.clearTransientCaches()

        assertNotNull(database.vnCacheDao().getById("v17"))
        assertNull(database.vnCacheDao().getById("v99"))
        assertNull(database.homeSectionCacheDao().getSection("RATING"))
        assertTrue(database.quoteCacheDao().getAll().isEmpty())
        // SiteStatsCache is preserved as last-known stats (F6)
        assertNotNull(database.siteStatsCacheDao().getStats())
    }
}
