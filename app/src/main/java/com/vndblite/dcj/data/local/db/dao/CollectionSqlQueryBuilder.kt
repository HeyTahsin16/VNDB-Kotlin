package com.vndblite.dcj.data.local.db.dao

import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.vndblite.dcj.domain.model.CollectionFilters
import com.vndblite.dcj.domain.model.SortField
import com.vndblite.dcj.domain.model.SortOption
import com.vndblite.dcj.domain.model.SyncState

/**
 * Builds parameterized SQLite queries for filtering and sorting the user's collection in SQL (S1).
 *
 * All user-supplied values are bound as positional `?` arguments in [SimpleSQLiteQuery] to prevent
 * SQL injection while using SQLite indexes on `collection_entries` and `vn_cache`.
 */
object CollectionSqlQueryBuilder {

    fun buildFilteredCollectionQuery(
        filters: CollectionFilters = CollectionFilters(),
        sort: SortOption = SortOption.DEFAULT_LOCAL
    ): SupportSQLiteQuery {
        val args = mutableListOf<Any>()
        val whereClauses = mutableListOf<String>()

        whereClauses += "c.syncState != ?"
        args += SyncState.PENDING_DELETE.name

        filters.status?.let { status ->
            whereClauses += "c.statusId = ?"
            args += status.labelId
        }

        val trimmedQuery = filters.searchQuery.trim()
        if (trimmedQuery.isNotEmpty()) {
            val likePattern = "%$trimmedQuery%"
            whereClauses += "(COALESCE(NULLIF(v.title, ''), c.title) LIKE ? COLLATE NOCASE " +
                "OR v.aliasesSearchText LIKE ? COLLATE NOCASE " +
                "OR c.vnId LIKE ? COLLATE NOCASE)"
            args += likePattern
            args += likePattern
            args += likePattern
        }

        val minAgeFilter = filters.minAge
        if (minAgeFilter != null && minAgeFilter > 0) {
            whereClauses += "v.minAge IS NOT NULL AND v.minAge >= ?"
            args += minAgeFilter
        }

        if (filters.originalLanguages.isNotEmpty()) {
            val placeholders = filters.originalLanguages.joinToString(", ") { "?" }
            whereClauses += "LOWER(v.originalLanguage) IN ($placeholders)"
            filters.originalLanguages.forEach { lang ->
                args += lang.code.lowercase()
            }
        }

        if (filters.devStatuses.isNotEmpty()) {
            val placeholders = filters.devStatuses.joinToString(", ") { "?" }
            whereClauses += "v.devStatusCode IN ($placeholders)"
            filters.devStatuses.forEach { status ->
                args += status.apiCode
            }
        }

        if (filters.languages.isNotEmpty()) {
            val langClauses = filters.languages.map { lang ->
                args += "%|${lang.code.lowercase()}|%"
                "v.languagesPipe LIKE ?"
            }
            whereClauses += "(${langClauses.joinToString(" OR ")})"
        }

        if (filters.platforms.isNotEmpty()) {
            val platClauses = filters.platforms.map { platform ->
                args += "%|${platform.code.lowercase()}|%"
                "v.platformsPipe LIKE ?"
            }
            whereClauses += "(${platClauses.joinToString(" OR ")})"
        }

        val whereSql = whereClauses.joinToString(" AND ")
        val orderSql = buildOrderByClause(sort)

        val sql = """
            SELECT c.*
            FROM collection_entries AS c
            LEFT JOIN vn_cache AS v ON c.vnId = v.vnId
            WHERE $whereSql
            ORDER BY $orderSql
        """.trimIndent()

        return SimpleSQLiteQuery(sql, args.toTypedArray())
    }

    private fun buildOrderByClause(sort: SortOption): String {
        val dir = if (sort.reverse) "DESC" else "ASC"
        val titleTieBreaker = "COALESCE(NULLIF(v.title, ''), c.title, c.vnId) COLLATE NOCASE ASC, c.vnId ASC"
        return when (sort.field) {
            SortField.TITLE, SortField.SEARCH_RANK, SortField.COLLECTION -> {
                "COALESCE(NULLIF(v.title, ''), c.title, c.vnId) COLLATE NOCASE $dir, c.vnId ASC"
            }
            SortField.ID -> {
                "CAST(SUBSTR(c.vnId, 2) AS INTEGER) $dir, c.vnId $dir"
            }
            SortField.ADDED -> {
                "c.addedEpochSeconds $dir, $titleTieBreaker"
            }
            SortField.LAST_MODIFIED -> {
                "c.lastModifiedEpochSeconds $dir, $titleTieBreaker"
            }
            SortField.STARTED -> {
                "CASE WHEN c.startedDate IS NULL OR c.startedDate = '' THEN 1 ELSE 0 END ASC, " +
                    "c.startedDate $dir, $titleTieBreaker"
            }
            SortField.FINISHED -> {
                "CASE WHEN c.finishedDate IS NULL OR c.finishedDate = '' THEN 1 ELSE 0 END ASC, " +
                    "c.finishedDate $dir, $titleTieBreaker"
            }
            SortField.LENGTH_MINUTES -> {
                val lengthExpr = "COALESCE(v.lengthMinutes, v.lengthBucketCode * 600)"
                "CASE WHEN $lengthExpr IS NULL THEN 1 ELSE 0 END ASC, " +
                    "$lengthExpr $dir, $titleTieBreaker"
            }
            SortField.RATING -> {
                "CASE WHEN v.rating IS NULL OR v.rating <= 0 THEN 1 ELSE 0 END ASC, " +
                    "v.rating $dir, $titleTieBreaker"
            }
            SortField.RELEASED -> {
                "CASE WHEN v.releasedSortKey IS NULL OR v.releasedSortKey = '' THEN 1 ELSE 0 END ASC, " +
                    "v.releasedSortKey $dir, $titleTieBreaker"
            }
            SortField.VOTE_COUNT -> {
                "COALESCE(v.voteCount, 0) $dir, $titleTieBreaker"
            }
            SortField.USER_VOTE -> {
                "CASE WHEN c.vote IS NULL OR c.vote <= 0 THEN 1 ELSE 0 END ASC, " +
                    "c.vote $dir, $titleTieBreaker"
            }
        }
    }
}
