package com.vndblite.dcj.domain.repository

import com.vndblite.dcj.core.AppResult
import com.vndblite.dcj.data.remote.VnFullDetailPayload
import com.vndblite.dcj.data.remote.VnQuerySpec
import com.vndblite.dcj.domain.model.Character
import com.vndblite.dcj.domain.model.CharacterSearchFilters
import com.vndblite.dcj.domain.model.HomeSection
import com.vndblite.dcj.domain.model.PaginatedList
import com.vndblite.dcj.domain.model.Producer
import com.vndblite.dcj.domain.model.Quote
import com.vndblite.dcj.domain.model.Review
import com.vndblite.dcj.domain.model.SiteStats
import com.vndblite.dcj.domain.model.Tag
import com.vndblite.dcj.domain.model.Trait
import com.vndblite.dcj.domain.model.VnDetails
import com.vndblite.dcj.domain.model.VnSummary
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for remote and cached visual novel metadata, characters, quotes,
 * site statistics, and community reviews (Section 5, A3, S4, F1, F2, F5, F6).
 */
interface VnRepository {
    fun observeCachedHomeSection(section: HomeSection): Flow<List<VnSummary>>

    suspend fun refreshHomeSection(
        section: HomeSection,
        maxItems: Int,
        forceRefresh: Boolean = false
    ): AppResult<List<VnSummary>>

    fun observeCachedQuotes(): Flow<List<Quote>>

    suspend fun refreshHomeQuotes(
        maxResults: Int = 10,
        forceRefresh: Boolean = false
    ): AppResult<List<Quote>>

    fun observeCachedSiteStats(): Flow<SiteStats?>

    suspend fun refreshSiteStats(forceRefresh: Boolean = false): AppResult<SiteStats>

    suspend fun searchVns(spec: VnQuerySpec): AppResult<PaginatedList<VnSummary>>

    suspend fun searchCharacters(
        filters: CharacterSearchFilters,
        page: Int = 1,
        maxResults: Int = 24
    ): AppResult<PaginatedList<Character>>

    suspend fun searchTags(query: String): AppResult<List<Tag>>

    suspend fun searchProducers(query: String): AppResult<List<Producer>>

    suspend fun searchTraits(query: String): AppResult<List<Trait>>

    suspend fun fetchRandomVn(): AppResult<VnSummary>

    fun observeVnSummaryAndDetails(vnId: String): Flow<Pair<VnSummary?, VnDetails?>>

    suspend fun getCachedVnSummary(vnId: String): VnSummary?

    suspend fun cacheVnSummary(summary: VnSummary)

    suspend fun loadVnFullDetails(
        vnId: String,
        forceRefresh: Boolean = false
    ): AppResult<VnFullDetailPayload>

    suspend fun fetchRelatedVnSummaries(vnIds: List<String>): AppResult<List<VnSummary>>

    suspend fun fetchCharactersForVn(vnId: String): AppResult<PaginatedList<Character>>

    suspend fun fetchQuotesForVn(vnId: String): AppResult<List<Quote>>

    suspend fun fetchReviewsForVn(vnId: String): AppResult<PaginatedList<Review>>

    suspend fun fetchFullReviewText(reviewId: String): AppResult<String>

    suspend fun clearTransientCaches()
}
