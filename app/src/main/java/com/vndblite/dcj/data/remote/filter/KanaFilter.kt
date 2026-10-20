package com.vndblite.dcj.data.remote.filter

import com.vndblite.dcj.domain.model.AndOr
import com.vndblite.dcj.domain.model.CharacterSearchFilters
import com.vndblite.dcj.domain.model.ReleaseWindow
import com.vndblite.dcj.domain.model.VnSearchFilters
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray

/**
 * Type-safe representation of VNDB Kana API v2 filter expressions (N3, N8).
 * Replaces hand-built `org.json.JSONArray` chains.
 */
sealed interface KanaFilter {
    fun toJsonArray(): JsonArray

    sealed interface Value {
        fun toJsonElement(): JsonElement

        data class Text(val value: String) : Value {
            override fun toJsonElement(): JsonElement = JsonPrimitive(value)
        }

        data class Number(val value: Int) : Value {
            override fun toJsonElement(): JsonElement = JsonPrimitive(value)
        }

        data class Nested(val filter: KanaFilter) : Value {
            override fun toJsonElement(): JsonElement = filter.toJsonArray()
        }

        data object Null : Value {
            override fun toJsonElement(): JsonElement = JsonNull
        }
    }

    data class Predicate(
        val field: String,
        val operator: String,
        val value: Value
    ) : KanaFilter {
        override fun toJsonArray(): JsonArray = buildJsonArray {
            add(JsonPrimitive(field))
            add(JsonPrimitive(operator))
            add(value.toJsonElement())
        }
    }

    data class Composite(
        val operator: AndOr,
        val clauses: List<KanaFilter>
    ) : KanaFilter {
        override fun toJsonArray(): JsonArray = buildJsonArray {
            add(JsonPrimitive(operator.apiToken))
            clauses.forEach { add(it.toJsonArray()) }
        }
    }

    companion object {
        fun eq(field: String, value: String): Predicate =
            Predicate(field, "=", Value.Text(value))

        fun eq(field: String, value: Int): Predicate =
            Predicate(field, "=", Value.Number(value))

        fun neq(field: String, value: String): Predicate =
            Predicate(field, "!=", Value.Text(value))

        fun gte(field: String, value: String): Predicate =
            Predicate(field, ">=", Value.Text(value))

        fun gte(field: String, value: Int): Predicate =
            Predicate(field, ">=", Value.Number(value))

        fun lte(field: String, value: String): Predicate =
            Predicate(field, "<=", Value.Text(value))

        fun gt(field: String, value: String): Predicate =
            Predicate(field, ">", Value.Text(value))

        fun nested(field: String, nestedFilter: KanaFilter): Predicate =
            Predicate(field, "=", Value.Nested(nestedFilter))

        fun and(clauses: List<KanaFilter>): Composite =
            Composite(AndOr.AND, clauses)

        fun or(clauses: List<KanaFilter>): Composite =
            Composite(AndOr.OR, clauses)
    }
}

/**
 * Typed filter builders for `/kana/vn`, `/kana/character`, `/kana/release`, and `/kana/quote` (N1, N3, N8, F1).
 */
object KanaFilters {
    const val MAX_BATCH_IDS: Int = 100
    const val TODAY_DATE_TOKEN: String = "today"
    const val TBA_DATE_TOKEN: String = "tba"

    /**
     * Quality filter constants used when selecting a random VN (F1).
     */
    const val RANDOM_VN_MIN_RATING: Int = 75
    const val RANDOM_VN_MIN_VOTE_COUNT: Int = 25

    /**
     * Builds a single `["or", ["id", "=", "v1"], ...]` filter for up to [MAX_BATCH_IDS] IDs (Section 3, N1).
     */
    fun byIds(ids: List<String>): KanaFilter {
        val cleaned = ids.map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(MAX_BATCH_IDS)
        require(cleaned.isNotEmpty()) { "ids must not be empty" }
        if (cleaned.size == 1) {
            return KanaFilter.eq("id", cleaned.first())
        }
        return KanaFilter.or(cleaned.map { id -> KanaFilter.eq("id", id) })
    }

    /**
     * Builds the VN search filter expression from [VnSearchFilters] (N3, N8).
     * Uses the API's `"today"` token instead of computing dates from the device clock (N8).
     */
    fun forVnSearch(filters: VnSearchFilters): KanaFilter {
        val clauses = mutableListOf<KanaFilter>()
        clauses.add(KanaFilter.eq("search", filters.searchQuery.trim()))

        when (filters.releaseWindow) {
            ReleaseWindow.LATEST -> {
                clauses.add(KanaFilter.lte("released", TODAY_DATE_TOKEN))
                clauses.add(KanaFilter.neq("released", TBA_DATE_TOKEN))
            }
            ReleaseWindow.UPCOMING -> {
                clauses.add(KanaFilter.gt("released", TODAY_DATE_TOKEN))
                clauses.add(KanaFilter.neq("released", TBA_DATE_TOKEN))
            }
            ReleaseWindow.ALL -> Unit
        }

        filters.languages.forEach { lang ->
            clauses.add(KanaFilter.eq("lang", lang.code))
        }
        filters.devStatuses.forEach { status ->
            clauses.add(KanaFilter.eq("devstatus", status.apiCode))
        }
        filters.originalLanguages.forEach { olang ->
            clauses.add(KanaFilter.eq("olang", olang.code))
        }
        filters.platforms.forEach { platform ->
            clauses.add(KanaFilter.eq("platform", platform.code))
        }
        filters.tags.forEach { tag ->
            clauses.add(KanaFilter.eq("tag", tag.id))
        }
        filters.developers.forEach { dev ->
            clauses.add(KanaFilter.nested("developer", KanaFilter.eq("id", dev.id)))
        }

        if (filters.characterRoles.size == 1) {
            clauses.add(
                KanaFilter.nested(
                    "character",
                    KanaFilter.eq("role", filters.characterRoles.first().apiCode)
                )
            )
        } else if (filters.characterRoles.size > 1) {
            clauses.add(
                KanaFilter.nested(
                    "character",
                    KanaFilter.Composite(
                        filters.characterRoleAndOr,
                        filters.characterRoles.map { KanaFilter.eq("role", it.apiCode) }
                    )
                )
            )
        }

        if (filters.characterSexes.size == 1) {
            clauses.add(
                KanaFilter.nested(
                    "character",
                    KanaFilter.eq("sex", filters.characterSexes.first().apiCode)
                )
            )
        } else if (filters.characterSexes.size > 1) {
            clauses.add(
                KanaFilter.nested(
                    "character",
                    KanaFilter.Composite(
                        filters.characterSexAndOr,
                        filters.characterSexes.map { KanaFilter.eq("sex", it.apiCode) }
                    )
                )
            )
        }

        if (filters.characterTraits.size == 1) {
            clauses.add(
                KanaFilter.nested(
                    "character",
                    KanaFilter.eq("trait", filters.characterTraits.first().id)
                )
            )
        } else if (filters.characterTraits.size > 1) {
            clauses.add(
                KanaFilter.nested(
                    "character",
                    KanaFilter.Composite(
                        filters.traitAndOr,
                        filters.characterTraits.map { KanaFilter.eq("trait", it.id) }
                    )
                )
            )
        }

        return KanaFilter.Composite(filters.andOr, clauses)
    }

    /**
     * Builds the `/kana/character` filter expression from [CharacterSearchFilters] (N3).
     */
    fun forCharacterSearch(filters: CharacterSearchFilters): KanaFilter {
        val clauses = mutableListOf<KanaFilter>()
        val trimmedQuery = filters.searchQuery.trim()
        if (trimmedQuery.isNotEmpty()) {
            clauses.add(KanaFilter.eq("search", trimmedQuery))
        }

        if (filters.traits.size == 1) {
            clauses.add(KanaFilter.eq("trait", filters.traits.first().id))
        } else if (filters.traits.size > 1) {
            clauses.add(
                KanaFilter.Composite(
                    filters.traitAndOr,
                    filters.traits.map { KanaFilter.eq("trait", it.id) }
                )
            )
        }

        if (filters.roles.size == 1) {
            clauses.add(KanaFilter.eq("role", filters.roles.first().apiCode))
        } else if (filters.roles.size > 1) {
            clauses.add(
                KanaFilter.Composite(
                    filters.roleAndOr,
                    filters.roles.map { KanaFilter.eq("role", it.apiCode) }
                )
            )
        }

        if (filters.sexes.size == 1) {
            clauses.add(KanaFilter.eq("sex", filters.sexes.first().apiCode))
        } else if (filters.sexes.size > 1) {
            clauses.add(
                KanaFilter.Composite(
                    filters.sexAndOr,
                    filters.sexes.map { KanaFilter.eq("sex", it.apiCode) }
                )
            )
        }

        if (clauses.isEmpty()) {
            clauses.add(KanaFilter.eq("search", ""))
        }
        return if (clauses.size == 1) clauses.first() else KanaFilter.and(clauses)
    }

    fun forVnCharacters(vnId: String): KanaFilter =
        KanaFilter.nested("vn", KanaFilter.eq("id", vnId))

    fun forVnReleases(vnId: String): KanaFilter =
        KanaFilter.nested("vn", KanaFilter.eq("id", vnId))

    fun forVnQuotes(vnId: String): KanaFilter =
        KanaFilter.nested("vn", KanaFilter.eq("id", vnId))

    fun forRandomQuote(): KanaFilter =
        KanaFilter.eq("random", 1)

    fun qualityRandomVnClauses(extraClauses: List<KanaFilter> = emptyList()): List<KanaFilter> =
        listOf(
            KanaFilter.gte("rating", RANDOM_VN_MIN_RATING),
            KanaFilter.gte("votecount", RANDOM_VN_MIN_VOTE_COUNT)
        ) + extraClauses
}
