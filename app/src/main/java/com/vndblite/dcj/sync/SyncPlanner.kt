package com.vndblite.dcj.sync

import com.vndblite.dcj.domain.model.CollectionEntry
import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.domain.model.RemoteUserListItem
import com.vndblite.dcj.domain.model.SyncState
import com.vndblite.dcj.domain.model.UserListPatch
import java.time.Instant
import java.time.ZoneOffset

/**
 * Pure, stateless, unit-testable synchronization planner implementing the VNDB two-way sync
 * decision table and minimal `PATCH /ulist/<id>` payload diffing (Y1-Y6).
 *
 * ### Conflict & Decision Table (Y1, Y5, Y6)
 * 1. **Pending Remote Delete (`vnId in pendingRemoteDeleteIds` or local `PENDING_DELETE` for active account):**
 *    -> [SyncPlanAction.DeleteRemote]. Never re-imported from `remoteItems` during the same run (Y4).
 * 2. **Remote Missing + Local `CLEAN` (for active account):**
 *    -> [SyncPlanAction.DeleteLocal] (deleted on `vndb.org`, Y5).
 * 3. **Remote Missing + Local `NEW` or `DIRTY`:**
 *    -> [SyncPlanAction.PushToRemote] (created/updated locally, Y5).
 * 4. **Local Missing + Remote Present (and not queued for deletion):**
 *    -> [SyncPlanAction.PullFromRemote] (insert locally, Y5).
 * 5. **Both Present:**
 *    - If local is `CLEAN`: pull if `remote.lastMod > local.remoteLastMod` or fields differ; else [SyncPlanAction.NoOp].
 *    - If local is `DIRTY` or `NEW`:
 *      - If remote has not changed since `local.remoteLastMod` (`remote.lastMod <= local.remoteLastMod`): local wins -> push.
 *      - If both changed since last sync: compare `local.lastModifiedAt.epochSecond` vs `remote.lastMod.epochSecond` in UTC (Y1).
 *        **Tie rule (Y5):** when `local.lastModifiedAt.epochSecond == remote.lastMod.epochSecond`, the local `DIRTY`/`NEW`
 *        edit wins so an explicit on-device modification made in the same second is never silently overwritten.
 */
object SyncPlanner {

    fun plan(input: SyncPlanInput): SyncPlan {
        val cleanAccountId = input.accountId.trim()
        require(cleanAccountId.isNotEmpty()) { "accountId must not be blank" }

        val localById = input.localEntries.associateBy { it.vnId }
        val remoteById = input.remoteItems.associateBy { it.vnId }
        val allVnIds = (localById.keys + remoteById.keys + input.pendingRemoteDeleteIds).sorted()

        val actions = allVnIds.map { vnId ->
            planSingleVn(
                vnId = vnId,
                accountId = cleanAccountId,
                rawLocal = localById[vnId],
                ownerAccountId = input.entryAccountIds[vnId],
                isQueuedForRemoteDelete = vnId in input.pendingRemoteDeleteIds,
                remote = remoteById[vnId]
            )
        }
        return SyncPlan(accountId = cleanAccountId, actions = actions)
    }

    private fun planSingleVn(
        vnId: String,
        accountId: String,
        rawLocal: CollectionEntry?,
        ownerAccountId: String?,
        isQueuedForRemoteDelete: Boolean,
        remote: RemoteUserListItem?
    ): SyncPlanAction {
        val belongsToDifferentAccount = ownerAccountId != null && ownerAccountId != accountId
        if (isQueuedForRemoteDelete) {
            return SyncPlanAction.DeleteRemote(vnId)
        }
        if (rawLocal?.syncState == SyncState.PENDING_DELETE) {
            return if (belongsToDifferentAccount) {
                if (remote != null) {
                    SyncPlanAction.PullFromRemote(
                        vnId = vnId,
                        remoteItem = remote,
                        existingLocal = null,
                        mergedEntry = mergeRemoteItem(remote, existingLocal = null)
                    )
                } else {
                    SyncPlanAction.DeleteLocal(
                        vnId = vnId,
                        reason = LocalDeletionReason.STALE_FOREIGN_PENDING_DELETE
                    )
                }
            } else {
                SyncPlanAction.DeleteRemote(vnId)
            }
        }

        val local = rawLocal?.let { normalizeForAccount(it, belongsToDifferentAccount) }
        return when {
            local != null && remote == null -> planLocalOnly(local)
            local == null && remote != null -> SyncPlanAction.PullFromRemote(
                vnId = vnId,
                remoteItem = remote,
                existingLocal = null,
                mergedEntry = mergeRemoteItem(remote, existingLocal = null)
            )
            local != null && remote != null -> planBothPresent(local, remote)
            else -> SyncPlanAction.NoOp(vnId)
        }
    }

    /**
     * When switching accounts while keeping the local collection, strips the previous account's
     * sync baseline so foreign `CLEAN` entries are not mistaken for remote deletions (Y6).
     */
    private fun normalizeForAccount(
        entry: CollectionEntry,
        belongsToDifferentAccount: Boolean
    ): CollectionEntry {
        if (!belongsToDifferentAccount) return entry
        return entry.copy(
            remoteLastMod = null,
            syncState = SyncState.NEW,
            lastSyncedStatus = null,
            lastSyncedVote = null,
            lastSyncedStartedDate = null,
            lastSyncedFinishedDate = null,
            lastSyncedNotes = null
        )
    }

    private fun planLocalOnly(local: CollectionEntry): SyncPlanAction {
        return when (local.syncState) {
            SyncState.CLEAN -> SyncPlanAction.DeleteLocal(
                vnId = local.vnId,
                reason = LocalDeletionReason.DELETED_ON_REMOTE
            )
            SyncState.NEW,
            SyncState.DIRTY -> SyncPlanAction.PushToRemote(
                vnId = local.vnId,
                entry = local,
                patch = buildPatch(local = local, remote = null),
                remoteLastMod = null
            )
            SyncState.PENDING_DELETE -> SyncPlanAction.DeleteRemote(local.vnId)
        }
    }

    private fun planBothPresent(
        local: CollectionEntry,
        remote: RemoteUserListItem
    ): SyncPlanAction {
        val localSec = local.lastModifiedAt.epochSecond
        val remoteSec = remote.lastMod.epochSecond
        val lastSyncedRemoteSec = local.remoteLastMod?.epochSecond

        if (local.syncState == SyncState.CLEAN) {
            val remoteAdvanced = lastSyncedRemoteSec == null || remoteSec > lastSyncedRemoteSec
            return if (remoteAdvanced || !isEquivalent(local, remote)) {
                SyncPlanAction.PullFromRemote(
                    vnId = local.vnId,
                    remoteItem = remote,
                    existingLocal = local,
                    mergedEntry = mergeRemoteItem(remote, existingLocal = local)
                )
            } else {
                SyncPlanAction.NoOp(local.vnId)
            }
        }

        // Local is DIRTY or NEW: check if remote also changed since last sync
        val remoteUnchangedSinceLastSync =
            lastSyncedRemoteSec != null && remoteSec <= lastSyncedRemoteSec
        val localWinsConflict = remoteUnchangedSinceLastSync || localSec >= remoteSec

        return if (localWinsConflict) {
            val patch = buildPatch(local = local, remote = remote)
            if (patch.hasChanges()) {
                SyncPlanAction.PushToRemote(
                    vnId = local.vnId,
                    entry = local,
                    patch = patch,
                    remoteLastMod = remote.lastMod
                )
            } else {
                SyncPlanAction.MarkClean(
                    vnId = local.vnId,
                    cleanEntry = markPushedClean(
                        local = local,
                        syncedAt = remote.lastMod,
                        knownRemoteLastMod = remote.lastMod
                    )
                )
            }
        } else {
            SyncPlanAction.PullFromRemote(
                vnId = local.vnId,
                remoteItem = remote,
                existingLocal = local,
                mergedEntry = mergeRemoteItem(remote, existingLocal = local)
            )
        }
    }

    /**
     * Builds a minimal [UserListPatch] containing only modified fields, explicit `null` for cleared
     * fields, and `labels_set` / `labels_unset` restricted to status IDs `1..6` (Y2, Y3).
     */
    fun buildPatch(
        local: CollectionEntry,
        remote: RemoteUserListItem? = null
    ): UserListPatch {
        val localVote = CollectionEntry.normalizeVote(local.vote)
        val localNotes = local.notes?.takeIf { it.isNotEmpty() }
        val lastVote = CollectionEntry.normalizeVote(local.lastSyncedVote)
        val lastNotes = local.lastSyncedNotes?.takeIf { it.isNotEmpty() }

        val statusChanged = shouldUpdateStatusLabels(local, remote)
        val voteChanged = hasFieldChanged(localVote, CollectionEntry.normalizeVote(remote?.vote), lastVote, remote != null)
        val notesChanged = hasFieldChanged(localNotes, remote?.notes?.takeIf { it.isNotEmpty() }, lastNotes, remote != null)
        val startedChanged = hasFieldChanged(local.startedDate, remote?.started, local.lastSyncedStartedDate, remote != null)
        val finishedChanged = hasFieldChanged(local.finishedDate, remote?.finished, local.lastSyncedFinishedDate, remote != null)

        return UserListPatch(
            vnId = local.vnId,
            includeVote = voteChanged,
            vote = if (voteChanged) localVote else null,
            includeNotes = notesChanged,
            notes = if (notesChanged) localNotes else null,
            includeStarted = startedChanged,
            started = if (startedChanged) local.startedDate else null,
            includeFinished = finishedChanged,
            finished = if (finishedChanged) local.finishedDate else null,
            labelsSet = if (statusChanged) local.labelsToSet() else emptyList(),
            labelsUnset = if (statusChanged) local.labelsToUnset() else emptyList()
        )
    }

    private fun shouldUpdateStatusLabels(
        local: CollectionEntry,
        remote: RemoteUserListItem?
    ): Boolean {
        if (remote == null) {
            return local.lastSyncedStatus == null || local.status != local.lastSyncedStatus || local.syncState == SyncState.NEW
        }
        val remoteStatusLabels = remote.labelIds.intersect(CollectionStatus.allStatusLabelIds)
        val exactSingleStatusOnRemote =
            remoteStatusLabels.size == 1 && local.status.labelId in remoteStatusLabels
        val changedSinceLastSync =
            local.lastSyncedStatus != null && local.status != local.lastSyncedStatus
        return !exactSingleStatusOnRemote || changedSinceLastSync
    }

    private fun <T> hasFieldChanged(
        localValue: T?,
        remoteValue: T?,
        lastSyncedValue: T?,
        hasRemoteEntry: Boolean
    ): Boolean {
        return if (hasRemoteEntry) {
            localValue != remoteValue || (lastSyncedValue != null && localValue != lastSyncedValue)
        } else {
            localValue != null || lastSyncedValue != null
        }
    }

    /**
     * Merges a [RemoteUserListItem] into a clean local [CollectionEntry], deriving status
     * deterministically from `labels[].id` and recording `lastSynced*` baselines (Y1, Y3, Y6).
     */
    fun mergeRemoteItem(
        remote: RemoteUserListItem,
        existingLocal: CollectionEntry? = null
    ): CollectionEntry {
        val status = remote.resolvedStatus
        val normalizedVote = CollectionEntry.normalizeVote(remote.vote)
        val normalizedNotes = remote.notes?.takeIf { it.isNotEmpty() }
        val resolvedTitle = remote.vnSummary?.title?.takeIf { it.isNotBlank() }
            ?: existingLocal?.title.orEmpty()
        val resolvedAddedDate = remote.added?.atZone(ZoneOffset.UTC)?.toLocalDate()
            ?: existingLocal?.addedDate
        val remoteModSeconds = Instant.ofEpochSecond(remote.lastMod.epochSecond)

        return CollectionEntry(
            vnId = remote.vnId,
            title = resolvedTitle,
            status = status,
            vote = normalizedVote,
            notes = normalizedNotes,
            addedDate = resolvedAddedDate,
            startedDate = remote.started,
            finishedDate = remote.finished,
            lastModifiedAt = remoteModSeconds,
            remoteLastMod = remoteModSeconds,
            syncState = SyncState.CLEAN,
            lastSyncedStatus = status,
            lastSyncedVote = normalizedVote,
            lastSyncedStartedDate = remote.started,
            lastSyncedFinishedDate = remote.finished,
            lastSyncedNotes = normalizedNotes
        )
    }

    /**
     * Transitions a pushed local [CollectionEntry] to [SyncState.CLEAN] and snapshots its
     * synced field values so subsequent edits or field clears compute exact diffs (Y1, Y3).
     */
    fun markPushedClean(
        local: CollectionEntry,
        syncedAt: Instant,
        knownRemoteLastMod: Instant? = null
    ): CollectionEntry {
        val normalizedVote = CollectionEntry.normalizeVote(local.vote)
        val normalizedNotes = local.notes?.takeIf { it.isNotEmpty() }
        val remoteModSeconds = maxOf(
            local.lastModifiedAt.epochSecond,
            syncedAt.epochSecond,
            knownRemoteLastMod?.epochSecond ?: 0L
        )
        return local.copy(
            vote = normalizedVote,
            notes = normalizedNotes,
            lastModifiedAt = Instant.ofEpochSecond(local.lastModifiedAt.epochSecond),
            remoteLastMod = Instant.ofEpochSecond(remoteModSeconds),
            syncState = SyncState.CLEAN,
            lastSyncedStatus = local.status,
            lastSyncedVote = normalizedVote,
            lastSyncedStartedDate = local.startedDate,
            lastSyncedFinishedDate = local.finishedDate,
            lastSyncedNotes = normalizedNotes
        )
    }

    /**
     * Checks whether [local] and [remote] have identical userlist data and clean status labels.
     */
    fun isEquivalent(local: CollectionEntry, remote: RemoteUserListItem): Boolean {
        val remoteStatusLabels = remote.labelIds.intersect(CollectionStatus.allStatusLabelIds)
        val hasExactStatusLabel =
            remoteStatusLabels.size == 1 && local.status.labelId in remoteStatusLabels
        val localVote = CollectionEntry.normalizeVote(local.vote)
        val remoteVote = CollectionEntry.normalizeVote(remote.vote)
        val localNotes = local.notes?.takeIf { it.isNotEmpty() }
        val remoteNotes = remote.notes?.takeIf { it.isNotEmpty() }

        return hasExactStatusLabel &&
            local.status == remote.resolvedStatus &&
            localVote == remoteVote &&
            localNotes == remoteNotes &&
            local.startedDate == remote.started &&
            local.finishedDate == remote.finished
    }
}

fun UserListPatch.hasChanges(): Boolean =
    includeVote ||
        includeNotes ||
        includeStarted ||
        includeFinished ||
        labelsSet.isNotEmpty() ||
        labelsUnset.isNotEmpty()
