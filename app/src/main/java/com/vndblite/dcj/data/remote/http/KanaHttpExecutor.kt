package com.vndblite.dcj.data.remote.http

import com.vndblite.dcj.core.AppResult
import com.vndblite.dcj.core.runSuspendCatching
import com.vndblite.dcj.data.remote.dto.KanaJson
import com.vndblite.dcj.data.remote.dto.KanaPageResponseDto
import com.vndblite.dcj.data.remote.dto.KanaQueryRequestDto
import com.vndblite.dcj.domain.model.PaginatedList
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Low-level HTTP transport and pagination helper for VNDB Kana API v2 endpoints (N1-N4).
 */
class KanaHttpExecutor(
    private val httpClient: OkHttpClient,
    private val rateLimiter: RateLimiter,
    private val json: Json = KanaJson,
    private val baseKanaUrl: String,
    private val maxAutoPages: Int
) {
    suspend fun <T> fetchAllPages(
        endpoint: String,
        baseRequest: KanaQueryRequestDto,
        itemSerializer: KSerializer<T>,
        authToken: String? = null,
        maxPages: Int = maxAutoPages
    ): AppResult<PaginatedList<T>> {
        val allItems = mutableListOf<T>()
        var currentPage = 1
        var hasMore = true
        while (hasMore && currentPage <= maxPages) {
            val pageReq = baseRequest.copy(page = currentPage)
            when (val pageRes = postPage(endpoint, pageReq, itemSerializer, authToken)) {
                is AppResult.Failure -> return pageRes
                is AppResult.Success -> {
                    allItems.addAll(pageRes.data.results)
                    hasMore = pageRes.data.more
                    if (hasMore) currentPage++
                }
            }
        }
        val truncated = hasMore && currentPage > maxPages
        return AppResult.Success(
            PaginatedList(
                items = allItems,
                hasMore = truncated,
                page = (currentPage - if (truncated) 1 else 0).coerceAtLeast(1)
            )
        )
    }

    suspend fun <T> postPage(
        endpoint: String,
        requestDto: KanaQueryRequestDto,
        itemSerializer: KSerializer<T>,
        authToken: String? = null
    ): AppResult<KanaPageResponseDto<T>> {
        val bodyJson = json.encodeToString(KanaQueryRequestDto.serializer(), requestDto)
        val pageSerializer = KanaPageResponseDto.serializer(itemSerializer)
        return executeJsonRequest(
            method = "POST",
            endpoint = endpoint,
            requestBodyJson = bodyJson,
            authToken = authToken,
            responseSerializer = pageSerializer
        )
    }

    suspend fun <R> executeJsonRequest(
        method: String,
        endpoint: String,
        requestBodyJson: String?,
        authToken: String?,
        responseSerializer: KSerializer<R>
    ): AppResult<R> = runSuspendCatching(
        tag = TAG,
        operation = "$method /kana/$endpoint",
        errorMapper = ::mapThrowableToApiError
    ) {
        val raw = performHttpCall(method, endpoint, requestBodyJson, authToken)
        if (raw.statusCode !in 200..299) {
            throw ApiErrorException(
                mapHttpStatusToApiError(raw.statusCode, raw.retryAfterSeconds?.toString())
            )
        }
        json.decodeFromString(responseSerializer, raw.body)
    }

    suspend fun executeUnitRequest(
        method: String,
        endpoint: String,
        requestBodyJson: String?,
        authToken: String?
    ): AppResult<Unit> = runSuspendCatching(
        tag = TAG,
        operation = "$method /kana/$endpoint",
        errorMapper = ::mapThrowableToApiError
    ) {
        val raw = performHttpCall(method, endpoint, requestBodyJson, authToken)
        if (raw.statusCode !in 200..299) {
            throw ApiErrorException(
                mapHttpStatusToApiError(raw.statusCode, raw.retryAfterSeconds?.toString())
            )
        }
        Unit
    }

    private suspend fun performHttpCall(
        method: String,
        endpoint: String,
        requestBodyJson: String?,
        authToken: String?
    ): RawHttpOutcome {
        val url = "${baseKanaUrl.trimEnd('/')}/${endpoint.trimStart('/')}"
        return rateLimiter.executeWithRetry(
            isRetryableStatus = { outcome ->
                if (outcome.statusCode in RETRYABLE_STATUS_CODES) {
                    RateLimiter.RetryDirective(outcome.statusCode, outcome.retryAfterSeconds)
                } else {
                    null
                }
            }
        ) {
            val builder = Request.Builder().url(url)
            if (!authToken.isNullOrBlank()) {
                builder.header(HEADER_AUTHORIZATION, "Token $authToken")
            }
            val mediaType = JSON_MEDIA_TYPE.toMediaType()
            when (method) {
                "GET" -> builder.get()
                "DELETE" -> builder.delete()
                "POST" -> builder.post(requestBodyJson.orEmpty().toRequestBody(mediaType))
                "PATCH" -> builder.patch(requestBodyJson.orEmpty().toRequestBody(mediaType))
                else -> builder.method(method, requestBodyJson?.toRequestBody(mediaType))
            }
            httpClient.newCall(builder.build()).await().use { response ->
                val status = response.code
                val retryAfter = parseRetryAfterSeconds(response.header(HEADER_RETRY_AFTER))
                val body = if (response.isSuccessful) response.body?.string().orEmpty() else ""
                RawHttpOutcome(statusCode = status, retryAfterSeconds = retryAfter, body = body)
            }
        }
    }

    private data class RawHttpOutcome(
        val statusCode: Int,
        val retryAfterSeconds: Long?,
        val body: String
    )

    companion object {
        private const val TAG = "KanaHttpExecutor"
        private const val JSON_MEDIA_TYPE = "application/json; charset=utf-8"
        private const val HEADER_AUTHORIZATION = "Authorization"
        private const val HEADER_RETRY_AFTER = "Retry-After"
        private val RETRYABLE_STATUS_CODES = setOf(429, 502, 503)
    }
}
