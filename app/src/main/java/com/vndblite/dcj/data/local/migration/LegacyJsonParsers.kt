package com.vndblite.dcj.data.local.migration

import com.vndblite.dcj.data.local.db.entity.CachedImageDto
import com.vndblite.dcj.data.local.db.entity.CachedProducerDto
import com.vndblite.dcj.data.local.db.entity.CachedRelationDto
import com.vndblite.dcj.data.local.db.entity.CachedTagDto
import com.vndblite.dcj.data.local.db.entity.CollectionEntryEntity
import com.vndblite.dcj.data.local.db.entity.QuoteCacheEntity
import com.vndblite.dcj.data.local.db.entity.SiteStatsCacheEntity
import com.vndblite.dcj.data.local.db.entity.VnCacheDetailsPayload
import com.vndblite.dcj.data.local.db.entity.VnCacheEntity
import com.vndblite.dcj.data.remote.dto.KanaJson
import com.vndblite.dcj.domain.model.CollectionEntry
import com.vndblite.dcj.domain.model.CollectionFilters
import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.domain.model.DevStatus
import com.vndblite.dcj.domain.model.Language
import com.vndblite.dcj.domain.model.Platform
import com.vndblite.dcj.domain.model.ReleaseDate
import com.vndblite.dcj.domain.model.SortField
import com.vndblite.dcj.domain.model.SortOption
import com.vndblite.dcj.domain.model.SyncState
import com.vndblite.dcj.domain.model.VnLengthBucket
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

internal data class LegacyUserIdentityParsed(
    val id: String,
    val username: String,
    val authToken: String,
    val permissions: Set<String>
)

internal data class LegacyRecordContext(
    val addedByAppIds: Set<String>,
    val toBeRemovedIds: Set<String>,
    val isUserSynced: Boolean,
    val accountId: String?,
    val nowEpochSeconds: Long
)

/**
 * Pure `kotlinx.serialization.json` parsers for legacy `VndbKotlinPreferences` blobs (S1, S5, S6, N3).
 */
internal object LegacyJsonParsers {

    private val authTokenRedactionRegex =
        Regex("(\"authToken\"\\s*:\\s*\")([^\"]*)(\")", RegexOption.IGNORE_CASE)

    fun redactSecrets(rawPayload: String): String =
        authTokenRedactionRegex.replace(rawPayload, "$1<REDACTED>$3")

    fun parseUserIdentity(rawJson: String): LegacyUserIdentityParsed {
        val obj = KanaJson.parseToJsonElement(rawJson).jsonObject
        val id = obj.optString("id").trim()
        val username = obj.optString("username").trim()
        val authToken = obj.optString("authToken").trim()
        require(id.isNotEmpty()) { "Legacy USER_IDENTITY is missing 'id'" }
        val perms = obj.optStringList("permissions").toSet()
        return LegacyUserIdentityParsed(
            id = id,
            username = username.ifEmpty { id },
            authToken = authToken,
            permissions = perms.ifEmpty { setOf("listread", "listwrite") }
        )
    }

    fun parseCollectionRecord(rawJson: String, ctx: LegacyRecordContext): CollectionEntryEntity {
        val obj = KanaJson.parseToJsonElement(rawJson).jsonObject
        val vnId = obj.optString("id").trim()
        require(vnId.isNotEmpty() && vnId.startsWith("v")) {
            "Legacy collection record has invalid or missing 'id': '$vnId'"
        }
        val status = CollectionStatus.fromStorageKeyOrDefault(obj.optString("status"))
        val vote = CollectionEntry.normalizeVote(obj.optInt("vote"))
        val notes = obj.optNullableString("notes")
        val startedDate = CollectionEntry.parseLocalDateOrNull(obj.optNullableString("started"))
        val finishedDate = CollectionEntry.parseLocalDateOrNull(obj.optNullableString("finished"))

        val lastModInstant = CollectionEntry.parseLastModInstant(
            raw = obj.optNullableString("lastmod"),
            fallback = Instant.ofEpochSecond(ctx.nowEpochSeconds)
        )
        val addedDate = CollectionEntry.parseLocalDateOrNull(obj.optNullableString("added"))
            ?: lastModInstant.atZone(ZoneOffset.UTC).toLocalDate()
        val syncState = when {
            vnId in ctx.toBeRemovedIds -> SyncState.PENDING_DELETE
            vnId in ctx.addedByAppIds || !ctx.isUserSynced -> SyncState.NEW
            else -> SyncState.CLEAN
        }
        val isClean = syncState == SyncState.CLEAN

        return CollectionEntryEntity(
            vnId = vnId,
            title = obj.optString("title").trim(),
            statusId = status.labelId,
            vote = vote,
            notes = notes,
            addedDate = addedDate.toString(),
            addedEpochSeconds = addedDate.atStartOfDay(ZoneOffset.UTC).toEpochSecond(),
            startedDate = startedDate?.toString(),
            finishedDate = finishedDate?.toString(),
            lastModifiedEpochSeconds = lastModInstant.epochSecond,
            remoteLastModEpochSeconds = if (isClean) lastModInstant.epochSecond else null,
            syncState = syncState.name,
            accountId = ctx.accountId,
            lastSyncedStatusId = if (isClean) status.labelId else null,
            lastSyncedVote = if (isClean) vote else null,
            lastSyncedStartedDate = if (isClean) startedDate?.toString() else null,
            lastSyncedFinishedDate = if (isClean) finishedDate?.toString() else null,
            lastSyncedNotes = if (isClean) notes else null
        )
    }

    fun parseVnCacheFromPhases(
        vnId: String,
        p1Raw: String?,
        p2Raw: String?,
        p3Raw: String?,
        nowEpochSeconds: Long
    ): VnCacheEntity {
        require(!p1Raw.isNullOrBlank()) { "Cannot migrate VN cache for $vnId without P01 payload" }
        val p1 = KanaJson.parseToJsonElement(p1Raw).jsonObject
        val parsedId = p1.optString("id").trim().ifEmpty { vnId }
        require(parsedId.startsWith("v")) { "Invalid VN ID '$parsedId' in P01 payload" }

        val title = p1.optString("title").trim().takeIf { it.isNotEmpty() && it != "Unknown" } ?: parsedId
        val imageDto = p1.optObject("image")?.let(::parseCachedImage)
        val released = ReleaseDate.parse(p1.optNullableString("released"))
        val p2 = p2Raw?.takeIf { it.isNotBlank() }?.let { KanaJson.parseToJsonElement(it).jsonObject }
        val p3 = p3Raw?.takeIf { it.isNotBlank() }?.let { KanaJson.parseToJsonElement(it).jsonObject }

        val aliases = p2?.optStringList("aliases").orEmpty()
        val minAges = p2?.optIntList("minage").orEmpty()
        val languages = p2?.optStringList("languages").orEmpty()
        val platforms = p2?.optStringList("platforms").orEmpty()
        val hasDetails = p2 != null || p3 != null
        val detailsJson = if (hasDetails) buildDetailsPayloadJson(p2, p3, aliases, minAges) else null

        return VnCacheEntity(
            vnId = parsedId,
            title = title,
            imageUrl = imageDto?.url,
            imageThumbnailUrl = imageDto?.thumbnailUrl,
            imageWidth = imageDto?.width,
            imageHeight = imageDto?.height,
            imageSexual = imageDto?.sexual ?: 0.0,
            imageViolence = imageDto?.violence ?: 0.0,
            rating = p1.optDouble("rating")?.takeIf { it > 0.0 },
            voteCount = p1.optInt("votecount") ?: 0,
            originalLanguage = p1.optNullableString("olang")?.takeUnless { it.equals("Unknown", ignoreCase = true) },
            lengthBucketCode = VnLengthBucket.fromApiCode(p1.optInt("length"))?.apiCode,
            releasedRaw = released?.toApiString(),
            releasedSortKey = released?.sortKey,
            description = p1.optNullableString("description")?.takeUnless { it == "--" },
            devStatusCode = DevStatus.fromApiCode(p2?.optInt("devstatus"))?.apiCode,
            lengthMinutes = p2?.optInt("length_minutes")?.takeIf { it > 0 },
            minAge = minAges.filter { it >= 0 }.maxOrNull(),
            languagesPipe = VnCacheEntity.encodePipeList(languages),
            platformsPipe = VnCacheEntity.encodePipeList(platforms),
            aliasesSearchText = aliases.joinToString("\n"),
            detailsPayloadJson = detailsJson,
            hasFullDetails = hasDetails,
            summaryFetchedAtEpochSeconds = nowEpochSeconds,
            detailsFetchedAtEpochSeconds = if (hasDetails) nowEpochSeconds else null,
            lastAccessedAtEpochSeconds = nowEpochSeconds
        )
    }

    private fun buildDetailsPayloadJson(
        p2: JsonObject?,
        p3: JsonObject?,
        aliases: List<String>,
        minAges: List<Int>
    ): String {
        val payload = VnCacheDetailsPayload(
            developers = parseLegacyDevelopers(p2?.get("developers")),
            aliases = aliases,
            minAges = minAges,
            tags = parseLegacyTags(p2?.get("tags")),
            screenshots = (p2?.get("screenshots") as? JsonArray)
                ?.mapNotNull { (it as? JsonObject)?.let(::parseCachedImage) }.orEmpty(),
            machineTranslatedLanguages = p2?.optStringList("mtl").orEmpty(),
            externalLinks = parseLegacyExtlinks(p3?.get("extlinks")),
            relations = parseLegacyRelations(p3?.get("relations")),
            releasesTruncated = false
        )
        return KanaJson.encodeToString(payload)
    }

    fun parseStringArray(rawJson: String): List<String> =
        KanaJson.parseToJsonElement(rawJson).jsonArray
            .mapNotNull { (it as? JsonPrimitive)?.content?.trim() }
            .filter { it.isNotEmpty() }

    fun parseQuotesArray(rawJson: String, nowEpochSeconds: Long): List<QuoteCacheEntity> =
        KanaJson.parseToJsonElement(rawJson).jsonArray.mapIndexedNotNull { index, element ->
            val obj = element as? JsonObject ?: return@mapIndexedNotNull null
            val id = obj.optString("id").trim()
            val text = obj.optString("quote").trim()
            if (id.isEmpty() || text.isEmpty()) return@mapIndexedNotNull null
            val vnObj = obj.optObject("vn")
            val vnImg = vnObj?.optObject("image")?.let(::parseCachedImage)
            val charObj = obj.optObject("character")
            QuoteCacheEntity(
                quoteId = id,
                text = text,
                score = obj.optInt("score") ?: 0,
                vnId = vnObj?.optNullableString("id"),
                vnTitle = vnObj?.optNullableString("title"),
                vnImageUrl = vnImg?.url,
                vnImageThumbnailUrl = vnImg?.thumbnailUrl,
                vnImageSexual = vnImg?.sexual ?: 0.0,
                vnImageViolence = vnImg?.violence ?: 0.0,
                vnRating = vnObj?.optDouble("rating")?.takeIf { it > 0.0 },
                vnVoteCount = vnObj?.optInt("votecount") ?: 0,
                vnOriginalLanguage = vnObj?.optNullableString("olang")
                    ?.takeUnless { it.equals("Unknown", ignoreCase = true) },
                vnLengthBucketCode = VnLengthBucket.fromApiCode(vnObj?.optInt("length"))?.apiCode,
                vnReleasedRaw = ReleaseDate.parse(vnObj?.optNullableString("released"))?.toApiString(),
                characterId = charObj?.optNullableString("id"),
                characterName = charObj?.optNullableString("name"),
                characterOriginalName = charObj?.optNullableString("original"),
                displayOrder = index,
                fetchedAtEpochSeconds = nowEpochSeconds
            )
        }

    fun parseSiteStats(rawJson: String, nowEpochSeconds: Long): SiteStatsCacheEntity? {
        val obj = KanaJson.parseToJsonElement(rawJson).jsonObject
        val chars = obj.optInt("chars") ?: return null
        val releases = obj.optInt("releases") ?: return null
        val vn = obj.optInt("vn") ?: return null
        val staff = obj.optInt("staff") ?: return null
        val tags = obj.optInt("tags") ?: return null
        val producers = obj.optInt("producers") ?: return null
        val traits = obj.optInt("traits") ?: return null
        if (chars <= 0 && vn <= 0) return null
        return SiteStatsCacheEntity(
            chars = chars,
            releases = releases,
            vn = vn,
            staff = staff,
            tags = tags,
            producers = producers,
            traits = traits,
            fetchedAtEpochSeconds = nowEpochSeconds
        )
    }

    fun parseCollectionFilters(rawJson: String): CollectionFilters {
        val obj = KanaJson.parseToJsonElement(rawJson).jsonObject
        return CollectionFilters(
            searchQuery = obj.optString("search"),
            minAge = obj.optInt("minage")?.takeIf { it > 0 },
            languages = obj.optStringList("lang").mapNotNull(Language::fromCode),
            devStatuses = obj.optIntList("devstatus").mapNotNull(DevStatus::fromApiCode),
            originalLanguages = obj.optStringList("olang").mapNotNull(Language::fromCode),
            platforms = obj.optStringList("platform").mapNotNull(Platform::fromCode)
        )
    }

    fun parseCollectionSort(rawJson: String): SortOption {
        val obj = KanaJson.parseToJsonElement(rawJson).jsonObject
        val field = SortField.fromCode(obj.optString("sort"), default = SortField.TITLE)
        val reverse = obj.optBoolean("reverse") ?: false
        return SortOption(field = field, reverse = reverse)
    }

    private fun parseCachedImage(obj: JsonObject): CachedImageDto? {
        val url = obj.optString("url").trim()
        if (url.isEmpty()) return null
        val dimsList = obj.optIntList("dims")
        return CachedImageDto(
            url = url,
            thumbnailUrl = obj.optNullableString("thumbnail")?.takeIf { it.isNotBlank() } ?: url,
            width = dimsList.getOrNull(0)?.takeIf { it > 0 },
            height = dimsList.getOrNull(1)?.takeIf { it > 0 },
            sexual = obj.optDouble("sexual") ?: 0.0,
            violence = obj.optDouble("violence") ?: 0.0
        )
    }

    private fun parseLegacyDevelopers(element: JsonElement?): List<CachedProducerDto> =
        (element as? JsonArray)?.mapIndexedNotNull { idx, item ->
            when (item) {
                is JsonObject -> item.optString("name").trim().takeIf { it.isNotEmpty() }?.let { name ->
                    CachedProducerDto(id = item.optString("id").trim().ifEmpty { "p_legacy_$idx" }, name = name)
                }
                is JsonPrimitive -> item.content.trim().takeIf { it.isNotEmpty() }?.let { name ->
                    CachedProducerDto(id = "p_legacy_$idx", name = name)
                }
                else -> null
            }
        }.orEmpty()

    private fun parseLegacyTags(element: JsonElement?): List<CachedTagDto> =
        (element as? JsonArray)?.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            val id = obj.optString("id").trim()
            val name = obj.optString("name").trim()
            if (id.isEmpty() || name.isEmpty()) return@mapNotNull null
            CachedTagDto(id = id, name = name, rating = obj.optDouble("rating") ?: 0.0, spoilerLevel = obj.optInt("spoiler") ?: 0)
        }.orEmpty()

    private fun parseLegacyExtlinks(element: JsonElement?): List<String> =
        (element as? JsonArray)?.mapNotNull { item ->
            when (item) {
                is JsonObject -> item.optString("url").trim().takeIf { it.isNotEmpty() }
                is JsonPrimitive -> item.content.trim().takeIf { it.isNotEmpty() }
                else -> null
            }
        }.orEmpty()

    private fun parseLegacyRelations(element: JsonElement?): List<CachedRelationDto> =
        (element as? JsonArray)?.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            val id = obj.optString("id").trim()
            val rel = obj.optString("relation").trim()
            if (id.isEmpty() || rel.isEmpty()) return@mapNotNull null
            CachedRelationDto(id = id, rawRelationCode = rel, isOfficial = obj.optBoolean("relation_official") ?: false)
        }.orEmpty()
}

private fun JsonObject.optString(key: String): String =
    when (val elem = this[key]) {
        is JsonNull, null -> ""
        is JsonPrimitive -> elem.content
        else -> ""
    }

private fun JsonObject.optNullableString(key: String): String? =
    optString(key).trim().takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }

private fun JsonObject.optInt(key: String): Int? = (this[key] as? JsonPrimitive)?.intOrNull

private fun JsonObject.optDouble(key: String): Double? =
    (this[key] as? JsonPrimitive)?.doubleOrNull?.takeUnless { it.isNaN() }

private fun JsonObject.optBoolean(key: String): Boolean? = (this[key] as? JsonPrimitive)?.booleanOrNull

private fun JsonObject.optObject(key: String): JsonObject? = this[key] as? JsonObject

private fun JsonObject.optStringList(key: String): List<String> =
    (this[key] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.content?.trim() }?.filter { it.isNotEmpty() }.orEmpty()

private fun JsonObject.optIntList(key: String): List<Int> =
    (this[key] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.intOrNull }.orEmpty()
