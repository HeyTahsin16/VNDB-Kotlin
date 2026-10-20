package com.vndblite.dcj.data.remote.http

import com.vndblite.dcj.core.Clock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RateLimiterTest {

    private class FakeClock(var currentMillis: Long = 1_000_000L) : Clock {
        override fun nowMillis(): Long = currentMillis
    }

    @Test
    fun `acquirePermission sleeps when sliding window is full and advances clock`() = runTest {
        val fakeClock = FakeClock()
        val recordedSleeps = mutableListOf<Long>()
        val limiter = RateLimiter(
            clock = fakeClock,
            maxRequestsPerWindow = 2,
            windowMillis = 5_000L,
            maxConcurrency = 1,
            sleeper = { ms ->
                recordedSleeps.add(ms)
                fakeClock.currentMillis += ms
            }
        )

        limiter.acquirePermission()
        fakeClock.currentMillis += 1_000L
        limiter.acquirePermission()
        // Window now holds [1_000_000, 1_001_000]. Next request at 1_001_500 must wait 3_500ms.
        fakeClock.currentMillis += 500L
        limiter.acquirePermission()

        assertEquals(listOf(3_500L), recordedSleeps)
    }

    @Test
    fun `executeWithRetry honors Retry-After header on 429 and retries until 200`() = runTest {
        val fakeClock = FakeClock()
        val recordedSleeps = mutableListOf<Long>()
        val limiter = RateLimiter(
            clock = fakeClock,
            maxRequestsPerWindow = 10,
            windowMillis = 60_000L,
            maxConcurrency = 2,
            maxRetries = 3,
            initialBackoffMillis = 1_000L,
            sleeper = { ms ->
                recordedSleeps.add(ms)
                fakeClock.currentMillis += ms
            }
        )

        var attempts = 0
        val finalStatus = limiter.executeWithRetry(
            isRetryableStatus = { status ->
                if (status == 429) RateLimiter.RetryDirective(429, retryAfterSeconds = 4L)
                else if (status == 502) RateLimiter.RetryDirective(502, retryAfterSeconds = null)
                else null
            }
        ) {
            attempts++
            when (attempts) {
                1 -> 429
                2 -> 502
                else -> 200
            }
        }

        assertEquals(200, finalStatus)
        assertEquals(3, attempts)
        // First retry uses Retry-After = 4s (4_000ms); second retry uses doubled backoff = 2_000ms
        assertEquals(listOf(4_000L, 2_000L), recordedSleeps)
    }
}
