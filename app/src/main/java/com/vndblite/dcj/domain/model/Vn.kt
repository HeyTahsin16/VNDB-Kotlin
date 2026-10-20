package com.vndblite.dcj.domain.model

/**
 * Core visual novel summary used across lists, grids, carousels, quotes, and detail headers.
 * Replaces `VnDataPhase01` and eliminates string/number sentinels (`"Unknown"`, `"--"`, `0.0`) (A6, A7, A8).
 */
data class VnSummary(
    val id: String,
    val title: String,
    val image: VnImage? = null,
    val rating: Double? = null,
    val voteCount: Int = 0,
    val originalLanguage: String? = null,
    val lengthBucket: VnLengthBucket? = null,
    val released: ReleaseDate? = null,
    val description: String? = null
)
