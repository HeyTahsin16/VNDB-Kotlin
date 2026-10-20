package com.vndblite.dcj.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.vndblite.dcj.data.local.db.entity.VnCacheEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for `vn_cache` with TTL expiration and LRU cap eviction that never evicts
 * collection entries (S1, S3, S4).
 */
@Dao
interface VnCacheDao {

    @Query("SELECT * FROM vn_cache WHERE vnId = :vnId LIMIT 1")
    fun observeById(vnId: String): Flow<VnCacheEntity?>

    @Query("SELECT * FROM vn_cache WHERE vnId = :vnId LIMIT 1")
    suspend fun getById(vnId: String): VnCacheEntity?

    @Query("SELECT * FROM vn_cache WHERE vnId IN (:vnIds)")
    suspend fun getByIds(vnIds: List<String>): List<VnCacheEntity>

    @Query("SELECT * FROM vn_cache WHERE vnId IN (:vnIds)")
    fun observeByIds(vnIds: List<String>): Flow<List<VnCacheEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: VnCacheEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<VnCacheEntity>)

    @Query("UPDATE vn_cache SET lastAccessedAtEpochSeconds = :accessedAtEpochSeconds WHERE vnId = :vnId")
    suspend fun touchLastAccessed(vnId: String, accessedAtEpochSeconds: Long)

    @Query("UPDATE vn_cache SET lastAccessedAtEpochSeconds = :accessedAtEpochSeconds WHERE vnId IN (:vnIds)")
    suspend fun touchLastAccessedBatch(vnIds: List<String>, accessedAtEpochSeconds: Long)

    @Query("SELECT COUNT(*) FROM vn_cache")
    suspend fun countAll(): Int

    @Query(
        """
        SELECT COUNT(*) FROM vn_cache
        WHERE vnId NOT IN (SELECT vnId FROM collection_entries WHERE syncState != 'PENDING_DELETE')
        """
    )
    suspend fun countNonCollectionEntries(): Int

    /**
     * Evicts non-collection VNs whose summary cache is older than [minFetchedAtEpochSeconds] (S4).
     * Never evicts any VN present in `collection_entries`.
     */
    @Query(
        """
        DELETE FROM vn_cache
        WHERE summaryFetchedAtEpochSeconds < :minFetchedAtEpochSeconds
          AND vnId NOT IN (SELECT vnId FROM collection_entries)
        """
    )
    suspend fun evictExpiredNonCollectionEntries(minFetchedAtEpochSeconds: Long): Int

    /**
     * Enforces an LRU cap ([maxNonCollectionEntries], default 500) on non-collection VNs
     * ordered by `lastAccessedAtEpochSeconds DESC` (S4).
     * Never evicts any VN present in `collection_entries`.
     */
    @Query(
        """
        DELETE FROM vn_cache
        WHERE vnId NOT IN (SELECT vnId FROM collection_entries)
          AND vnId NOT IN (
              SELECT vnId FROM vn_cache
              WHERE vnId NOT IN (SELECT vnId FROM collection_entries)
              ORDER BY lastAccessedAtEpochSeconds DESC, vnId ASC
              LIMIT :maxNonCollectionEntries
          )
        """
    )
    suspend fun evictOverflowNonCollectionLru(
        maxNonCollectionEntries: Int = DEFAULT_MAX_NON_COLLECTION_ENTRIES
    ): Int

    /**
     * Runs both TTL expiration and LRU cap eviction in a single transaction (S4).
     */
    @Transaction
    suspend fun pruneNonCollectionCache(
        minFetchedAtEpochSeconds: Long,
        maxNonCollectionEntries: Int = DEFAULT_MAX_NON_COLLECTION_ENTRIES
    ): Int {
        val expiredRemoved = evictExpiredNonCollectionEntries(minFetchedAtEpochSeconds)
        val overflowRemoved = evictOverflowNonCollectionLru(maxNonCollectionEntries)
        return expiredRemoved + overflowRemoved
    }

    /**
     * Deletes all non-collection VN cache entries for the Settings "Clear cache" action
     * while preserving collection VNs so offline collection viewing continues to work (S4, Section 7).
     */
    @Query("DELETE FROM vn_cache WHERE vnId NOT IN (SELECT vnId FROM collection_entries)")
    suspend fun deleteAllNonCollectionEntries(): Int

    companion object {
        const val DEFAULT_MAX_NON_COLLECTION_ENTRIES: Int = 500
        const val DEFAULT_SUMMARY_TTL_SECONDS: Long = 12L * 3600L
        const val DEFAULT_DETAILS_TTL_SECONDS: Long = 24L * 3600L
    }
}
