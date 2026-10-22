package com.vndblite.dcj.data.repository

import androidx.room.withTransaction
import com.vndblite.dcj.core.AppResult
import com.vndblite.dcj.core.Clock
import com.vndblite.dcj.core.DefaultDispatcherProvider
import com.vndblite.dcj.core.DispatcherProvider
import com.vndblite.dcj.core.SystemClock
import com.vndblite.dcj.core.onSuccess
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
import com.vndblite.dcj.data.remote.filter.KanaFilter
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
import com.vndblite.dcj.domain.model.VnSearchFilters
import com.vndblite.dcj.domain.model.VnSummary
import com.vndblite.dcj.domain.repository.HomeSectionSnapshot
import com.vndblite.dcj.domain.repository.VnDetailBundle
import com.vndblite.dcj.domain.repository.VnRepository
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class VnRepositoryImpl(
    private val api: VndbApi,
    private val reviewSource: ReviewSource,
    private val database: AppDatabase,
    private val clock: Clock = SystemClock,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider,
    private val maxNonCollectionCacheEntries: Int = VnCacheDao.DEFAULT_MAX_NON_COLLECTION_ENTRIES
) : VnRepository {

    private val vnCacheDao = database.vnCacheDao()
    private val homeSectionDao = database.homeSectionCacheDao()
    private val quoteCacheDao = database.quoteCacheDao()
    private val siteStatsDao = database.siteStatsCacheDao()
    private val collectionDao = database.collectionDao()

    override fun observeHomeSection(section: HomeSection, maxItems: Int): Flow<HomeSectionSnapshot> {
        val cappedMax = maxItems.coerceIn(1, 50)
        if (section == HomeSection.COLLECTION) {
            return collectionDao.observeRecentCollectionEntries(cappedMax).map { rows ->
                HomeSectionSnapshot(
                    section = section,
                    items = rows.map { it.toDomain().vn },
                    fetchedAt = Instant.ofEpochSecond(clock.nowEpochSeconds()),
                    isStale = false
                )
            }
        }
        return homeSectionDao.observeSection(section.name).flatMapLatest { sectionEntity ->
            val orderedIds = sectionEntity?.decodeVnIds()?.take(cappedMax).orEmpty()
            if (orderedIds.isEmpty()) {
                flowOf(
                    HomeSectionSnapshot(
                        section = section,
                        items = emptyList(),
                        fetchedAt = sectionEntity?.fetchedAtEpochSeconds?.let(Instant::ofEpochSecond),
                        isStale = true
                    )
                )
            } else {
                vnCacheDao.observeByIds(orderedIds).map { cachedRows ->
                    val byId = cachedRows.associateBy { it.vnId }
                    val orderedSummaries = orderedIds.mapNotNull { byId[it]?.toSummary() }
                    val fetchedSec = sectionEntity?.fetchedAtEpochSeconds ?: 0L
                    HomeSectionSnapshot(
                        section = section,
                        items = orderedSummaries,
                        fetchedAt = if (fetchedSec > 0L) Instant.ofEpochSecond(fetchedSec) else null,
                        isStale = isSectionStale(section, fetchedSec, clock.nowEpochSeconds())
                    )
                }
            }
        }
    }

    override suspend fun refreshHomeSection(
        section: HomeSection,
        maxItems: Int,
        force: Boolean
    ): AppResult<List<VnSummary>> = withContext(dispatchers.io) {
        val cappedMax = maxItems.coerceIn(1, 50)
        if (section == HomeSection.COLLECTION) {
            val localItems = collectionDao.getAllVisibleEntries().take(cappedMax).map {
                vnCacheDao.getById(it.vnId)?.toSummary() ?: VnSummary(id = it.vnId, title = it.title)
            }
            return@withContext AppResult.Success(localItems)
        }

        val nowSec = clock.nowEpochSeconds()
        if (!force) {
            val cachedSection = homeSectionDao.getSection(section.name)
            if (cachedSection != null && !isSectionStale(section, cachedSection.fetchedAtEpochSeconds, nowSec)) {
                val ids = cachedSection.decodeVnIds().take(cappedMax)
                val cachedById = vnCacheDao.getByIds(ids).associateBy { it.vnId }
                val ordered = ids.mapNotNull { cachedById[it]?.toSummary() }
                if (ordered.isNotEmpty() && ordered.size == ids.size) {
                    vnCacheDao.touchLastAccessedBatch(ids, nowSec)
                    return@withContext AppResult.Success(ordered)
                }
            }
        }

        val spec = buildHomeSectionQuerySpec(section, cappedMax)
        when (val result = api.queryVns(spec)) {
            is AppResult.Failure -> result
            is AppResult.Success -> {
                val items = result.data.items
                val fetchedSec = clock.nowEpochSeconds()
                database.withTransaction {
                    upsertSummariesPreservingExisting(items, fetchedSec)
                    homeSectionDao.upsertSection(
                        HomeSectionCacheEntity.create(
                            sectionCode = section.name,
                            vnIds = items.map { it.id },
                            fetchedAtEpochSeconds = fetchedSec
                        )
                    )
                    enforceLruCap(fetchedSec)
                }
                AppResult.Success(items)
            }
        }
    }

    override suspend fun refreshAllRemoteHomeSections(
        maxItems: Int,
        force: Boolean
    ): Map<HomeSection, AppResult<List<VnSummary>>> = withContext(dispatchers.io) {
        val remoteSections = HomeSection.entries.filter { it != HomeSection.COLLECTION }
        coroutineScope {
            remoteSections.map { section ->
                async {
                    section to refreshHomeSection(section = section, maxItems = maxItems, force = force)
                }
            }.awaitAll().toMap()
        }
    }

    override fun observeCachedQuotes(): Flow<List<Quote>> =
        quoteCacheDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun refreshQuotes(maxResults: Int): AppResult<List<Quote>> =
        withContext(dispatchers.io) {
            api.fetchTopQuotes(maxResults).onSuccess { quotes ->
                val nowSec = clock.nowEpochSeconds()
                val entities = quotes.mapIndexed { idx, quote ->
                    QuoteCacheEntity.fromDomain(quote = quote, displayOrder = idx, fetchedAtEpochSeconds = nowSec)
                }
                quoteCacheDao.replaceAll(entities)
            }
        }

    override suspend fun fetchRandomQuote(): AppResult<Quote?> =
        withContext(dispatchers.io) { api.fetchRandomQuote() }

    override fun observeSiteStats(): Flow<SiteStats?> =
        siteStatsDao.observeStats().map { it?.toDomain() }

    override suspend fun refreshSiteStats(): AppResult<SiteStats> =
        withContext(dispatchers.io) {
            api.fetchStats().onSuccess { stats ->
                siteStatsDao.upsertStats(SiteStatsCacheEntity.fromDomain(stats))
            }
        }

    override fun observeVnDetail(vnId: String): Flow<VnDetailBundle?> =
        vnCacheDao.observeById(vnId.trim()).map { entity ->
            entity?.let {
                val nowSec = clock.nowEpochSeconds()
                val summaryAge = nowSec - it.summaryFetchedAtEpochSeconds
                val detailsAge = it.detailsFetchedAtEpochSeconds?.let { dSec -> nowSec - dSec } ?: Long.MAX_VALUE
                VnDetailBundle(
                    summary = it.toSummary(),
                    details = it.toDetailsOrNull(),
                    isSummaryStale = summaryAge >= VnCacheDao.DEFAULT_SUMMARY_TTL_SECONDS,
                    isDetailsStale = !it.hasFullDetails || detailsAge >= VnCacheDao.DEFAULT_DETAILS_TTL_SECONDS
                )
            }
        }

    override suspend fun getVnDetail(
        vnId: String,
        forceRefresh: Boolean
    ): AppResult<VnFullDetailPayload> = withContext(dispatchers.io) {
        val cleanId = vnId.trim()
        val nowSec = clock.nowEpochSeconds()
        val existing = vnCacheDao.getById(cleanId)
        if (!forceRefresh && existing != null && existing.hasFullDetails) {
            val detailsFetchedAt = existing.detailsFetchedAtEpochSeconds ?: 0L
            if (nowSec - detailsFetchedAt < VnCacheDao.DEFAULT_DETAILS_TTL_SECONDS) {
                val cachedDetails = existing.toDetailsOrNull()
                if (cachedDetails != null) {
                    vnCacheDao.touchLastAccessed(cleanId, nowSec)
                    return@withContext AppResult.Success(
                        VnFullDetailPayload(summary = existing.toSummary(), details = cachedDetails)
                    )
                }
            }
        }

        when (val remoteRes = api.fetchVnFullDetails(cleanId)) {
            is AppResult.Failure -> remoteRes
            is AppResult.Success -> {
                val payload = remoteRes.data
                val savedSec = clock.nowEpochSeconds()
                database.withTransaction {
                    val latestExisting = vnCacheDao.getById(cleanId)
                    val mergedEntity = VnCacheEntity.fromSummaryAndDetails(
                        summary = payload.summary,
                        details = payload.details,
                        nowEpochSeconds = savedSec,
                        existing = latestExisting
                    )
                    vnCacheDao.upsert(mergedEntity)
                    val relatedSummaries = payload.details.relations.mapNotNull { it.relatedVn }
                    if (relatedSummaries.isNotEmpty()) {
                        upsertSummariesPreservingExisting(relatedSummaries, savedSec)
                    }
                    enforceLruCap(savedSec)
                }
                AppResult.Success(payload)
            }
        }
    }

    override suspend fun queryVns(spec: VnQuerySpec): AppResult<PaginatedList<VnSummary>> =
        withContext(dispatchers.io) {
            api.queryVns(spec).onSuccess { page ->
                if (page.items.isNotEmpty()) {
                    val nowSec = clock.nowEpochSeconds()
                    database.withTransaction {
                        upsertSummariesPreservingExisting(page.items, nowSec)
                        enforceLruCap(nowSec)
                    }
                }
            }
        }

    override suspend fun searchCharacters(
        filters: CharacterSearchFilters,
        page: Int,
        maxResults: Int
    ): AppResult<PaginatedList<Character>> =
        withContext(dispatchers.io) { api.searchCharacters(filters, page, maxResults) }

    override suspend fun fetchCharactersForVn(vnId: String): AppResult<PaginatedList<Character>> =
        withContext(dispatchers.io) { api.fetchCharactersForVn(vnId) }

    override suspend fun fetchQuotesForVn(vnId: String, maxResults: Int): AppResult<List<Quote>> =
        withContext(dispatchers.io) { api.fetchQuotesForVn(vnId, maxResults) }

    override suspend fun fetchReviewsForVn(vnId: String): AppResult<PaginatedList<Review>> =
        withContext(dispatchers.io) { reviewSource.fetchReviewsForVn(vnId) }

    override suspend fun fetchFullReviewText(reviewId: String): AppResult<String> =
        withContext(dispatchers.io) { reviewSource.fetchFullReviewText(reviewId) }

    override suspend fun fetchRandomVn(extraFilterClauses: List<KanaFilter>): AppResult<VnSummary> =
        withContext(dispatchers.io) {
            api.fetchRandomVn(extraFilterClauses).onSuccess { summary ->
                val nowSec = clock.nowEpochSeconds()
                database.withTransaction {
                    upsertSummariesPreservingExisting(listOf(summary), nowSec)
                    enforceLruCap(nowSec)
                }
            }
        }

    override suspend fun searchTags(query: String, maxResults: Int): AppResult<List<Tag>> =
        withContext(dispatchers.io) { api.searchTags(query, maxResults) }

    override suspend fun searchProducers(query: String, maxResults: Int): AppResult<List<Producer>> =
        withContext(dispatchers.io) { api.searchProducers(query, maxResults) }

    override suspend fun searchTraits(query: String, maxResults: Int): AppResult<List<Trait>> =
        withContext(dispatchers.io) { api.searchTraits(query, maxResults) }

    override suspend fun clearTransientCaches() =
        withContext(dispatchers.io) { database.clearTransientCaches() }

    private suspend fun upsertSummariesPreservingExisting(
        summaries: List<VnSummary>,
        nowEpochSeconds: Long
    ) {
        if (summaries.isEmpty()) return
        val existingById = vnCacheDao.getByIds(summaries.map { it.id }).associateBy { it.vnId }
        val entities = summaries.map { summary ->
            VnCacheEntity.fromSummary(
                summary = summary,
                nowEpochSeconds = nowEpochSeconds,
                existing = existingById[summary.id]
            )
        }
        vnCacheDao.upsertAll(entities)
    }

    private suspend fun enforceLruCap(nowEpochSeconds: Long) {
        val minFetchedAt = (nowEpochSeconds - MAX_CACHE_RETENTION_SECONDS).coerceAtLeast(0L)
        vnCacheDao.pruneNonCollectionCache(
            minFetchedAtEpochSeconds = minFetchedAt,
            maxNonCollectionEntries = maxNonCollectionCacheEntries
        )
    }

    companion object {
        private const val MAX_CACHE_RETENTION_SECONDS: Long = 7L * 24L * 3600L

        fun ttlSecondsForSection(section: HomeSection): Long = when (section) {
            HomeSection.LATEST_RELEASES,
            HomeSection.UPCOMING_RELEASES -> HomeSectionCacheDao.LATEST_AND_UPCOMING_TTL_SECONDS
            HomeSection.TOP_RATED,
            HomeSection.TOP_VOTED,
            HomeSection.POPULAR_ONGOING,
            HomeSection.COLLECTION -> HomeSectionCacheDao.STANDARD_SECTION_TTL_SECONDS
        }

        fun isSectionStale(section: HomeSection, fetchedAtEpochSeconds: Long, nowEpochSeconds: Long): Boolean {
            if (fetchedAtEpochSeconds <= 0L) return true
            return (nowEpochSeconds - fetchedAtEpochSeconds) >= ttlSecondsForSection(section)
        }

        fun buildHomeSectionQuerySpec(section: HomeSection, maxItems: Int): VnQuerySpec =
            VnQuerySpec(
                filters = VnSearchFilters(
                    devStatuses = listOfNotNull(section.devStatusFilter),
                    releaseWindow = section.releaseWindow
                ),
                sort = SortOption(field = section.sortField, reverse = section.reverseSort),
                page = 1,
                maxResults = maxItems.coerceIn(1, 50),
                includeDescription = false
            )
    }
}
