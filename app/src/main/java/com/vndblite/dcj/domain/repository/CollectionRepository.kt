package com.vndblite.dcj.domain.repository

import com.vndblite.dcj.domain.model.CollectionEntry
import com.vndblite.dcj.domain.model.CollectionFilters
import com.vndblite.dcj.domain.model.CollectionItemWithVn
import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.domain.model.SortOption
import com.vndblite.dcj.domain.model.VnSummary
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for local and cloud-synced user collection entries (Section 5, A3, S1-S3, F3, F8, F9, Y1-Y6).
 */
interface CollectionRepository {
    fun observeRecentCollection(limit: Int): Flow<List<CollectionItemWithVn>>

    fun observeAllVisibleEntriesMap(): Flow<Map<String, CollectionEntry>>

    fun observeEntryById(vnId: String): Flow<CollectionEntry?>

    fun observeFilteredCollection(
        filters: CollectionFilters,
        sort: SortOption
    ): Flow<List<CollectionItemWithVn>>

    fun observeVisibleCount(): Flow<Int>

    suspend fun saveOrUpdateEntry(
        vn: VnSummary,
        status: CollectionStatus,
        vote: Int?,
        notes: String?,
        startedDate: LocalDate?,
        finishedDate: LocalDate?
    )

    suspend fun removeEntry(vnId: String)

    suspend fun batchUpdateEntries(
        vnIds: List<String>,
        status: CollectionStatus,
        overrideVote: Int?
    )

    suspend fun removeEntries(vnIds: List<String>)

    suspend fun clearLocalCollection(queueForRemoteDelete: Boolean)

    suspend fun hydrateMissingSummaries(vnIds: List<String>)
}
