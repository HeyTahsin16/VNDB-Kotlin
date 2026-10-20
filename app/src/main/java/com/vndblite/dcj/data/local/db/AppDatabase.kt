package com.vndblite.dcj.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import com.vndblite.dcj.data.local.db.dao.CollectionDao
import com.vndblite.dcj.data.local.db.dao.HomeSectionCacheDao
import com.vndblite.dcj.data.local.db.dao.PendingRemoteDeletionDao
import com.vndblite.dcj.data.local.db.dao.QuarantineDao
import com.vndblite.dcj.data.local.db.dao.QuoteCacheDao
import com.vndblite.dcj.data.local.db.dao.SiteStatsCacheDao
import com.vndblite.dcj.data.local.db.dao.SyncMetadataDao
import com.vndblite.dcj.data.local.db.dao.VnCacheDao
import com.vndblite.dcj.data.local.db.entity.CollectionEntryEntity
import com.vndblite.dcj.data.local.db.entity.HomeSectionCacheEntity
import com.vndblite.dcj.data.local.db.entity.PendingRemoteDeletionEntity
import com.vndblite.dcj.data.local.db.entity.QuarantinedRecordEntity
import com.vndblite.dcj.data.local.db.entity.QuoteCacheEntity
import com.vndblite.dcj.data.local.db.entity.SiteStatsCacheEntity
import com.vndblite.dcj.data.local.db.entity.SyncMetadataEntity
import com.vndblite.dcj.data.local.db.entity.VnCacheEntity

/**
 * Room database for VNDB Kotlin storing the user collection, VN metadata cache,
 * home section previews, quotes, site statistics, account-scoped sync queues, and
 * quarantined legacy records (Section 2, S1-S6, Y1-Y6).
 */
@Database(
    entities = [
        CollectionEntryEntity::class,
        VnCacheEntity::class,
        HomeSectionCacheEntity::class,
        QuoteCacheEntity::class,
        SiteStatsCacheEntity::class,
        PendingRemoteDeletionEntity::class,
        SyncMetadataEntity::class,
        QuarantinedRecordEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun collectionDao(): CollectionDao
    abstract fun vnCacheDao(): VnCacheDao
    abstract fun homeSectionCacheDao(): HomeSectionCacheDao
    abstract fun quoteCacheDao(): QuoteCacheDao
    abstract fun siteStatsCacheDao(): SiteStatsCacheDao
    abstract fun pendingRemoteDeletionDao(): PendingRemoteDeletionDao
    abstract fun syncMetadataDao(): SyncMetadataDao
    abstract fun quarantineDao(): QuarantineDao

    /**
     * Clears transient caches (`home_section_cache`, `quote_cache`, and non-collection `vn_cache` rows)
     * in a single transaction while keeping collection entries and their cached VN summaries intact (S4).
     */
    suspend fun clearTransientCaches() {
        withTransaction {
            homeSectionCacheDao().clearAll()
            quoteCacheDao().clearAll()
            vnCacheDao().deleteAllNonCollectionEntries()
        }
    }

    /**
     * Clears sync queues and metadata for [accountId] on logout, optionally clearing or detaching
     * the local collection in one atomic transaction (Y6).
     */
    suspend fun clearAccountSyncData(accountId: String, clearLocalCollection: Boolean) {
        withTransaction {
            pendingRemoteDeletionDao().clearForAccount(accountId)
            syncMetadataDao().deleteForAccount(accountId)
            if (clearLocalCollection) {
                collectionDao().deleteAllEntriesHard()
            } else {
                collectionDao().deleteAllPendingDeleteEntries()
                collectionDao().detachSyncStateFromAllVisibleEntries()
            }
        }
    }

    companion object {
        const val DATABASE_NAME: String = "vndb_kotlin.db"

        fun buildPersistent(context: Context): AppDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            ).build()

        fun buildInMemory(context: Context): AppDatabase =
            Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                AppDatabase::class.java
            ).allowMainThreadQueries().build()
    }
}
