package com.vndblite.dcj.sync

import com.vndblite.dcj.core.ApiError
import com.vndblite.dcj.core.AppResult
import com.vndblite.dcj.domain.model.CollectionEntry
import com.vndblite.dcj.domain.model.RemoteUserListItem
import com.vndblite.dcj.domain.model.SyncReport
import com.vndblite.dcj.domain.model.UserAccount
import com.vndblite.dcj.domain.model.UserListPatch
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Reason why a local collection row is scheduled for local removal during synchronization (Y5, Y6).
 */
enum class LocalDeletionReason {
    /** Entry was `CLEAN` locally and is no longer present on the remote VNDB userlist (Y5). */
    DELETED_ON_REMOTE,
    /** Entry was a `PENDING_DELETE` tombstone from a different account and must not be replayed (Y6). */
    STALE_FOREIGN_PENDING_DELETE
}

/**
 * Per-VN synchronization action produced by [SyncPlanner] (Y1-Y6).
 */
sealed interface SyncPlanAction {
    val vnId: String

    /**
     * Execute `DELETE /kana/ulist/<vnId>` and dequeue only after HTTP 2xx or 404 (Y4, Y5).
     */
    data class DeleteRemote(
        override val vnId: String
    ) : SyncPlanAction

    /**
     * Execute `PATCH /kana/ulist/<vnId>` with minimal diff [patch] (Y1, Y2, Y3, Y5).
     */
    data class PushToRemote(
        override val vnId: String,
        val entry: CollectionEntry,
        val patch: UserListPatch,
        val remoteLastMod: Instant? = null
    ) : SyncPlanAction

    /**
     * Insert or overwrite the local row with [mergedEntry] pulled from VNDB (Y1, Y5).
     */
    data class PullFromRemote(
        override val vnId: String,
        val remoteItem: RemoteUserListItem,
        val existingLocal: CollectionEntry?,
        val mergedEntry: CollectionEntry
    ) : SyncPlanAction

    /**
     * Delete the local row from `collection_entries` without calling remote `DELETE` (Y5, Y6).
     */
    data class DeleteLocal(
        override val vnId: String,
        val reason: LocalDeletionReason
    ) : SyncPlanAction {
        val countInReport: Boolean
            get() = reason == LocalDeletionReason.DELETED_ON_REMOTE
    }

    /**
     * Local and remote payloads are already identical; update local sync metadata to `CLEAN`
     * without issuing a redundant network `PATCH` request (Y1, Y3).
     */
    data class MarkClean(
        override val vnId: String,
        val cleanEntry: CollectionEntry
    ) : SyncPlanAction

    /**
     * Local row is already `CLEAN` and matches the remote state and timestamp.
     */
    data class NoOp(
        override val vnId: String
    ) : SyncPlanAction
}

/**
 * Input bundle for [SyncPlanner.plan] (Rule 2: <= 8 parameters).
 */
data class SyncPlanInput(
    val accountId: String,
    val localEntries: List<CollectionEntry>,
    val remoteItems: List<RemoteUserListItem>,
    val pendingRemoteDeleteIds: Set<String> = emptySet(),
    /** Optional mapping of `vnId -> owning accountId` from local storage for account-switch safety (Y6). */
    val entryAccountIds: Map<String, String?> = emptyMap()
)

/**
 * Deterministic synchronization plan produced by [SyncPlanner] (Y1-Y6).
 */
data class SyncPlan(
    val accountId: String,
    val actions: List<SyncPlanAction>
) {
    val remoteDeletions: List<SyncPlanAction.DeleteRemote>
        get() = actions.filterIsInstance<SyncPlanAction.DeleteRemote>()

    val remotePushes: List<SyncPlanAction.PushToRemote>
        get() = actions.filterIsInstance<SyncPlanAction.PushToRemote>()

    val localPulls: List<SyncPlanAction.PullFromRemote>
        get() = actions.filterIsInstance<SyncPlanAction.PullFromRemote>()

    val localDeletions: List<SyncPlanAction.DeleteLocal>
        get() = actions.filterIsInstance<SyncPlanAction.DeleteLocal>()

    val localCleanups: List<SyncPlanAction.MarkClean>
        get() = actions.filterIsInstance<SyncPlanAction.MarkClean>()

    val noOps: List<SyncPlanAction.NoOp>
        get() = actions.filterIsInstance<SyncPlanAction.NoOp>()
}

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
