package com.vndblite.dcj.data.remote

import com.vndblite.dcj.core.ApiError
import com.vndblite.dcj.core.AppResult
import com.vndblite.dcj.core.Clock
import com.vndblite.dcj.core.DefaultDispatcherProvider
import com.vndblite.dcj.core.DispatcherProvider
import com.vndblite.dcj.core.SystemClock
import com.vndblite.dcj.core.map
import com.vndblite.dcj.data.remote.dto.AuthInfoDto
import com.vndblite.dcj.data.remote.dto.CharacterDto
import com.vndblite.dcj.data.remote.dto.KanaFieldSets
import com.vndblite.dcj.data.remote.dto.KanaJson
import com.vndblite.dcj.data.remote.dto.KanaQueryRequestDto
import com.vndblite.dcj.data.remote.dto.ProducerDto
import com.vndblite.dcj.data.remote.dto.QuoteDto
import com.vndblite.dcj.data.remote.dto.StatsDto
import com.vndblite.dcj.data.remote.dto.TagDto
import com.vndblite.dcj.data.remote.dto.TraitDto
import com.vndblite.dcj.data.remote.dto.UserListItemDto
import com.vndblite.dcj.data.remote.dto.UserListPatchPayloadBuilder
import com.vndblite.dcj.data.remote.dto.VnDto
import com.vndblite.dcj.data.remote.filter.KanaFilter
import com.vndblite.dcj.data.remote.filter.KanaFilters
import com.vndblite.dcj.data.remote.http.HttpClientFactory
import com.vndblite.dcj.data.remote.http.KanaHttpExecutor
import com.vndblite.dcj.data.remote.http.RateLimiter
import com.vndblite.dcj.domain.model.Character
import com.vndblite.dcj.domain.model.CharacterSearchFilters
import com.vndblite.dcj.domain.model.PaginatedList
import com.vndblite.dcj.domain.model.Producer
import com.vndblite.dcj.domain.model.Quote
import com.vndblite.dcj.domain.model.RemoteUserListItem
import com.vndblite.dcj.domain.model.SiteStats
import com.vndblite.dcj.domain.model.Tag
import com.vndblite.dcj.domain.model.Trait
import com.vndblite.dcj.domain.model.UserAccount
import com.vndblite.dcj.domain.model.UserListPatch
import com.vndblite.dcj.domain.model.VnSummary
import kotlin.random.Random
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.OkHttpClient

/**
 * Production implementation of [VndbApi] using `kotlinx.serialization`, cancellable OkHttp calls,
 * batched ID queries (`<= 100`), paginated `/release`, `/character`, and `/ulist` loops,
 * token permission validation (`listread` + `listwrite`), and the official 2-step random VN algorithm
 * (N1-N10, F1, F6, R10).
 */
class VndbApiImpl(
    httpClient: OkHttpClient = HttpClientFactory.create(),
    rateLimiter: RateLimiter = RateLimiter(),
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider,
    private val clock: Clock = SystemClock,
    private val json: Json = KanaJson,
    baseKanaUrl: String = DEFAULT_KANA_BASE_URL,
    private val randomGenerator: Random = Random.Default,
    maxAutoPages: Int = DEFAULT_MAX_AUTO_PAGES
) : VndbApi {

    private val executor = KanaHttpExecutor(
        httpClient = httpClient,
        rateLimiter = rateLimiter,
        json = json,
        baseKanaUrl = baseKanaUrl,
        maxAutoPages = maxAutoPages
    )

    override suspend fun queryVns(spec: VnQuerySpec): AppResult<PaginatedList<VnSummary>> =
        withContext(dispatchers.io) {
            val fields = if (spec.includeDescription) {
                KanaFieldSets.VN_SUMMARY_WITH_DESCRIPTION
            } else {
                KanaFieldSets.VN_CARD_SUMMARY
            }
            val requestDto = KanaQueryRequestDto(
                filters = KanaFilters.forVnSearch(spec.filters).toJsonArray(),
                fields = fields,
                sort = spec.sort.field.code,
                reverse = spec.sort.reverse,
                results = spec.maxResults.coerceIn(1, MAX_PAGE_SIZE),
                page = spec.page.coerceAtLeast(1)
            )
            executor.postPage("vn", requestDto, VnDto.serializer()).map { page ->
                PaginatedList(
                    items = page.results.map { it.toSummary() },
                    hasMore = page.more,
                    page = spec.page.coerceAtLeast(1)
                )
            }
        }

    override suspend fun fetchVnSummariesByIds(
        ids: List<String>,
        includeDescription: Boolean
    ): AppResult<List<VnSummary>> = withContext(dispatchers.io) {
        val distinctIds = ids.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (distinctIds.isEmpty()) return@withContext AppResult.Success(emptyList())
        val fields = if (includeDescription) {
            KanaFieldSets.VN_SUMMARY_WITH_DESCRIPTION
        } else {
            KanaFieldSets.VN_CARD_SUMMARY
        }
        val fetchedById = linkedMapOf<String, VnSummary>()
        for (chunk in distinctIds.chunked(KanaFilters.MAX_BATCH_IDS)) {
            val req = KanaQueryRequestDto(
                filters = KanaFilters.byIds(chunk).toJsonArray(),
                fields = fields,
                results = chunk.size.coerceAtMost(MAX_PAGE_SIZE),
                page = 1
            )
            when (val pageRes = executor.postPage("vn", req, VnDto.serializer())) {
                is AppResult.Failure -> return@withContext pageRes
                is AppResult.Success -> {
                    pageRes.data.results.forEach { dto ->
                        val summary = dto.toSummary()
                        fetchedById[summary.id] = summary
                    }
                }
            }
        }
        AppResult.Success(distinctIds.mapNotNull { fetchedById[it] })
    }

    override suspend fun fetchVnFullDetails(vnId: String): AppResult<VnFullDetailPayload> =
        withContext(dispatchers.io) {
            VndbApiHelpers.fetchVnFullDetails(
                vnId = vnId,
                executor = executor,
                fetchSummariesByIds = ::fetchVnSummariesByIds
            )
        }

    override suspend fun fetchRandomVn(
        extraFilterClauses: List<KanaFilter>
    ): AppResult<VnSummary> = withContext(dispatchers.io) {
        VndbApiHelpers.fetchRandomVn(
            extraFilterClauses = extraFilterClauses,
            executor = executor,
            randomGenerator = randomGenerator
        )
    }

    override suspend fun fetchCharactersForVn(vnId: String): AppResult<PaginatedList<Character>> =
        withContext(dispatchers.io) {
            val baseRequest = KanaQueryRequestDto(
                filters = KanaFilters.forVnCharacters(vnId.trim()).toJsonArray(),
                fields = KanaFieldSets.CHARACTER_FULL,
                results = MAX_PAGE_SIZE
            )
            executor.fetchAllPages("character", baseRequest, CharacterDto.serializer()).map { page ->
                PaginatedList(
                    items = page.items.map { it.toDomain() },
                    hasMore = page.hasMore,
                    page = page.page
                )
            }
        }

    override suspend fun searchCharacters(
        filters: CharacterSearchFilters,
        page: Int,
        maxResults: Int
    ): AppResult<PaginatedList<Character>> = withContext(dispatchers.io) {
        val sortField = if (filters.searchQuery.isNotBlank()) "searchrank" else "id"
        val req = KanaQueryRequestDto(
            filters = KanaFilters.forCharacterSearch(filters).toJsonArray(),
            fields = KanaFieldSets.CHARACTER_FULL,
            sort = sortField,
            reverse = false,
            results = maxResults.coerceIn(1, MAX_PAGE_SIZE),
            page = page.coerceAtLeast(1)
        )
        executor.postPage("character", req, CharacterDto.serializer()).map { res ->
            PaginatedList(
                items = res.results.map { it.toDomain() },
                hasMore = res.more,
                page = page.coerceAtLeast(1)
            )
        }
    }

    override suspend fun fetchTopQuotes(maxResults: Int): AppResult<List<Quote>> =
        withContext(dispatchers.io) {
            val req = KanaQueryRequestDto(
                filters = null,
                fields = KanaFieldSets.QUOTE_WITH_LEAN_VN,
                sort = "score",
                reverse = true,
                results = maxResults.coerceIn(1, MAX_PAGE_SIZE),
                page = 1
            )
            executor.postPage("quote", req, QuoteDto.serializer()).map { page ->
                page.results.mapNotNull { it.toDomainOrNull() }
            }
        }

    override suspend fun fetchRandomQuote(): AppResult<Quote?> =
        withContext(dispatchers.io) {
            val req = KanaQueryRequestDto(
                filters = KanaFilters.forRandomQuote().toJsonArray(),
                fields = KanaFieldSets.QUOTE_WITH_LEAN_VN,
                results = 1
            )
            executor.postPage("quote", req, QuoteDto.serializer()).map { page ->
                page.results.firstOrNull()?.toDomainOrNull()
            }
        }

    override suspend fun fetchQuotesForVn(vnId: String, maxResults: Int): AppResult<List<Quote>> =
        withContext(dispatchers.io) {
            val req = KanaQueryRequestDto(
                filters = KanaFilters.forVnQuotes(vnId.trim()).toJsonArray(),
                fields = KanaFieldSets.QUOTE_WITH_LEAN_VN,
                sort = "score",
                reverse = true,
                results = maxResults.coerceIn(1, MAX_PAGE_SIZE),
                page = 1
            )
            executor.postPage("quote", req, QuoteDto.serializer()).map { page ->
                page.results.mapNotNull { it.toDomainOrNull() }
            }
        }

    override suspend fun searchTags(query: String, maxResults: Int): AppResult<List<Tag>> =
        searchLookup("tag", query, KanaFieldSets.TAG_AUTOCOMPLETE, maxResults, TagDto.serializer()) {
            it.toDomainOrNull()
        }

    override suspend fun searchProducers(query: String, maxResults: Int): AppResult<List<Producer>> =
        searchLookup("producer", query, KanaFieldSets.PRODUCER_AUTOCOMPLETE, maxResults, ProducerDto.serializer()) {
            it.toDomainOrNull()
        }

    override suspend fun searchTraits(query: String, maxResults: Int): AppResult<List<Trait>> =
        searchLookup("trait", query, KanaFieldSets.TRAIT_AUTOCOMPLETE, maxResults, TraitDto.serializer()) {
            it.toDomainOrNull()
        }

    override suspend fun fetchStats(): AppResult<SiteStats> =
        withContext(dispatchers.io) {
            executor.executeJsonRequest(
                method = "GET",
                endpoint = "stats",
                requestBodyJson = null,
                authToken = null,
                responseSerializer = StatsDto.serializer()
            ).map { it.toDomain(fetchedAt = clock.nowInstant()) }
        }

    override suspend fun authenticate(token: String): AppResult<UserAccount> =
        withContext(dispatchers.io) {
            val cleanToken = token.trim()
            if (cleanToken.isEmpty()) return@withContext AppResult.Failure(ApiError.Unauthorized)
            val result = executor.executeJsonRequest(
                method = "GET",
                endpoint = "authinfo",
                requestBodyJson = null,
                authToken = cleanToken,
                responseSerializer = AuthInfoDto.serializer()
            )
            when (result) {
                is AppResult.Failure -> result
                is AppResult.Success -> {
                    val account = result.data.toDomain()
                    val missing = account.missingListPermissions
                    if (missing.isNotEmpty()) {
                        AppResult.Failure(ApiError.MissingPermission(missing))
                    } else {
                        AppResult.Success(account)
                    }
                }
            }
        }

    override suspend fun fetchUserList(
        userId: String,
        token: String
    ): AppResult<List<RemoteUserListItem>> = withContext(dispatchers.io) {
        val baseRequest = KanaQueryRequestDto(
            user = userId.trim(),
            fields = KanaFieldSets.USER_LIST_ENTRY,
            sort = "lastmod",
            reverse = true,
            results = MAX_PAGE_SIZE
        )
        executor.fetchAllPages(
            endpoint = "ulist",
            baseRequest = baseRequest,
            itemSerializer = UserListItemDto.serializer(),
            authToken = token.trim(),
            maxPages = MAX_ULIST_PAGES
        ).map { page ->
            page.items.mapNotNull { it.toDomainOrNull() }
        }
    }

    override suspend fun patchUserListEntry(
        token: String,
        patch: UserListPatch
    ): AppResult<Unit> = withContext(dispatchers.io) {
        val payloadJson = json.encodeToString(
            JsonObject.serializer(),
            UserListPatchPayloadBuilder.toJsonObject(patch)
        )
        executor.executeUnitRequest(
            method = "PATCH",
            endpoint = "ulist/${patch.vnId.trim()}",
            requestBodyJson = payloadJson,
            authToken = token.trim()
        )
    }

    override suspend fun deleteUserListEntry(
        token: String,
        vnId: String
    ): AppResult<Unit> = withContext(dispatchers.io) {
        executor.executeUnitRequest(
            method = "DELETE",
            endpoint = "ulist/${vnId.trim()}",
            requestBodyJson = null,
            authToken = token.trim()
        )
    }

    private suspend fun <Dto, Domain : Any> searchLookup(
        endpoint: String,
        query: String,
        fields: String,
        maxResults: Int,
        serializer: KSerializer<Dto>,
        mapper: (Dto) -> Domain?
    ): AppResult<List<Domain>> = withContext(dispatchers.io) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext AppResult.Success(emptyList())
        val req = KanaQueryRequestDto(
            filters = KanaFilter.eq("search", trimmed).toJsonArray(),
            fields = fields,
            sort = "searchrank",
            results = maxResults.coerceIn(1, MAX_PAGE_SIZE),
            page = 1
        )
        executor.postPage(endpoint, req, serializer).map { page ->
            page.results.mapNotNull(mapper)
        }
    }

    companion object {
        private const val TAG = "VndbApiImpl"
        const val DEFAULT_KANA_BASE_URL: String = "https://api.vndb.org/kana"
        const val MAX_PAGE_SIZE: Int = 100
        const val DEFAULT_MAX_AUTO_PAGES: Int = 10
        const val MAX_ULIST_PAGES: Int = 50
    }
}
