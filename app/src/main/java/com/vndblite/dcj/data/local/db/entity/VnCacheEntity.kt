package com.vndblite.dcj.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vndblite.dcj.data.remote.dto.KanaJson
import com.vndblite.dcj.domain.model.DevStatus
import com.vndblite.dcj.domain.model.ImageDimensions
import com.vndblite.dcj.domain.model.Producer
import com.vndblite.dcj.domain.model.ReleaseDate
import com.vndblite.dcj.domain.model.Tag
import com.vndblite.dcj.domain.model.VnDetails
import com.vndblite.dcj.domain.model.VnImage
import com.vndblite.dcj.domain.model.VnLengthBucket
import com.vndblite.dcj.domain.model.VnRelation
import com.vndblite.dcj.domain.model.VnRelationType
import com.vndblite.dcj.domain.model.VnSummary
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

/**
 * Room entity caching visual novel summary and extended detail metadata in `vn_cache` (S1, S4).
 *
 * Filterable and sortable columns (`title`, `rating`, `voteCount`, `releasedSortKey`,
 * `lengthMinutes`, `lengthBucketCode`, `originalLanguage`, `devStatusCode`, `minAge`,
 * `languagesPipe`, `platformsPipe`) are stored as indexed SQLite columns for direct SQL queries.
 */
@Entity(
    tableName = "vn_cache",
    indices = [
        Index(value = ["title"]),
        Index(value = ["rating"]),
        Index(value = ["voteCount"]),
        Index(value = ["releasedSortKey"]),
        Index(value = ["lengthMinutes"]),
        Index(value = ["originalLanguage"]),
        Index(value = ["devStatusCode"]),
        Index(value = ["minAge"]),
        Index(value = ["lastAccessedAtEpochSeconds"]),
        Index(value = ["summaryFetchedAtEpochSeconds"])
    ]
)
data class VnCacheEntity(
    @PrimaryKey
    val vnId: String,
    val title: String,
    val imageUrl: String? = null,
    val imageThumbnailUrl: String? = null,
    val imageWidth: Int? = null,
    val imageHeight: Int? = null,
    val imageSexual: Double = 0.0,
    val imageViolence: Double = 0.0,
    val rating: Double? = null,
    val voteCount: Int = 0,
    val originalLanguage: String? = null,
    val lengthBucketCode: Int? = null,
    val releasedRaw: String? = null,
    val releasedSortKey: String? = null,
    val description: String? = null,
    val devStatusCode: Int? = null,
    val lengthMinutes: Int? = null,
    val minAge: Int? = null,
    /** Pipe-delimited language codes (e.g. `"|en|ja|"`) for exact SQL `LIKE '%|en|%'` matching (S1). */
    val languagesPipe: String = "",
    /** Pipe-delimited platform codes (e.g. `"|win|swi|"`) for exact SQL `LIKE '%|win|%'` matching (S1). */
    val platformsPipe: String = "",
    /** Newline-separated aliases for SQL search queries (S1). */
    val aliasesSearchText: String = "",
    /** JSON-serialized [VnCacheDetailsPayload] for the detail screen. */
    val detailsPayloadJson: String? = null,
    val hasFullDetails: Boolean = false,
    val summaryFetchedAtEpochSeconds: Long = 0L,
    val detailsFetchedAtEpochSeconds: Long? = null,
    val lastAccessedAtEpochSeconds: Long = 0L
) {
    fun toSummary(): VnSummary {
        val image = imageUrl?.takeIf { it.isNotBlank() }?.let { url ->
            val dims = if (imageWidth != null && imageHeight != null && imageWidth > 0 && imageHeight > 0) {
                ImageDimensions(width = imageWidth, height = imageHeight)
            } else {
                null
            }
            VnImage(
                url = url,
                thumbnailUrl = imageThumbnailUrl,
                dims = dims,
                sexual = imageSexual,
                violence = imageViolence
            )
        }
        return VnSummary(
            id = vnId,
            title = title,
            image = image,
            rating = rating,
            voteCount = voteCount,
            originalLanguage = originalLanguage,
            lengthBucket = VnLengthBucket.fromApiCode(lengthBucketCode),
            released = ReleaseDate.parse(releasedRaw),
            description = description
        )
    }

    fun toDetailsOrNull(): VnDetails? {
        if (!hasFullDetails) return null
        val payload = detailsPayloadJson?.takeIf { it.isNotBlank() }?.let { raw ->
            runCatching { KanaJson.decodeFromString<VnCacheDetailsPayload>(raw) }.getOrNull()
        }
        val languages = decodePipeList(languagesPipe)
        val platforms = decodePipeList(platformsPipe)
        return VnDetails(
            id = vnId,
            developers = payload?.developers?.map { Producer(id = it.id, name = it.name) }.orEmpty(),
            devStatus = DevStatus.fromApiCode(devStatusCode),
            lengthMinutes = lengthMinutes,
            aliases = payload?.aliases ?: aliasesSearchText.lines().filter { it.isNotBlank() },
            minAges = payload?.minAges ?: listOfNotNull(minAge),
            tags = payload?.tags?.map {
                Tag(id = it.id, name = it.name, rating = it.rating, spoilerLevel = it.spoilerLevel)
            }.orEmpty(),
            screenshots = payload?.screenshots?.map { it.toDomain() }.orEmpty(),
            languages = languages,
            machineTranslatedLanguages = payload?.machineTranslatedLanguages.orEmpty(),
            platforms = platforms,
            externalLinks = payload?.externalLinks.orEmpty(),
            relations = payload?.relations?.map { it.toDomain() }.orEmpty(),
            releasesTruncated = payload?.releasesTruncated ?: false
        )
    }

    companion object {
        fun encodePipeList(values: Collection<String>): String {
            val cleaned = values.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.distinct()
            return if (cleaned.isEmpty()) "" else "|${cleaned.joinToString("|")}|"
        }

        fun decodePipeList(pipeString: String): List<String> =
            pipeString.split('|').map { it.trim() }.filter { it.isNotEmpty() }

        fun fromSummary(
            summary: VnSummary,
            nowEpochSeconds: Long,
            existing: VnCacheEntity? = null
        ): VnCacheEntity {
            val released = summary.released ?: ReleaseDate.parse(existing?.releasedRaw)
            val description = summary.description ?: existing?.description
            return VnCacheEntity(
                vnId = summary.id,
                title = summary.title.ifBlank { existing?.title.orEmpty() },
                imageUrl = summary.image?.url ?: existing?.imageUrl,
                imageThumbnailUrl = summary.image?.thumbnailUrl ?: existing?.imageThumbnailUrl,
                imageWidth = summary.image?.dims?.width ?: existing?.imageWidth,
                imageHeight = summary.image?.dims?.height ?: existing?.imageHeight,
                imageSexual = summary.image?.sexual ?: existing?.imageSexual ?: 0.0,
                imageViolence = summary.image?.violence ?: existing?.imageViolence ?: 0.0,
                rating = summary.rating ?: existing?.rating,
                voteCount = if (summary.voteCount > 0) summary.voteCount else (existing?.voteCount ?: 0),
                originalLanguage = summary.originalLanguage ?: existing?.originalLanguage,
                lengthBucketCode = summary.lengthBucket?.apiCode ?: existing?.lengthBucketCode,
                releasedRaw = released?.toApiString(),
                releasedSortKey = released?.sortKey,
                description = description,
                devStatusCode = existing?.devStatusCode,
                lengthMinutes = existing?.lengthMinutes,
                minAge = existing?.minAge,
                languagesPipe = existing?.languagesPipe.orEmpty(),
                platformsPipe = existing?.platformsPipe.orEmpty(),
                aliasesSearchText = existing?.aliasesSearchText.orEmpty(),
                detailsPayloadJson = existing?.detailsPayloadJson,
                hasFullDetails = existing?.hasFullDetails ?: false,
                summaryFetchedAtEpochSeconds = nowEpochSeconds,
                detailsFetchedAtEpochSeconds = existing?.detailsFetchedAtEpochSeconds,
                lastAccessedAtEpochSeconds = nowEpochSeconds
            )
        }

        fun fromSummaryAndDetails(
            summary: VnSummary,
            details: VnDetails,
            nowEpochSeconds: Long,
            existing: VnCacheEntity? = null
        ): VnCacheEntity {
            val base = fromSummary(summary = summary, nowEpochSeconds = nowEpochSeconds, existing = existing)
            val payload = VnCacheDetailsPayload.fromDomain(details)
            val resolvedMinAge = details.minAges.filter { it >= 0 }.maxOrNull()
            return base.copy(
                devStatusCode = details.devStatus?.apiCode,
                lengthMinutes = details.lengthMinutes,
                minAge = resolvedMinAge,
                languagesPipe = encodePipeList(details.languages),
                platformsPipe = encodePipeList(details.platforms),
                aliasesSearchText = details.aliases.joinToString("\n"),
                detailsPayloadJson = KanaJson.encodeToString(payload),
                hasFullDetails = true,
                detailsFetchedAtEpochSeconds = nowEpochSeconds,
                lastAccessedAtEpochSeconds = nowEpochSeconds
            )
        }
    }
}

@Serializable
data class VnCacheDetailsPayload(
    val developers: List<CachedProducerDto> = emptyList(),
    val aliases: List<String> = emptyList(),
    val minAges: List<Int> = emptyList(),
    val tags: List<CachedTagDto> = emptyList(),
    val screenshots: List<CachedImageDto> = emptyList(),
    val machineTranslatedLanguages: List<String> = emptyList(),
    val externalLinks: List<String> = emptyList(),
    val relations: List<CachedRelationDto> = emptyList(),
    val releasesTruncated: Boolean = false
) {
    companion object {
        fun fromDomain(details: VnDetails): VnCacheDetailsPayload = VnCacheDetailsPayload(
            developers = details.developers.map { CachedProducerDto(id = it.id, name = it.name) },
            aliases = details.aliases,
            minAges = details.minAges,
            tags = details.tags.map {
                CachedTagDto(id = it.id, name = it.name, rating = it.rating, spoilerLevel = it.spoilerLevel)
            },
            screenshots = details.screenshots.map(CachedImageDto::fromDomain),
            machineTranslatedLanguages = details.machineTranslatedLanguages,
            externalLinks = details.externalLinks,
            relations = details.relations.map(CachedRelationDto::fromDomain),
            releasesTruncated = details.releasesTruncated
        )
    }
}

@Serializable
data class CachedProducerDto(val id: String, val name: String)

@Serializable
data class CachedTagDto(
    val id: String,
    val name: String,
    val rating: Double = 0.0,
    val spoilerLevel: Int = 0
)

@Serializable
data class CachedImageDto(
    val url: String,
    val thumbnailUrl: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val sexual: Double = 0.0,
    val violence: Double = 0.0
) {
    fun toDomain(): VnImage = VnImage(
        url = url,
        thumbnailUrl = thumbnailUrl,
        dims = if (width != null && height != null && width > 0 && height > 0) {
            ImageDimensions(width = width, height = height)
        } else {
            null
        },
        sexual = sexual,
        violence = violence
    )

    companion object {
        fun fromDomain(image: VnImage): CachedImageDto = CachedImageDto(
            url = image.url,
            thumbnailUrl = image.thumbnailUrl,
            width = image.dims?.width,
            height = image.dims?.height,
            sexual = image.sexual,
            violence = image.violence
        )
    }
}

@Serializable
data class CachedRelationDto(
    val id: String,
    val rawRelationCode: String,
    val isOfficial: Boolean = false,
    val relatedTitle: String? = null,
    val relatedImage: CachedImageDto? = null,
    val relatedRating: Double? = null,
    val relatedVoteCount: Int = 0,
    val relatedOriginalLanguage: String? = null,
    val relatedLengthBucketCode: Int? = null,
    val relatedReleasedRaw: String? = null
) {
    fun toDomain(): VnRelation = VnRelation(
        id = id,
        relationType = VnRelationType.fromApiCode(rawRelationCode),
        rawRelationCode = rawRelationCode,
        isOfficial = isOfficial,
        relatedVn = relatedTitle?.let { title ->
            VnSummary(
                id = id,
                title = title,
                image = relatedImage?.toDomain(),
                rating = relatedRating,
                voteCount = relatedVoteCount,
                originalLanguage = relatedOriginalLanguage,
                lengthBucket = VnLengthBucket.fromApiCode(relatedLengthBucketCode),
                released = ReleaseDate.parse(relatedReleasedRaw)
            )
        }
    )

    companion object {
        fun fromDomain(relation: VnRelation): CachedRelationDto = CachedRelationDto(
            id = relation.id,
            rawRelationCode = relation.rawRelationCode,
            isOfficial = relation.isOfficial,
            relatedTitle = relation.relatedVn?.title,
            relatedImage = relation.relatedVn?.image?.let(CachedImageDto::fromDomain),
            relatedRating = relation.relatedVn?.rating,
            relatedVoteCount = relation.relatedVn?.voteCount ?: 0,
            relatedOriginalLanguage = relation.relatedVn?.originalLanguage,
            relatedLengthBucketCode = relation.relatedVn?.lengthBucket?.apiCode,
            relatedReleasedRaw = relation.relatedVn?.released?.toApiString()
        )
    }
}
