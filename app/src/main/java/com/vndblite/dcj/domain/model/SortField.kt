package com.vndblite.dcj.domain.model

enum class SortOrder(val isReverse: Boolean) {
    ASCENDING(isReverse = false),
    DESCENDING(isReverse = true);

    companion object {
        fun fromReverse(reverse: Boolean): SortOrder =
            if (reverse) DESCENDING else ASCENDING
    }
}

/**
 * Unified sort field enum for remote VN search and local collection sorting (A6, A7, A8).
 */
enum class SortField(
    val code: String,
    val supportsRemoteVnSort: Boolean,
    val supportsLocalSort: Boolean
) {
    SEARCH_RANK("searchrank", supportsRemoteVnSort = true, supportsLocalSort = false),
    ID("id", supportsRemoteVnSort = true, supportsLocalSort = true),
    TITLE("title", supportsRemoteVnSort = true, supportsLocalSort = true),
    ADDED("added", supportsRemoteVnSort = false, supportsLocalSort = true),
    STARTED("started", supportsRemoteVnSort = false, supportsLocalSort = true),
    FINISHED("finished", supportsRemoteVnSort = false, supportsLocalSort = true),
    LAST_MODIFIED("lastmod", supportsRemoteVnSort = false, supportsLocalSort = true),
    LENGTH_MINUTES("length_minutes", supportsRemoteVnSort = false, supportsLocalSort = true),
    RATING("rating", supportsRemoteVnSort = true, supportsLocalSort = true),
    RELEASED("released", supportsRemoteVnSort = true, supportsLocalSort = true),
    VOTE_COUNT("votecount", supportsRemoteVnSort = true, supportsLocalSort = true),
    USER_VOTE("vote", supportsRemoteVnSort = false, supportsLocalSort = true),
    COLLECTION("collection", supportsRemoteVnSort = false, supportsLocalSort = false);

    companion object {
        val remoteSortableFields: List<SortField> = listOf(
            SEARCH_RANK,
            TITLE,
            RATING,
            RELEASED,
            VOTE_COUNT
        )

        val localSortableFields: List<SortField> = listOf(
            TITLE,
            ADDED,
            STARTED,
            LENGTH_MINUTES,
            RATING,
            RELEASED,
            VOTE_COUNT,
            USER_VOTE
        )

        fun fromCode(code: String?, default: SortField = SEARCH_RANK): SortField {
            if (code.isNullOrBlank()) return default
            return entries.firstOrNull { it.code.equals(code.trim(), ignoreCase = true) } ?: default
        }
    }
}

data class SortOption(
    val field: SortField = SortField.SEARCH_RANK,
    val reverse: Boolean = true
) {
    companion object {
        val DEFAULT_REMOTE = SortOption(field = SortField.SEARCH_RANK, reverse = true)
        val DEFAULT_LOCAL = SortOption(field = SortField.TITLE, reverse = false)
    }
}
