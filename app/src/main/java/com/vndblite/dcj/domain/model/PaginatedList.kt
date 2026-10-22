package com.vndblite.dcj.domain.model

data class PaginatedList<out T>(
    val items: List<T>,
    val hasMore: Boolean,
    val page: Int = 1
) {
    val isTruncated: Boolean
        get() = hasMore
}
