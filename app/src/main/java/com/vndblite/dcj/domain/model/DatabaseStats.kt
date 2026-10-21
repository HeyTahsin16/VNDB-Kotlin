package com.vndblite.dcj.domain.model

data class DatabaseStats(
    val cachedVnSummariesCount: Int = 0,
    val cachedVnDetailsCount: Int = 0,
    val collectionEntriesCount: Int = 0,
    val dirtySyncEntriesCount: Int = 0,
    val databaseSizeBytes: Long = 0L
)
