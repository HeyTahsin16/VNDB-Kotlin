package com.vndblite.dcj.data.remote

import com.vndblite.dcj.core.AppResult
import com.vndblite.dcj.data.remote.filter.KanaFilter
import com.vndblite.dcj.domain.model.Character
import com.vndblite.dcj.domain.model.CharacterSearchFilters
import com.vndblite.dcj.domain.model.PaginatedList
import com.vndblite.dcj.domain.model.Producer
import com.vndblite.dcj.domain.model.Quote
import com.vndblite.dcj.domain.model.RemoteUserListItem
import com.vndblite.dcj.domain.model.SiteStats
import com.vndblite.dcj.domain.model.SortOption
import com.vndblite.dcj.domain.model.Tag
import com.vndblite.dcj.domain.model.Trait
import com.vndblite.dcj.domain.model.UserAccount
import com.vndblite.dcj.domain.model.UserListPatch
import com.vndblite.dcj.domain.model.VnDetails
import com.vndblite.dcj.domain.model.VnSearchFilters
import com.vndblite.dcj.domain.model.VnSummary

data class VnFullDetailPayload(
    val summary: VnSummary,
    val details: VnDetails
)

data class VnQuerySpec(
    val filters: VnSearchFilters = VnSearchFilters(),
    val sort: SortOption = SortOption.DEFAULT_REMOTE,
    val page: Int = 1,
    val maxResults: Int = 24,
    val includeDescription: Boolean = false
)

/**
 * Typed interface for the VNDB Kana v2 HTTP API (`https://api.vndb.org/kana`) (A3, N1-N10, F1, F6).
 */
interface VndbApi {
    suspend fun queryVns(spec: VnQuerySpec): AppResult<PaginatedList<VnSummary>>

    suspend fun fetchVnSummariesByIds(
        ids: List<String>,
        includeDescription: Boolean = false
    ): AppResult<List<VnSummary>>

    suspend fun fetchVnFullDetails(vnId: String): AppResult<VnFullDetailPayload>

    suspend fun fetchRandomVn(
        extraFilterClauses: List<KanaFilter> = emptyList()
    ): AppResult<VnSummary>

    suspend fun fetchCharactersForVn(vnId: String): AppResult<PaginatedList<Character>>

    suspend fun searchCharacters(
        filters: CharacterSearchFilters,
        page: Int = 1,
        maxResults: Int = 24
    ): AppResult<PaginatedList<Character>>

    suspend fun fetchTopQuotes(maxResults: Int = 10): AppResult<List<Quote>>

    suspend fun fetchRandomQuote(): AppResult<Quote?>

    suspend fun fetchQuotesForVn(vnId: String, maxResults: Int = 30): AppResult<List<Quote>>

    suspend fun searchTags(query: String, maxResults: Int = 10): AppResult<List<Tag>>

    suspend fun searchProducers(query: String, maxResults: Int = 10): AppResult<List<Producer>>

    suspend fun searchTraits(query: String, maxResults: Int = 15): AppResult<List<Trait>>

    suspend fun fetchStats(): AppResult<SiteStats>

    suspend fun authenticate(token: String): AppResult<UserAccount>

    suspend fun fetchUserList(
        userId: String,
        token: String
    ): AppResult<List<RemoteUserListItem>>

    suspend fun patchUserListEntry(
        token: String,
        patch: UserListPatch
    ): AppResult<Unit>

    suspend fun deleteUserListEntry(
        token: String,
        vnId: String
    ): AppResult<Unit>
}
