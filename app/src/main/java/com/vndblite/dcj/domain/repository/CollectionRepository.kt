package com.vndblite.dcj.domain.repository

import com.vndblite.dcj.domain.model.CollectionEntry
import com.vndblite.dcj.domain.model.CollectionFilters
import com.vndblite.dcj.domain.model.CollectionItemWithVn
import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.domain.model.SortOption
import com.vndblite.dcj.domain.model.VnSummary
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class CollectionSaveDraft(
    val vn: VnSummary,
    val status: CollectionStatus,
    val vote: Int? = null,
    val startedDate: LocalDate? = null,
    val finishedDate: LocalDate? = null,
    val notes: String? = null
)

data class CollectionBatchUpdateSpec(
    val vnIds: List<String>,
    val newStatus: CollectionStatus? = null,
    val updateVote: Boolean = false,
    val newVote: Int? = null
)

interface CollectionRepository {
    fun observeFilteredCollection(
        filters: CollectionFilters = CollectionFilters(),
        sort: SortOption = SortOption.DEFAULT_LOCAL
    ): Flow<List<CollectionItemWithVn>>

    fun observeVisibleCollectionByLastModifiedDesc(): Flow<List<CollectionItemWithVn>>

    fun observeRecentCollectionEntries(limit: Int = 20): Flow<List<CollectionItemWithVn>>

    fun observeEntryById(vnId: String): Flow<CollectionEntry?>

    fun observeVisibleCollectionIds(): Flow<Set<String>>

    fun observeVisibleCount(): Flow<Int>

    suspend fun getVisibleEntryById(vnId: String): CollectionEntry?

    suspend fun saveEntry(draft: CollectionSaveDraft): CollectionEntry

    suspend fun batchUpdateEntries(spec: CollectionBatchUpdateSpec): Int

    suspend fun removeEntry(vnId: String)

    suspend fun batchRemoveEntries(vnIds: List<String>)

    suspend fun clearAllEntries(queueRemoteDeletesIfSynced: Boolean)

    fun observeAllWithVn(): Flow<List<CollectionItemWithVn>> =
        observeVisibleCollectionByLastModifiedDesc()

    fun observeFilteredWithVn(
        filters: CollectionFilters = CollectionFilters(),
        sort: SortOption = SortOption.DEFAULT_LOCAL
    ): Flow<List<CollectionItemWithVn>> = observeFilteredCollection(filters, sort)

    fun observeEntry(vnId: String): Flow<CollectionEntry?> = observeEntryById(vnId)

    fun observeStatusCounts(): Flow<Map<CollectionStatus, Int>> =
        observeVisibleCollectionByLastModifiedDesc().map { list ->
            list.groupingBy { it.entry.status }.eachCount()
        }

    suspend fun upsertEntry(
        vnSummary: VnSummary,
        status: CollectionStatus,
        vote: Int?,
        notes: String?,
        startedDate: LocalDate?,
        finishedDate: LocalDate?
    ): CollectionEntry = saveEntry(
        CollectionSaveDraft(
            vn = vnSummary,
            status = status,
            vote = vote,
            startedDate = startedDate,
            finishedDate = finishedDate,
            notes = notes
        )
    )

    suspend fun batchUpdate(
        vnIds: Set<String>,
        newStatus: CollectionStatus?,
        newVote: Int?,
        updateVote: Boolean
    ): Int = batchUpdateEntries(
        CollectionBatchUpdateSpec(
            vnIds = vnIds.toList(),
            newStatus = newStatus,
            updateVote = updateVote,
            newVote = newVote
        )
    )

    suspend fun removeEntries(vnIds: Set<String>) = batchRemoveEntries(vnIds.toList())

    suspend fun clearAllLocalEntries(queueForRemoteDelete: Boolean) =
        clearAllEntries(queueRemoteDeletesIfSynced = queueForRemoteDelete)
}
