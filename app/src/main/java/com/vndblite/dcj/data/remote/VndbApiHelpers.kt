package com.vndblite.dcj.data.remote

import com.vndblite.dcj.core.ApiError
import com.vndblite.dcj.core.AppLog
import com.vndblite.dcj.core.AppResult
import com.vndblite.dcj.data.remote.dto.KanaFieldSets
import com.vndblite.dcj.data.remote.dto.KanaQueryRequestDto
import com.vndblite.dcj.data.remote.dto.ReleaseDto
import com.vndblite.dcj.data.remote.dto.VnDto
import com.vndblite.dcj.data.remote.dto.summarizeReleases
import com.vndblite.dcj.data.remote.filter.KanaFilter
import com.vndblite.dcj.data.remote.filter.KanaFilters
import com.vndblite.dcj.data.remote.http.KanaHttpExecutor
import com.vndblite.dcj.domain.model.VnSummary
import kotlin.random.Random

/**
 * Internal orchestration helpers for multi-step Kana API flows (full VN detail aggregation
 * and 2-step random VN rolling) keeping [VndbApiImpl] concise and under 400 lines (N2, N5, N6, F1).
 */
internal object VndbApiHelpers {

    private const val TAG = "VndbApiHelpers"

    suspend fun fetchVnFullDetails(
        vnId: String,
        executor: KanaHttpExecutor,
        fetchSummariesByIds: suspend (List<String>, Boolean) -> AppResult<List<VnSummary>>
    ): AppResult<VnFullDetailPayload> {
        val cleanId = vnId.trim()
        if (cleanId.isEmpty()) return AppResult.Failure(ApiError.BadRequest())

        val vnReq = KanaQueryRequestDto(
            filters = KanaFilter.eq("id", cleanId).toJsonArray(),
            fields = KanaFieldSets.VN_EXTENDED_DETAILS,
            results = 1,
            page = 1
        )
        val vnPage = when (val res = executor.postPage("vn", vnReq, VnDto.serializer())) {
            is AppResult.Failure -> return res
            is AppResult.Success -> res.data
        }
        val vnDto = vnPage.results.firstOrNull()
            ?: return AppResult.Failure(ApiError.NotFound())

        val releasesPageResult = executor.fetchAllPages(
            endpoint = "release",
            baseRequest = KanaQueryRequestDto(
                filters = KanaFilters.forVnReleases(cleanId).toJsonArray(),
                fields = KanaFieldSets.RELEASE_AGE_AND_MTL,
                results = VndbApiImpl.MAX_PAGE_SIZE
            ),
            itemSerializer = ReleaseDto.serializer()
        )
        val (mtlLangs, minAges, releasesTruncated) = when (releasesPageResult) {
            is AppResult.Success -> {
                val (mtl, ages) = summarizeReleases(releasesPageResult.data.items)
                Triple(mtl, ages, releasesPageResult.data.hasMore)
            }
            is AppResult.Failure -> {
                AppLog.w(TAG, "Release metadata fetch failed for $cleanId: ${releasesPageResult.error}")
                Triple(emptyList(), emptyList(), false)
            }
        }

        val relationIds = vnDto.relations.orEmpty().map { it.id.trim() }.filter { it.isNotEmpty() }.distinct()
        val relatedSummariesById = if (relationIds.isNotEmpty()) {
            when (val relSummaries = fetchSummariesByIds(relationIds, false)) {
                is AppResult.Success -> relSummaries.data.associateBy { it.id }
                is AppResult.Failure -> emptyMap()
            }
        } else {
            emptyMap()
        }

        return AppResult.Success(
            VnFullDetailPayload(
                summary = vnDto.toSummary(),
                details = vnDto.toDetails(
                    minAges = minAges,
                    machineTranslatedLanguages = mtlLangs,
                    relatedSummariesById = relatedSummariesById,
                    releasesTruncated = releasesTruncated
                )
            )
        )
    }

    suspend fun fetchRandomVn(
        extraFilterClauses: List<KanaFilter>,
        executor: KanaHttpExecutor,
        randomGenerator: Random
    ): AppResult<VnSummary> {
        val baseClauses = KanaFilters.qualityRandomVnClauses(extraFilterClauses)
        val maxIdRequest = KanaQueryRequestDto(
            filters = KanaFilter.and(baseClauses).toJsonArray(),
            fields = KanaFieldSets.VN_ID_ONLY,
            sort = "id",
            reverse = true,
            results = 1,
            page = 1
        )
        val maxIdPage = when (val res = executor.postPage("vn", maxIdRequest, VnDto.serializer())) {
            is AppResult.Failure -> return res
            is AppResult.Success -> res.data
        }
        val maxVnIdNumeric = maxIdPage.results.firstOrNull()
            ?.id
            ?.removePrefix("v")
            ?.toIntOrNull()
            ?.takeIf { it >= 1 }
            ?: return AppResult.Failure(ApiError.NotFound())

        val pickedNumber = randomGenerator.nextInt(1, maxVnIdNumeric + 1)
        val targetVnId = "v$pickedNumber"

        val gteRequest = KanaQueryRequestDto(
            filters = KanaFilter.and(baseClauses + KanaFilter.gte("id", targetVnId)).toJsonArray(),
            fields = KanaFieldSets.VN_SUMMARY_WITH_DESCRIPTION,
            sort = "id",
            reverse = false,
            results = 1,
            page = 1
        )
        return when (val gteRes = executor.postPage("vn", gteRequest, VnDto.serializer())) {
            is AppResult.Failure -> gteRes
            is AppResult.Success -> {
                val found = gteRes.data.results.firstOrNull()
                if (found != null) {
                    AppResult.Success(found.toSummary())
                } else {
                    val lteRequest = gteRequest.copy(
                        filters = KanaFilter.and(baseClauses + KanaFilter.lte("id", targetVnId)).toJsonArray(),
                        reverse = true
                    )
                    when (val fallbackRes = executor.postPage("vn", lteRequest, VnDto.serializer())) {
                        is AppResult.Failure -> fallbackRes
                        is AppResult.Success -> {
                            val fallback = fallbackRes.data.results.firstOrNull()
                            if (fallback != null) {
                                AppResult.Success(fallback.toSummary())
                            } else {
                                AppResult.Failure(ApiError.NotFound())
                            }
                        }
                    }
                }
            }
        }
    }
}
