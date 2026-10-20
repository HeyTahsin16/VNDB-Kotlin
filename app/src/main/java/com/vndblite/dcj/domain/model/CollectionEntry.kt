package com.vndblite.dcj.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeParseException

/**
 * Per-entry synchronization state (Y1).
 */
enum class SyncState {
    /** Entry is in sync with the remote VNDB userlist as of [CollectionEntry.remoteLastMod]. */
    CLEAN,
    /** Newly created locally and never pushed to the remote userlist. */
    NEW,
    /** Previously synced entry with local modifications waiting to be pushed. */
    DIRTY,
    /** Marked for remote deletion on the next sync run. */
    PENDING_DELETE
}

/**
 * Local and cloud-synced user collection entry for a visual novel (A7, Y1, Y2, Y3).
 *
 * Uses nullable types (`vote: Int?`, `started: LocalDate?`, `finished: LocalDate?`)
 * instead of `0` or `"Unknown"` sentinels, and second-precision UTC [Instant] timestamps.
 */
data class CollectionEntry(
    val vnId: String,
    val title: String = "",
    val status: CollectionStatus = CollectionStatus.DEFAULT_UNKNOWN,
    /** Vote in VNDB's 10..100 integer scale, or `null` if unvoted (A7, Y3). */
    val vote: Int? = null,
    val notes: String? = null,
    val addedDate: LocalDate? = null,
    val startedDate: LocalDate? = null,
    val finishedDate: LocalDate? = null,
    /** UTC timestamp (second precision) of the latest local or remote modification (Y1). */
    val lastModifiedAt: Instant = Instant.EPOCH,
    /** UTC timestamp (second precision) reported by VNDB at the last successful sync, or `null` if never synced (Y1). */
    val remoteLastMod: Instant? = null,
    val syncState: SyncState = SyncState.NEW,
    /** Snapshot of the last synced status so PATCH only updates labels when status changed (Y2, Y3). */
    val lastSyncedStatus: CollectionStatus? = null,
    /** Snapshot of the last synced vote (10..100 or null) so cleared votes send explicit null (Y3). */
    val lastSyncedVote: Int? = null,
    /** Snapshot of the last synced start date so cleared dates send explicit null (Y3). */
    val lastSyncedStartedDate: LocalDate? = null,
    /** Snapshot of the last synced finish date so cleared dates send explicit null (Y3). */
    val lastSyncedFinishedDate: LocalDate? = null,
    /** Snapshot of the last synced notes so cleared notes send explicit empty/null (Y3). */
    val lastSyncedNotes: String? = null
) {
    val hasVote: Boolean
        get() = vote != null && vote in MIN_VOTE..MAX_VOTE

    val voteTenPoint: Double?
        get() = vote?.takeIf { it in MIN_VOTE..MAX_VOTE }?.let { it / 10.0 }

    /**
     * Label IDs to add in `PATCH /ulist/<id>` (`labels_set`) without touching custom labels (Y2).
     */
    fun labelsToSet(): List<Int> = listOf(status.labelId)

    /**
     * Other status label IDs (from 1..6) to remove in `PATCH /ulist/<id>` (`labels_unset`)
     * so custom labels (> 7) and virtual label 7 are never overwritten (Y2).
     */
    fun labelsToUnset(): List<Int> =
        CollectionStatus.allStatusLabelIds.filter { it != status.labelId }

    companion object {
        const val MIN_VOTE: Int = 10
        const val MAX_VOTE: Int = 100

        fun normalizeVote(rawVote: Int?): Int? =
            rawVote?.takeIf { it in MIN_VOTE..MAX_VOTE }

        fun parseLocalDateOrNull(raw: String?): LocalDate? {
            val trimmed = raw?.trim().orEmpty()
            if (trimmed.isEmpty() ||
                trimmed.equals("unknown", ignoreCase = true) ||
                trimmed.equals("null", ignoreCase = true) ||
                trimmed == "--"
            ) {
                return null
            }
            return try {
                LocalDate.parse(trimmed.take(10))
            } catch (_: DateTimeParseException) {
                null
            }
        }

        fun parseLastModInstant(raw: String?, fallback: Instant = Instant.EPOCH): Instant {
            val trimmed = raw?.trim().orEmpty()
            if (trimmed.isEmpty() || trimmed.equals("null", ignoreCase = true)) return fallback
            trimmed.toLongOrNull()?.let { epochSeconds ->
                if (epochSeconds > 0L) return Instant.ofEpochSecond(epochSeconds)
            }
            return try {
                Instant.parse(trimmed)
            } catch (_: DateTimeParseException) {
                parseLocalDateOrNull(trimmed)
                    ?.atStartOfDay(ZoneOffset.UTC)
                    ?.toInstant()
                    ?: fallback
            }
        }
    }
}

/**
 * Combined projection of a user's [CollectionEntry] together with its cached [VnSummary] metadata
 * and optional [VnDetails].
 */
data class CollectionItemWithVn(
    val entry: CollectionEntry,
    val vn: VnSummary,
    val details: VnDetails? = null
)
