package com.vndblite.dcj.data.remote.dto

import com.vndblite.dcj.domain.model.UserListPatch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject

/**
 * Standard request body for VNDB Kana `POST /kana/<endpoint>` queries (N3, N10).
 * All query members are optional so endpoints like `/kana/quote` can omit `filters` (N10).
 */
@Serializable
data class KanaQueryRequestDto(
    val filters: JsonArray? = null,
    val fields: String? = null,
    val sort: String? = null,
    val reverse: Boolean? = null,
    val results: Int? = null,
    val page: Int? = null,
    val user: String? = null
)

/**
 * Named, documented field-selection constants for VNDB Kana API v2 queries (A6, N3, N6, N9).
 * Replaces opaque `P1_FIELDS` / `P2A_FIELDS` / `P2B_FIELDS` / `P3_FIELDS` strings.
 */
object KanaFieldSets {
    /**
     * Lean fields for VN cards in lists, grids, carousels, and embedded quote/relation references (N9).
     * Omits `description` to minimize response payload size on large list queries.
     */
    const val VN_CARD_SUMMARY: String =
        "id, title, image{url, thumbnail, dims, sexual, violence}, rating, votecount, length, olang, released"

    /**
     * Summary fields including `description` for screens that display the synopsis (VN detail header, random VN preview) (N9).
     */
    const val VN_SUMMARY_WITH_DESCRIPTION: String =
        "$VN_CARD_SUMMARY, description"

    /**
     * Full extended metadata for the VN Detail screen, requesting `screenshots.url` and `screenshots.thumbnail`
     * directly from the API instead of hand-building URLs from screenshot IDs (N6).
     */
    const val VN_EXTENDED_DETAILS: String =
        "id, title, image{url, thumbnail, dims, sexual, violence}, rating, votecount, length, olang, released, " +
            "description, aliases, devstatus, length_minutes, languages, platforms, " +
            "tags{id, name, rating, spoiler}, developers{id, name}, " +
            "screenshots{id, url, thumbnail, dims, sexual, violence}, " +
            "extlinks{url, label, name, id}, relations{id, relation, relation_official}"

    /**
     * Minimal ID-only field for discovering the highest VN ID during random VN selection (F1).
     */
    const val VN_ID_ONLY: String = "id"

    /**
     * Release fields needed to compute minimum age ratings and machine-translated language flags (N2).
     */
    const val RELEASE_AGE_AND_MTL: String =
        "id, minage, languages{lang, mtl}"

    /**
     * Character fields for VN detail character grids and the Character Trait Explorer.
     */
    const val CHARACTER_FULL: String =
        "id, name, original, aliases, description, " +
            "image{url, thumbnail, dims, sexual, violence}, " +
            "blood_type, height, weight, bust, waist, hips, cup, age, birthday, sex, " +
            "vns{id, title, role, spoiler}, " +
            "traits{id, name, group_id, group_name, spoiler}"

    /**
     * Quote fields for `/kana/quote`, embedding only lean VN card metadata rather than a full VN description (N9).
     */
    const val QUOTE_WITH_LEAN_VN: String =
        "id, quote, score, character{id, name, original}, " +
            "vn{id, title, image{url, thumbnail, dims, sexual, violence}, rating, votecount, released, olang, length}"

    const val TAG_AUTOCOMPLETE: String = "id, name"

    const val PRODUCER_AUTOCOMPLETE: String = "id, name"

    const val TRAIT_AUTOCOMPLETE: String =
        "id, name, group_id, group_name, description, aliases, char_count"

    /**
     * Userlist fields for `POST /kana/ulist` (Section 3, N1, Y1-Y6).
     */
    const val USER_LIST_ENTRY: String =
        "id, added, lastmod, voted, vote, notes, started, finished, labels{id, label}, " +
            "vn{$VN_CARD_SUMMARY}"
}

/**
 * Serializes a [UserListPatch] to a JSON object for `PATCH /kana/ulist/<id>` (Section 3, Y2, Y3).
 *
 * - Uses `labels_set` and `labels_unset` instead of `labels` so custom labels (> 6) are preserved (Y2).
 * - Sends explicit `null` (`JsonNull`) when `vote`, `started`, or `finished` are cleared (Y3).
 * - Omits unchanged fields so partial updates only touch modified properties.
 */
object UserListPatchPayloadBuilder {
    fun toJsonObject(patch: UserListPatch): JsonObject = buildJsonObject {
        if (patch.includeVote) {
            put("vote", patch.vote?.let { JsonPrimitive(it) } ?: JsonNull)
        }
        if (patch.includeNotes) {
            put("notes", patch.notes?.let { JsonPrimitive(it) } ?: JsonNull)
        }
        if (patch.includeStarted) {
            put("started", patch.started?.let { JsonPrimitive(it.toString()) } ?: JsonNull)
        }
        if (patch.includeFinished) {
            put("finished", patch.finished?.let { JsonPrimitive(it.toString()) } ?: JsonNull)
        }
        if (patch.labelsSet.isNotEmpty()) {
            put(
                "labels_set",
                buildJsonArray {
                    patch.labelsSet.forEach { add(JsonPrimitive(it)) }
                }
            )
        }
        if (patch.labelsUnset.isNotEmpty()) {
            put(
                "labels_unset",
                buildJsonArray {
                    patch.labelsUnset.forEach { add(JsonPrimitive(it)) }
                }
            )
        }
    }
}
