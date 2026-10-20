package com.vndblite.dcj.sync

import com.vndblite.dcj.data.remote.dto.UserListPatchPayloadBuilder
import com.vndblite.dcj.domain.model.CollectionEntry
import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.domain.model.RemoteUserListItem
import com.vndblite.dcj.domain.model.SyncState
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Test

class SyncPlannerTest {

    @Test
    fun `same-day local edit later in UTC beats earlier remote edit at second precision (Y1)`() {
        val morningUtc = Instant.parse("2026-03-15T09:15:00Z")
        val afternoonUtc = Instant.parse("2026-03-15T16:45:30Z")

        val local = CollectionEntry(
            vnId = "v17",
            title = "Ever17",
            status = CollectionStatus.FINISHED,
            vote = 95,
            lastModifiedAt = afternoonUtc,
            remoteLastMod = morningUtc,
            syncState = SyncState.DIRTY,
            lastSyncedStatus = CollectionStatus.PLAYING,
            lastSyncedVote = 80
        )
        val remote = RemoteUserListItem(
            vnId = "v17",
            lastMod = morningUtc,
            vote = 80,
            labelIds = setOf(CollectionStatus.PLAYING.labelId)
        )

        val plan = SyncPlanner.plan(
            SyncPlanInput(
                accountId = "u42",
                localEntries = listOf(local),
                remoteItems = listOf(remote)
            )
        )

        val action = assertIs<SyncPlanAction.PushToRemote>(plan.actions.single())
        assertEquals("v17", action.vnId)
        assertTrue(action.patch.includeVote)
        assertEquals(95, action.patch.vote)
        assertEquals(listOf(CollectionStatus.FINISHED.labelId), action.patch.labelsSet)
    }

    @Test
    fun `same-day remote edit later in UTC beats earlier local edit when both changed (Y1, Y5)`() {
        val baselineUtc = Instant.parse("2026-03-15T08:00:00Z")
        val localEditUtc = Instant.parse("2026-03-15T11:00:00Z")
        val remoteEditUtc = Instant.parse("2026-03-15T19:20:05Z")

        val local = CollectionEntry(
            vnId = "v17",
            title = "Ever17",
            status = CollectionStatus.STALLED,
            vote = 70,
            lastModifiedAt = localEditUtc,
            remoteLastMod = baselineUtc,
            syncState = SyncState.DIRTY
        )
        val remote = RemoteUserListItem(
            vnId = "v17",
            lastMod = remoteEditUtc,
            vote = 92,
            labelIds = setOf(CollectionStatus.FINISHED.labelId)
        )

        val plan = SyncPlanner.plan(
            SyncPlanInput(
                accountId = "u42",
                localEntries = listOf(local),
                remoteItems = listOf(remote)
            )
        )

        val action = assertIs<SyncPlanAction.PullFromRemote>(plan.actions.single())
        assertEquals(CollectionStatus.FINISHED, action.mergedEntry.status)
        assertEquals(92, action.mergedEntry.vote)
        assertEquals(SyncState.CLEAN, action.mergedEntry.syncState)
        assertEquals(remoteEditUtc, action.mergedEntry.remoteLastMod)
    }

    @Test
    fun `clearing vote and dates sends explicit null only for cleared fields and preserves custom labels (Y2, Y3)`() {
        val lastSync = Instant.parse("2026-03-10T12:00:00Z")
        val editTime = Instant.parse("2026-03-10T15:00:00Z")
        val existingFinishDate = LocalDate.of(2026, 2, 20)

        val local = CollectionEntry(
            vnId = "v92",
            title = "Muv-Luv",
            status = CollectionStatus.FINISHED,
            vote = null, // Cleared!
            notes = "Still great", // Unchanged!
            startedDate = null, // Cleared!
            finishedDate = existingFinishDate, // Unchanged!
            lastModifiedAt = editTime,
            remoteLastMod = lastSync,
            syncState = SyncState.DIRTY,
            lastSyncedStatus = CollectionStatus.FINISHED,
            lastSyncedVote = 88,
            lastSyncedNotes = "Still great",
            lastSyncedStartedDate = LocalDate.of(2026, 2, 1),
            lastSyncedFinishedDate = existingFinishDate
        )
        val remote = RemoteUserListItem(
            vnId = "v92",
            lastMod = lastSync,
            vote = 88,
            notes = "Still great",
            started = LocalDate.of(2026, 2, 1),
            finished = existingFinishDate,
            // Remote has Finished (2), virtual Voted (7), and custom user label (14)
            labelIds = setOf(2, 7, 14)
        )

        val patch = SyncPlanner.buildPatch(local = local, remote = remote)

        // Cleared fields are included with explicit null
        assertTrue(patch.includeVote)
        assertNull(patch.vote)
        assertTrue(patch.includeStarted)
        assertNull(patch.started)

        // Unchanged fields are omitted
        assertFalse(patch.includeFinished)
        assertFalse(patch.includeNotes)
        assertTrue(patch.labelsSet.isEmpty())
        assertTrue(patch.labelsUnset.isEmpty())

        // Verify serialized JSON contains explicit null for cleared fields and omits unchanged fields
        val json = UserListPatchPayloadBuilder.toJsonObject(patch)
        assertEquals(JsonNull, json["vote"])
        assertEquals(JsonNull, json["started"])
        assertFalse(json.containsKey("finished"))
        assertFalse(json.containsKey("notes"))
        assertFalse(json.containsKey("labels_set"))
        assertFalse(json.containsKey("labels_unset"))
    }

    @Test
    fun `changing status sets target label and unsets only the other five status labels 1 to 6 (Y2)`() {
        val local = CollectionEntry(
            vnId = "v11",
            status = CollectionStatus.PLAYING,
            lastModifiedAt = Instant.ofEpochSecond(2000L),
            remoteLastMod = Instant.ofEpochSecond(1000L),
            syncState = SyncState.DIRTY,
            lastSyncedStatus = CollectionStatus.WISHLIST
        )
        val remote = RemoteUserListItem(
            vnId = "v11",
            lastMod = Instant.ofEpochSecond(1000L),
            labelIds = setOf(5, 7, 42) // Wishlist (5), Voted (7), custom label (42)
        )

        val patch = SyncPlanner.buildPatch(local = local, remote = remote)
        assertEquals(listOf(1), patch.labelsSet)
        assertEquals(listOf(2, 3, 4, 5, 6), patch.labelsUnset)
        assertFalse(0 in patch.labelsSet || 0 in patch.labelsUnset)
        assertFalse(7 in patch.labelsSet || 7 in patch.labelsUnset)
        assertFalse(42 in patch.labelsUnset)

        val json = UserListPatchPayloadBuilder.toJsonObject(patch)
        assertEquals(JsonPrimitive(1), json["labels_set"]?.let { it as? kotlinx.serialization.json.JsonArray }?.single())
    }

    @Test
    fun `decision table handles deletion on either side and remote-only insertions (Y5)`() {
        val syncTime = Instant.ofEpochSecond(1_700_000_000L)

        // 1. Remote missing + local CLEAN -> DeleteLocal (deleted on vndb.org)
        val localCleanRemovedOnWeb = CollectionEntry(
            vnId = "v10",
            title = "Deleted On Web",
            status = CollectionStatus.FINISHED,
            lastModifiedAt = syncTime,
            remoteLastMod = syncTime,
            syncState = SyncState.CLEAN
        )
        // 2. Remote missing + local NEW -> PushToRemote
        val localNewAddedInApp = CollectionEntry(
            vnId = "v20",
            title = "Added In App",
            status = CollectionStatus.PLAYING,
            lastModifiedAt = syncTime,
            syncState = SyncState.NEW
        )
        // 3. Local PENDING_DELETE (even though still on remote) -> DeleteRemote (never re-imported)
        val remoteStillHasDeleted = RemoteUserListItem(
            vnId = "v30",
            lastMod = syncTime,
            labelIds = setOf(CollectionStatus.WISHLIST.labelId)
        )
        // 4. Remote only -> PullFromRemote (insert locally)
        val remoteOnlyItem = RemoteUserListItem(
            vnId = "v40",
            lastMod = syncTime,
            vote = 85,
            labelIds = setOf(CollectionStatus.WISHLIST.labelId, CollectionStatus.FINISHED.labelId)
        )

        val plan = SyncPlanner.plan(
            SyncPlanInput(
                accountId = "u42",
                localEntries = listOf(localCleanRemovedOnWeb, localNewAddedInApp),
                remoteItems = listOf(remoteStillHasDeleted, remoteOnlyItem),
                pendingRemoteDeleteIds = setOf("v30")
            )
        )

        val actionByVn = plan.actions.associateBy { it.vnId }
        val deleteLocal = assertIs<SyncPlanAction.DeleteLocal>(actionByVn["v10"])
        assertEquals(LocalDeletionReason.DELETED_ON_REMOTE, deleteLocal.reason)
        assertTrue(deleteLocal.countInReport)

        assertIs<SyncPlanAction.PushToRemote>(actionByVn["v20"])
        assertIs<SyncPlanAction.DeleteRemote>(actionByVn["v30"])

        val pulled = assertIs<SyncPlanAction.PullFromRemote>(actionByVn["v40"])
        // Priority order: FINISHED (2) beats WISHLIST (5) (Y6)
        assertEquals(CollectionStatus.FINISHED, pulled.mergedEntry.status)
        assertEquals(85, pulled.mergedEntry.vote)
    }

    @Test
    fun `switching accounts never replays foreign pending deletions or deletes foreign clean entries (Y6)`() {
        val syncTime = Instant.ofEpochSecond(1_700_000_000L)

        // Entry that was CLEAN under account u1, kept locally when switching to u2
        val keptFromUser1 = CollectionEntry(
            vnId = "v17",
            title = "Kept Entry",
            status = CollectionStatus.FINISHED,
            lastModifiedAt = syncTime,
            remoteLastMod = syncTime,
            syncState = SyncState.CLEAN
        )
        // Stale PENDING_DELETE tombstone belonging to u1
        val staleTombstoneFromUser1 = CollectionEntry(
            vnId = "v99",
            title = "Deleted By User 1",
            status = CollectionStatus.DROPPED,
            lastModifiedAt = syncTime,
            syncState = SyncState.PENDING_DELETE
        )

        val plan = SyncPlanner.plan(
            SyncPlanInput(
                accountId = "u2",
                localEntries = listOf(keptFromUser1, staleTombstoneFromUser1),
                remoteItems = emptyList(),
                pendingRemoteDeleteIds = emptySet(),
                entryAccountIds = mapOf("v17" to "u1", "v99" to "u1")
            )
        )

        val actionByVn = plan.actions.associateBy { it.vnId }
        // v17 is normalized to NEW for u2 and pushed rather than deleted locally!
        assertIs<SyncPlanAction.PushToRemote>(actionByVn["v17"])
        // v99 is cleaned up locally as a stale foreign tombstone and NEVER sent as DeleteRemote to u2!
        val staleCleanup = assertIs<SyncPlanAction.DeleteLocal>(actionByVn["v99"])
        assertEquals(LocalDeletionReason.STALE_FOREIGN_PENDING_DELETE, staleCleanup.reason)
        assertFalse(staleCleanup.countInReport)
        assertTrue(plan.remoteDeletions.isEmpty())
    }
}
