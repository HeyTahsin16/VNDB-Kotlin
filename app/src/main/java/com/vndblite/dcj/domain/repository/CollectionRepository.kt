package com.vndblite.dcj.domain.repository

import com.vndblite.dcj.domain.model.CollectionEntry
import com.vndblite.dcj.domain.model.CollectionFilters
import com.vndblite.dcj.domain.model.CollectionItemWithVn
import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.domain.model.SortOption
import com.vndblite.dcj.domain.model.VnSummary
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

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
}
