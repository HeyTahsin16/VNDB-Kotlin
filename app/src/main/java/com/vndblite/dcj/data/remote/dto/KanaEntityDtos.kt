package com.vndblite.dcj.data.remote.dto

import com.vndblite.dcj.domain.model.Character
import com.vndblite.dcj.domain.model.CharacterBirthday
import com.vndblite.dcj.domain.model.CharacterMeasurements
import com.vndblite.dcj.domain.model.CharacterRole
import com.vndblite.dcj.domain.model.CharacterSex
import com.vndblite.dcj.domain.model.CharacterVnAppearance
import com.vndblite.dcj.domain.model.CollectionEntry
import com.vndblite.dcj.domain.model.Quote
import com.vndblite.dcj.domain.model.RemoteUserListItem
import com.vndblite.dcj.domain.model.SiteStats
import com.vndblite.dcj.domain.model.Tag
import com.vndblite.dcj.domain.model.Trait
import com.vndblite.dcj.domain.model.UserAccount
import java.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TraitDto(
    val id: String = "",
    val name: String = "",
    @SerialName("group_id")
    val groupId: String? = null,
    @SerialName("group_name")
    val groupName: String? = null,
    val description: String? = null,
    val aliases: List<String>? = null,
    @SerialName("char_count")
    val charCount: Int? = null,
    val spoiler: Int? = null
) {
    fun toDomainOrNull(): Trait? {
        val cleanId = id.trim()
        val cleanName = name.trim()
        if (cleanId.isEmpty() || cleanName.isEmpty()) return null
        return Trait(
            id = cleanId,
            name = cleanName,
            groupId = groupId?.trim()?.takeIf { it.isNotEmpty() },
            groupName = groupName?.trim()?.takeIf { it.isNotEmpty() },
            description = description?.trim()?.takeIf { it.isNotEmpty() },
            aliases = aliases.orEmpty().map { it.trim() }.filter { it.isNotEmpty() },
            characterCount = charCount ?: 0,
            spoilerLevel = spoiler ?: 0
        )
    }
}

@Serializable
data class CharacterVnDto(
    val id: String = "",
    val title: String? = null,
    val role: String? = null,
    val spoiler: Int? = null
) {
    fun toDomainOrNull(): CharacterVnAppearance? {
        val cleanId = id.trim()
        if (cleanId.isEmpty()) return null
        return CharacterVnAppearance(
            vnId = cleanId,
            vnTitle = title?.trim()?.takeIf { it.isNotEmpty() } ?: cleanId,
            role = CharacterRole.fromApiCode(role),
            spoilerLevel = spoiler ?: 0
        )
    }
}

@Serializable
data class CharacterDto(
    val id: String = "",
    val name: String = "",
    val original: String? = null,
    val aliases: List<String>? = null,
    val description: String? = null,
    val image: ImageDto? = null,
    @SerialName("blood_type")
    val bloodType: String? = null,
    val height: Int? = null,
    val weight: Int? = null,
    val bust: Int? = null,
    val waist: Int? = null,
    val hips: Int? = null,
    val cup: String? = null,
    val age: Int? = null,
    val birthday: List<Int?>? = null,
    val sex: List<String?>? = null,
    val vns: List<CharacterVnDto>? = null,
    val traits: List<TraitDto>? = null
) {
    fun toDomain(): Character {
        val validBirthday = birthday
            ?.takeIf { it.size >= 2 }
            ?.let { parts ->
                val month = parts[0] ?: 0
                val day = parts[1] ?: 0
                if (month in 1..12 && day in 1..31) {
                    CharacterBirthday(month = month, day = day)
                } else {
                    null
                }
            }

        val validMeasurements = if (
            bust != null && bust > 0 &&
            waist != null && waist > 0 &&
            hips != null && hips > 0
        ) {
            CharacterMeasurements(
                bustCm = bust,
                waistCm = waist,
                hipsCm = hips,
                cupSize = cup?.trim()?.takeIf { it.isNotEmpty() }
            )
        } else {
            null
        }

        val apparentSex = sex?.firstOrNull()?.let { CharacterSex.fromApiCode(it) }

        val distinctVns = mutableListOf<CharacterVnAppearance>()
        val seenVnIds = hashSetOf<String>()
        for (vnDto in vns.orEmpty()) {
            val appearance = vnDto.toDomainOrNull() ?: continue
            if (seenVnIds.add(appearance.vnId)) {
                distinctVns.add(appearance)
            }
        }

        return Character(
            id = id.trim(),
            name = name.trim().ifEmpty { id.trim() },
            originalName = original?.trim()?.takeIf { it.isNotEmpty() },
            aliases = aliases.orEmpty().map { it.trim() }.filter { it.isNotEmpty() },
            description = description?.trim()?.takeIf { it.isNotEmpty() },
            image = image?.toDomainOrNull(),
            bloodType = bloodType?.trim()?.takeIf { it.isNotEmpty() },
            heightCm = height?.takeIf { it > 0 },
            weightKg = weight?.takeIf { it > 0 },
            measurements = validMeasurements,
            ageYears = age?.takeIf { it > 0 },
            birthday = validBirthday,
            sex = apparentSex,
            vns = distinctVns,
            traits = traits.orEmpty().mapNotNull { it.toDomainOrNull() }
        )
    }
}

@Serializable
data class QuoteCharacterDto(
    val id: String = "",
    val name: String = "",
    val original: String? = null
)

@Serializable
data class QuoteDto(
    val id: String = "",
    val quote: String = "",
    val score: Int? = null,
    val vn: VnDto? = null,
    val character: QuoteCharacterDto? = null
) {
    fun toDomainOrNull(): Quote? {
        val cleanId = id.trim()
        val cleanText = quote.trim()
        if (cleanId.isEmpty() || cleanText.isEmpty()) return null
        return Quote(
            id = cleanId,
            text = cleanText,
            score = score ?: 0,
            vn = vn?.takeIf { it.id.isNotBlank() }?.toSummary(),
            characterId = character?.id?.trim()?.takeIf { it.isNotEmpty() },
            characterName = character?.name?.trim()?.takeIf { it.isNotEmpty() },
            characterOriginalName = character?.original?.trim()?.takeIf { it.isNotEmpty() }
        )
    }
}

@Serializable
data class TagDto(
    val id: String = "",
    val name: String = ""
) {
    fun toDomainOrNull(): Tag? {
        val cleanId = id.trim()
        val cleanName = name.trim()
        if (cleanId.isEmpty() || cleanName.isEmpty()) return null
        return Tag(id = cleanId, name = cleanName)
    }
}

@Serializable
data class UserListLabelDto(
    val id: Int,
    val label: String = ""
)

@Serializable
data class UserListItemDto(
    val id: String = "",
    val added: Long? = null,
    val lastmod: Long? = null,
    val voted: Long? = null,
    val vote: Int? = null,
    val notes: String? = null,
    val started: String? = null,
    val finished: String? = null,
    val labels: List<UserListLabelDto>? = null,
    val vn: VnDto? = null
) {
    fun toDomainOrNull(): RemoteUserListItem? {
        val cleanId = id.trim()
        if (cleanId.isEmpty()) return null
        return RemoteUserListItem(
            vnId = cleanId,
            added = added?.takeIf { it > 0L }?.let { Instant.ofEpochSecond(it) },
            lastMod = lastmod?.takeIf { it > 0L }?.let { Instant.ofEpochSecond(it) } ?: Instant.EPOCH,
            voted = voted?.takeIf { it > 0L }?.let { Instant.ofEpochSecond(it) },
            vote = CollectionEntry.normalizeVote(vote),
            notes = notes?.takeIf { it.isNotEmpty() },
            started = CollectionEntry.parseLocalDateOrNull(started),
            finished = CollectionEntry.parseLocalDateOrNull(finished),
            labelIds = labels.orEmpty().map { it.id }.toSet(),
            vnSummary = vn?.takeIf { it.id.isNotBlank() }?.toSummary()
        )
    }
}

@Serializable
data class AuthInfoDto(
    val id: String = "",
    val username: String = "",
    val permissions: List<String> = emptyList()
) {
    fun toDomain(): UserAccount = UserAccount(
        id = id.trim(),
        username = username.trim().ifEmpty { id.trim() },
        permissions = permissions.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
    )
}

@Serializable
data class StatsDto(
    val chars: Int = 0,
    val releases: Int = 0,
    val vn: Int = 0,
    val staff: Int = 0,
    val tags: Int = 0,
    val producers: Int = 0,
    val traits: Int = 0
) {
    fun toDomain(fetchedAt: Instant): SiteStats = SiteStats(
        chars = chars,
        releases = releases,
        vn = vn,
        staff = staff,
        tags = tags,
        producers = producers,
        traits = traits,
        fetchedAt = fetchedAt
    )
}
