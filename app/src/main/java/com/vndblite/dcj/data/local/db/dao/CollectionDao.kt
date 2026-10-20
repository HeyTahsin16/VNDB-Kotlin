package com.vndblite.dcj.data.local.db.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Relation
import androidx.room.Transaction
import androidx.sqlite.db.SupportSQLiteQuery
import com.vndblite.dcj.data.local.db.entity.CollectionEntryEntity
import com.vndblite.dcj.data.local.db.entity.VnCacheEntity
import com.vndblite.dcj.domain.model.CollectionEntry
import com.vndblite.dcj.domain.model.CollectionItemWithVn
import com.vndblite.dcj.domain.model.VnSummary
import kotlinx.coroutines.flow.Flow

/**
 * Joined Room projection pairing a [CollectionEntryEntity] with its cached [VnCacheEntity] row (S1, S3).
 */
data class CollectionWithVnRow(
    @Embedded
    val entry: CollectionEntryEntity,
    @Relation(
        parentColumn = "vnId",
        entityColumn = "vnId"
    )
    val vn: VnCacheEntity?
) {
    fun toDomain(): CollectionItemWithVn {
        val domainEntry: CollectionEntry = entry.toDomain(fallbackTitle = vn?.title.orEmpty())
        val domainVn: VnSummary = vn?.toSummary() ?: VnSummary(
            id = entry.vnId,
            title = domainEntry.title
        )
        return CollectionItemWithVn(
            entry = domainEntry,
            vn = domainVn,
            details = vn?.toDetailsOrNull()
        )
    }
}

/**
 * Room DAO for per-row and transactional batch operations on `collection_entries` (S1, S2, S3, F3, Y1-Y6).
 */
@Dao
interface CollectionDao {

    @Transaction
    @RawQuery(observedEntities = [CollectionEntryEntity::class, VnCacheEntity::class])
    fun observeCollectionByQuery(query: SupportSQLiteQuery): Flow<List<CollectionWithVnRow>>

    @Transaction
    @RawQuery(observedEntities = [CollectionEntryEntity::class, VnCacheEntity::class])
    suspend fun getCollectionByQuery(query: SupportSQLiteQuery): List<CollectionWithVnRow>

    /**
     * Reactive stream of visible collection entries ordered deterministically by `lastModifiedEpochSeconds DESC`
     * for the Home "My Collection" carousel (F3, S3).
     */
    @Transaction
    @Query(
        """
        SELECT * FROM collection_entries
        WHERE syncState != 'PENDING_DELETE'
        ORDER BY lastModifiedEpochSeconds DESC, vnId ASC
        """
    )
    fun observeVisibleCollectionByLastModifiedDesc(): Flow<List<CollectionWithVnRow>>

    @Transaction
    @Query(
        """
        SELECT * FROM collection_entries
        WHERE syncState != 'PENDING_DELETE'
        ORDER BY lastModifiedEpochSeconds DESC, vnId ASC
        LIMIT :limit
        """
    )
    fun observeRecentCollectionEntries(limit: Int): Flow<List<CollectionWithVnRow>>

    @Query("SELECT * FROM collection_entries WHERE vnId = :vnId AND syncState != 'PENDING_DELETE' LIMIT 1")
    fun observeEntryById(vnId: String): Flow<CollectionEntryEntity?>

    @Query("SELECT * FROM collection_entries WHERE vnId = :vnId LIMIT 1")
    suspend fun getEntryByIdIncludingPendingDelete(vnId: String): CollectionEntryEntity?

    @Query("SELECT * FROM collection_entries WHERE vnId = :vnId AND syncState != 'PENDING_DELETE' LIMIT 1")
    suspend fun getVisibleEntryById(vnId: String): CollectionEntryEntity?

    @Query("SELECT * FROM collection_entries WHERE syncState != 'PENDING_DELETE' ORDER BY lastModifiedEpochSeconds DESC, vnId ASC")
    suspend fun getAllVisibleEntries(): List<CollectionEntryEntity>

    @Query("SELECT * FROM collection_entries ORDER BY vnId ASC")
    suspend fun getAllEntriesIncludingPendingDelete(): List<CollectionEntryEntity>

    @Query("SELECT vnId FROM collection_entries WHERE syncState != 'PENDING_DELETE'")
    fun observeVisibleCollectionIds(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM collection_entries WHERE syncState != 'PENDING_DELETE'")
    fun observeVisibleCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntry(entry: CollectionEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntries(entries: List<CollectionEntryEntity>)

    /**
     * Batch-updates status and/or timestamps for multiple VN IDs in a single SQLite statement (S2).
     */
    @Query(
        """
        UPDATE collection_entries
        SET statusId = :newStatusId,
            lastModifiedEpochSeconds = :modifiedEpochSeconds,
            syncState = CASE WHEN syncState = 'NEW' THEN 'NEW' ELSE 'DIRTY' END
        WHERE vnId IN (:vnIds) AND syncState != 'PENDING_DELETE'
        """
    )
    suspend fun batchUpdateStatus(
        vnIds: List<String>,
        newStatusId: Int,
        modifiedEpochSeconds: Long
    ): Int

    /**
     * Marks the given VN IDs as `PENDING_DELETE` in one statement so they are hidden in UI
     * and queued for `DELETE /ulist/<id>` on the next sync (S2, Y1, Y4).
     */
    @Query(
        """
        UPDATE collection_entries
        SET syncState = 'PENDING_DELETE',
            lastModifiedEpochSeconds = :modifiedEpochSeconds
        WHERE vnId IN (:vnIds)
        """
    )
    suspend fun markEntriesPendingDelete(
        vnIds: List<String>,
        modifiedEpochSeconds: Long
    ): Int

    @Query("DELETE FROM collection_entries WHERE vnId = :vnId")
    suspend fun deleteEntryHard(vnId: String): Int

    @Query("DELETE FROM collection_entries WHERE vnId IN (:vnIds)")
    suspend fun deleteEntriesHard(vnIds: List<String>): Int

    @Query("DELETE FROM collection_entries WHERE syncState = 'PENDING_DELETE'")
    suspend fun deleteAllPendingDeleteEntries(): Int

    @Query("DELETE FROM collection_entries")
    suspend fun deleteAllEntriesHard()

    /**
     * When a user logs out and chooses to keep their local collection, resets sync state to `NEW`
     * and clears `remoteLastModEpochSeconds` and `accountId` so another account won't inherit stale sync state (Y6).
     */
    @Query(
        """
        UPDATE collection_entries
        SET syncState = 'NEW',
            remoteLastModEpochSeconds = NULL,
            accountId = NULL,
            lastSyncedStatusId = NULL,
            lastSyncedVote = NULL,
            lastSyncedStartedDate = NULL,
            lastSyncedFinishedDate = NULL,
            lastSyncedNotes = NULL
        WHERE syncState != 'PENDING_DELETE'
        """
    )
    suspend fun detachSyncStateFromAllVisibleEntries()
}
