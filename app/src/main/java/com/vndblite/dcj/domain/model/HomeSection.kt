package com.vndblite.dcj.domain.model

/**
 * Pure domain representation of the sections displayed on the Home screen (A6, A8, F2).
 */
enum class HomeSection(
    val id: Int,
    val sortField: SortField,
    val reverseSort: Boolean,
    val devStatusFilter: DevStatus? = null,
    val releaseWindow: ReleaseWindow = ReleaseWindow.ALL
) {
    COLLECTION(
        id = 1,
        sortField = SortField.COLLECTION,
        reverseSort = true
    ),
    TOP_RATED(
        id = 2,
        sortField = SortField.RATING,
        reverseSort = true
    ),
    TOP_VOTED(
        id = 3,
        sortField = SortField.VOTE_COUNT,
        reverseSort = true
    ),
    POPULAR_ONGOING(
        id = 4,
        sortField = SortField.VOTE_COUNT,
        reverseSort = true,
        devStatusFilter = DevStatus.IN_DEVELOPMENT
    ),
    LATEST_RELEASES(
        id = 5,
        sortField = SortField.RELEASED,
        reverseSort = true,
        releaseWindow = ReleaseWindow.LATEST
    ),
    UPCOMING_RELEASES(
        id = 6,
        sortField = SortField.RELEASED,
        reverseSort = false,
        releaseWindow = ReleaseWindow.UPCOMING
    );

    companion object {
        fun fromId(id: Int): HomeSection? = entries.firstOrNull { it.id == id }
    }
}
