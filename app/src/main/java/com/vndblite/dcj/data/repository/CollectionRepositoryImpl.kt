package com.vndblite.dcj.data.repository

import androidx.room.withTransaction
import com.vndblite.dcj.core.AppLog
import com.vndblite.dcj.core.AppResult
import com.vndblite.dcj.core.Clock
import com.vndblite.dcj.core.DefaultDispatcherProvider
import com.vndblite.dcj.core.DispatcherProvider
import com.vndblite.dcj.core.SystemClock
import com.vndblite.dcj.data.local.db.AppDatabase
import com.vndblite.dcj.data.local.db.dao.CollectionSqlQueryBuilder
import com.vndblite.dcj.data.local.db.entity.CollectionEntryEntity
import com.vndblite.dcj.data.local.db.entity.VnCacheEntity
import com.vndblite.dcj.data.local.prefs.SettingsStore
import com.vndblite.dcj.data.remote.VndbApi
import com.vndblite.dcj.domain.model.CollectionEntry
import com.vndblite.dcj.domain.model.CollectionFilters
import com.vndblite.dcj.domain.model.CollectionItemWithVn
import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.domain.model.SortOption
import com.vndblite.dcj.domain.model.SyncState
import com.vndblite.dcj.domain.model.VnSummary
import com.vndblite.dcj.domain.repository.CollectionRepository
import com.vndblite.dcj.sync.SyncService
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Room-backed [CollectionRepository] executing SQL filter/sort queries, transactional batch
 * updates, and account-scoped remote deletion queueing (Section 5, A3, S1-S3, F3, F8, F9, Y1-Y6).
 */
class CollectionRepositoryImpl(
    private val database: AppDatabase,
    private val api: VndbApi,
    private val settingsStore: SettingsStore,
    private val syncService: SyncService,
    private val clock: Clock = SystemClock,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider
) : CollectionRepository {

    private val collectionDao = database.collectionDao()
    private val vnCacheDao = database.vnCacheDao()

    override fun observeRecentCollection(limit: Int): Flow<List<CollectionItemWithVn>> =
        collectionDao.observeRecentCollectionEntries(limit.coerceIn(1, 100)).map { rows ->
            rows.map { it.toDomain() }
        }

    override fun observeAllVisibleEntriesMap(): Flow<Map<String, CollectionEntry>> =
        collectionDao.observeVisibleCollectionByLastModifiedDesc().map { rows ->
            rows.associate { row -> row.entry.vnId to row.entry.toDomain(row.vn?.title.orEmpty()) }
        }

    override fun observeEntryById(vnId: String): Flow<CollectionEntry?> =
        collectionDao.observeEntryById(vnId).map { it?.toDomain() }

    override fun observeFilteredCollection(
        filters: CollectionFilters,
        sort: SortOption
    ): Flow<List<CollectionItemWithVn>> {
        val sqlQuery = CollectionSqlQueryBuilder.buildFilteredCollectionQuery(filters, sort)
        return collectionDao.observeCollectionByQuery(sqlQuery).map { rows ->
            rows.map { it.toDomain() }
        }
    }

    override fun observeVisibleCount(): Flow<Int> =
        collectionDao.observeVisibleCount()

    override suspend fun saveOrUpdateEntry(
        vn: VnSummary,
        status: CollectionStatus,
        vote: Int?,
        notes: String?,
        startedDate: LocalDate?,
        finishedDate: LocalDate?
    ) = withContext(dispatchers.io) {
        val nowSeconds = clock.nowEpochSeconds()
        val nowInstant = Instant.ofEpochSecond(nowSeconds)
        val todayUtc = nowInstant.atZone(ZoneOffset.UTC).toLocalDate()
        val activeAccountId = settingsStore.getActiveAccountId()?.trim()?.takeIf { it.isNotEmpty() }

        database.withTransaction {
            val existingRow = collectionDao.getEntryByIdIncludingPendingDelete(vn.id)
            val existingDomain = existingRow?.toDomain(fallbackTitle = vn.title)
            val nextSyncState = when (existingDomain?.syncState) {
                SyncState.CLEAN, SyncState.DIRTY, SyncState.PENDING_DELETE ->
                    if (existingDomain.remoteLastMod != null) SyncState.DIRTY else SyncState.NEW
                else -> SyncState.NEW
            }
            val updatedEntry = CollectionEntry(
                vnId = vn.id,
                title = vn.title.ifBlank { existingDomain?.title.orEmpty() },
                status = status,
                vote = CollectionEntry.normalizeVote(vote),
                notes = notes?.trim()?.takeIf { it.isNotEmpty() },
                addedDate = existingDomain?.addedDate ?: todayUtc,
                startedDate = startedDate,
                finishedDate = finishedDate,
                lastModifiedAt = nowInstant,
                remoteLastMod = existingDomain?.remoteLastMod,
                syncState = nextSyncState,
                lastSyncedStatus = existingDomain?.lastSyncedStatus,
                lastSyncedVote = existingDomain?.lastSyncedVote,
                lastSyncedStartedDate = existingDomain?.lastSyncedStartedDate,
                lastSyncedFinishedDate = existingDomain?.lastSyncedFinishedDate,
                lastSyncedNotes = existingDomain?.lastSyncedNotes
            )
            collectionDao.upsertEntry(
                CollectionEntryEntity.fromDomain(
                    entry = updatedEntry,
                    accountId = existingRow?.accountId ?: activeAccountId
                )
            )
            val existingCache = vnCacheDao.getById(vn.id)
            if (vn.title.isNotBlank() || existingCache == null) {
                vnCacheDao.upsert(
                    VnCacheEntity.fromSummary(
                        summary = vn,
                        nowEpochSeconds = nowSeconds,
                        existing = existingCache
                    )
                )
            }
        }
    }

    override suspend fun removeEntry(vnId: String) {
        syncService.queueLocalDeletions(listOf(vnId))
    }

    override suspend fun batchUpdateEntries(
        vnIds: List<String>,
        status: CollectionStatus,
        overrideVote: Int?
    ): Unit = withContext(dispatchers.io) {
        val cleanedIds = vnIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (cleanedIds.isEmpty()) return@withContext
        val nowSeconds = clock.nowEpochSeconds()
        val nowInstant = Instant.ofEpochSecond(nowSeconds)
        val normalizedVote = CollectionEntry.normalizeVote(overrideVote)

        database.withTransaction {
            if (normalizedVote == null) {
                collectionDao.batchUpdateStatus(
                    vnIds = cleanedIds,
                    newStatusId = status.labelId,
                    modifiedEpochSeconds = nowSeconds
                )
            } else {
                val updatedRows = cleanedIds.mapNotNull { id ->
                    val existing = collectionDao.getVisibleEntryById(id) ?: return@mapNotNull null
                    val domain = existing.toDomain()
                    val nextSyncState = if (domain.remoteLastMod != null) SyncState.DIRTY else SyncState.NEW
                    CollectionEntryEntity.fromDomain(
                        entry = domain.copy(
                            status = status,
                            vote = normalizedVote,
                            lastModifiedAt = nowInstant,
                            syncState = nextSyncState
                        ),
                        accountId = existing.accountId
                    )
                }
                if (updatedRows.isNotEmpty()) {
                    collectionDao.upsertEntries(updatedRows)
                }
            }
            Unit
        }
    }

    override suspend fun removeEntries(vnIds: List<String>) {
        syncService.queueLocalDeletions(vnIds)
    }

    override suspend fun clearLocalCollection(queueForRemoteDelete: Boolean) = withContext(dispatchers.io) {
        if (queueForRemoteDelete) {
            val allVisibleIds = collectionDao.getAllVisibleEntries().map { it.vnId }
            if (allVisibleIds.isNotEmpty()) {
                syncService.queueLocalDeletions(allVisibleIds)
            }
        } else {
            collectionDao.deleteAllEntriesHard()
        }
    }

    override suspend fun hydrateMissingSummaries(vnIds: List<String>) = withContext(dispatchers.io) {
        val cleaned = vnIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (cleaned.isEmpty()) return@withContext
        val cachedRows = vnCacheDao.getByIds(cleaned).associateBy { it.vnId }
        val uncachedIds = cleaned.filter { id ->
            val cached = cachedRows[id]
            cached == null || (cached.imageUrl.isNullOrBlank() && cached.rating == null)
        }
        if (uncachedIds.isEmpty()) return@withContext
        when (val result = api.fetchVnSummariesByIds(uncachedIds, includeDescription = false)) {
            is AppResult.Success -> {
                val nowSeconds = clock.nowEpochSeconds()
                val entities = result.data.map { summary ->
                    VnCacheEntity.fromSummary(
                        summary = summary,
                        nowEpochSeconds = nowSeconds,
                        existing = cachedRows[summary.id]
                    )
                }
                vnCacheDao.upsertAll(entities)
            }
            is AppResult.Failure -> {
                AppLog.w(TAG, "Failed to hydrate missing collection summaries: ${result.error}")
            }
        }
    }

    companion object {
        private const val TAG = "CollectionRepository"
    }
}
