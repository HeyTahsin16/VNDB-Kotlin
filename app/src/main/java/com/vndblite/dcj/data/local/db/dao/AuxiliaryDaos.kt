package com.vndblite.dcj.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.vndblite.dcj.data.local.db.entity.HomeSectionCacheEntity
import com.vndblite.dcj.data.local.db.entity.PendingRemoteDeletionEntity
import com.vndblite.dcj.data.local.db.entity.QuarantinedRecordEntity
import com.vndblite.dcj.data.local.db.entity.QuoteCacheEntity
import com.vndblite.dcj.data.local.db.entity.SiteStatsCacheEntity
import com.vndblite.dcj.data.local.db.entity.SyncMetadataEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for `home_section_cache` supporting per-section TTL and stale-while-revalidate (S1, S4, F2).
 */
@Dao
interface HomeSectionCacheDao {

    @Query("SELECT * FROM home_section_cache WHERE sectionCode = :sectionCode LIMIT 1")
    fun observeSection(sectionCode: String): Flow<HomeSectionCacheEntity?>

    @Query("SELECT * FROM home_section_cache WHERE sectionCode = :sectionCode LIMIT 1")
    suspend fun getSection(sectionCode: String): HomeSectionCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSection(entity: HomeSectionCacheEntity)

    @Query("DELETE FROM home_section_cache WHERE sectionCode = :sectionCode")
    suspend fun deleteSection(sectionCode: String)

    @Query("DELETE FROM home_section_cache")
    suspend fun clearAll()

    companion object {
        const val LATEST_AND_UPCOMING_TTL_SECONDS: Long = 3600L
        const val STANDARD_SECTION_TTL_SECONDS: Long = 12L * 3600L
    }
}

/**
 * DAO for `quote_cache` (S1, S4).
 */
@Dao
interface QuoteCacheDao {

    @Query("SELECT * FROM quote_cache ORDER BY displayOrder ASC, quoteId ASC")
    fun observeAll(): Flow<List<QuoteCacheEntity>>

    @Query("SELECT * FROM quote_cache ORDER BY displayOrder ASC, quoteId ASC")
    suspend fun getAll(): List<QuoteCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(quotes: List<QuoteCacheEntity>)

    @Query("DELETE FROM quote_cache")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceAll(quotes: List<QuoteCacheEntity>) {
        clearAll()
        if (quotes.isNotEmpty()) {
            insertAll(quotes)
        }
    }
}

/**
 * DAO for `site_stats_cache` (S1, F6).
 */
@Dao
interface SiteStatsCacheDao {

    @Query("SELECT * FROM site_stats_cache WHERE singletonId = :id LIMIT 1")
    fun observeStats(id: Int = SiteStatsCacheEntity.SINGLETON_ID): Flow<SiteStatsCacheEntity?>

    @Query("SELECT * FROM site_stats_cache WHERE singletonId = :id LIMIT 1")
    suspend fun getStats(id: Int = SiteStatsCacheEntity.SINGLETON_ID): SiteStatsCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStats(entity: SiteStatsCacheEntity)

    @Query("DELETE FROM site_stats_cache")
    suspend fun clear()
}

/**
 * DAO for account-scoped `pending_remote_deletions` (Y4, Y6).
 */
@Dao
interface PendingRemoteDeletionDao {

    @Query("SELECT * FROM pending_remote_deletions WHERE accountId = :accountId ORDER BY queuedAtEpochSeconds ASC, vnId ASC")
    suspend fun getPendingForAccount(accountId: String): List<PendingRemoteDeletionEntity>

    @Query("SELECT vnId FROM pending_remote_deletions WHERE accountId = :accountId ORDER BY vnId ASC")
    fun observePendingIdsForAccount(accountId: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(deletion: PendingRemoteDeletionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueueAll(deletions: List<PendingRemoteDeletionEntity>)

    @Query("DELETE FROM pending_remote_deletions WHERE accountId = :accountId AND vnId = :vnId")
    suspend fun removeConfirmed(accountId: String, vnId: String): Int

    @Query("DELETE FROM pending_remote_deletions WHERE accountId = :accountId AND vnId IN (:vnIds)")
    suspend fun removeConfirmedBatch(accountId: String, vnIds: List<String>): Int

    @Query("DELETE FROM pending_remote_deletions WHERE accountId = :accountId")
    suspend fun clearForAccount(accountId: String): Int
}

/**
 * DAO for account-scoped `sync_metadata` (S1, Y1, Y6).
 */
@Dao
interface SyncMetadataDao {

    @Query("SELECT * FROM sync_metadata WHERE accountId = :accountId LIMIT 1")
    fun observeByAccountId(accountId: String): Flow<SyncMetadataEntity?>

    @Query("SELECT * FROM sync_metadata WHERE accountId = :accountId LIMIT 1")
    suspend fun getByAccountId(accountId: String): SyncMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(metadata: SyncMetadataEntity)

    @Query(
        """
        UPDATE sync_metadata
        SET isSynced = :isSynced,
            lastSyncedAtEpochSeconds = :lastSyncedAtEpochSeconds
        WHERE accountId = :accountId
        """
    )
    suspend fun updateSyncStatus(
        accountId: String,
        isSynced: Boolean,
        lastSyncedAtEpochSeconds: Long?
    )

    @Query("DELETE FROM sync_metadata WHERE accountId = :accountId")
    suspend fun deleteForAccount(accountId: String)
}

/**
 * DAO for `quarantined_records` captured during legacy SharedPreferences migration (S6).
 */
@Dao
interface QuarantineDao {

    @Query("SELECT * FROM quarantined_records ORDER BY id ASC")
    suspend fun getAll(): List<QuarantinedRecordEntity>

    @Query("SELECT * FROM quarantined_records WHERE sourceKey = :sourceKey ORDER BY id ASC")
    suspend fun getBySourceKey(sourceKey: String): List<QuarantinedRecordEntity>

    @Query("SELECT COUNT(*) FROM quarantined_records")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: QuarantinedRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<QuarantinedRecordEntity>)

    @Query("DELETE FROM quarantined_records")
    suspend fun clearAll()
}
