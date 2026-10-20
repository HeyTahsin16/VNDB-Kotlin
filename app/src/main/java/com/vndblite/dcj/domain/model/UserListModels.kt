package com.vndblite.dcj.domain.model

import java.time.Instant
import java.time.LocalDate

/**
 * Remote userlist entry returned by `POST /kana/ulist` (Section 3, Y1-Y6).
 */
data class RemoteUserListItem(
    val vnId: String,
    val added: Instant? = null,
    val lastMod: Instant = Instant.EPOCH,
    val voted: Instant? = null,
    /** Vote in VNDB's 10..100 integer scale, or `null` if unvoted. */
    val vote: Int? = null,
    val notes: String? = null,
    val started: LocalDate? = null,
    val finished: LocalDate? = null,
    val labelIds: Set<Int> = emptySet(),
    val vnSummary: VnSummary? = null
) {
    /**
     * Derives the primary [CollectionStatus] deterministically from `labels[].id` (Y6).
     */
    val resolvedStatus: CollectionStatus
        get() = CollectionStatus.fromLabelIdsOrDefault(labelIds)
}

/**
 * Patch payload for `PATCH /kana/ulist/<id>` (Section 3, Y2, Y3).
 *
 * Uses `labels_set` and `labels_unset` so custom labels are never clobbered,
 * and flags which nullable fields should be serialized (including explicit `null` when cleared).
 */
data class UserListPatch(
    val vnId: String,
    val includeVote: Boolean = false,
    val vote: Int? = null,
    val includeNotes: Boolean = false,
    val notes: String? = null,
    val includeStarted: Boolean = false,
    val started: LocalDate? = null,
    val includeFinished: Boolean = false,
    val finished: LocalDate? = null,
    val labelsSet: List<Int> = emptyList(),
    val labelsUnset: List<Int> = emptyList()
)
