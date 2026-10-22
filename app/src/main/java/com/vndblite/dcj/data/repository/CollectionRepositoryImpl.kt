package com.vndblite.dcj.data.repository

import androidx.room.withTransaction
import com.vndblite.dcj.core.Clock
import com.vndblite.dcj.core.DefaultDispatcherProvider
import com.vndblite.dcj.core.DispatcherProvider
import com.vndblite.dcj.core.SystemClock
import com.vndblite.dcj.data.local.db.AppDatabase
import com.vndblite.dcj.data.local.db.dao.CollectionSqlQueryBuilder
import com.vndblite.dcj.data.local.db.entity.CollectionEntryEntity
import com.vndblite.dcj.data.local.db.entity.PendingRemoteDeletionEntity
import com.vndblite.dcj.data.local.db.entity.VnCacheEntity
import com.vndblite.dcj.data.local.prefs.SettingsStore
import com.vndblite.dcj.domain.model.CollectionEntry
import com.vndblite.dcj.domain.model.CollectionFilters
import com.vndblite.dcj.domain.model.CollectionItemWithVn
import com.vndblite.dcj.domain.model.SortOption
import com.vndblite.dcj.domain.model.SyncState
import com.vndblite.dcj.domain.repository.CollectionBatchUpdateSpec
import com.vndblite.dcj.domain.repository.CollectionRepository
import com.vndblite.dcj.domain.repository.CollectionSaveDraft
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class CollectionRepositoryImpl(
    private val database: AppDatabase,
    private val settingsStore: SettingsStore,
    private val clock: Clock = SystemClock,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider
) : CollectionRepository {

    private val collectionDao = database.collectionDao()
    private val vnCacheDao = database.vnCacheDao()
    private val pendingDeletionDao = database.pendingRemoteDeletionDao()

    override fun observeFilteredCollection(
        filters: CollectionFilters,
        sort: SortOption
    ): Flow<List<CollectionItemWithVn>> {
        val query = CollectionSqlQueryBuilder.buildFilteredCollectionQuery(filters = filters, sort = sort)
        return collectionDao.observeCollectionByQuery(query).map { rows ->
            rows.map { it.toDomain() }
        }
    }

    override fun observeVisibleCollectionByLastModifiedDesc(): Flow<List<CollectionItemWithVn>> =
        collectionDao.observeVisibleCollectionByLastModifiedDesc().map { rows ->
            rows.map { it.toDomain() }
        }

    override fun observeRecentCollectionEntries(limit: Int): Flow<List<CollectionItemWithVn>> =
        collectionDao.observeRecentCollectionEntries(limit.coerceAtLeast(1)).map { rows ->
            rows.map { it.toDomain() }
        }

    override fun observeEntryById(vnId: String): Flow<CollectionEntry?> =
        collectionDao.observeEntryById(vnId.trim()).map { it?.toDomain() }

    override fun observeVisibleCollectionIds(): Flow<Set<String>> =
        collectionDao.observeVisibleCollectionIds().map { it.toSet() }

    override fun observeVisibleCount(): Flow<Int> =
        collectionDao.observeVisibleCount()

    override suspend fun getVisibleEntryById(vnId: String): CollectionEntry? =
        withContext(dispatchers.io) {
            collectionDao.getVisibleEntryById(vnId.trim())?.toDomain()
        }

    override suspend fun saveEntry(draft: CollectionSaveDraft): CollectionEntry =
        withContext(dispatchers.io) {
            val cleanVnId = draft.vn.id.trim()
            require(cleanVnId.isNotEmpty()) { "vnId must not be blank" }
            val nowSec = clock.nowEpochSeconds()
            val nowInstant = Instant.ofEpochSecond(nowSec)
            val todayUtc = nowInstant.atZone(ZoneOffset.UTC).toLocalDate()
            val activeAccountId = settingsStore.getActiveAccountId()?.trim()?.takeIf { it.isNotEmpty() }

            database.withTransaction {
                val existingEntity = collectionDao.getEntryByIdIncludingPendingDelete(cleanVnId)
                val existingDomain = existingEntity?.toDomain(fallbackTitle = draft.vn.title)
                val wasPreviouslySynced = existingDomain?.remoteLastMod != null ||
                    existingDomain?.syncState == SyncState.CLEAN ||
                    existingDomain?.syncState == SyncState.DIRTY ||
                    (existingDomain?.syncState == SyncState.PENDING_DELETE && existingDomain.lastSyncedStatus != null)

                val nextSyncState = if (wasPreviouslySynced) SyncState.DIRTY else SyncState.NEW
                val resolvedTitle = draft.vn.title.ifBlank { existingDomain?.title.orEmpty() }
                val updatedEntry = CollectionEntry(
                    vnId = cleanVnId,
                    title = resolvedTitle,
                    status = draft.status,
                    vote = CollectionEntry.normalizeVote(draft.vote),
                    notes = draft.notes?.trim()?.takeIf { it.isNotEmpty() },
                    addedDate = existingDomain?.addedDate ?: todayUtc,
                    startedDate = draft.startedDate,
                    finishedDate = draft.finishedDate,
                    lastModifiedAt = nowInstant,
                    remoteLastMod = existingDomain?.remoteLastMod,
                    syncState = nextSyncState,
                    lastSyncedStatus = existingDomain?.lastSyncedStatus,
                    lastSyncedVote = existingDomain?.lastSyncedVote,
                    lastSyncedStartedDate = existingDomain?.lastSyncedStartedDate,
                    lastSyncedFinishedDate = existingDomain?.lastSyncedFinishedDate,
                    lastSyncedNotes = existingDomain?.lastSyncedNotes
                )

                val resolvedOwnerAccount = existingEntity?.accountId ?: activeAccountId
                collectionDao.upsertEntry(
                    CollectionEntryEntity.fromDomain(updatedEntry, accountId = resolvedOwnerAccount)
                )
                if (activeAccountId != null) {
                    pendingDeletionDao.removeConfirmed(activeAccountId, cleanVnId)
                }
                val existingCache = vnCacheDao.getById(cleanVnId)
                vnCacheDao.upsert(
                    VnCacheEntity.fromSummary(
                        summary = draft.vn.copy(title = resolvedTitle),
                        nowEpochSeconds = nowSec,
                        existing = existingCache
                    )
                )
                updatedEntry
            }
        }

    override suspend fun batchUpdateEntries(spec: CollectionBatchUpdateSpec): Int =
        withContext(dispatchers.io) {
            val cleanedIds = spec.vnIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
            if (cleanedIds.isEmpty()) return@withContext 0
            val nowSec = clock.nowEpochSeconds()
            val nowInstant = Instant.ofEpochSecond(nowSec)
            val normalizedVote = CollectionEntry.normalizeVote(spec.newVote)

            database.withTransaction {
                if (spec.newStatus != null && !spec.updateVote) {
                    return@withTransaction collectionDao.batchUpdateStatus(
                        vnIds = cleanedIds,
                        newStatusId = spec.newStatus.labelId,
                        modifiedEpochSeconds = nowSec
                    )
                }
                val existingRows = cleanedIds.mapNotNull { collectionDao.getVisibleEntryById(it) }
                if (existingRows.isEmpty()) return@withTransaction 0

                val updatedEntities = existingRows.map { row ->
                    val domain = row.toDomain()
                    val nextStatus = spec.newStatus ?: domain.status
                    val nextVote = if (spec.updateVote) normalizedVote else domain.vote
                    val nextSyncState = if (domain.syncState == SyncState.NEW && domain.remoteLastMod == null) {
                        SyncState.NEW
                    } else {
                        SyncState.DIRTY
                    }
                    val updatedDomain = domain.copy(
                        status = nextStatus,
                        vote = nextVote,
                        lastModifiedAt = nowInstant,
                        syncState = nextSyncState
                    )
                    CollectionEntryEntity.fromDomain(updatedDomain, accountId = row.accountId)
                }
                collectionDao.upsertEntries(updatedEntities)
                updatedEntities.size
            }
        }

    override suspend fun removeEntry(vnId: String) {
        batchRemoveEntries(listOf(vnId))
    }

    override suspend fun batchRemoveEntries(vnIds: List<String>) =
        withContext(dispatchers.io) {
            val cleanedIds = vnIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
            if (cleanedIds.isEmpty()) return@withContext
            val activeAccountId = settingsStore.getActiveAccountId()?.trim()?.takeIf { it.isNotEmpty() }
            val nowSec = clock.nowEpochSeconds()

            database.withTransaction {
                val existingById = cleanedIds.associateWith {
                    collectionDao.getEntryByIdIncludingPendingDelete(it)
                }
                if (activeAccountId != null) {
                    val remoteDeleteCandidates = cleanedIds.filter { vnId ->
                        val row = existingById[vnId]
                        row == null ||
                            row.syncState != SyncState.NEW.name ||
                            row.remoteLastModEpochSeconds != null
                    }
                    if (remoteDeleteCandidates.isNotEmpty()) {
                        pendingDeletionDao.enqueueAll(
                            remoteDeleteCandidates.map {
                                PendingRemoteDeletionEntity(
                                    accountId = activeAccountId,
                                    vnId = it,
                                    queuedAtEpochSeconds = nowSec
                                )
                            }
                        )
                    }
                }
                val syncedIdsToMark = cleanedIds.filter { vnId ->
                    val row = existingById[vnId]
                    row != null && (row.syncState != SyncState.NEW.name || row.remoteLastModEpochSeconds != null)
                }
                val unsyncedIdsToDeleteHard = cleanedIds.filter { it !in syncedIdsToMark }

                if (syncedIdsToMark.isNotEmpty()) {
                    collectionDao.markEntriesPendingDelete(syncedIdsToMark, nowSec)
                }
                if (unsyncedIdsToDeleteHard.isNotEmpty()) {
                    collectionDao.deleteEntriesHard(unsyncedIdsToDeleteHard)
                }
            }
        }

    override suspend fun clearAllEntries(queueRemoteDeletesIfSynced: Boolean) =
        withContext(dispatchers.io) {
            val activeAccountId = settingsStore.getActiveAccountId()?.trim()?.takeIf { it.isNotEmpty() }
            val nowSec = clock.nowEpochSeconds()

            database.withTransaction {
                if (queueRemoteDeletesIfSynced && activeAccountId != null) {
                    val allRows = collectionDao.getAllEntriesIncludingPendingDelete()
                    val remoteCandidates = allRows.filter {
                        it.syncState != SyncState.NEW.name || it.remoteLastModEpochSeconds != null
                    }
                    if (remoteCandidates.isNotEmpty()) {
                        pendingDeletionDao.enqueueAll(
                            remoteCandidates.map {
                                PendingRemoteDeletionEntity(
                                    accountId = activeAccountId,
                                    vnId = it.vnId,
                                    queuedAtEpochSeconds = nowSec
                                )
                            }
                        )
                    }
                }
                collectionDao.deleteAllEntriesHard()
            }
        }
}
