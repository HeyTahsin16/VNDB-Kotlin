package com.vndblite.dcj.domain.model

import java.time.Instant

/**
 * Live or cached VNDB database statistics returned by `GET /kana/stats` (F6).
 * Never populated with hardcoded fake numbers.
 */
data class SiteStats(
    val chars: Int,
    val releases: Int,
    val vn: Int,
    val staff: Int,
    val tags: Int,
    val producers: Int,
    val traits: Int,
    val fetchedAt: Instant = Instant.EPOCH
)
