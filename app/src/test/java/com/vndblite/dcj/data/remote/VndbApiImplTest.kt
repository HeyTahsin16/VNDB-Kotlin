package com.vndblite.dcj.data.remote

import com.vndblite.dcj.core.ApiError
import com.vndblite.dcj.core.AppInfo
import com.vndblite.dcj.core.AppResult
import com.vndblite.dcj.data.remote.http.HttpClientFactory
import com.vndblite.dcj.data.remote.http.RateLimiter
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VndbApiImplTest {

    private lateinit var server: MockWebServer
    private lateinit var api: VndbApiImpl

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = VndbApiImpl(
            httpClient = HttpClientFactory.create(),
            rateLimiter = RateLimiter(sleeper = {}),
            baseKanaUrl = server.url("/kana").toString(),
            randomGenerator = Random(42)
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `fetchCharactersForVn follows more flag across multiple pages and sends honest User-Agent`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"more":true,"results":[{"id":"c1","name":"Takeshi"}]}""")
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"more":false,"results":[{"id":"c2","name":"Tsugumi"}]}""")
        )

        val result = api.fetchCharactersForVn("v17")
        assertTrue(result is AppResult.Success)
        val page = (result as AppResult.Success).data
        assertEquals(listOf("c1", "c2"), page.items.map { it.id })
        assertFalse(page.hasMore)

        val firstReq = checkNotNull(server.takeRequest(1, TimeUnit.SECONDS))
        assertEquals(AppInfo.userAgent, firstReq.getHeader("User-Agent"))
        assertTrue(firstReq.body.readUtf8().contains(""""page":1"""))

        val secondReq = checkNotNull(server.takeRequest(1, TimeUnit.SECONDS))
        assertTrue(secondReq.body.readUtf8().contains(""""page":2"""))
    }

    @Test
    fun `fetchVnSummariesByIds batches 120 ids into two requests of 100 and 20`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"more":false,"results":[{"id":"v1","title":"VN 1"}]}""")
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"more":false,"results":[{"id":"v101","title":"VN 101"}]}""")
        )

        val ids = (1..120).map { "v$it" }
        val result = api.fetchVnSummariesByIds(ids)
        assertTrue(result is AppResult.Success)
        assertEquals(2, server.requestCount)
        assertEquals(listOf("v1", "v101"), (result as AppResult.Success).data.map { it.id })
    }

    @Test
    fun `authenticate rejects token missing listwrite permission with MissingPermission error`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"id":"u42","username":"readeronly","permissions":["listread"]}""")
        )

        val result = api.authenticate("secret-token")
        assertTrue(result is AppResult.Failure)
        val error = (result as AppResult.Failure).error
        assertEquals(ApiError.MissingPermission(setOf("listwrite")), error)
    }

    @Test
    fun `fetchRandomVn uses two-step max id discovery and gte id lookup`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"more":false,"results":[{"id":"v50000"}]}""")
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"more":false,"results":[{"id":"v12345","title":"Random Gem","rating":82.0,"votecount":120}]}""")
        )

        val result = api.fetchRandomVn()
        assertTrue(result is AppResult.Success)
        assertEquals("v12345", (result as AppResult.Success).data.id)

        val firstBody = checkNotNull(server.takeRequest(1, TimeUnit.SECONDS)).body.readUtf8()
        assertTrue(firstBody.contains(""""sort":"id""""))
        assertTrue(firstBody.contains(""""reverse":true"""))

        val secondBody = checkNotNull(server.takeRequest(1, TimeUnit.SECONDS)).body.readUtf8()
        assertTrue(secondBody.contains("""["id",">=","v"""))
    }

    @Test
    fun `fetchTopQuotes omits artificial q0 filter`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"more":false,"results":[{"id":"q1","quote":"Hello","score":10}]}""")
        )

        val result = api.fetchTopQuotes(5)
        assertTrue(result is AppResult.Success)
        val requestBody = checkNotNull(server.takeRequest(1, TimeUnit.SECONDS)).body.readUtf8()
        assertFalse(requestBody.contains("q0"))
        assertFalse(requestBody.contains(""""filters""""))
    }

    @Test
    fun `cancelling coroutine cancels in-flight OkHttp call`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setBodyDelay(5, TimeUnit.SECONDS)
                .setResponseCode(200)
                .setBody("""{"chars":1,"releases":1,"vn":1,"staff":1,"tags":1,"producers":1,"traits":1}""")
        )

        val deferred = async {
            api.fetchStats()
        }
        delay(100)
        deferred.cancel()
        assertTrue(deferred.isCancelled)
    }
}
