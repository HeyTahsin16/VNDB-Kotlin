package com.vndblite.dcj

import com.vndblite.dcj.data.model.CharacterRoleData
import com.vndblite.dcj.data.model.CharacterSexData
import com.vndblite.dcj.data.model.DefaultChartStats
import com.vndblite.dcj.data.model.Developer
import com.vndblite.dcj.data.model.FilterData
import com.vndblite.dcj.data.model.HomeSectionsCode
import com.vndblite.dcj.data.model.SettingsState
import com.vndblite.dcj.data.model.ThemeCode
import com.vndblite.dcj.data.model.VnCharacter
import com.vndblite.dcj.data.model.VnDataPhase01
import com.vndblite.dcj.data.model.VnQuote
import com.vndblite.dcj.data.model.VnTag
import com.vndblite.dcj.data.model.VnTrait
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VndbModelsTest {

    @Test
    fun `toVndbFilterJsonArray builds character trait, role, sex, and release filters properly`() {
        val filter = FilterData(
            search = "Fate",
            andOr = "and",
            lang = listOf("en"),
            tag = listOf(VnTag(id = "g12", name = "Action")),
            dev = listOf(Developer(id = "p14", name = "Type-Moon")),
            trait = listOf(VnTrait(id = "i154", name = "Tsundere", groupName = "Personality")),
            charRoles = listOf("main", "primary"),
            charRoleAndOr = "or",
            charSexes = listOf("f"),
            charSexAndOr = "or"
        )

        val jsonArray = filter.toVndbFilterJsonArray()
        assertEquals("and", jsonArray.getString(0))
        val serialized = jsonArray.toString()
        assertTrue(serialized.contains("""["search","=","Fate"]"""))
        assertTrue(serialized.contains("""["lang","=","en"]"""))
        assertTrue(serialized.contains("""["tag","=","g12"]"""))
        assertTrue(serialized.contains("""["developer","=",["id","=","p14"]]"""))
        assertTrue(serialized.contains("""["character","=",["or",["role","=","main"],["role","=","primary"]]]"""))
        assertTrue(serialized.contains("""["character","=",["sex","=","f"]]"""))
        assertTrue(serialized.contains("""["character","=",["trait","=","i154"]]"""))

        val latestFilter = HomeSectionsCode.LATEST_RELEASES.filter.toVndbFilterJsonArray().toString()
        assertTrue(latestFilter.contains("""["released","<=""""))
        assertTrue(latestFilter.contains("""["released","!=","tba"]"""))
        assertTrue(HomeSectionsCode.LATEST_RELEASES.reverseSort)

        val upcomingFilter = HomeSectionsCode.UPCOMING_RELEASES.filter.toVndbFilterJsonArray().toString()
        assertTrue(upcomingFilter.contains("""["released",">""""))
        assertTrue(upcomingFilter.contains("""["released","!=","tba"]"""))
        assertFalse(HomeSectionsCode.UPCOMING_RELEASES.reverseSort)
    }

    @Test
    fun `VnQuote and CharacterRoleData and CharacterSexData parse and map properly`() {
        assertEquals("Protagonist", CharacterRoleData.getLabel("main"))
        assertEquals("Main character", CharacterRoleData.getLabel("primary"))
        assertEquals("Side character", CharacterRoleData.getLabel("side"))
        assertEquals("Makes an appearance", CharacterRoleData.getLabel("appears"))

        assertEquals("Female", CharacterSexData.getLabel("f"))
        assertEquals("Male", CharacterSexData.getLabel("m"))
        assertEquals("Both", CharacterSexData.getLabel("b"))
        assertEquals("Sexless", CharacterSexData.getLabel("n"))

        val quoteJson = JSONObject(
            """
            {
                "id": "q1520",
                "quote": "People die if they are killed.",
                "score": 320,
                "character": {
                    "id": "c14",
                    "name": "Emiya Shirou",
                    "original": "衛宮 士郎"
                },
                "vn": {
                    "id": "v11",
                    "title": "Fate/stay night",
                    "rating": 86.4,
                    "released": "2004-01-30"
                }
            }
            """.trimIndent()
        )
        val q = VnQuote.fromJson(quoteJson)
        assertEquals("q1520", q.id)
        assertEquals("People die if they are killed.", q.quote)
        assertEquals(320, q.score)
        assertEquals("Emiya Shirou", q.characterName)
        assertEquals("v11", q.vn?.id)
        assertEquals("Fate/stay night", q.vn?.title)
    }

    @Test
    fun `VnDataPhase01 and VnCharacter parse JSON with traits properly`() {
        val p1Json = JSONObject(
            """
            {
                "id": "v11",
                "title": "Fate/stay night",
                "rating": 86.4,
                "votecount": 12450,
                "released": "2004-01-30",
                "image": {
                    "url": "https://t.vndb.org/cv/01/11.jpg",
                    "sexual": 0.2,
                    "violence": 0.4
                }
            }
            """.trimIndent()
        )
        val p1 = VnDataPhase01.fromJson(p1Json)
        assertEquals("v11", p1.id)
        assertEquals("Fate/stay night", p1.title)
        assertEquals(86.4, p1.rating ?: 0.0, 0.01)
        assertEquals(12450, p1.votecount)
        assertFalse(p1.image?.isExplicit ?: true)

        val charJson = JSONObject(
            """
            {
                "id": "c15",
                "name": "Tohsaka Rin",
                "original": "遠坂 凛",
                "sex": ["f", "f"],
                "age": 17,
                "birthday": [2, 3],
                "description": "A proud magus and [spoiler]Master of Archer[/spoiler].",
                "image": {
                    "url": "https://t.vndb.org/ch/15.jpg",
                    "sexual": 0.0,
                    "violence": 0.0
                },
                "traits": [
                    {
                        "id": "i154",
                        "name": "Tsundere",
                        "group_id": "i72",
                        "group_name": "Personality",
                        "spoiler": 0,
                        "lie": false
                    },
                    {
                        "id": "i8",
                        "name": "Twin Tails",
                        "group_id": "i1",
                        "group_name": "Hair",
                        "spoiler": 0,
                        "lie": false
                    }
                ],
                "vns": [
                    {
                        "id": "v11",
                        "title": "Fate/stay night",
                        "role": "main",
                        "spoiler": 0
                    }
                ]
            }
            """.trimIndent()
        )
        val character = VnCharacter.fromJson(charJson)
        assertEquals("c15", character.id)
        assertEquals("Tohsaka Rin", character.name)
        assertEquals(2, character.traits.size)
        assertEquals("Tsundere", character.traits[0].name)
        assertEquals("Personality", character.traits[0].groupName)
        assertEquals(1, character.vns.size)
        assertEquals("main", character.vns[0].role)
    }

    @Test
    fun `ThemeCode and DefaultChartStats work cleanly`() {
        assertEquals(11, ThemeCode.entries.size)
        val stats = DefaultChartStats.items()
        assertEquals(7, stats.size)

        val settings = SettingsState(themeCode = ThemeCode.FATE, showCoverCensor = true, maxPreviewItem = 20)
        assertEquals(ThemeCode.FATE, ThemeCode.fromCode(settings.themeCode.code))
    }

    @Test
    fun `VnReview parses VNDB reviews overview and full review HTML`() {
        val sampleHtml = """
            <div class="mainbox"><h1>Reviews</h1></div>
            <div class="reviews">
            <article><div><span>Long by <a href="/u168146">fullbuster</a> on 2020-09-03</span><span>10/10</span></div><div></div><div><span>&lt;<a href="/report/w80">report</a>></span>Fate/stay night is a masterpiece.<br>Great character writing. <a href="/w80#review">Read more »</a></div><div><a href="/w80#threadstart">5 comments</a><small>21 points </small></div></article>
            </div>
        """.trimIndent()
        val parsed = com.vndblite.dcj.data.model.VnReview.parseReviewsFromVndbHtml(sampleHtml)
        assertEquals(1, parsed.size)
        val rev = parsed[0]
        assertEquals("w80", rev.id)
        assertEquals("fullbuster", rev.username)
        assertEquals("Long", rev.reviewType)
        assertEquals("2020-09-03", rev.date)
        assertEquals("10/10", rev.rating)
        assertEquals("5 comments", rev.commentsCount)
        assertEquals("21 points", rev.points)
        assertTrue(rev.hasReadMore)
        assertTrue(rev.text.contains("Fate/stay night is a masterpiece."))

        val fullPageHtml = """
            <table><tr><td>Review</td><td>Fate/stay night is a masterpiece.<br><br>Full review body here with <span class="spoiler">hidden route detail</span>.</td></tr></table>
        """.trimIndent()
        val fullText = com.vndblite.dcj.data.model.VnReview.parseFullReviewFromHtml(fullPageHtml)
        assertEquals("Fate/stay night is a masterpiece.\n\nFull review body here with [Spoiler: hidden route detail].", fullText)
    }
}
