package com.vndblite.dcj.domain.repository

import com.vndblite.dcj.core.AppResult
import com.vndblite.dcj.core.map
import com.vndblite.dcj.data.remote.VnFullDetailPayload
import com.vndblite.dcj.data.remote.VnQuerySpec
import com.vndblite.dcj.data.remote.filter.KanaFilter
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
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class HomeSectionSnapshot(
    val section: HomeSection,
    val items: List<VnSummary>,
    val fetchedAt: Instant?,
    val isStale: Boolean
)

data class VnDetailBundle(
    val summary: VnSummary,
    val details: VnDetails?,
    val isSummaryStale: Boolean,
    val isDetailsStale: Boolean
)

interface VnRepository {
    fun observeHomeSection(section: HomeSection, maxItems: Int = 20): Flow<HomeSectionSnapshot>

    suspend fun refreshHomeSection(
        section: HomeSection,
        maxItems: Int = 20,
        force: Boolean = false
    ): AppResult<List<VnSummary>>

    suspend fun refreshAllRemoteHomeSections(
        maxItems: Int = 20,
        force: Boolean = false
    ): Map<HomeSection, AppResult<List<VnSummary>>>

    fun observeCachedQuotes(): Flow<List<Quote>>

    fun observeQuotes(): Flow<List<Quote>> = observeCachedQuotes()

    suspend fun refreshQuotes(maxResults: Int = 10): AppResult<List<Quote>>

    suspend fun fetchRandomQuote(): AppResult<Quote?>

    fun observeSiteStats(): Flow<SiteStats?>

    suspend fun refreshSiteStats(): AppResult<SiteStats>

    fun observeVnDetail(vnId: String): Flow<VnDetailBundle?>

    fun observeVnSummary(vnId: String): Flow<VnSummary?> =
        observeVnDetail(vnId).map { it?.summary }

    fun observeVnDetails(vnId: String): Flow<VnDetails?> =
        observeVnDetail(vnId).map { it?.details }

    suspend fun getVnDetail(
        vnId: String,
        forceRefresh: Boolean = false
    ): AppResult<VnFullDetailPayload>

    suspend fun loadVnDetail(
        vnId: String,
        force: Boolean = false
    ): AppResult<Pair<VnSummary, VnDetails>> =
        getVnDetail(vnId, forceRefresh = force).map { it.summary to it.details }

    suspend fun queryVns(spec: VnQuerySpec): AppResult<PaginatedList<VnSummary>>

    suspend fun searchVn(
        filters: VnSearchFilters,
        sort: SortOption = SortOption.DEFAULT_REMOTE,
        page: Int = 1,
        pageSize: Int = 24
    ): AppResult<PaginatedList<VnSummary>> =
        queryVns(
            VnQuerySpec(
                filters = filters,
                sort = sort,
                page = page,
                maxResults = pageSize
            )
        )

    suspend fun searchCharacters(
        filters: CharacterSearchFilters,
        page: Int = 1,
        maxResults: Int = 24
    ): AppResult<PaginatedList<Character>>

    suspend fun fetchCharactersForVn(vnId: String): AppResult<PaginatedList<Character>>

    suspend fun fetchVnCharacters(vnId: String, pageSize: Int = 50): AppResult<List<Character>> =
        fetchCharactersForVn(vnId).map { it.items.take(pageSize) }

    suspend fun fetchQuotesForVn(vnId: String, maxResults: Int = 30): AppResult<List<Quote>>

    suspend fun fetchVnQuotes(vnId: String, count: Int = 8): AppResult<List<Quote>> =
        fetchQuotesForVn(vnId, maxResults = count)

    suspend fun fetchReviewsForVn(vnId: String): AppResult<PaginatedList<Review>>

    suspend fun fetchVnReviews(vnId: String): AppResult<List<Review>> =
        fetchReviewsForVn(vnId).map { it.items }

    suspend fun fetchFullReviewText(reviewId: String): AppResult<String>

    suspend fun fetchRandomVn(
        extraFilterClauses: List<KanaFilter> = emptyList()
    ): AppResult<VnSummary>

    suspend fun fetchRandomHighRatedVn(): AppResult<VnSummary> = fetchRandomVn()

    suspend fun searchTags(query: String, maxResults: Int = 10): AppResult<List<Tag>>

    suspend fun searchProducers(query: String, maxResults: Int = 10): AppResult<List<Producer>>

    suspend fun searchTraits(query: String, maxResults: Int = 15): AppResult<List<Trait>>

    suspend fun clearTransientCaches()

    suspend fun clearTransientCache() = clearTransientCaches()
}
