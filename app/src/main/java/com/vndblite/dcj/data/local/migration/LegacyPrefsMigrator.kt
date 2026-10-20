package com.vndblite.dcj.data.local.migration

import android.content.Context
import android.content.SharedPreferences
import androidx.room.withTransaction
import com.vndblite.dcj.core.AppLog
import com.vndblite.dcj.core.Clock
import com.vndblite.dcj.core.DefaultDispatcherProvider
import com.vndblite.dcj.core.DispatcherProvider
import com.vndblite.dcj.core.SystemClock
import com.vndblite.dcj.data.local.db.AppDatabase
import com.vndblite.dcj.data.local.db.entity.CollectionEntryEntity
import com.vndblite.dcj.data.local.db.entity.PendingRemoteDeletionEntity
import com.vndblite.dcj.data.local.db.entity.QuarantinedRecordEntity
import com.vndblite.dcj.data.local.db.entity.SyncMetadataEntity
import com.vndblite.dcj.data.local.db.entity.VnCacheEntity
import com.vndblite.dcj.data.local.prefs.SettingsStore
import com.vndblite.dcj.data.local.secure.TokenStore
import com.vndblite.dcj.domain.model.AppSettings
import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.domain.model.ThemeId
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class MigrationReport(
    val alreadyMigrated: Boolean = false,
    val migratedCollectionCount: Int = 0,
    val migratedVnCacheCount: Int = 0,
    val migratedPendingDeletionsCount: Int = 0,
    val migratedToken: Boolean = false,
    val quarantinedCount: Int = 0
)

/**
 * Idempotent, one-time migrator from legacy `VndbKotlinPreferences` (`SharedPreferences`)
 * into Room ([AppDatabase]), DataStore ([SettingsStore]), and [TokenStore] (S1, S5, S6).
 */
class LegacyPrefsMigrator(
    private val legacyPrefs: SharedPreferences,
    private val database: AppDatabase,
    private val settingsStore: SettingsStore,
    private val tokenStore: TokenStore,
    private val clock: Clock = SystemClock,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider
) {
    private val mutex = Mutex()

    suspend fun migrateIfNeeded(): MigrationReport = mutex.withLock {
        withContext(dispatchers.io) {
            val alreadyCompleted = settingsStore.isLegacyMigrationCompleted()
            val hasLingeringIdentity = legacyPrefs.contains(KEY_USER_IDENTITY)
            if (alreadyCompleted && !hasLingeringIdentity) {
                return@withContext MigrationReport(alreadyMigrated = true)
            }

            val allEntries = legacyPrefs.all
            if (allEntries.isEmpty()) {
                settingsStore.markLegacyMigrationCompleted()
                return@withContext MigrationReport(alreadyMigrated = alreadyCompleted)
            }

            val nowEpochSeconds = clock.nowEpochSeconds()
            val quarantined = mutableListOf<QuarantinedRecordEntity>()

            val (migratedIdentity, migratedToken) = migrateIdentityAndToken(
                nowEpochSeconds = nowEpochSeconds,
                quarantined = quarantined
            )
            val isUserSynced = legacyPrefs.getBoolean(KEY_USER_SYNCED, false)
            val accountScope = migratedIdentity?.id ?: LOCAL_UNSCOPED_ACCOUNT_ID

            val addedByAppIds = legacyPrefs.getStringSet(KEY_ADDED_BY_APP, emptySet()).orEmpty()
            val toBeRemovedIds = legacyPrefs.getStringSet(KEY_TO_BE_REMOVED, emptySet()).orEmpty()

            val recordContext = LegacyRecordContext(
                addedByAppIds = addedByAppIds,
                toBeRemovedIds = toBeRemovedIds,
                isUserSynced = isUserSynced,
                accountId = migratedIdentity?.id,
                nowEpochSeconds = nowEpochSeconds
            )
            val collectionEntities = LegacyPrefsExtractor.parseCollection(
                legacyPrefs = legacyPrefs,
                ctx = recordContext,
                quarantined = quarantined
            )
            val vnCacheEntities = LegacyPrefsExtractor.parseVnCache(
                legacyPrefs = legacyPrefs,
                allKeys = allEntries.keys,
                nowEpochSeconds = nowEpochSeconds,
                quarantined = quarantined
            )
            val homeCacheEntities = LegacyPrefsExtractor.parseHomeSections(
                legacyPrefs = legacyPrefs,
                allKeys = allEntries.keys,
                nowEpochSeconds = nowEpochSeconds,
                quarantined = quarantined
            )
            val quoteEntities = LegacyPrefsExtractor.parseQuotes(
                legacyPrefs = legacyPrefs,
                nowEpochSeconds = nowEpochSeconds,
                quarantined = quarantined
            )
            val statsEntity = LegacyPrefsExtractor.parseStats(
                legacyPrefs = legacyPrefs,
                nowEpochSeconds = nowEpochSeconds,
                quarantined = quarantined
            )
            val pendingDeletions = toBeRemovedIds
                .map { it.trim() }
                .filter { it.startsWith("v") }
                .map { vnId ->
                    PendingRemoteDeletionEntity(
                        accountId = accountScope,
                        vnId = vnId,
                        queuedAtEpochSeconds = nowEpochSeconds
                    )
                }

            migrateSettingsAndFilters(
                migratedAccountId = migratedIdentity?.id,
                nowEpochSeconds = nowEpochSeconds,
                quarantined = quarantined
            )

            database.withTransaction {
                if (migratedIdentity != null) {
                    database.syncMetadataDao().upsert(
                        SyncMetadataEntity(
                            accountId = migratedIdentity.id,
                            username = migratedIdentity.username,
                            permissionsPipe = VnCacheEntity.encodePipeList(migratedIdentity.permissions),
                            isSynced = isUserSynced,
                            lastSyncedAtEpochSeconds = if (isUserSynced) nowEpochSeconds else null
                        )
                    )
                }
                upsertCollectionIdempotently(collectionEntities)
                upsertVnCacheIdempotently(vnCacheEntities)
                homeCacheEntities.forEach { database.homeSectionCacheDao().upsertSection(it) }
                if (quoteEntities.isNotEmpty() && database.quoteCacheDao().getAll().isEmpty()) {
                    database.quoteCacheDao().replaceAll(quoteEntities)
                }
                if (statsEntity != null && database.siteStatsCacheDao().getStats() == null) {
                    database.siteStatsCacheDao().upsertStats(statsEntity)
                }
                if (pendingDeletions.isNotEmpty()) {
                    database.pendingRemoteDeletionDao().enqueueAll(pendingDeletions)
                }
                if (quarantined.isNotEmpty()) {
                    database.quarantineDao().insertAll(quarantined)
                }
            }

            if (hasLingeringIdentity) {
                legacyPrefs.edit().remove(KEY_USER_IDENTITY).commit()
            }
            settingsStore.markLegacyMigrationCompleted()

            AppLog.i(
                TAG,
                "Legacy migration completed: collection=${collectionEntities.size}, " +
                    "vnCache=${vnCacheEntities.size}, pendingDeletes=${pendingDeletions.size}, " +
                    "quarantined=${quarantined.size}"
            )

            MigrationReport(
                alreadyMigrated = false,
                migratedCollectionCount = collectionEntities.size,
                migratedVnCacheCount = vnCacheEntities.size,
                migratedPendingDeletionsCount = pendingDeletions.size,
                migratedToken = migratedToken,
                quarantinedCount = quarantined.size
            )
        }
    }

    private suspend fun migrateIdentityAndToken(
        nowEpochSeconds: Long,
        quarantined: MutableList<QuarantinedRecordEntity>
    ): Pair<LegacyUserIdentityParsed?, Boolean> {
        val rawIdentity = legacyPrefs.getString(KEY_USER_IDENTITY, null) ?: return null to false
        return try {
            val identity = LegacyJsonParsers.parseUserIdentity(rawIdentity)
            var tokenSaved = false
            if (identity.authToken.isNotEmpty()) {
                tokenStore.saveToken(accountId = identity.id, token = identity.authToken)
                tokenSaved = true
            }
            identity to tokenSaved
        } catch (e: Exception) {
            AppLog.w(TAG, "Quarantining corrupt legacy USER_IDENTITY record", e)
            quarantined += QuarantinedRecordEntity(
                sourceKey = KEY_USER_IDENTITY,
                rawPayload = LegacyJsonParsers.redactSecrets(rawIdentity),
                errorReason = e.message ?: "Invalid USER_IDENTITY JSON",
                quarantinedAtEpochSeconds = nowEpochSeconds
            )
            null to false
        }
    }

    private suspend fun upsertCollectionIdempotently(entities: List<CollectionEntryEntity>) {
        if (entities.isEmpty()) return
        val toInsert = entities.filter { candidate ->
            val existing = database.collectionDao().getEntryByIdIncludingPendingDelete(candidate.vnId)
            existing == null || candidate.lastModifiedEpochSeconds > existing.lastModifiedEpochSeconds
        }
        if (toInsert.isNotEmpty()) {
            database.collectionDao().upsertEntries(toInsert)
        }
    }

    private suspend fun upsertVnCacheIdempotently(entities: List<VnCacheEntity>) {
        if (entities.isEmpty()) return
        val toInsert = entities.filter { candidate ->
            val existing = database.vnCacheDao().getById(candidate.vnId)
            existing == null || (!existing.hasFullDetails && candidate.hasFullDetails)
        }
        if (toInsert.isNotEmpty()) {
            database.vnCacheDao().upsertAll(toInsert)
        }
    }

    private suspend fun migrateSettingsAndFilters(
        migratedAccountId: String?,
        nowEpochSeconds: Long,
        quarantined: MutableList<QuarantinedRecordEntity>
    ) {
        if (migratedAccountId != null && settingsStore.getActiveAccountId() == null) {
            settingsStore.setActiveAccountId(migratedAccountId)
        }
        if (hasLegacySettingsKeys()) {
            val statusOrder = legacyPrefs.getString(KEY_COLLECTION_ARRANGEMENT, null)?.let { raw ->
                runCatching {
                    LegacyJsonParsers.parseStringArray(raw)
                        .map { key -> CollectionStatus.fromStorageKeyOrDefault(key) }
                        .distinct()
                }.getOrNull()
            } ?: CollectionStatus.entries
            settingsStore.saveSettings(
                AppSettings(
                    themeId = ThemeId.fromCode(legacyPrefs.getString(KEY_THEME_CONF, ThemeId.DEFAULT.code)),
                    showCoverCensor = legacyPrefs.getBoolean(KEY_COVER_CENSOR_CONF, true),
                    showChart = legacyPrefs.getBoolean(KEY_SHOW_CHART_CONF, true),
                    fontSizeOffset = legacyPrefs.getFloat(KEY_FONT_SIZE_CONF, 0.0f),
                    maxPreviewItem = legacyPrefs.getInt(KEY_MAX_PREVIEW_CONF, 20),
                    maxItemPerRowPortrait = legacyPrefs.getInt(KEY_PORTRAIT_ROW_CONF, 2),
                    maxItemPerRowLandscape = legacyPrefs.getInt(KEY_LANDSCAPE_ROW_CONF, 4),
                    collectionStatusTabArrangement = (statusOrder + CollectionStatus.entries).distinct()
                )
            )
        }
        legacyPrefs.getString(KEY_COLLECTION_FILTER_CONF, null)?.let { raw ->
            try {
                settingsStore.saveCollectionFilters(LegacyJsonParsers.parseCollectionFilters(raw))
            } catch (e: Exception) {
                AppLog.w(TAG, "Quarantining corrupt legacy collection filter config", e)
                quarantined += QuarantinedRecordEntity(
                    sourceKey = KEY_COLLECTION_FILTER_CONF,
                    rawPayload = raw,
                    errorReason = e.message ?: "Unparsable filter config",
                    quarantinedAtEpochSeconds = nowEpochSeconds
                )
            }
        }
        legacyPrefs.getString(KEY_COLLECTION_SORT_CONF, null)?.let { raw ->
            try {
                settingsStore.saveCollectionSort(LegacyJsonParsers.parseCollectionSort(raw))
            } catch (e: Exception) {
                AppLog.w(TAG, "Quarantining corrupt legacy collection sort config", e)
                quarantined += QuarantinedRecordEntity(
                    sourceKey = KEY_COLLECTION_SORT_CONF,
                    rawPayload = raw,
                    errorReason = e.message ?: "Unparsable sort config",
                    quarantinedAtEpochSeconds = nowEpochSeconds
                )
            }
        }
    }

    private fun hasLegacySettingsKeys(): Boolean =
        legacyPrefs.contains(KEY_THEME_CONF) ||
            legacyPrefs.contains(KEY_COVER_CENSOR_CONF) ||
            legacyPrefs.contains(KEY_SHOW_CHART_CONF) ||
            legacyPrefs.contains(KEY_FONT_SIZE_CONF) ||
            legacyPrefs.contains(KEY_MAX_PREVIEW_CONF) ||
            legacyPrefs.contains(KEY_PORTRAIT_ROW_CONF) ||
            legacyPrefs.contains(KEY_LANDSCAPE_ROW_CONF) ||
            legacyPrefs.contains(KEY_COLLECTION_ARRANGEMENT)

    companion object {
        private const val TAG = "LegacyPrefsMigrator"
        const val LEGACY_PREFS_NAME = "VndbKotlinPreferences"
        const val LOCAL_UNSCOPED_ACCOUNT_ID = "_local_"

        internal const val PREFIX_P1 = "P01_"
        internal const val PREFIX_P2 = "P02_"
        internal const val PREFIX_P3 = "P03_"
        internal const val PREFIX_HOME_PREVIEW = "HOME_PREVIEW_"
        internal const val KEY_SAVED_COLLECTION = "SAVED_COLLECTION_SECTIONS"
        internal const val KEY_COLLECTION_FILTER_CONF = "COLLECTION_FILTER_CONF"
        internal const val KEY_COLLECTION_SORT_CONF = "COLLECTION_SORT_CONF"
        internal const val KEY_TO_BE_REMOVED = "VN_RECORDS_TO_BE_REMOVED"
        internal const val KEY_ADDED_BY_APP = "VN_RECORDS_ADDED_BY_APP"
        internal const val KEY_COLLECTION_ARRANGEMENT = "COLLECTION_ARRANGEMENT"
        internal const val KEY_USER_IDENTITY = "USER_IDENTITY"
        internal const val KEY_USER_SYNCED = "USER_SYNCED"
        internal const val KEY_VNDB_STATS = "VNDB_STATS"
        internal const val KEY_QUOTES_CACHE = "VNDB_QUOTES_CACHE"
        internal const val KEY_THEME_CONF = "THEME_CONF"
        internal const val KEY_COVER_CENSOR_CONF = "NSFW_COVER_CENSOR_DEFAULT_ON"
        internal const val KEY_SHOW_CHART_CONF = "SHOW_CHART_CONF"
        internal const val KEY_FONT_SIZE_CONF = "CUSTOM_FONT_SIZE_CONF"
        internal const val KEY_MAX_PREVIEW_CONF = "MAX_PREVIEW_ITEMS_CONF"
        internal const val KEY_PORTRAIT_ROW_CONF = "MAX_ITEMS_PER_ROW_PORTRAIT_CONF"
        internal const val KEY_LANDSCAPE_ROW_CONF = "MAX_ITEMS_PER_ROW_LANDSCAPE_CONF"

        fun create(
            context: Context,
            database: AppDatabase,
            settingsStore: SettingsStore,
            tokenStore: TokenStore,
            clock: Clock = SystemClock,
            dispatchers: DispatcherProvider = DefaultDispatcherProvider
        ): LegacyPrefsMigrator {
            val prefs = context.applicationContext.getSharedPreferences(
                LEGACY_PREFS_NAME,
                Context.MODE_PRIVATE
            )
            return LegacyPrefsMigrator(
                legacyPrefs = prefs,
                database = database,
                settingsStore = settingsStore,
                tokenStore = tokenStore,
                clock = clock,
                dispatchers = dispatchers
            )
        }
    }
}
