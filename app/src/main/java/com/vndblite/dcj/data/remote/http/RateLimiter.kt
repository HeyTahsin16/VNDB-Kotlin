package com.vndblite.dcj.data.remote.http

import com.vndblite.dcj.core.AppLog
import com.vndblite.dcj.core.Clock
import com.vndblite.dcj.core.SystemClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit

/**
 * Shared rate limiter and concurrency gate for VNDB API requests (Section 3, N1).
 *
 * - Enforces a sliding-window token bucket for VNDB's 200 requests / 5 minutes limit
 *   with a safety margin (default 180 requests per 300 seconds).
 * - Bounds concurrent in-flight requests (`maxConcurrency = 2`) so the 1 second / minute
 *   server execution time quota is not overwhelmed by parallel bursts.
 * - Executes exponential backoff honoring `Retry-After` on HTTP 429 / 502 / 503 responses.
 */
class RateLimiter(
    private val clock: Clock = SystemClock,
    private val maxRequestsPerWindow: Int = DEFAULT_MAX_REQUESTS_PER_WINDOW,
    private val windowMillis: Long = DEFAULT_WINDOW_MILLIS,
    maxConcurrency: Int = DEFAULT_MAX_CONCURRENCY,
    private val maxRetries: Int = DEFAULT_MAX_RETRIES,
    private val initialBackoffMillis: Long = DEFAULT_INITIAL_BACKOFF_MILLIS,
    private val sleeper: suspend (Long) -> Unit = { ms -> delay(ms) }
) {
    private val concurrencySemaphore = Semaphore(maxConcurrency.coerceAtLeast(1))
    private val windowMutex = Mutex()
    private val requestTimestampsMillis = ArrayDeque<Long>()

    /**
     * Acquires a rate-limit slot inside the sliding window, waiting via [sleeper] if the window is full.
     */
    suspend fun acquirePermission() {
        while (true) {
            val waitMillis = windowMutex.withLock {
                val now = clock.nowMillis()
                evictExpiredTimestamps(now)
                if (requestTimestampsMillis.size < maxRequestsPerWindow) {
                    requestTimestampsMillis.addLast(now)
                    0L
                } else {
                    val oldest = requestTimestampsMillis.first()
                    (oldest + windowMillis - now).coerceAtLeast(1L)
                }
            }
            if (waitMillis <= 0L) return
            AppLog.d(TAG, "RateLimiter window full; waiting ${waitMillis}ms before next request")
            sleeper(waitMillis)
        }
    }

    /**
     * Runs [block] within the bounded concurrency semaphore and rate-limit token bucket,
     * automatically retrying transient 429 / 502 / 503 responses with exponential backoff
     * or the server's `Retry-After` delay (N1).
     */
    suspend fun <T> executeWithRetry(
        isRetryableStatus: (T) -> RetryDirective?,
        block: suspend () -> T
    ): T = concurrencySemaphore.withPermit {
        retryLoop(isRetryableStatus, block)
    }

    private suspend fun <T> retryLoop(
        isRetryableStatus: (T) -> RetryDirective?,
        block: suspend () -> T
    ): T {
        var attempt = 0
        var currentBackoff = initialBackoffMillis
        while (true) {
            acquirePermission()
            val outcome = block()
            val directive = isRetryableStatus(outcome)
            if (directive == null || attempt >= maxRetries) {
                return outcome
            }
            val delayMs = directive.retryAfterSeconds
                ?.let { it * 1_000L }
                ?.coerceAtLeast(currentBackoff)
                ?: currentBackoff
            AppLog.w(
                TAG,
                "Transient HTTP ${directive.statusCode} on attempt ${attempt + 1}; backing off ${delayMs}ms"
            )
            sleeper(delayMs)
            attempt++
            currentBackoff = (currentBackoff * 2L).coerceAtMost(MAX_BACKOFF_MILLIS)
        }
    }

    private fun evictExpiredTimestamps(nowMillis: Long) {
        val cutoff = nowMillis - windowMillis
        while (requestTimestampsMillis.isNotEmpty() && requestTimestampsMillis.first() <= cutoff) {
            requestTimestampsMillis.removeFirst()
        }
    }

    data class RetryDirective(
        val statusCode: Int,
        val retryAfterSeconds: Long? = null
    )

    companion object {
        private const val TAG = "RateLimiter"
        const val DEFAULT_MAX_REQUESTS_PER_WINDOW: Int = 180
        const val DEFAULT_WINDOW_MILLIS: Long = 5L * 60L * 1_000L
        const val DEFAULT_MAX_CONCURRENCY: Int = 2
        const val DEFAULT_MAX_RETRIES: Int = 3
        const val DEFAULT_INITIAL_BACKOFF_MILLIS: Long = 1_000L
        const val MAX_BACKOFF_MILLIS: Long = 30_000L
    }
}
