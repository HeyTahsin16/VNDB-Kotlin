package com.vndblite.dcj.domain.model

import java.time.LocalDate

/**
 * Pure domain model of a user review scraped from `vndb.org` (A6, A8, N5).
 */
data class Review(
    val id: String,
    val vnId: String = "",
    val username: String,
    val userId: String? = null,
    val reviewType: String = "Short",
    val date: LocalDate? = null,
    val rawDate: String? = null,
    val ratingText: String? = null,
    val text: String,
    val pointsText: String? = null,
    val commentsCountText: String? = null,
    val hasReadMore: Boolean = false
)
