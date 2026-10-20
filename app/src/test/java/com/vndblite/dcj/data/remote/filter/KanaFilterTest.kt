package com.vndblite.dcj.data.remote.filter

import com.vndblite.dcj.data.remote.dto.KanaJson
import com.vndblite.dcj.domain.model.AndOr
import com.vndblite.dcj.domain.model.CharacterRole
import com.vndblite.dcj.domain.model.CharacterSearchFilters
import com.vndblite.dcj.domain.model.CharacterSex
import com.vndblite.dcj.domain.model.DevStatus
import com.vndblite.dcj.domain.model.Language
import com.vndblite.dcj.domain.model.Platform
import com.vndblite.dcj.domain.model.Producer
import com.vndblite.dcj.domain.model.ReleaseWindow
import com.vndblite.dcj.domain.model.Tag
import com.vndblite.dcj.domain.model.Trait
import com.vndblite.dcj.domain.model.VnSearchFilters
import kotlinx.serialization.json.JsonArray
import org.junit.Assert.assertEquals
import org.junit.Test

class KanaFilterTest {

    @Test
    fun `forVnSearch produces exact Kana JSON with today token for latest release window`() {
        val filters = VnSearchFilters(
            searchQuery = "  Steins;Gate  ",
            andOr = AndOr.AND,
            languages = listOf(Language.EN),
            devStatuses = listOf(DevStatus.FINISHED),
            originalLanguages = listOf(Language.JA),
            platforms = listOf(Platform.WIN),
            tags = listOf(Tag(id = "g105", name = "Sci-fi")),
            developers = listOf(Producer(id = "p98", name = "5pb.")),
            characterTraits = listOf(
                Trait(id = "i154", name = "Tsundere"),
                Trait(id = "i47", name = "Glasses")
            ),
            traitAndOr = AndOr.AND,
            characterRoles = listOf(CharacterRole.MAIN),
            characterSexes = listOf(CharacterSex.FEMALE, CharacterSex.MALE),
            characterSexAndOr = AndOr.OR,
            releaseWindow = ReleaseWindow.LATEST
        )

        val serialized = KanaJson.encodeToString(
            JsonArray.serializer(),
            KanaFilters.forVnSearch(filters).toJsonArray()
        )

        val expected = """["and",["search","=","Steins;Gate"],["released","<=","today"],["released","!=","tba"],["lang","=","en"],["devstatus","=",0],["olang","=","ja"],["platform","=","win"],["tag","=","g105"],["developer","=",["id","=","p98"]],["character","=",["role","=","main"]],["character","=",["or",["sex","=","f"],["sex","=","m"]]],["character","=",["and",["trait","=","i154"],["trait","=","i47"]]]]"""
        assertEquals(expected, serialized)
    }

    @Test
    fun `forVnSearch upcoming window uses greater than today and excludes tba`() {
        val filters = VnSearchFilters(
            searchQuery = "",
            releaseWindow = ReleaseWindow.UPCOMING
        )

        val serialized = KanaJson.encodeToString(
            JsonArray.serializer(),
            KanaFilters.forVnSearch(filters).toJsonArray()
        )

        assertEquals(
            """["and",["search","=",""],["released",">","today"],["released","!=","tba"]]""",
            serialized
        )
    }

    @Test
    fun `byIds builds single predicate for one id and or-clause for multiple ids capped at 100`() {
        val single = KanaJson.encodeToString(
            JsonArray.serializer(),
            KanaFilters.byIds(listOf("v17")).toJsonArray()
        )
        assertEquals("""["id","=","v17"]""", single)

        val multi = KanaJson.encodeToString(
            JsonArray.serializer(),
            KanaFilters.byIds(listOf("v17", "v2", "v17")).toJsonArray()
        )
        assertEquals("""["or",["id","=","v17"],["id","=","v2"]]""", multi)

        val overHundred = (1..125).map { "v$it" }
        val cappedArray = KanaFilters.byIds(overHundred).toJsonArray()
        // 1 operator token ("or") + 100 id predicates = 101 elements
        assertEquals(101, cappedArray.size)
    }

    @Test
    fun `forCharacterSearch produces exact Kana JSON for traits roles and sexes`() {
        val filters = CharacterSearchFilters(
            searchQuery = "Sora",
            traits = listOf(Trait(id = "i154", name = "Tsundere")),
            roles = listOf(CharacterRole.MAIN, CharacterRole.PRIMARY),
            roleAndOr = AndOr.OR,
            sexes = listOf(CharacterSex.FEMALE)
        )

        val serialized = KanaJson.encodeToString(
            JsonArray.serializer(),
            KanaFilters.forCharacterSearch(filters).toJsonArray()
        )

        assertEquals(
            """["and",["search","=","Sora"],["trait","=","i154"],["or",["role","=","main"],["role","=","primary"]],["sex","=","f"]]""",
            serialized
        )
    }
}
