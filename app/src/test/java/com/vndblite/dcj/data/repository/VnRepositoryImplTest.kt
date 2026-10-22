package com.vndblite.dcj.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.vndblite.dcj.core.AppResult
import com.vndblite.dcj.core.Clock
import com.vndblite.dcj.core.DispatcherProvider
import com.vndblite.dcj.data.local.db.AppDatabase
import com.vndblite.dcj.data.local.db.entity.CollectionEntryEntity
import com.vndblite.dcj.data.remote.VndbApiImpl
import com.vndblite.dcj.data.remote.http.HttpClientFactory
import com.vndblite.dcj.data.remote.http.RateLimiter
import com.vndblite.dcj.data.remote.scraper.JsoupReviewSource
import com.vndblite.dcj.domain.model.CollectionEntry
import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.domain.model.HomeSection
import com.vndblite.dcj.domain.model.SyncState
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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
class VnRepositoryImplTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = AppDatabase.buildInMemory(context)
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        database.close()
        server.shutdown()
    }

    private fun TestScope.createRepo(
        nowSeconds: AtomicLong = AtomicLong(1_750_000_000L),
        maxNonCollectionEntries: Int = 500
    ): VnRepositoryImpl {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val dispatchers = object : DispatcherProvider {
            override val main: CoroutineDispatcher = testDispatcher
            override val io: CoroutineDispatcher = testDispatcher
            override val default: CoroutineDispatcher = testDispatcher
        }
        val clock = Clock { nowSeconds.get() * 1_000L }
        val sharedClient = HttpClientFactory.create()
        val sharedLimiter = RateLimiter(clock = clock, sleeper = {})
        val api = VndbApiImpl(
            httpClient = sharedClient,
            rateLimiter = sharedLimiter,
            dispatchers = dispatchers,
            clock = clock,
            baseKanaUrl = server.url("/kana").toString(),
            randomGenerator = Random(42)
        )
        val reviewSource = JsoupReviewSource(
            httpClient = sharedClient,
            rateLimiter = sharedLimiter,
            dispatchers = dispatchers,
            baseWebUrl = server.url("/").toString()
        )
        return VnRepositoryImpl(
            api = api,
            reviewSource = reviewSource,
            database = database,
            clock = clock,
            dispatchers = dispatchers,
            maxNonCollectionCacheEntries = maxNonCollectionEntries
        )
    }

    @Test
    fun `home section respects 1h TTL for LATEST_RELEASES and 12h TTL for TOP_RATED with stale-while-revalidate`() = runTest {
        val nowSeconds = AtomicLong(1_750_000_000L)
        val repo = createRepo(nowSeconds = nowSeconds)

        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"more":false,"results":[{"id":"v17","title":"Ever17","rating":86.5,"votecount":5000}]}"""
            )
        )
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"more":false,"results":[{"id":"v99","title":"Latest Release VN","released":"2026-10-01"}]}"""
            )
        )

        val topRatedRes = repo.refreshHomeSection(HomeSection.TOP_RATED, maxItems = 10)
        assertIs<AppResult.Success<*>>(topRatedRes)
        val latestRes = repo.refreshHomeSection(HomeSection.LATEST_RELEASES, maxItems = 10)
        assertIs<AppResult.Success<*>>(latestRes)
        assertEquals(2, server.requestCount)

        // Advance 2 hours: LATEST_RELEASES (1h TTL) is stale, TOP_RATED (12h TTL) is still fresh
        nowSeconds.addAndGet(2L * 3600L)

        val topSnapshot = repo.observeHomeSection(HomeSection.TOP_RATED, maxItems = 10).first()
        assertFalse(topSnapshot.isStale)
        assertEquals(listOf("v17"), topSnapshot.items.map { it.id })

        val latestSnapshot = repo.observeHomeSection(HomeSection.LATEST_RELEASES, maxItems = 10).first()
        assertTrue(latestSnapshot.isStale)
        // Stale cached items remain available while revalidating
        assertEquals(listOf("v99"), latestSnapshot.items.map { it.id })

        // Non-forced refresh of TOP_RATED hits cache without network call
        repo.refreshHomeSection(HomeSection.TOP_RATED, maxItems = 10, force = false)
        assertEquals(2, server.requestCount)

        // Non-forced refresh of stale LATEST_RELEASES triggers network revalidation
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"more":false,"results":[{"id":"v100","title":"Newer Release VN","released":"2026-10-15"}]}"""
            )
        )
        val refreshedLatest = repo.refreshHomeSection(HomeSection.LATEST_RELEASES, maxItems = 10, force = false)
        assertIs<AppResult.Success<*>>(refreshedLatest)
        assertEquals(3, server.requestCount)
        val updatedSnapshot = repo.observeHomeSection(HomeSection.LATEST_RELEASES, maxItems = 10).first()
        assertFalse(updatedSnapshot.isStale)
        assertEquals(listOf("v100"), updatedSnapshot.items.map { it.id })
    }

    @Test
    fun `LRU cap prunes oldest non-collection cache rows while preserving collection VNs`() = runTest {
        val nowSeconds = AtomicLong(1_750_000_000L)
        val repo = createRepo(nowSeconds = nowSeconds, maxNonCollectionEntries = 2)

        // Put v1 in user collection so it must NEVER be evicted
        database.collectionDao().upsertEntry(
            CollectionEntryEntity.fromDomain(
                CollectionEntry(
                    vnId = "v1",
                    title = "Collection Protected VN",
                    status = CollectionStatus.PLAYING,
                    lastModifiedAt = Instant.ofEpochSecond(nowSeconds.get()),
                    syncState = SyncState.NEW
                ),
                accountId = null
            )
        )

        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"more":false,"results":[
                    {"id":"v1","title":"Collection Protected VN"},
                    {"id":"v2","title":"Non-Collection Old"},
                    {"id":"v3","title":"Non-Collection Mid"},
                    {"id":"v4","title":"Non-Collection New"}
                ]}"""
            )
        )
        val res = repo.refreshHomeSection(HomeSection.TOP_RATED, maxItems = 10, force = true)
        assertIs<AppResult.Success<*>>(res)

        // Collection entry v1 is preserved, and at most 2 non-collection entries remain
        assertNotNull(database.vnCacheDao().getById("v1"))
        assertEquals(2, database.vnCacheDao().countNonCollectionEntries())
    }

    @Test
    fun `refreshAllRemoteHomeSections loads all 5 remote sections in parallel under rate limiter`() = runTest {
        val repo = createRepo()
        repeat(5) { idx ->
            server.enqueue(
                MockResponse().setResponseCode(200).setBody(
                    """{"more":false,"results":[{"id":"v${idx + 1}","title":"Section VN ${idx + 1}"}]}"""
                )
            )
        }

        val results = repo.refreshAllRemoteHomeSections(maxItems = 5, force = true)
        assertEquals(5, results.size)
        assertTrue(results.values.all { it is AppResult.Success })
        assertEquals(5, server.requestCount)
    }
}
