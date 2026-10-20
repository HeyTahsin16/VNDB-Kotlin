package com.vndblite.dcj.data.remote.scraper

import com.vndblite.dcj.core.AppResult
import com.vndblite.dcj.domain.model.PaginatedList
import com.vndblite.dcj.domain.model.Review

/**
 * Abstraction for loading user reviews for a visual novel, isolated from the Kana JSON API
 * because VNDB Kana v2 does not expose a reviews endpoint (Section 3, N5).
 */
interface ReviewSource {
    /**
     * Fetches reviews for [vnId], paging up to a bounded cap and distinguishing
     * "no reviews" (`Success(emptyList())`) from network or HTML parse failures (`Failure(ApiError)`) (N2, N5).
     */
    suspend fun fetchReviewsForVn(vnId: String): AppResult<PaginatedList<Review>>

    /**
     * Fetches the full text of a truncated review (`w<id>`) from `vndb.org` (N5).
     */
    suspend fun fetchFullReviewText(reviewId: String): AppResult<String>
}
