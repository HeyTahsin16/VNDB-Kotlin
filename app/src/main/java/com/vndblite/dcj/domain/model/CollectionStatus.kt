package com.vndblite.dcj.domain.model

/**
 * Pure domain enum representing VNDB predefined userlist status labels (IDs 1..6) (A6, A7, A8, Y2, Y6).
 * Contains no Compose `Color` or asset paths; presentation mappings live in `ui/`.
 *
 * Label 7 ("Voted") is virtual on VNDB and derived automatically from a non-null vote.
 */
enum class CollectionStatus(
    val labelId: Int,
    val storageKey: String
) {
    PLAYING(1, "playing"),
    FINISHED(2, "finished"),
    STALLED(3, "stalled"),
    DROPPED(4, "dropped"),
    WISHLIST(5, "wishlist"),
    BLACKLIST(6, "blacklist");

    companion object {
        /**
         * Single canonical fallback status when a record has an unrecognized or missing status label (A7).
         */
        val DEFAULT_UNKNOWN: CollectionStatus = WISHLIST

        /**
         * Priority order used when a remote VNDB `/ulist` entry has multiple status labels (1..6) applied (Y6):
         * Playing > Finished > Stalled > Dropped > Wishlist > Blacklist.
         */
        val priorityOrder: List<CollectionStatus> = listOf(
            PLAYING,
            FINISHED,
            STALLED,
            DROPPED,
            WISHLIST,
            BLACKLIST
        )

        val allStatusLabelIds: Set<Int> = entries.map { it.labelId }.toSet()

        fun fromLabelId(id: Int?): CollectionStatus? =
            entries.firstOrNull { it.labelId == id }

        fun fromStorageKey(key: String?): CollectionStatus? {
            if (key.isNullOrBlank()) return null
            val trimmed = key.trim()
            return entries.firstOrNull {
                it.storageKey.equals(trimmed, ignoreCase = true) ||
                    it.name.equals(trimmed, ignoreCase = true)
            }
        }

        fun fromStorageKeyOrDefault(key: String?): CollectionStatus =
            fromStorageKey(key) ?: DEFAULT_UNKNOWN

        /**
         * Deterministically resolves a single [CollectionStatus] from a set of VNDB label IDs (Y6).
         * Returns `null` if none of the predefined status labels (1..6) are present.
         */
        fun fromLabelIds(ids: Iterable<Int>): CollectionStatus? {
            val idSet = ids.toSet()
            return priorityOrder.firstOrNull { it.labelId in idSet }
        }

        fun fromLabelIdsOrDefault(ids: Iterable<Int>): CollectionStatus =
            fromLabelIds(ids) ?: DEFAULT_UNKNOWN
    }
}
