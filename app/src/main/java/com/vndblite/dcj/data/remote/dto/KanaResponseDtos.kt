package com.vndblite.dcj.data.remote.dto

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
import kotlin.math.roundToInt
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KanaPageResponseDto<T>(
    val results: List<T> = emptyList(),
    val more: Boolean = false,
    val count: Int? = null
)

@Serializable
data class ImageDto(
    val id: String? = null,
    val url: String? = null,
    val thumbnail: String? = null,
    val dims: List<Int>? = null,
    val sexual: Double? = null,
    val violence: Double? = null
) {
    fun toDomainOrNull(): VnImage? {
        val cleanUrl = url?.trim().orEmpty()
        val cleanThumb = thumbnail?.trim()?.takeIf { it.isNotEmpty() }
        val resolvedUrl = cleanUrl.ifEmpty { cleanThumb.orEmpty() }
        if (resolvedUrl.isEmpty()) return null
        val dimensions = dims?.takeIf { it.size >= 2 && it[0] > 0 && it[1] > 0 }?.let {
            ImageDimensions(width = it[0], height = it[1])
        }
        return VnImage(
            url = resolvedUrl,
            thumbnailUrl = cleanThumb,
            dims = dimensions,
            sexual = sexual?.takeIf { !it.isNaN() } ?: 0.0,
            violence = violence?.takeIf { !it.isNaN() } ?: 0.0
        )
    }
}

@Serializable
data class TagLinkDto(
    val id: String = "",
    val name: String = "",
    val rating: Double? = null,
    val spoiler: Double? = null
) {
    fun toDomainOrNull(): Tag? {
        val cleanId = id.trim()
        val cleanName = name.trim()
        if (cleanId.isEmpty() || cleanName.isEmpty()) return null
        return Tag(
            id = cleanId,
            name = cleanName,
            rating = rating?.takeIf { !it.isNaN() } ?: 0.0,
            spoilerLevel = spoiler?.takeIf { !it.isNaN() }?.roundToInt() ?: 0
        )
    }
}

@Serializable
data class ProducerDto(
    val id: String = "",
    val name: String = ""
) {
    fun toDomainOrNull(): Producer? {
        val cleanId = id.trim()
        val cleanName = name.trim()
        if (cleanId.isEmpty() || cleanName.isEmpty()) return null
        return Producer(id = cleanId, name = cleanName)
    }
}

@Serializable
data class ExtLinkDto(
    val url: String? = null,
    val label: String? = null,
    val name: String? = null,
    val id: String? = null
)

@Serializable
data class RelationDto(
    val id: String = "",
    val relation: String = "",
    @SerialName("relation_official")
    val relationOfficial: Boolean = false
) {
    fun toDomainOrNull(relatedVn: VnSummary? = null): VnRelation? {
        val cleanId = id.trim()
        if (cleanId.isEmpty()) return null
        return VnRelation(
            id = cleanId,
            relationType = VnRelationType.fromApiCode(relation),
            rawRelationCode = relation.trim(),
            isOfficial = relationOfficial,
            relatedVn = relatedVn
        )
    }
}

@Serializable
data class VnDto(
    val id: String = "",
    val title: String = "",
    val image: ImageDto? = null,
    val rating: Double? = null,
    val votecount: Int? = null,
    val length: Int? = null,
    @SerialName("length_minutes")
    val lengthMinutes: Int? = null,
    val olang: String? = null,
    val released: String? = null,
    val description: String? = null,
    val aliases: List<String>? = null,
    val devstatus: Int? = null,
    val languages: List<String>? = null,
    val platforms: List<String>? = null,
    val tags: List<TagLinkDto>? = null,
    val developers: List<ProducerDto>? = null,
    val screenshots: List<ImageDto>? = null,
    val extlinks: List<ExtLinkDto>? = null,
    val relations: List<RelationDto>? = null
) {
    fun toSummary(): VnSummary = VnSummary(
        id = id.trim(),
        title = title.trim().ifEmpty { id.trim() },
        image = image?.toDomainOrNull(),
        rating = rating?.takeIf { !it.isNaN() && it > 0.0 },
        voteCount = votecount ?: 0,
        originalLanguage = olang?.trim()?.takeIf { it.isNotEmpty() && !it.equals("unknown", ignoreCase = true) },
        lengthBucket = VnLengthBucket.fromApiCode(length),
        released = ReleaseDate.parse(released),
        description = description?.trim()?.takeIf { it.isNotEmpty() && it != "--" }
    )

    fun toDetails(
        minAges: List<Int> = emptyList(),
        machineTranslatedLanguages: List<String> = emptyList(),
        relatedSummariesById: Map<String, VnSummary> = emptyMap(),
        releasesTruncated: Boolean = false
    ): VnDetails = VnDetails(
        id = id.trim(),
        developers = developers.orEmpty().mapNotNull { it.toDomainOrNull() },
        devStatus = DevStatus.fromApiCode(devstatus),
        lengthMinutes = lengthMinutes?.takeIf { it > 0 },
        aliases = aliases.orEmpty().map { it.trim() }.filter { it.isNotEmpty() },
        minAges = minAges,
        tags = tags.orEmpty().mapNotNull { it.toDomainOrNull() },
        screenshots = screenshots.orEmpty().mapNotNull { it.toDomainOrNull() },
        languages = languages.orEmpty().map { it.trim() }.filter { it.isNotEmpty() },
        machineTranslatedLanguages = machineTranslatedLanguages,
        platforms = platforms.orEmpty().map { it.trim() }.filter { it.isNotEmpty() },
        externalLinks = extlinks.orEmpty().mapNotNull { it.url?.trim()?.takeIf { u -> u.isNotEmpty() } },
        relations = relations.orEmpty().mapNotNull { rel ->
            rel.toDomainOrNull(relatedSummariesById[rel.id.trim()])
        },
        releasesTruncated = releasesTruncated
    )
}

@Serializable
data class ReleaseLangDto(
    val lang: String? = null,
    val mtl: Boolean = false
)

@Serializable
data class ReleaseDto(
    val id: String = "",
    val minage: Int? = null,
    val languages: List<ReleaseLangDto>? = null
)

/**
 * Aggregates release age ratings (min & max distinct values) and machine-translated languages
 * across all fetched `/kana/release` pages (N2).
 */
fun summarizeReleases(releases: List<ReleaseDto>): Pair<List<String>, List<Int>> {
    val minAges = mutableListOf<Int>()
    val mtlLangs = linkedSetOf<String>()
    for (release in releases) {
        release.minage?.let { minAges.add(it) }
        val firstLang = release.languages?.firstOrNull()
        if (firstLang != null && firstLang.mtl) {
            val code = firstLang.lang?.trim().orEmpty()
            if (code.isNotEmpty()) {
                mtlLangs.add(code)
            }
        }
    }
    val ageBounds = if (minAges.isNotEmpty()) {
        val minVal = minAges.min()
        val maxVal = minAges.max()
        listOf(minVal, maxVal).distinct()
    } else {
        emptyList()
    }
    return mtlLangs.toList() to ageBounds
}
