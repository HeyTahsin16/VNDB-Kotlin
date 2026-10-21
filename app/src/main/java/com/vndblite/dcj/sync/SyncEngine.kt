package com.vndblite.dcj.sync

import androidx.room.withTransaction
import com.vndblite.dcj.core.ApiError
import com.vndblite.dcj.core.AppLog
import com.vndblite.dcj.core.AppResult
import com.vndblite.dcj.core.Clock
import com.vndblite.dcj.core.DefaultDispatcherProvider
import com.vndblite.dcj.core.DispatcherProvider
import com.vndblite.dcj.core.SystemClock
import com.vndblite.dcj.data.local.db.AppDatabase
import com.vndblite.dcj.data.local.db.entity.CollectionEntryEntity
import com.vndblite.dcj.data.local.db.entity.PendingRemoteDeletionEntity
import com.vndblite.dcj.data.local.db.entity.SyncMetadataEntity
import com.vndblite.dcj.data.local.db.entity.VnCacheEntity
import com.vndblite.dcj.data.local.prefs.SettingsStore
import com.vndblite.dcj.data.local.secure.TokenStore
import com.vndblite.dcj.data.remote.VndbApi
import com.vndblite.dcj.domain.model.RemoteUserListItem
import com.vndblite.dcj.domain.model.SyncFailure
import com.vndblite.dcj.domain.model.SyncReport
import com.vndblite.dcj.domain.model.SyncState
import com.vndblite.dcj.domain.model.UserAccount
import com.vndblite.dcj.domain.model.VnSummary
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * High-level stage of a synchronization run for reactive UI progress reporting without
 * hardcoded English strings in non-UI layers (Rule 4, A11, Y6).
 */
enum class SyncStage {
    IDLE,
    AUTHENTICATING,
    FETCHING_REMOTE_LIST,
    PLANNING,
    DELETING_REMOTE,
    PUSHING_LOCAL_CHANGES,
    APPLYING_LOCAL_UPDATES,
    CACHING_SUMMARIES,
    COMPLETED,
    FAILED
}

/**
 * Structured progress snapshot emitted by [SyncService.progressFlow] during authentication and sync (Y6).
 */
data class SyncProgress(
    val stage: SyncStage = SyncStage.IDLE,
    val isRunning: Boolean = false,
    val completedOperations: Int = 0,
    val totalOperations: Int = 0,
    val currentVnId: String? = null,
    val lastReport: SyncReport? = null,
    val lastError: ApiError? = null
)

/**
 * Contract for account authentication, two-way userlist synchronization, and account-scoped logout (A3, Y1-Y6).
 */
interface SyncService {
    val progressFlow: StateFlow<SyncProgress>
    val activeAccountFlow: Flow<UserAccount?>
    val isAccountSyncedFlow: Flow<Boolean>

    suspend fun authenticateAndSync(
        rawToken: String,
        onProgress: (SyncProgress) -> Unit = {}
    ): AppResult<Pair<UserAccount, SyncReport>>

    suspend fun syncCurrentAccount(
        onProgress: (SyncProgress) -> Unit = {}
    ): AppResult<SyncReport>

    suspend fun queueLocalDeletions(vnIds: List<String>)

    suspend fun logout(clearLocalCollection: Boolean)
}

/**
 * Coroutine-safe synchronization engine coordinating [VndbApi], [AppDatabase], [SettingsStore],
 * [TokenStore], and [SyncPlanner] (Section 5, N1, N7, Y1-Y6).
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SyncEngine(
    private val api: VndbApi,
    private val database: AppDatabase,
    private val settingsStore: SettingsStore,
    private val tokenStore: TokenStore,
    private val clock: Clock = SystemClock,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider
) : SyncService {

    private val syncMutex = Mutex()
    private val _progressFlow = MutableStateFlow(SyncProgress())
    override val progressFlow: StateFlow<SyncProgress> = _progressFlow.asStateFlow()

    private val collectionDao = database.collectionDao()
    private val vnCacheDao = database.vnCacheDao()
    private val pendingDeletionDao = database.pendingRemoteDeletionDao()
    private val syncMetadataDao = database.syncMetadataDao()

    override val activeAccountFlow: Flow<UserAccount?> =
        settingsStore.activeAccountIdFlow.flatMapLatest { accountId ->
            if (accountId.isNullOrBlank()) flowOf(null)
            else syncMetadataDao.observeByAccountId(accountId).map { it?.toUserAccount() }
        }

    override val isAccountSyncedFlow: Flow<Boolean> =
        settingsStore.activeAccountIdFlow.flatMapLatest { accountId ->
            if (accountId.isNullOrBlank()) flowOf(false)
            else syncMetadataDao.observeByAccountId(accountId).map { it?.isSynced ?: false }
        }

    override suspend fun authenticateAndSync(
        rawToken: String,
        onProgress: (SyncProgress) -> Unit
    ): AppResult<Pair<UserAccount, SyncReport>> = withContext(dispatchers.io) {
        syncMutex.withLock {
            try {
                emitProgress(SyncProgress(stage = SyncStage.AUTHENTICATING, isRunning = true), onProgress)
                val cleanToken = rawToken.trim()
                if (cleanToken.isEmpty()) return@withLock failSync(ApiError.Unauthorized, onProgress)

                val account = when (val authRes = api.authenticate(cleanToken)) {
                    is AppResult.Failure -> return@withLock failSync(authRes.error, onProgress)
                    is AppResult.Success -> authRes.data
                }

                prepareAccountSwitchIfNeeded(newAccountId = account.id)
                tokenStore.saveToken(accountId = account.id, token = cleanToken)
                syncMetadataDao.upsert(SyncMetadataEntity.fromUserAccount(account, isSynced = false, lastSyncedAtEpochSeconds = null))
                settingsStore.setActiveAccountId(account.id)

                when (val syncResult = executeSyncLocked(account, cleanToken, onProgress)) {
                    is AppResult.Failure -> AppResult.Failure(syncResult.error)
                    is AppResult.Success -> AppResult.Success(account to syncResult.data)
                }
            } finally {
                ensureNotRunningFlag()
            }
        }
    }

    override suspend fun syncCurrentAccount(
        onProgress: (SyncProgress) -> Unit
    ): AppResult<SyncReport> = withContext(dispatchers.io) {
        syncMutex.withLock {
            try {
                val accountId = settingsStore.getActiveAccountId()?.trim()?.takeIf { it.isNotEmpty() }
                    ?: return@withLock failSync(ApiError.Unauthorized, onProgress)
                val token = tokenStore.getToken(accountId)?.trim()?.takeIf { it.isNotEmpty() }
                    ?: return@withLock failSync(ApiError.Unauthorized, onProgress)

                val account = when (val resolved = resolveVerifiedAccount(accountId, token, onProgress)) {
                    is AppResult.Failure -> return@withLock failSync(resolved.error, onProgress)
                    is AppResult.Success -> resolved.data
                }
                executeSyncLocked(account, token, onProgress)
            } finally {
                ensureNotRunningFlag()
            }
        }
    }

    override suspend fun queueLocalDeletions(vnIds: List<String>) = withContext(dispatchers.io) {
        val cleanedIds = vnIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (cleanedIds.isEmpty()) return@withContext
        val activeAccountId = settingsStore.getActiveAccountId()?.trim()?.takeIf { it.isNotEmpty() }
        val nowSeconds = clock.nowEpochSeconds()

        database.withTransaction {
            if (activeAccountId != null) {
                val toQueueRemotely = cleanedIds.filter { vnId ->
                    val existing = collectionDao.getEntryByIdIncludingPendingDelete(vnId)
                    existing == null || existing.syncState != SyncState.NEW.name || existing.remoteLastModEpochSeconds != null
                }
                if (toQueueRemotely.isNotEmpty()) {
                    pendingDeletionDao.enqueueAll(
                        toQueueRemotely.map { PendingRemoteDeletionEntity(activeAccountId, it, nowSeconds) }
                    )
                }
            }
            collectionDao.deleteEntriesHard(cleanedIds)
            Unit
        }
    }

    override suspend fun logout(clearLocalCollection: Boolean) = withContext(dispatchers.io) {
        syncMutex.withLock {
            val activeAccountId = settingsStore.getActiveAccountId()?.trim()
            tokenStore.clearToken(activeAccountId)
            settingsStore.setActiveAccountId(null)
            database.clearAccountSyncData(
                accountId = activeAccountId.orEmpty(),
                clearLocalCollection = clearLocalCollection
            )
            _progressFlow.value = SyncProgress()
        }
    }

    private suspend fun resolveVerifiedAccount(
        accountId: String,
        token: String,
        onProgress: (SyncProgress) -> Unit
    ): AppResult<UserAccount> {
        val cachedMetadata = syncMetadataDao.getByAccountId(accountId)
        val cachedAccount = cachedMetadata?.toUserAccount()
        if (cachedAccount != null && cachedAccount.missingListPermissions.isEmpty()) {
            return AppResult.Success(cachedAccount)
        }
        emitProgress(SyncProgress(stage = SyncStage.AUTHENTICATING, isRunning = true), onProgress)
        val authRes = api.authenticate(token)
        if (authRes is AppResult.Success) {
            syncMetadataDao.upsert(
                SyncMetadataEntity.fromUserAccount(
                    account = authRes.data,
                    isSynced = cachedMetadata?.isSynced ?: false,
                    lastSyncedAtEpochSeconds = cachedMetadata?.lastSyncedAtEpochSeconds
                )
            )
        }
        return authRes
    }

    private suspend fun prepareAccountSwitchIfNeeded(newAccountId: String) {
        val previousAccountId = settingsStore.getActiveAccountId()?.trim()
        database.withTransaction {
            val hasForeignRows = (previousAccountId != null && previousAccountId != newAccountId) ||
                collectionDao.getAllEntriesIncludingPendingDelete().any { it.accountId != null && it.accountId != newAccountId }
            if (hasForeignRows) {
                collectionDao.deleteAllPendingDeleteEntries()
                collectionDao.detachSyncStateFromAllVisibleEntries()
            }
        }
    }

    private suspend fun executeSyncLocked(
        account: UserAccount,
        token: String,
        onProgress: (SyncProgress) -> Unit
    ): AppResult<SyncReport> {
        coroutineContext.ensureActive()
        emitProgress(SyncProgress(stage = SyncStage.FETCHING_REMOTE_LIST, isRunning = true), onProgress)
        val remoteList = when (val remoteRes = api.fetchUserList(userId = account.id, token = token)) {
            is AppResult.Failure -> return failSync(remoteRes.error, onProgress)
            is AppResult.Success -> remoteRes.data
        }

        coroutineContext.ensureActive()
        emitProgress(SyncProgress(stage = SyncStage.PLANNING, isRunning = true), onProgress)
        val plan = buildPlanFromDatabase(account.id, remoteList)
        val totalRemoteOps = plan.remoteDeletions.size + plan.remotePushes.size
        val failures = mutableListOf<SyncFailure>()

        val deletedRemoteCount = executeRemoteDeletions(account.id, token, plan.remoteDeletions, totalRemoteOps, failures, onProgress)
        val pushedCount = executeRemotePushes(account.id, token, plan.remotePushes, plan.remoteDeletions.size, totalRemoteOps, failures, onProgress)

        coroutineContext.ensureActive()
        emitProgress(SyncProgress(SyncStage.APPLYING_LOCAL_UPDATES, true, totalRemoteOps, totalRemoteOps), onProgress)
        val (pulledCount, deletedLocalCount) = applyLocalPlanChanges(account.id, plan)

        coroutineContext.ensureActive()
        emitProgress(SyncProgress(SyncStage.CACHING_SUMMARIES, true, totalRemoteOps, totalRemoteOps), onProgress)
        hydrateCollectionSummaries(remoteList)

        syncMetadataDao.upsert(
            SyncMetadataEntity.fromUserAccount(account, isSynced = failures.isEmpty(), lastSyncedAtEpochSeconds = clock.nowEpochSeconds())
        )
        val report = SyncReport(
            pushed = pushedCount,
            pulled = pulledCount,
            deletedLocal = deletedLocalCount,
            deletedRemote = deletedRemoteCount,
            failed = failures
        )
        emitProgress(SyncProgress(SyncStage.COMPLETED, false, totalRemoteOps, totalRemoteOps, lastReport = report), onProgress)
        return AppResult.Success(report)
    }

    private suspend fun buildPlanFromDatabase(
        accountId: String,
        remoteList: List<RemoteUserListItem>
    ): SyncPlan {
        val nowSeconds = clock.nowEpochSeconds()
        val localRows = collectionDao.getAllEntriesIncludingPendingDelete()
        val localPendingForAccount = localRows.filter {
            it.syncState == SyncState.PENDING_DELETE.name && (it.accountId == null || it.accountId == accountId)
        }
        if (localPendingForAccount.isNotEmpty()) {
            pendingDeletionDao.enqueueAll(
                localPendingForAccount.map { PendingRemoteDeletionEntity(accountId, it.vnId, nowSeconds) }
            )
        }
        val queuedDeleteIds = pendingDeletionDao.getPendingForAccount(accountId).map { it.vnId }.toSet()
        return SyncPlanner.plan(
            SyncPlanInput(
                accountId = accountId,
                localEntries = localRows.map { it.toDomain() },
                remoteItems = remoteList,
                pendingRemoteDeleteIds = queuedDeleteIds,
                entryAccountIds = localRows.associate { it.vnId to it.accountId }
            )
        )
    }

    private suspend fun executeRemoteDeletions(
        accountId: String,
        token: String,
        deletions: List<SyncPlanAction.DeleteRemote>,
        totalOperations: Int,
        failures: MutableList<SyncFailure>,
        onProgress: (SyncProgress) -> Unit
    ): Int {
        var deletedCount = 0
        deletions.forEachIndexed { index, action ->
            coroutineContext.ensureActive()
            emitProgress(SyncProgress(SyncStage.DELETING_REMOTE, true, index, totalOperations, action.vnId), onProgress)
            val result = api.deleteUserListEntry(token = token, vnId = action.vnId)
            val confirmedDeleted = result is AppResult.Success ||
                (result is AppResult.Failure && result.error is ApiError.NotFound)

            if (confirmedDeleted) {
                database.withTransaction {
                    pendingDeletionDao.removeConfirmed(accountId, action.vnId)
                    collectionDao.deleteEntryHard(action.vnId)
                }
                deletedCount++
            } else if (result is AppResult.Failure) {
                AppLog.w(TAG, "Remote delete failed for vnId=${action.vnId}: ${result.error}")
                failures += SyncFailure(action.vnId, OP_DELETE_REMOTE, result.error)
            }
        }
        return deletedCount
    }

    private suspend fun executeRemotePushes(
        accountId: String,
        token: String,
        pushes: List<SyncPlanAction.PushToRemote>,
        completedOffset: Int,
        totalOperations: Int,
        failures: MutableList<SyncFailure>,
        onProgress: (SyncProgress) -> Unit
    ): Int {
        var pushedCount = 0
        pushes.forEachIndexed { index, action ->
            coroutineContext.ensureActive()
            emitProgress(SyncProgress(SyncStage.PUSHING_LOCAL_CHANGES, true, completedOffset + index, totalOperations, action.vnId), onProgress)
            when (val result = api.patchUserListEntry(token = token, patch = action.patch)) {
                is AppResult.Success -> {
                    val cleanEntry = SyncPlanner.markPushedClean(action.entry, clock.nowInstant(), action.remoteLastMod)
                    collectionDao.upsertEntry(CollectionEntryEntity.fromDomain(cleanEntry, accountId = accountId))
                    pushedCount++
                }
                is AppResult.Failure -> {
                    AppLog.w(TAG, "Remote push failed for vnId=${action.vnId}: ${result.error}")
                    val dirtyEntry = action.entry.copy(syncState = SyncState.DIRTY)
                    collectionDao.upsertEntry(CollectionEntryEntity.fromDomain(dirtyEntry, accountId = accountId))
                    failures += SyncFailure(action.vnId, OP_PUSH_REMOTE, result.error)
                }
            }
        }
        return pushedCount
    }

    private suspend fun applyLocalPlanChanges(
        accountId: String,
        plan: SyncPlan
    ): Pair<Int, Int> {
        var pulledCount = 0
        var deletedLocalCount = 0
        database.withTransaction {
            if (plan.localDeletions.isNotEmpty()) {
                collectionDao.deleteEntriesHard(plan.localDeletions.map { it.vnId })
                deletedLocalCount = plan.localDeletions.count { it.countInReport }
            }
            if (plan.localPulls.isNotEmpty()) {
                collectionDao.upsertEntries(
                    plan.localPulls.map { CollectionEntryEntity.fromDomain(it.mergedEntry, accountId = accountId) }
                )
                pulledCount = plan.localPulls.size
            }
            if (plan.localCleanups.isNotEmpty()) {
                collectionDao.upsertEntries(
                    plan.localCleanups.map { CollectionEntryEntity.fromDomain(it.cleanEntry, accountId = accountId) }
                )
            }
        }
        return pulledCount to deletedLocalCount
    }

    private suspend fun hydrateCollectionSummaries(remoteList: List<RemoteUserListItem>) {
        val nowSeconds = clock.nowEpochSeconds()
        val visibleIds = collectionDao.getAllVisibleEntries().map { it.vnId }.toSet()
        if (visibleIds.isEmpty()) return

        val embeddedSummaries = remoteList.mapNotNull { item -> item.vnSummary?.takeIf { item.vnId in visibleIds } }
        upsertSummariesPreservingDetails(embeddedSummaries, nowSeconds)

        val cachedIds = vnCacheDao.getByIds(visibleIds.toList()).map { it.vnId }.toSet()
        val missingIds = visibleIds.filter { it !in cachedIds }
        if (missingIds.isEmpty()) return

        when (val res = api.fetchVnSummariesByIds(missingIds, includeDescription = false)) {
            is AppResult.Success -> upsertSummariesPreservingDetails(res.data, nowSeconds)
            is AppResult.Failure -> AppLog.w(TAG, "Batch summary hydration failed: ${res.error}")
        }
    }

    private suspend fun upsertSummariesPreservingDetails(
        summaries: List<VnSummary>,
        nowSeconds: Long
    ) {
        if (summaries.isEmpty()) return
        val existingById = vnCacheDao.getByIds(summaries.map { it.id }).associateBy { it.vnId }
        val entities = summaries.map { summary ->
            VnCacheEntity.fromSummary(summary = summary, existing = existingById[summary.id], nowEpochSeconds = nowSeconds)
        }
        vnCacheDao.upsertAll(entities)
    }

    private fun failSync(
        error: ApiError,
        onProgress: (SyncProgress) -> Unit
    ): AppResult<Nothing> {
        emitProgress(SyncProgress(stage = SyncStage.FAILED, isRunning = false, lastError = error), onProgress)
        return AppResult.Failure(error)
    }

    private fun emitProgress(progress: SyncProgress, onProgress: (SyncProgress) -> Unit) {
        _progressFlow.value = progress
        onProgress(progress)
    }

    private fun ensureNotRunningFlag() {
        val current = _progressFlow.value
        if (current.isRunning) {
            _progressFlow.value = current.copy(isRunning = false)
        }
    }

    companion object {
        private const val TAG = "SyncEngine"
        const val OP_DELETE_REMOTE = "delete_remote"
        const val OP_PUSH_REMOTE = "push_remote"
    }
}
