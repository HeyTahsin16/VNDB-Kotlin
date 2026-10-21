package com.vndblite.dcj.data.repository

import androidx.room.withTransaction
import com.vndblite.dcj.core.AppLog
import com.vndblite.dcj.core.AppResult
import com.vndblite.dcj.core.Clock
import com.vndblite.dcj.core.DefaultDispatcherProvider
import com.vndblite.dcj.core.DispatcherProvider
import com.vndblite.dcj.core.SystemClock
import com.vndblite.dcj.data.local.db.AppDatabase
import com.vndblite.dcj.data.local.db.dao.HomeSectionCacheDao
import com.vndblite.dcj.data.local.db.dao.VnCacheDao
import com.vndblite.dcj.data.local.db.entity.HomeSectionCacheEntity
import com.vndblite.dcj.data.local.db.entity.QuoteCacheEntity
import com.vndblite.dcj.data.local.db.entity.SiteStatsCacheEntity
import com.vndblite.dcj.data.local.db.entity.VnCacheEntity
import com.vndblite.dcj.data.remote.VnFullDetailPayload
import com.vndblite.dcj.data.remote.VnQuerySpec
import com.vndblite.dcj.data.remote.VndbApi
import com.vndblite.dcj.data.remote.scraper.ReviewSource
import com.vndblite.dcj.domain.model.Character
import com.vndblite.dcj.domain.model.CharacterSearchFilters
import com.vndblite.dcj.domain.model.HomeSection
import com.vndblite.dcj.domain.model.PaginatedList
import com.vndblite.dcj.domain.model.Producer
import com.vndblite.dcj.domain.model.Quote
import com.vndblite.dcj.domain.model.Review
import com.vndblite.dcj.domain.model.SiteStats
import com.vndblite.dcj.domain.model.SortOption
import com.vndblite.dcj.domain.model.Tag
import com.vndblite.dcj.domain.model.Trait
import com.vndblite.dcj.domain.model.VnDetails
import com.vndblite.dcj.domain.model.VnSearchFilters
import com.vndblite.dcj.domain.model.VnSummary
import com.vndblite.dcj.domain.repository.VnRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Default [VnRepository] coordinating [VndbApi], [ReviewSource], and [AppDatabase] with
 * per-section TTLs, stale-while-revalidate, and LRU cache pruning (Section 5, A3, S4, F1, F2, F5, F6).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VnRepositoryImpl(
    private val api: VndbApi,
    private val reviewSource: ReviewSource,
    private val database: AppDatabase,
    private val clock: Clock = SystemClock,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider
) : VnRepository {

    private val vnCacheDao = database.vnCacheDao()
    private val homeSectionDao = database.homeSectionCacheDao()
    private val quoteCacheDao = database.quoteCacheDao()
    private val siteStatsDao = database.siteStatsCacheDao()

    override fun observeCachedHomeSection(section: HomeSection): Flow<List<VnSummary>> =
        homeSectionDao.observeSection(section.name).flatMapLatest { sectionRow ->
            val orderedIds = sectionRow?.decodeVnIds().orEmpty()
            if (orderedIds.isEmpty()) {
                flowOf(emptyList())
            } else {
                vnCacheDao.observeByIds(orderedIds).map { cachedEntities ->
                    val byId = cachedEntities.associateBy { it.vnId }
                    orderedIds.mapNotNull { id -> byId[id]?.toSummary() }
                }
            }
        }

    override suspend fun refreshHomeSection(
        section: HomeSection,
        maxItems: Int,
        forceRefresh: Boolean
    ): AppResult<List<VnSummary>> = withContext(dispatchers.io) {
        val nowSeconds = clock.nowEpochSeconds()
        val cachedSection = homeSectionDao.getSection(section.name)
        val ttlSeconds = ttlSecondsForSection(section)
        if (!forceRefresh && cachedSection != null && (nowSeconds - cachedSection.fetchedAtEpochSeconds) < ttlSeconds) {
            val cachedIds = cachedSection.decodeVnIds()
            val cachedRows = vnCacheDao.getByIds(cachedIds).associateBy { it.vnId }
            val orderedSummaries = cachedIds.mapNotNull { cachedRows[it]?.toSummary() }
            if (orderedSummaries.isNotEmpty()) {
                return@withContext AppResult.Success(orderedSummaries)
            }
        }

        val filters = VnSearchFilters(
            devStatuses = listOfNotNull(section.devStatusFilter),
            releaseWindow = section.releaseWindow
        )
        val spec = VnQuerySpec(
            filters = filters,
            sort = SortOption(field = section.sortField, reverse = section.reverseSort),
            page = 1,
            maxResults = maxItems.coerceIn(5, 50),
            includeDescription = false
        )
        when (val result = api.queryVns(spec)) {
            is AppResult.Success -> {
                val items = result.data.items
                cacheSummariesAndPrune(items, nowSeconds)
                homeSectionDao.upsertSection(
                    HomeSectionCacheEntity.create(
                        sectionCode = section.name,
                        vnIds = items.map { it.id },
                        fetchedAtEpochSeconds = nowSeconds
                    )
                )
                AppResult.Success(items)
            }
            is AppResult.Failure -> result
        }
    }

    override fun observeCachedQuotes(): Flow<List<Quote>> =
        quoteCacheDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun refreshHomeQuotes(
        maxResults: Int,
        forceRefresh: Boolean
    ): AppResult<List<Quote>> = withContext(dispatchers.io) {
        val nowSeconds = clock.nowEpochSeconds()
        val existingQuotes = quoteCacheDao.getAll()
        val newestFetchedAt = existingQuotes.maxOfOrNull { it.fetchedAtEpochSeconds } ?: 0L
        if (!forceRefresh &&
            existingQuotes.isNotEmpty() &&
            (nowSeconds - newestFetchedAt) < HomeSectionCacheDao.STANDARD_SECTION_TTL_SECONDS
        ) {
            return@withContext AppResult.Success(existingQuotes.map { it.toDomain() })
        }

        when (val result = api.fetchTopQuotes(maxResults = maxResults)) {
            is AppResult.Success -> {
                val quotes = result.data
                val entities = quotes.mapIndexed { idx, quote ->
                    QuoteCacheEntity.fromDomain(quote, displayOrder = idx, fetchedAtEpochSeconds = nowSeconds)
                }
                quoteCacheDao.replaceAll(entities)
                val embeddedVns = quotes.mapNotNull { it.vn }
                cacheSummariesAndPrune(embeddedVns, nowSeconds)
                AppResult.Success(quotes)
            }
            is AppResult.Failure -> result
        }
    }

    override fun observeCachedSiteStats(): Flow<SiteStats?> =
        siteStatsDao.observeStats().map { it?.toDomain() }

    override suspend fun refreshSiteStats(forceRefresh: Boolean): AppResult<SiteStats> =
        withContext(dispatchers.io) {
            val nowSeconds = clock.nowEpochSeconds()
            val cached = siteStatsDao.getStats()
            if (!forceRefresh &&
                cached != null &&
                (nowSeconds - cached.fetchedAtEpochSeconds) < HomeSectionCacheDao.STANDARD_SECTION_TTL_SECONDS
            ) {
                return@withContext AppResult.Success(cached.toDomain())
            }
            when (val result = api.fetchStats()) {
                is AppResult.Success -> {
                    val stamped = result.data.copy(fetchedAt = clock.nowInstant())
                    siteStatsDao.upsertStats(SiteStatsCacheEntity.fromDomain(stamped))
                    AppResult.Success(stamped)
                }
                is AppResult.Failure -> result
            }
        }

    override suspend fun searchVns(spec: VnQuerySpec): AppResult<PaginatedList<VnSummary>> =
        withContext(dispatchers.io) {
            when (val res = api.queryVns(spec)) {
                is AppResult.Success -> {
                    cacheSummariesAndPrune(res.data.items, clock.nowEpochSeconds())
                    res
                }
                is AppResult.Failure -> res
            }
        }

    override suspend fun searchCharacters(
        filters: CharacterSearchFilters,
        page: Int,
        maxResults: Int
    ): AppResult<PaginatedList<Character>> = withContext(dispatchers.io) {
        api.searchCharacters(filters = filters, page = page, maxResults = maxResults)
    }

    override suspend fun searchTags(query: String): AppResult<List<Tag>> =
        withContext(dispatchers.io) { api.searchTags(query) }

    override suspend fun searchProducers(query: String): AppResult<List<Producer>> =
        withContext(dispatchers.io) { api.searchProducers(query) }

    override suspend fun searchTraits(query: String): AppResult<List<Trait>> =
        withContext(dispatchers.io) { api.searchTraits(query) }

    override suspend fun fetchRandomVn(): AppResult<VnSummary> = withContext(dispatchers.io) {
        when (val res = api.fetchRandomVn()) {
            is AppResult.Success -> {
                cacheSummariesAndPrune(listOf(res.data), clock.nowEpochSeconds())
                res
            }
            is AppResult.Failure -> res
        }
    }

    override fun observeVnSummaryAndDetails(vnId: String): Flow<Pair<VnSummary?, VnDetails?>> =
        vnCacheDao.observeById(vnId).map { entity ->
            if (entity == null) null to null
            else entity.toSummary() to entity.toDetailsOrNull()
        }

    override suspend fun getCachedVnSummary(vnId: String): VnSummary? = withContext(dispatchers.io) {
        vnCacheDao.getById(vnId)?.toSummary()
    }

    override suspend fun cacheVnSummary(summary: VnSummary) = withContext(dispatchers.io) {
        cacheSummariesAndPrune(listOf(summary), clock.nowEpochSeconds())
    }

    override suspend fun loadVnFullDetails(
        vnId: String,
        forceRefresh: Boolean
    ): AppResult<VnFullDetailPayload> = withContext(dispatchers.io) {
        val nowSeconds = clock.nowEpochSeconds()
        val cached = vnCacheDao.getById(vnId)
        val detailsFetchedAt = cached?.detailsFetchedAtEpochSeconds
        if (!forceRefresh &&
            cached != null &&
            cached.hasFullDetails &&
            !cached.description.isNullOrBlank() &&
            detailsFetchedAt != null &&
            (nowSeconds - detailsFetchedAt) < VnCacheDao.DEFAULT_DETAILS_TTL_SECONDS
        ) {
            val details = cached.toDetailsOrNull()
            if (details != null) {
                vnCacheDao.touchLastAccessed(vnId, nowSeconds)
                return@withContext AppResult.Success(VnFullDetailPayload(cached.toSummary(), details))
            }
        }

        when (val result = api.fetchVnFullDetails(vnId)) {
            is AppResult.Success -> {
                val payload = result.data
                val missingRelationIds = payload.details.relations
                    .filter { it.relatedVn == null }
                    .map { it.id }
                    .distinct()
                val hydratedDetails = if (missingRelationIds.isNotEmpty()) {
                    val relatedById = fetchRelatedVnSummaries(missingRelationIds)
                        .getOrNull()
                        ?.associateBy { it.id }
                        .orEmpty()
                    payload.details.copy(
                        relations = payload.details.relations.map { rel ->
                            if (rel.relatedVn != null) rel else rel.copy(relatedVn = relatedById[rel.id])
                        }
                    )
                } else {
                    payload.details
                }
                val hydratedPayload = payload.copy(details = hydratedDetails)
                val updatedEntity = VnCacheEntity.fromSummaryAndDetails(
                    summary = hydratedPayload.summary,
                    details = hydratedPayload.details,
                    nowEpochSeconds = nowSeconds,
                    existing = cached
                )
                vnCacheDao.upsert(updatedEntity)
                AppResult.Success(hydratedPayload)
            }
            is AppResult.Failure -> result
        }
    }

    override suspend fun fetchRelatedVnSummaries(vnIds: List<String>): AppResult<List<VnSummary>> =
        withContext(dispatchers.io) {
            val cleaned = vnIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
            if (cleaned.isEmpty()) return@withContext AppResult.Success(emptyList())
            val nowSeconds = clock.nowEpochSeconds()
            val cachedById = vnCacheDao.getByIds(cleaned).associateBy { it.vnId }
            val missingIds = cleaned.filter { id ->
                val row = cachedById[id]
                row == null || (row.imageUrl.isNullOrBlank() && row.rating == null)
            }
            if (missingIds.isNotEmpty()) {
                when (val fetched = api.fetchVnSummariesByIds(missingIds, includeDescription = false)) {
                    is AppResult.Success -> cacheSummariesAndPrune(fetched.data, nowSeconds)
                    is AppResult.Failure -> {
                        if (cachedById.isEmpty()) return@withContext fetched
                        AppLog.w(TAG, "Partial related VN summary fetch failed: ${fetched.error}")
                    }
                }
            }
            val finalById = vnCacheDao.getByIds(cleaned).associateBy { it.vnId }
            AppResult.Success(cleaned.mapNotNull { finalById[it]?.toSummary() })
        }

    override suspend fun fetchCharactersForVn(vnId: String): AppResult<PaginatedList<Character>> =
        withContext(dispatchers.io) { api.fetchCharactersForVn(vnId) }

    override suspend fun fetchQuotesForVn(vnId: String): AppResult<List<Quote>> =
        withContext(dispatchers.io) { api.fetchQuotesForVn(vnId) }

    override suspend fun fetchReviewsForVn(vnId: String): AppResult<PaginatedList<Review>> =
        withContext(dispatchers.io) { reviewSource.fetchReviewsForVn(vnId) }

    override suspend fun fetchFullReviewText(reviewId: String): AppResult<String> =
        withContext(dispatchers.io) { reviewSource.fetchFullReviewText(reviewId) }

    override suspend fun clearTransientCaches() = withContext(dispatchers.io) {
        database.clearTransientCaches()
    }

    private suspend fun cacheSummariesAndPrune(
        summaries: List<VnSummary>,
        nowSeconds: Long
    ) {
        if (summaries.isEmpty()) return
        database.withTransaction {
            val existingById = vnCacheDao.getByIds(summaries.map { it.id }).associateBy { it.vnId }
            val entities = summaries.map { summary ->
                VnCacheEntity.fromSummary(
                    summary = summary,
                    nowEpochSeconds = nowSeconds,
                    existing = existingById[summary.id]
                )
            }
            vnCacheDao.upsertAll(entities)
            val minFetchedAt = nowSeconds - VnCacheDao.DEFAULT_SUMMARY_TTL_SECONDS
            vnCacheDao.pruneNonCollectionCache(minFetchedAtEpochSeconds = minFetchedAt)
        }
    }

    private fun ttlSecondsForSection(section: HomeSection): Long = when (section) {
        HomeSection.LATEST_RELEASES,
        HomeSection.UPCOMING_RELEASES -> HomeSectionCacheDao.LATEST_AND_UPCOMING_TTL_SECONDS
        else -> HomeSectionCacheDao.Builders.STANDARD_TTL
    }

    companion object {
        private const val TAG = "VnRepository"
    }
}

private object HomeSectionCacheDaoBuilders {
    const val STANDARD_TTL: Long = HomeSectionCacheDao.STANDARD_SECTION_TTL_SECONDS
}

private val HomeSectionCacheDao.Companion.Builders: HomeSectionCacheDaoBuilders
    get() = HomeSectionCacheDaoBuilders
