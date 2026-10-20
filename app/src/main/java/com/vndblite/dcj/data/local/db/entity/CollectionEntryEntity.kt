package com.vndblite.dcj.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vndblite.dcj.domain.model.CollectionEntry
import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.domain.model.SyncState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Room entity representing a user collection entry in `collection_entries` (S1, S2, Y1-Y6).
 */
@Entity(
    tableName = "collection_entries",
    indices = [
        Index(value = ["statusId"]),
        Index(value = ["syncState"]),
        Index(value = ["lastModifiedEpochSeconds"]),
        Index(value = ["addedEpochSeconds"]),
        Index(value = ["startedDate"]),
        Index(value = ["vote"]),
        Index(value = ["accountId"])
    ]
)
data class CollectionEntryEntity(
    @PrimaryKey
    val vnId: String,
    val title: String = "",
    val statusId: Int = CollectionStatus.DEFAULT_UNKNOWN.labelId,
    /** Vote in 10..100 or `null` if unvoted (A7, Y3). */
    val vote: Int? = null,
    val notes: String? = null,
    /** ISO `"YYYY-MM-DD"` date when the entry was added, or `null`. */
    val addedDate: String? = null,
    /** Added timestamp in UTC epoch seconds for SQL sorting by `added` (S1). */
    val addedEpochSeconds: Long = 0L,
    /** ISO `"YYYY-MM-DD"` start date, or `null` (A7, F8, Y3). */
    val startedDate: String? = null,
    /** ISO `"YYYY-MM-DD"` finish date, or `null` (A7, F8, Y3). */
    val finishedDate: String? = null,
    /** Second-precision UTC modification timestamp (Y1). */
    val lastModifiedEpochSeconds: Long = 0L,
    /** Remote `lastmod` (epoch seconds) at the last successful sync, or `null` if never synced (Y1). */
    val remoteLastModEpochSeconds: Long? = null,
    /** Name of [SyncState] (`CLEAN`, `NEW`, `DIRTY`, `PENDING_DELETE`) (Y1). */
    val syncState: String = SyncState.NEW.name,
    /** Account ID that owns the sync state for this entry, or `null` if local-only (Y6). */
    val accountId: String? = null,
    /** Snapshot of the last synced status ID (1..6) for minimal PATCH calculation (Y2, Y3). */
    val lastSyncedStatusId: Int? = null,
    /** Snapshot of the last synced vote (10..100 or null) for minimal PATCH calculation (Y3). */
    val lastSyncedVote: Int? = null,
    /** Snapshot of the last synced start date (`"YYYY-MM-DD"` or null) (Y3). */
    val lastSyncedStartedDate: String? = null,
    /** Snapshot of the last synced finish date (`"YYYY-MM-DD"` or null) (Y3). */
    val lastSyncedFinishedDate: String? = null,
    /** Snapshot of the last synced notes (Y3). */
    val lastSyncedNotes: String? = null
) {
    fun toDomain(fallbackTitle: String = ""): CollectionEntry {
        val resolvedTitle = title.ifBlank { fallbackTitle }
        val parsedAddedDate = CollectionEntry.parseLocalDateOrNull(addedDate)
            ?: addedEpochSeconds.takeIf { it > 0L }?.let {
                Instant.ofEpochSecond(it).atZone(ZoneOffset.UTC).toLocalDate()
            }
        val parsedSyncState = runCatching { SyncState.valueOf(syncState) }
            .getOrDefault(SyncState.NEW)
        return CollectionEntry(
            vnId = vnId,
            title = resolvedTitle,
            status = CollectionStatus.fromLabelId(statusId) ?: CollectionStatus.DEFAULT_UNKNOWN,
            vote = CollectionEntry.normalizeVote(vote),
            notes = notes?.takeIf { it.isNotEmpty() },
            addedDate = parsedAddedDate,
            startedDate = CollectionEntry.parseLocalDateOrNull(startedDate),
            finishedDate = CollectionEntry.parseLocalDateOrNull(finishedDate),
            lastModifiedAt = Instant.ofEpochSecond(lastModifiedEpochSeconds),
            remoteLastMod = remoteLastModEpochSeconds?.let(Instant::ofEpochSecond),
            syncState = parsedSyncState,
            lastSyncedStatus = lastSyncedStatusId?.let(CollectionStatus::fromLabelId),
            lastSyncedVote = CollectionEntry.normalizeVote(lastSyncedVote),
            lastSyncedStartedDate = CollectionEntry.parseLocalDateOrNull(lastSyncedStartedDate),
            lastSyncedFinishedDate = CollectionEntry.parseLocalDateOrNull(lastSyncedFinishedDate),
            lastSyncedNotes = lastSyncedNotes
        )
    }

    companion object {
        fun fromDomain(entry: CollectionEntry, accountId: String? = null): CollectionEntryEntity {
            val resolvedAddedDate: LocalDate? = entry.addedDate
                ?: entry.lastModifiedAt.takeIf { it.epochSecond > 0L }
                    ?.atZone(ZoneOffset.UTC)
                    ?.toLocalDate()
            val addedEpoch = resolvedAddedDate
                ?.atStartOfDay(ZoneOffset.UTC)
                ?.toEpochSecond()
                ?: entry.lastModifiedAt.epochSecond
            return CollectionEntryEntity(
                vnId = entry.vnId,
                title = entry.title,
                statusId = entry.status.labelId,
                vote = CollectionEntry.normalizeVote(entry.vote),
                notes = entry.notes,
                addedDate = resolvedAddedDate?.toString(),
                addedEpochSeconds = addedEpoch,
                startedDate = entry.startedDate?.toString(),
                finishedDate = entry.finishedDate?.toString(),
                lastModifiedEpochSeconds = entry.lastModifiedAt.epochSecond,
                remoteLastModEpochSeconds = entry.remoteLastMod?.epochSecond,
                syncState = entry.syncState.name,
                accountId = accountId,
                lastSyncedStatusId = entry.lastSyncedStatus?.labelId,
                lastSyncedVote = CollectionEntry.normalizeVote(entry.lastSyncedVote),
                lastSyncedStartedDate = entry.lastSyncedStartedDate?.toString(),
                lastSyncedFinishedDate = entry.lastSyncedFinishedDate?.toString(),
                lastSyncedNotes = entry.lastSyncedNotes
            )
        }
    }
}
