package com.vndblite.dcj.data.remote.dto

import com.vndblite.dcj.domain.model.CharacterRole
import com.vndblite.dcj.domain.model.CharacterSex
import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.domain.model.DevStatus
import com.vndblite.dcj.domain.model.ReleaseDate
import com.vndblite.dcj.domain.model.UserListPatch
import com.vndblite.dcj.domain.model.VnLengthBucket
import com.vndblite.dcj.domain.model.VnRelationType
import java.time.Instant
import java.time.LocalDate
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KanaDtoParsingTest {

    private fun readFixture(path: String): String {
        val stream = checkNotNull(javaClass.classLoader?.getResourceAsStream(path)) {
            "Missing test fixture: $path"
        }
        return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    @Test
    fun `parses vn_response fixture and maps cover and screenshot urls and thumbnails`() {
        val raw = readFixture("fixtures/kana/vn_response.json")
        val page = KanaJson.decodeFromString(
            KanaPageResponseDto.serializer(VnDto.serializer()),
            raw
        )
        assertEquals(1, page.results.size)
        val dto = page.results.first()

        val summary = dto.toSummary()
        assertEquals("v17", summary.id)
        assertEquals("Ever17 -the out of infinity-", summary.title)
        assertEquals(86.4, summary.rating ?: 0.0, 0.001)
        assertEquals(7540, summary.voteCount)
        assertEquals("ja", summary.originalLanguage)
        assertEquals(VnLengthBucket.LONG, summary.lengthBucket)
        assertEquals(ReleaseDate.Exact(LocalDate.of(2002, 8, 29)), summary.released)
        assertEquals("https://t.vndb.org/cv.t/17/12345.jpg", summary.image?.previewUrl)

        val details = dto.toDetails(minAges = listOf(12, 18), machineTranslatedLanguages = listOf("en"))
        assertEquals(DevStatus.FINISHED, details.devStatus)
        assertEquals(2150, details.lengthMinutes)
        assertEquals(1, details.screenshots.size)
        val shot = details.screenshots.first()
        // N6: previewUrl uses thumbnail while url holds the full-resolution screenshot URL
        assertEquals("https://t.vndb.org/sf/01/101.jpg", shot.url)
        assertEquals("https://t.vndb.org/sf.t/01/101.jpg", shot.previewUrl)
        assertEquals(1280, shot.dims?.width)
        assertEquals(720, shot.dims?.height)
        assertEquals(VnRelationType.SAME_SERIES, details.relations.first().relationType)
    }

    @Test
    fun `parses character_response fixture into Character domain model`() {
        val raw = readFixture("fixtures/kana/character_response.json")
        val page = KanaJson.decodeFromString(
            KanaPageResponseDto.serializer(CharacterDto.serializer()),
            raw
        )
        val character = page.results.first().toDomain()
        assertEquals("c35", character.id)
        assertEquals("Yagami Koko", character.name)
        assertEquals("八神 ココ", character.originalName)
        assertEquals(CharacterSex.FEMALE, character.sex)
        assertEquals(CharacterRole.PRIMARY, character.roleSummary)
        assertEquals(12, character.birthday?.month)
        assertEquals(17, character.birthday?.day)
        assertEquals(77, character.measurements?.bustCm)
        assertEquals("B", character.measurements?.cupSize)
        assertEquals("Hair", character.traits.first().groupName)
    }

    @Test
    fun `parses trait tag producer quote release authinfo stats and ulist fixtures`() {
        val traits = KanaJson.decodeFromString(
            KanaPageResponseDto.serializer(TraitDto.serializer()),
            readFixture("fixtures/kana/trait_response.json")
        ).results.mapNotNull { it.toDomainOrNull() }
        assertEquals("i154", traits.first().id)
        assertEquals(4820, traits.first().characterCount)

        val tags = KanaJson.decodeFromString(
            KanaPageResponseDto.serializer(TagDto.serializer()),
            readFixture("fixtures/kana/tag_response.json")
        ).results.mapNotNull { it.toDomainOrNull() }
        assertEquals(2, tags.size)

        val producers = KanaJson.decodeFromString(
            KanaPageResponseDto.serializer(ProducerDto.serializer()),
            readFixture("fixtures/kana/producer_response.json")
        ).results.mapNotNull { it.toDomainOrNull() }
        assertEquals("KID", producers.first().name)

        val quotes = KanaJson.decodeFromString(
            KanaPageResponseDto.serializer(QuoteDto.serializer()),
            readFixture("fixtures/kana/quote_response.json")
        ).results.mapNotNull { it.toDomainOrNull() }
        assertEquals("q101", quotes.first().id)
        assertEquals("v17", quotes.first().vn?.id)
        assertNull(quotes.first().vn?.description)

        val releases = KanaJson.decodeFromString(
            KanaPageResponseDto.serializer(ReleaseDto.serializer()),
            readFixture("fixtures/kana/release_response.json")
        ).results
        val (mtl, ages) = summarizeReleases(releases)
        assertEquals(listOf("en"), mtl)
        assertEquals(listOf(12, 18), ages)

        val auth = KanaJson.decodeFromString(
            AuthInfoDto.serializer(),
            readFixture("fixtures/kana/authinfo_response.json")
        ).toDomain()
        assertEquals("u42", auth.id)
        assertTrue(auth.hasFullListPermissions)

        val stats = KanaJson.decodeFromString(
            StatsDto.serializer(),
            readFixture("fixtures/kana/stats_response.json")
        ).toDomain(Instant.ofEpochSecond(1700000000L))
        assertEquals(52100, stats.vn)
        assertEquals(39100, stats.staff)
        assertEquals(3100, stats.tags)

        val ulist = KanaJson.decodeFromString(
            KanaPageResponseDto.serializer(UserListItemDto.serializer()),
            readFixture("fixtures/kana/ulist_response.json")
        ).results.mapNotNull { it.toDomainOrNull() }
        val ulistItem = ulist.first()
        assertEquals("v17", ulistItem.vnId)
        assertEquals(CollectionStatus.FINISHED, ulistItem.resolvedStatus)
        assertEquals(90, ulistItem.vote)
        assertEquals(LocalDate.of(2023, 11, 1), ulistItem.started)
        assertTrue(12 in ulistItem.labelIds)
    }

    @Test
    fun `UserListPatchPayloadBuilder uses labels_set and labels_unset and explicit nulls for cleared fields`() {
        val patch = UserListPatch(
            vnId = "v17",
            includeVote = true,
            vote = null,
            includeStarted = true,
            started = LocalDate.of(2024, 1, 10),
            includeFinished = true,
            finished = null,
            labelsSet = listOf(1),
            labelsUnset = listOf(2, 3, 4, 5, 6)
        )
        val jsonStr = KanaJson.encodeToString(
            JsonObject.serializer(),
            UserListPatchPayloadBuilder.toJsonObject(patch)
        )
        assertEquals(
            """{"vote":null,"started":"2024-01-10","finished":null,"labels_set":[1],"labels_unset":[2,3,4,5,6]}""",
            jsonStr
        )
        assertFalse(jsonStr.contains("\"labels\":"))
        assertNotNull(patch.started)
    }
}
