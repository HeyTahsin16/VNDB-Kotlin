package com.vndblite.dcj.data.model

import androidx.compose.ui.graphics.Color
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min

enum class ThemeCode(
    val code: String,
    val themeName: String,
    val backgroundImgPath: String,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val isDark: Boolean
) {
    AGL(
        code = "agl",
        themeName = "Angel Serenade",
        backgroundImgPath = "assets/images/bg/bgAngel.png",
        primary = Color(0xFF325064),
        secondary = Color(0xFF8CDCFF),
        tertiary = Color.White,
        isDark = false
    ),
    AIR(
        code = "air",
        themeName = "AIR",
        backgroundImgPath = "assets/images/bg/bgAIR.png",
        primary = Color(0xFFB4DCFF),
        secondary = Color(0xFF5082E1),
        tertiary = Color.Black,
        isDark = true
    ),
    EV17(
        code = "ev17",
        themeName = "Ever17",
        backgroundImgPath = "assets/images/bg/bgEver17.png",
        primary = Color(0xFF6EC8D2),
        secondary = Color(0xFFF0AA64),
        tertiary = Color.Black,
        isDark = true
    ),
    FATE(
        code = "fate",
        themeName = "Fate/Stay Night",
        backgroundImgPath = "assets/images/bg/bgFate.png",
        primary = Color(0xFF463232),
        secondary = Color(0xFFD26446),
        tertiary = Color.White,
        isDark = false
    ),
    CARNVL(
        code = "carnvl",
        themeName = "Gekkou No Carnevale",
        backgroundImgPath = "assets/images/bg/bgCarnevale.png",
        primary = Color(0xFF141414),
        secondary = Color(0xFFBEBEBE),
        tertiary = Color.White,
        isDark = false
    ),
    HGRSHI(
        code = "hgrshi",
        themeName = "Higurashi No Naku Koro Ni",
        backgroundImgPath = "assets/images/bg/bgHigurashi.png",
        primary = Color(0xFFF5B4AF),
        secondary = Color(0xFFDCDCDC),
        tertiary = Color.Black,
        isDark = true
    ),
    TSUKHME(
        code = "tsukhme",
        themeName = "Tsukihime",
        backgroundImgPath = "assets/images/bg/bgTsukihime.png",
        primary = Color(0xFF645050),
        secondary = Color(0xFFFA4646),
        tertiary = Color.White,
        isDark = false
    ),
    TOHO(
        code = "toho",
        themeName = "Touhou",
        backgroundImgPath = "assets/images/bg/bgTouhou.png",
        primary = Color(0xFFAFAFAF),
        secondary = Color(0xFFDCD7D7),
        tertiary = Color.Black,
        isDark = true
    ),
    SEINRKN(
        code = "seinrkn",
        themeName = "Seinarukana",
        backgroundImgPath = "assets/images/bg/bgSeinaru.png",
        primary = Color(0xFFFFF0F0),
        secondary = Color(0xFFFF4696),
        tertiary = Color.Black,
        isDark = true
    ),
    LILBUSTS(
        code = "lilbusts",
        themeName = "Little Busters!",
        backgroundImgPath = "assets/images/bg/bgLittleBusters.png",
        primary = Color(0xFFFAC8FA),
        secondary = Color(0xFFF050D7),
        tertiary = Color.Black,
        isDark = true
    ),
    SAYA(
        code = "saya",
        themeName = "Saya No Uta",
        backgroundImgPath = "assets/images/bg/bgSaya.png",
        primary = Color(0xFF326450),
        secondary = Color(0xFFF0B48C),
        tertiary = Color.White,
        isDark = false
    );

    companion object {
        fun fromCode(code: String?): ThemeCode {
            if (code == null) return AGL
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) || it.name.equals(code, ignoreCase = true) } ?: AGL
        }
    }
}

data class VnImage(
    val url: String = "",
    val thumbnail: String = "",
    val sexual: Double = 0.0,
    val violence: Double = 0.0
) {
    val isExplicit: Boolean
        get() = sexual >= 1.0 || violence >= 1.0

    val displayUrl: String
        get() = thumbnail.ifEmpty { url }

    fun toJson(): JSONObject = JSONObject().apply {
        put("url", url)
        put("thumbnail", thumbnail)
        put("sexual", sexual)
        put("violence", violence)
    }

    companion object {
        fun fromJson(obj: JSONObject?): VnImage {
            if (obj == null) return VnImage()
            return VnImage(
                url = obj.optString("url", ""),
                thumbnail = obj.optString("thumbnail", ""),
                sexual = obj.optDouble("sexual", 0.0).let { if (it.isNaN()) 0.0 else it },
                violence = obj.optDouble("violence", 0.0).let { if (it.isNaN()) 0.0 else it }
            )
        }
    }
}

data class VnTag(
    val id: String,
    val name: String,
    val rating: Double = 0.0,
    val spoiler: Double = 0.0
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("rating", rating)
        put("spoiler", spoiler)
    }

    companion object {
        fun fromJson(obj: JSONObject): VnTag = VnTag(
            id = obj.optString("id", ""),
            name = obj.optString("name", "???"),
            rating = obj.optDouble("rating", 0.0).let { if (it.isNaN()) 0.0 else it },
            spoiler = obj.optDouble("spoiler", 0.0).let { if (it.isNaN()) 0.0 else it }
        )
    }
}

data class VnTrait(
    val id: String,
    val name: String,
    val groupId: String = "",
    val groupName: String = "",
    val description: String = "",
    val aliases: List<String> = emptyList(),
    val charCount: Int = 0,
    val spoiler: Int = 0
) {
    val displayLabel: String
        get() = if (groupName.isNotBlank()) "$groupName: $name" else name

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("group_id", groupId)
        put("group_name", groupName)
        put("description", description)
        put("char_count", charCount)
        put("spoiler", spoiler)
        put("aliases", JSONArray(aliases))
    }

    companion object {
        fun fromJson(obj: JSONObject): VnTrait {
            val aliasesArr = obj.optJSONArray("aliases")
            val aliasList = mutableListOf<String>()
            if (aliasesArr != null) {
                for (i in 0 until aliasesArr.length()) {
                    val a = aliasesArr.optString(i)
                    if (a.isNotBlank()) aliasList.add(a)
                }
            }
            return VnTrait(
                id = obj.optString("id", ""),
                name = obj.optString("name", "???"),
                groupId = obj.optString("group_id", ""),
                groupName = obj.optString("group_name", ""),
                description = obj.optString("description", ""),
                aliases = aliasList,
                charCount = obj.optInt("char_count", 0),
                spoiler = obj.optInt("spoiler", 0)
            )
        }
    }
}

data class CharacterVnLink(
    val id: String,
    val title: String,
    val role: String = "appears",
    val spoiler: Int = 0
) {
    val roleDisplay: String
        get() = when (role.lowercase()) {
            "main" -> "Protagonist"
            "primary" -> "Main Character"
            "side" -> "Side Character"
            else -> "Appears"
        }
}

data class VnCharacter(
    val id: String,
    val name: String,
    val original: String = "",
    val aliases: List<String> = emptyList(),
    val description: String = "",
    val image: VnImage? = null,
    val bloodType: String = "",
    val height: Int? = null,
    val weight: Int? = null,
    val bust: Int? = null,
    val waist: Int? = null,
    val hips: Int? = null,
    val cup: String = "",
    val age: Int? = null,
    val birthday: List<Int> = emptyList(),
    val sex: String = "",
    val vns: List<CharacterVnLink> = emptyList(),
    val traits: List<VnTrait> = emptyList()
) {
    val RoleSummary: String
        get() = vns.firstOrNull()?.roleDisplay ?: ""

    val formattedStats: String
        get() {
            val parts = mutableListOf<String>()
            if (sex.isNotBlank()) {
                val s = when (sex.lowercase()) {
                    "m" -> "Male"
                    "f" -> "Female"
                    "b" -> "Both"
                    else -> sex
                }
                parts.add(s)
            }
            if (age != null && age > 0) parts.add("${age}yo")
            if (birthday.size >= 2 && birthday[0] > 0 && birthday[1] > 0) {
                val months = listOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                val m = months.getOrElse(birthday[0]) { "${birthday[0]}" }
                parts.add("Birthday: $m ${birthday[1]}")
            }
            if (height != null && height > 0) parts.add("${height}cm")
            if (weight != null && weight > 0) parts.add("${weight}kg")
            if (bust != null && waist != null && hips != null && bust > 0) {
                val cupStr = if (cup.isNotBlank()) " ($cup)" else ""
                parts.add("B$bust/W$waist/H$hips$cupStr")
            }
            if (bloodType.isNotBlank()) parts.add("Blood: ${bloodType.uppercase()}")
            return parts.joinToString(" • ")
        }

    companion object {
        fun fromJson(obj: JSONObject): VnCharacter {
            val aliasesArr = obj.optJSONArray("aliases")
            val aliases = mutableListOf<String>()
            if (aliasesArr != null) {
                for (i in 0 until aliasesArr.length()) {
                    val s = aliasesArr.optString(i)
                    if (s.isNotBlank()) aliases.add(s)
                }
            }

            val bdayArr = obj.optJSONArray("birthday")
            val bday = mutableListOf<Int>()
            if (bdayArr != null && bdayArr.length() >= 2) {
                bday.add(bdayArr.optInt(0, 0))
                bday.add(bdayArr.optInt(1, 0))
            }

            val sexArr = obj.optJSONArray("sex")
            val sexStr = if (sexArr != null && sexArr.length() > 0) {
                sexArr.optString(0, "")
            } else {
                obj.optString("sex", "")
            }

            val vnsArr = obj.optJSONArray("vns")
            val vnLinks = mutableListOf<CharacterVnLink>()
            if (vnsArr != null) {
                for (i in 0 until vnsArr.length()) {
                    val v = vnsArr.optJSONObject(i) ?: continue
                    val id = v.optString("id", "")
                    val title = v.optString("title", id)
                    val role = v.optString("role", "appears")
                    val spoiler = v.optInt("spoiler", 0)
                    if (id.isNotBlank() && vnLinks.none { it.id == id }) {
                        vnLinks.add(CharacterVnLink(id = id, title = title, role = role, spoiler = spoiler))
                    }
                }
            }

            val traitsArr = obj.optJSONArray("traits")
            val traitList = mutableListOf<VnTrait>()
            if (traitsArr != null) {
                for (i in 0 until traitsArr.length()) {
                    val t = traitsArr.optJSONObject(i) ?: continue
                    traitList.add(VnTrait.fromJson(t))
                }
            }

            val imgObj = obj.optJSONObject("image")
            val image = if (imgObj != null) VnImage.fromJson(imgObj) else null

            return VnCharacter(
                id = obj.optString("id", ""),
                name = obj.optString("name", "Unknown"),
                original = if (obj.isNull("original")) "" else obj.optString("original", ""),
                aliases = aliases,
                description = if (obj.isNull("description")) "" else obj.optString("description", ""),
                image = image,
                bloodType = if (obj.isNull("blood_type")) "" else obj.optString("blood_type", ""),
                height = if (obj.isNull("height")) null else obj.optInt("height", 0).takeIf { it > 0 },
                weight = if (obj.isNull("weight")) null else obj.optInt("weight", 0).takeIf { it > 0 },
                bust = if (obj.isNull("bust")) null else obj.optInt("bust", 0).takeIf { it > 0 },
                waist = if (obj.isNull("waist")) null else obj.optInt("waist", 0).takeIf { it > 0 },
                hips = if (obj.isNull("hips")) null else obj.optInt("hips", 0).takeIf { it > 0 },
                cup = if (obj.isNull("cup")) "" else obj.optString("cup", ""),
                age = if (obj.isNull("age")) null else obj.optInt("age", 0).takeIf { it > 0 },
                birthday = bday,
                sex = sexStr,
                vns = vnLinks,
                traits = traitList
            )
        }
    }
}

data class Developer(
    val id: String,
    val name: String
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
    }

    companion object {
        fun fromJson(obj: JSONObject): Developer = Developer(
            id = obj.optString("id", ""),
            name = obj.optString("name", "")
        )
    }
}

data class VnRelation(
    val id: String,
    val relation: String,
    val relationOfficial: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("relation", relation)
        put("relation_official", relationOfficial)
    }

    companion object {
        fun fromJson(obj: JSONObject): VnRelation = VnRelation(
            id = obj.optString("id", ""),
            relation = obj.optString("relation", ""),
            relationOfficial = obj.optBoolean("relation_official", false)
        )
    }
}

data class VnDataPhase01(
    val id: String,
    val title: String,
    val image: VnImage? = null,
    val rating: Double = 0.0,
    val votecount: Int = 0,
    val olang: String = "Unknown",
    val length: Int = 0,
    val released: String = "Unknown",
    val description: String = "--"
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        if (image != null) put("image", image.toJson())
        put("rating", rating)
        put("votecount", votecount)
        put("olang", olang)
        put("length", length)
        put("released", released)
        put("description", description)
    }

    companion object {
        fun fromJson(obj: JSONObject): VnDataPhase01 {
            val imgObj = obj.optJSONObject("image")
            return VnDataPhase01(
                id = obj.optString("id", ""),
                title = obj.optString("title", "Unknown"),
                image = VnImage.fromJson(imgObj),
                rating = obj.optDouble("rating", 0.0).let { if (it.isNaN()) 0.0 else it },
                votecount = obj.optInt("votecount", 0),
                olang = if (obj.isNull("olang")) "Unknown" else obj.optString("olang", "Unknown"),
                length = obj.optInt("length", 0),
                released = if (obj.isNull("released")) "Unknown" else obj.optString("released", "Unknown"),
                description = if (obj.isNull("description") || obj.optString("description").isBlank()) "--" else obj.optString("description", "--")
            )
        }
    }
}

data class VnDataPhase02(
    val id: String,
    val developers: List<String> = emptyList(),
    val devstatus: Int = -1,
    val lengthMinutes: Int = 0,
    val aliases: List<String> = emptyList(),
    val minage: List<Int> = emptyList(),
    val tags: List<VnTag> = emptyList(),
    val screenshots: List<VnImage> = emptyList(),
    val languages: List<String> = emptyList(),
    val mtl: List<String> = emptyList(),
    val platforms: List<String> = emptyList()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("developers", JSONArray(developers))
        put("devstatus", devstatus)
        put("length_minutes", lengthMinutes)
        put("aliases", JSONArray(aliases))
        put("minage", JSONArray(minage))
        put("tags", JSONArray().apply { tags.forEach { put(it.toJson()) } })
        put("screenshots", JSONArray().apply { screenshots.forEach { put(it.toJson()) } })
        put("languages", JSONArray(languages))
        put("mtl", JSONArray(mtl))
        put("platforms", JSONArray(platforms))
    }

    companion object {
        private fun getScreenshotUrl(screenshotId: String): String {
            if (screenshotId.length < 3) return ""
            val pathCode = screenshotId.substring(screenshotId.length - 2)
            val screenshotCode = screenshotId.substring(2)
            return "https://t.vndb.org/sf/$pathCode/$screenshotCode.jpg"
        }

        fun fromJson(obj: JSONObject): VnDataPhase02 {
            val aliasesArr = obj.optJSONArray("aliases")
            val aliases = mutableListOf<String>()
            if (aliasesArr != null) {
                for (i in 0 until aliasesArr.length()) {
                    val s = aliasesArr.optString(i)
                    if (s.isNotBlank()) aliases.add(s)
                }
            }

            val devsArr = obj.optJSONArray("developers")
            val developers = mutableListOf<String>()
            if (devsArr != null) {
                for (i in 0 until devsArr.length()) {
                    val item = devsArr.opt(i)
                    when (item) {
                        is JSONObject -> {
                            val name = item.optString("name", "")
                            if (name.isNotBlank()) developers.add(name)
                        }
                        is String -> if (item.isNotBlank()) developers.add(item)
                    }
                }
            }

            val mtlArr = obj.optJSONArray("mtl")
            val mtl = mutableListOf<String>()
            if (mtlArr != null) {
                for (i in 0 until mtlArr.length()) {
                    val s = mtlArr.optString(i)
                    if (s.isNotBlank()) mtl.add(s)
                }
            }

            val minageArr = obj.optJSONArray("minage")
            val minage = mutableListOf<Int>()
            if (minageArr != null) {
                for (i in 0 until minageArr.length()) {
                    minage.add(minageArr.optInt(i, 0))
                }
            }

            val langsArr = obj.optJSONArray("languages")
            val languages = mutableListOf<String>()
            if (langsArr != null) {
                for (i in 0 until langsArr.length()) {
                    val s = langsArr.optString(i)
                    if (s.isNotBlank()) languages.add(s)
                }
            }

            val platfArr = obj.optJSONArray("platforms")
            val platforms = mutableListOf<String>()
            if (platfArr != null) {
                for (i in 0 until platfArr.length()) {
                    val s = platfArr.optString(i)
                    if (s.isNotBlank()) platforms.add(s)
                }
            }

            val screensArr = obj.optJSONArray("screenshots")
            val screenshots = mutableListOf<VnImage>()
            if (screensArr != null) {
                for (i in 0 until screensArr.length()) {
                    val sc = screensArr.optJSONObject(i) ?: continue
                    val rawUrl = sc.optString("url", "")
                    val url = if (rawUrl.contains("t.vndb.org")) {
                        rawUrl
                    } else {
                        getScreenshotUrl(sc.optString("id", ""))
                    }
                    screenshots.add(
                        VnImage(
                            url = url,
                            thumbnail = url,
                            sexual = sc.optDouble("sexual", 0.0).let { if (it.isNaN()) 0.0 else it },
                            violence = sc.optDouble("violence", 0.0).let { if (it.isNaN()) 0.0 else it }
                        )
                    )
                }
            }

            val tagsArr = obj.optJSONArray("tags")
            val tags = mutableListOf<VnTag>()
            if (tagsArr != null) {
                for (i in 0 until tagsArr.length()) {
                    val t = tagsArr.optJSONObject(i) ?: continue
                    tags.add(VnTag.fromJson(t))
                }
            }

            return VnDataPhase02(
                id = obj.optString("id", ""),
                developers = developers,
                devstatus = obj.optInt("devstatus", -1),
                lengthMinutes = obj.optInt("length_minutes", 0),
                aliases = aliases,
                minage = minage,
                tags = tags,
                screenshots = screenshots,
                languages = languages,
                mtl = mtl,
                platforms = platforms
            )
        }

        fun processAdditionalP2(releasesArray: JSONArray?): Pair<List<String>, List<Int>> {
            if (releasesArray == null) return emptyList<String>() to emptyList()
            val minages = mutableListOf<Int>()
            val mtlLangs = mutableSetOf<String>()

            for (i in 0 until releasesArray.length()) {
                val seg = releasesArray.optJSONObject(i) ?: continue
                if (!seg.isNull("minage")) {
                    minages.add(seg.optInt("minage", 0))
                }
                val langs = seg.optJSONArray("languages")
                if (langs != null && langs.length() > 0) {
                    val firstLang = langs.optJSONObject(0)
                    if (firstLang != null && firstLang.optBoolean("mtl", false)) {
                        val code = firstLang.optString("lang", "")
                        if (code.isNotBlank()) mtlLangs.add(code)
                    }
                }
            }

            val ages = if (minages.isNotEmpty()) {
                val minVal = minages.minOrNull() ?: 0
                val maxVal = minages.maxOrNull() ?: 0
                listOf(minVal, maxVal).distinct()
            } else {
                emptyList()
            }

            return mtlLangs.toList() to ages
        }
    }
}

data class VnDataPhase03(
    val id: String,
    val extlinks: List<String> = emptyList(),
    val relations: List<VnRelation> = emptyList()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("extlinks", JSONArray(extlinks))
        put("relations", JSONArray().apply { relations.forEach { put(it.toJson()) } })
    }

    companion object {
        fun fromJson(obj: JSONObject): VnDataPhase03 {
            val extArr = obj.optJSONArray("extlinks")
            val extlinks = mutableListOf<String>()
            if (extArr != null) {
                for (i in 0 until extArr.length()) {
                    val item = extArr.opt(i)
                    when (item) {
                        is JSONObject -> {
                            val u = item.optString("url", "")
                            if (u.isNotBlank()) extlinks.add(u)
                        }
                        is String -> if (item.isNotBlank()) extlinks.add(item)
                    }
                }
            }

            val relArr = obj.optJSONArray("relations")
            val relations = mutableListOf<VnRelation>()
            if (relArr != null) {
                for (i in 0 until relArr.length()) {
                    val r = relArr.optJSONObject(i) ?: continue
                    relations.add(VnRelation.fromJson(r))
                }
            }

            return VnDataPhase03(
                id = obj.optString("id", ""),
                extlinks = extlinks,
                relations = relations
            )
        }
    }
}

data class VnRecord(
    val id: String,
    val title: String,
    val status: String,
    val vote: Int = 0,
    val added: String? = null,
    val started: String? = null,
    val finished: String? = null,
    val lastmod: String? = null,
    val notes: String? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("status", status)
        put("vote", vote)
        if (added != null) put("added", added)
        if (started != null) put("started", started)
        if (finished != null) put("finished", finished)
        if (lastmod != null) put("lastmod", lastmod)
        if (notes != null) put("notes", notes)
    }

    companion object {
        fun fromJson(obj: JSONObject): VnRecord = VnRecord(
            id = obj.optString("id", ""),
            title = obj.optString("title", ""),
            status = obj.optString("status", "playing"),
            vote = obj.optInt("vote", 0),
            added = obj.optString("added").takeIf { it.isNotBlank() && it != "null" },
            started = obj.optString("started").takeIf { it.isNotBlank() && it != "null" },
            finished = obj.optString("finished").takeIf { it.isNotBlank() && it != "null" },
            lastmod = obj.optString("lastmod").takeIf { it.isNotBlank() && it != "null" },
            notes = obj.optString("notes").takeIf { it.isNotBlank() && it != "null" }
        )
    }
}

enum class CollectionStatusCode(
    val id: Int,
    val statusName: String,
    val displayTitle: String,
    val color: Color,
    val imagePath: String
) {
    PLAYING(1, "playing", "Playing", Color(0xFF00C3FF), "assets/images/status/playing.png"),
    FINISHED(2, "finished", "Finished", Color(0xFF14E614), "assets/images/status/finished.png"),
    STALLED(3, "stalled", "Stalled", Color(0xFFFA9600), "assets/images/status/stalled.png"),
    DROPPED(4, "dropped", "Dropped", Color(0xFFFF3C32), "assets/images/status/dropped.png"),
    WISHLIST(5, "wishlist", "Wishlist", Color(0xFFFFE600), "assets/images/status/wishlist.png"),
    BLACKLIST(6, "blacklist", "Blacklist", Color(0xFFBE3737), "assets/images/status/blacklist.png");

    companion object {
        fun fromName(name: String?): CollectionStatusCode =
            entries.firstOrNull { it.statusName.equals(name, ignoreCase = true) } ?: PLAYING
    }
}

enum class SortableCode(val code: String, val title: String) {
    SEARCHRANK("searchrank", "None"),
    TITLE("title", "Title"),
    ADDED("added", "Time added"),
    STARTED("started", "Time started"),
    LENGTH_MINUTES("length_minutes", "Length"),
    RATING("rating", "Rating"),
    RELEASED("released", "Release date"),
    VOTECOUNT("votecount", "Vote count"),
    VOTE("vote", "Voted"),
    COLLECTION("collection", "Collection");

    companion object {
        val localSortables = listOf(
            TITLE, ADDED, STARTED, LENGTH_MINUTES, RATING, RELEASED, VOTECOUNT, VOTE
        )
        val remoteSortables = listOf(
            SEARCHRANK, TITLE, RATING, RELEASED, VOTECOUNT
        )
    }
}

data class SortData(
    val sort: String = SortableCode.SEARCHRANK.code,
    val reverse: Boolean = true
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("sort", sort)
        put("reverse", reverse)
    }

    companion object {
        val DEFAULT_REMOTE = SortData(sort = SortableCode.SEARCHRANK.code, reverse = true)
        val DEFAULT_LOCAL = SortData(sort = SortableCode.TITLE.code, reverse = false)

        fun fromJson(obj: JSONObject?): SortData {
            if (obj == null) return DEFAULT_LOCAL
            return SortData(
                sort = obj.optString("sort", SortableCode.TITLE.code),
                reverse = obj.optBoolean("reverse", false)
            )
        }
    }
}

data class FilterData(
    val search: String = "",
    val andOr: String = "and",
    val lang: List<String> = emptyList(),
    val devstatus: List<Int> = emptyList(),
    val olang: List<String> = emptyList(),
    val platform: List<String> = emptyList(),
    val tag: List<VnTag> = emptyList(),
    val dev: List<Developer> = emptyList(),
    val trait: List<VnTrait> = emptyList(),
    val traitAndOr: String = "and",
    val charRoles: List<String> = emptyList(),
    val charRoleAndOr: String = "or",
    val charSexes: List<String> = emptyList(),
    val charSexAndOr: String = "or",
    val releaseWindow: String = "",
    val minage: Int = 0
) {
    val hasActiveRemoteFilters: Boolean
        get() = lang.isNotEmpty() ||
            devstatus.isNotEmpty() ||
            olang.isNotEmpty() ||
            platform.isNotEmpty() ||
            tag.isNotEmpty() ||
            dev.isNotEmpty() ||
            trait.isNotEmpty() ||
            charRoles.isNotEmpty() ||
            charSexes.isNotEmpty() ||
            releaseWindow.isNotBlank()

    val hasActiveLocalFilters: Boolean
        get() = minage > 0 ||
            lang.isNotEmpty() ||
            devstatus.isNotEmpty() ||
            olang.isNotEmpty() ||
            platform.isNotEmpty()

    fun toVndbFilterJsonArray(): JSONArray {
        val arr = JSONArray()
        arr.put(andOr)
        arr.put(JSONArray().put("search").put("=").put(search.trim()))

        if (releaseWindow.equals("latest", ignoreCase = true)) {
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
            arr.put(JSONArray().put("released").put("<=").put(today))
            arr.put(JSONArray().put("released").put("!=").put("tba"))
        } else if (releaseWindow.equals("upcoming", ignoreCase = true)) {
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
            arr.put(JSONArray().put("released").put(">").put(today))
            arr.put(JSONArray().put("released").put("!=").put("tba"))
        }

        lang.forEach { l ->
            arr.put(JSONArray().put("lang").put("=").put(l))
        }
        devstatus.forEach { ds ->
            arr.put(JSONArray().put("devstatus").put("=").put(ds))
        }
        olang.forEach { ol ->
            arr.put(JSONArray().put("olang").put("=").put(ol))
        }
        platform.forEach { p ->
            arr.put(JSONArray().put("platform").put("=").put(p))
        }
        tag.forEach { t ->
            arr.put(JSONArray().put("tag").put("=").put(t.id))
        }
        dev.forEach { d ->
            arr.put(
                JSONArray()
                    .put("developer")
                    .put("=")
                    .put(JSONArray().put("id").put("=").put(d.id))
            )
        }
        // VNDB API v2 character filters on /kana/vn:
        if (charRoles.size == 1) {
            arr.put(
                JSONArray()
                    .put("character")
                    .put("=")
                    .put(JSONArray().put("role").put("=").put(charRoles[0]))
            )
        } else if (charRoles.size > 1) {
            val roleGroup = JSONArray().put(charRoleAndOr)
            charRoles.forEach { r ->
                roleGroup.put(JSONArray().put("role").put("=").put(r))
            }
            arr.put(JSONArray().put("character").put("=").put(roleGroup))
        }

        if (charSexes.size == 1) {
            arr.put(
                JSONArray()
                    .put("character")
                    .put("=")
                    .put(JSONArray().put("sex").put("=").put(charSexes[0]))
            )
        } else if (charSexes.size > 1) {
            val sexGroup = JSONArray().put(charSexAndOr)
            charSexes.forEach { s ->
                sexGroup.put(JSONArray().put("sex").put("=").put(s))
            }
            arr.put(JSONArray().put("character").put("=").put(sexGroup))
        }

        if (trait.size == 1) {
            arr.put(
                JSONArray()
                    .put("character")
                    .put("=")
                    .put(JSONArray().put("trait").put("=").put(trait[0].id))
            )
        } else if (trait.size > 1) {
            val traitGroup = JSONArray().put(traitAndOr)
            trait.forEach { tr ->
                traitGroup.put(JSONArray().put("trait").put("=").put(tr.id))
            }
            arr.put(JSONArray().put("character").put("=").put(traitGroup))
        }
        return arr
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("search", search)
        put("andOr", andOr)
        put("lang", JSONArray(lang))
        put("devstatus", JSONArray(devstatus))
        put("olang", JSONArray(olang))
        put("platform", JSONArray(platform))
        put("tag", JSONArray().apply { tag.forEach { put(it.toJson()) } })
        put("dev", JSONArray().apply { dev.forEach { put(it.toJson()) } })
        put("trait", JSONArray().apply { trait.forEach { put(it.toJson()) } })
        put("traitAndOr", traitAndOr)
        put("charRoles", JSONArray(charRoles))
        put("charRoleAndOr", charRoleAndOr)
        put("charSexes", JSONArray(charSexes))
        put("charSexAndOr", charSexAndOr)
        put("releaseWindow", releaseWindow)
        put("minage", minage)
    }

    companion object {
        val DEFAULT_REMOTE = FilterData(search = "", andOr = "and")
        val DEFAULT_LOCAL = FilterData(search = "", minage = 0)

        fun fromJson(obj: JSONObject?): FilterData {
            if (obj == null) return DEFAULT_LOCAL
            val langList = mutableListOf<String>()
            obj.optJSONArray("lang")?.let { a -> for (i in 0 until a.length()) langList.add(a.optString(i)) }

            val devStatusList = mutableListOf<Int>()
            obj.optJSONArray("devstatus")?.let { a -> for (i in 0 until a.length()) devStatusList.add(a.optInt(i)) }

            val olangList = mutableListOf<String>()
            obj.optJSONArray("olang")?.let { a -> for (i in 0 until a.length()) olangList.add(a.optString(i)) }

            val platfList = mutableListOf<String>()
            obj.optJSONArray("platform")?.let { a -> for (i in 0 until a.length()) platfList.add(a.optString(i)) }

            val tagList = mutableListOf<VnTag>()
            obj.optJSONArray("tag")?.let { a ->
                for (i in 0 until a.length()) {
                    a.optJSONObject(i)?.let { tagList.add(VnTag.fromJson(it)) }
                }
            }

            val devList = mutableListOf<Developer>()
            obj.optJSONArray("dev")?.let { a ->
                for (i in 0 until a.length()) {
                    a.optJSONObject(i)?.let { devList.add(Developer.fromJson(it)) }
                }
            }

            val traitList = mutableListOf<VnTrait>()
            obj.optJSONArray("trait")?.let { a ->
                for (i in 0 until a.length()) {
                    a.optJSONObject(i)?.let { traitList.add(VnTrait.fromJson(it)) }
                }
            }

            val roleList = mutableListOf<String>()
            obj.optJSONArray("charRoles")?.let { a -> for (i in 0 until a.length()) roleList.add(a.optString(i)) }

            val sexList = mutableListOf<String>()
            obj.optJSONArray("charSexes")?.let { a -> for (i in 0 until a.length()) sexList.add(a.optString(i)) }

            return FilterData(
                search = obj.optString("search", ""),
                andOr = obj.optString("andOr", "and"),
                lang = langList,
                devstatus = devStatusList,
                olang = olangList,
                platform = platfList,
                tag = tagList,
                dev = devList,
                trait = traitList,
                traitAndOr = obj.optString("traitAndOr", "and"),
                charRoles = roleList,
                charRoleAndOr = obj.optString("charRoleAndOr", "or"),
                charSexes = sexList,
                charSexAndOr = obj.optString("charSexAndOr", "or"),
                releaseWindow = obj.optString("releaseWindow", ""),
                minage = obj.optInt("minage", 0)
            )
        }
    }
}

data class VnQuote(
    val id: String,
    val quote: String,
    val score: Int = 0,
    val vn: VnDataPhase01? = null,
    val characterId: String = "",
    val characterName: String = "",
    val characterOriginal: String = ""
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("quote", quote)
        put("score", score)
        if (vn != null) put("vn", vn.toJson())
        if (characterId.isNotBlank() || characterName.isNotBlank()) {
            put("character", JSONObject().apply {
                put("id", characterId)
                put("name", characterName)
                put("original", characterOriginal)
            })
        }
    }

    companion object {
        fun fromJson(obj: JSONObject): VnQuote {
            val vnObj = obj.optJSONObject("vn")
            val charObj = obj.optJSONObject("character")
            return VnQuote(
                id = obj.optString("id", ""),
                quote = obj.optString("quote", "").trim(),
                score = obj.optInt("score", 0),
                vn = if (vnObj != null && vnObj.has("id")) VnDataPhase01.fromJson(vnObj) else null,
                characterId = charObj?.optString("id", "") ?: "",
                characterName = charObj?.optString("name", "") ?: "",
                characterOriginal = if (charObj == null || charObj.isNull("original")) "" else charObj.optString("original", "")
            )
        }
    }
}

data class VnReview(
    val id: String,
    val username: String,
    val userId: String = "",
    val reviewType: String = "Short",
    val date: String = "",
    val rating: String = "",
    val text: String = "",
    val points: String = "",
    val commentsCount: String = "",
    val hasReadMore: Boolean = false
) {
    companion object {
        fun parseReviewsFromVndbHtml(html: String): List<VnReview> {
            val reviewsIdx = html.indexOf("<div class=\"reviews\">")
            if (reviewsIdx == -1) return emptyList()
            val section = html.substring(reviewsIdx)
            val articleRegex = Regex("<article>(.*?)</article>", setOf(RegexOption.DOT_MATCHES_ALL))
            val headerRegex = Regex("(Short|Medium|Long)\\s+by\\s+<a\\s+href=\"/(u\\d+)\"[^>]*>([^<]+)</a>\\s+on\\s+([0-9-]+)")
            val idRegex = Regex("/(w\\d+)")
            val ratingRegex = Regex("<span>(\\d+(?:\\.\\d+)?/10)</span>")
            val bodyRegex = Regex("report</a>&gt;</span>(.*?)</div>\\s*<div>\\s*<a\\s+href=\"/w\\d+#threadstart\"", setOf(RegexOption.DOT_MATCHES_ALL))
            val bodyFallbackRegex = Regex("report</a>></span>(.*?)</div>\\s*<div>\\s*<a\\s+href=\"/w\\d+#threadstart\"", setOf(RegexOption.DOT_MATCHES_ALL))
            val commentsRegex = Regex(">(\\d+\\s+comments?)</a>")
            val pointsRegex = Regex("<small>\\s*([^<]*points?)\\s*</small>")

            val list = mutableListOf<VnReview>()
            for (match in articleRegex.findAll(section)) {
                val content = match.groupValues[1]
                val idMatch = idRegex.find(content) ?: continue
                val reviewId = idMatch.groupValues[1]
                val headerMatch = headerRegex.find(content)
                val reviewType = headerMatch?.groupValues?.getOrElse(1) { "Review" } ?: "Review"
                val userId = headerMatch?.groupValues?.getOrElse(2) { "" } ?: ""
                val username = headerMatch?.groupValues?.getOrElse(3) { "VNDB User" } ?: "VNDB User"
                val date = headerMatch?.groupValues?.getOrElse(4) { "" } ?: ""
                val rating = ratingRegex.find(content)?.groupValues?.getOrElse(1) { "" } ?: ""
                val rawBody = bodyFallbackRegex.find(content)?.groupValues?.getOrElse(1) { "" }
                    ?: bodyRegex.find(content)?.groupValues?.getOrElse(1) { "" }
                    ?: ""
                val hasReadMore = rawBody.contains("Read more")
                val cleanedText = cleanHtmlText(
                    rawBody.replace(Regex("<a\\s+href=\"/w\\d+#review\"[^>]*>\\s*Read more\\s*»\\s*</a>"), "")
                )
                val comments = commentsRegex.find(content)?.groupValues?.getOrElse(1) { "" } ?: ""
                val points = pointsRegex.find(content)?.groupValues?.getOrElse(1) { "" }?.trim() ?: ""

                if (cleanedText.isNotBlank()) {
                    list.add(
                        VnReview(
                            id = reviewId,
                            username = username,
                            userId = userId,
                            reviewType = reviewType,
                            date = date,
                            rating = rating,
                            text = cleanedText,
                            points = points,
                            commentsCount = comments,
                            hasReadMore = hasReadMore
                        )
                    )
                }
            }
            return list
        }

        fun parseFullReviewFromHtml(html: String): String? {
            val fullMatch = Regex("<tr><td>Review</td><td>(.*?)</td></tr>", setOf(RegexOption.DOT_MATCHES_ALL))
                .find(html)?.groupValues?.getOrNull(1) ?: return null
            return cleanHtmlText(fullMatch).takeIf { it.isNotBlank() }
        }

        private fun cleanHtmlText(raw: String): String {
            return raw
                .replace(Regex("<span\\s+class=\"spoiler\"[^>]*>(.*?)</span>", setOf(RegexOption.DOT_MATCHES_ALL)), "[Spoiler: $1]")
                .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
                .replace(Regex("</p>", RegexOption.IGNORE_CASE), "\n\n")
                .replace(Regex("<[^>]+>"), "")
                .replace("&quot;", "\"")
                .replace("&#039;", "'")
                .replace("&apos;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace(Regex("\n{3,}"), "\n\n")
                .trim()
        }
    }
}

object CharacterRoleData {
    val DEFINED_CODES: Map<String, String> = linkedMapOf(
        "main" to "Protagonist",
        "primary" to "Main character",
        "side" to "Side character",
        "appears" to "Makes an appearance"
    )

    fun getLabel(code: String): String = DEFINED_CODES[code] ?: code
}

object CharacterSexData {
    val DEFINED_CODES: Map<String, String> = linkedMapOf(
        "f" to "Female",
        "m" to "Male",
        "b" to "Both",
        "n" to "Sexless"
    )

    fun getLabel(code: String): String = DEFINED_CODES[code] ?: code
}

enum class HomeSectionsCode(
    val id: Int,
    val title: String,
    val labelCode: SortableCode,
    val filter: FilterData,
    val reverseSort: Boolean = true
) {
    COLLECTION(1, "My Collection", SortableCode.COLLECTION, FilterData.DEFAULT_REMOTE, reverseSort = true),
    RATING(2, "Top Rated", SortableCode.RATING, FilterData.DEFAULT_REMOTE, reverseSort = true),
    VOTECOUNT(3, "Top Voted", SortableCode.VOTECOUNT, FilterData.DEFAULT_REMOTE, reverseSort = true),
    POPULAR_ONGOING(4, "Popular Ongoing", SortableCode.VOTECOUNT, FilterData(devstatus = listOf(1)), reverseSort = true),
    LATEST_RELEASES(5, "Latest Releases", SortableCode.RELEASED, FilterData(releaseWindow = "latest"), reverseSort = true),
    UPCOMING_RELEASES(6, "Upcoming Releases", SortableCode.RELEASED, FilterData(releaseWindow = "upcoming"), reverseSort = false)
}

enum class PredefinedHomeSearch(val tag: VnTag, val assetPath: String) {
    ACTION(VnTag(id = "g12", name = "Action"), "assets/images/search/action.png"),
    COMEDY(VnTag(id = "g104", name = "Comedy"), "assets/images/search/comedy.png"),
    DRAMA(VnTag(id = "g147", name = "Drama"), "assets/images/search/drama.png"),
    FANTASY(VnTag(id = "g2", name = "Fantasy"), "assets/images/search/fantasy.png"),
    HORROR(VnTag(id = "g7", name = "Horror"), "assets/images/search/horror.png"),
    MYSTERY(VnTag(id = "g19", name = "Mystery"), "assets/images/search/mystery.png"),
    ROMANCE(VnTag(id = "g96", name = "Romance"), "assets/images/search/romance.png"),
    SCIFI(VnTag(id = "g105", name = "Sci-fi"), "assets/images/search/scifi.png")
}

object PopularCharacterTraits {
    val presets: List<VnTrait> = listOf(
        VnTrait(id = "i154", name = "Tsundere", groupName = "Personality"),
        VnTrait(id = "i162", name = "Kuudere", groupName = "Personality"),
        VnTrait(id = "i155", name = "Yandere", groupName = "Personality"),
        VnTrait(id = "i159", name = "Dandere", groupName = "Personality"),
        VnTrait(id = "i232", name = "Childhood Friend", groupName = "Role"),
        VnTrait(id = "i125", name = "Maid", groupName = "Role"),
        VnTrait(id = "i47", name = "Glasses", groupName = "Clothes"),
        VnTrait(id = "i5", name = "Blonde Hair", groupName = "Hair"),
        VnTrait(id = "i2", name = "Black Hair", groupName = "Hair"),
        VnTrait(id = "i9", name = "Silver/White Hair", groupName = "Hair"),
        VnTrait(id = "i17", name = "Twin Tails", groupName = "Hair"),
        VnTrait(id = "i18", name = "Ponytail", groupName = "Hair")
    )
}

object LangData {
    val DEFINED_CODES: Map<String, String> = linkedMapOf(
        "ar" to "Arabic",
        "bg" to "Bulgarian",
        "ca" to "Catalan",
        "cs" to "Czech",
        "de" to "German",
        "el" to "Greek",
        "en" to "English",
        "es" to "Spanish",
        "fi" to "Finnish",
        "fr" to "French",
        "hr" to "Croatian",
        "hu" to "Hungarian",
        "id" to "Indonesian",
        "it" to "Italian",
        "ja" to "Japanese",
        "ko" to "Korean",
        "ms" to "Malay",
        "nl" to "Dutch",
        "no" to "Norwegian",
        "pl" to "Polish",
        "pt-br" to "Portuguese (Brazil)",
        "pt-pt" to "Portuguese (Portugal)",
        "ro" to "Romanian",
        "ru" to "Russian",
        "sk" to "Slovak",
        "sr" to "Serbian",
        "sv" to "Swedish",
        "ta" to "Tamil",
        "th" to "Thai",
        "tr" to "Turkish",
        "uk" to "Ukrainian",
        "vi" to "Vietnamese",
        "zh-Hans" to "Simplified Chinese",
        "zh-Hant" to "Traditional Chinese"
    )

    val DEFINED_CODES_ORIGIN: Map<String, String> = linkedMapOf(
        "ar" to "Arabic",
        "de" to "German",
        "en" to "English",
        "es" to "Spanish",
        "fr" to "French",
        "id" to "Indonesian",
        "ja" to "Japanese",
        "ko" to "Korean",
        "pt-br" to "Portuguese (Brazil)",
        "ru" to "Russian",
        "ta" to "Tamil",
        "th" to "Thai",
        "tr" to "Turkish",
        "vi" to "Vietnamese",
        "zh-Hans" to "Simplified Chinese",
        "zh-Hant" to "Traditional Chinese"
    )

    fun getFullLanguage(key: String?): String = DEFINED_CODES[key] ?: "Others"

    fun getFlagPath(key: String?): String {
        if (key != null && DEFINED_CODES.containsKey(key)) {
            return "assets/images/flags/$key.png"
        }
        return "assets/images/flags/xx.png"
    }
}

object PlatfData {
    val DEFINED_CODES: Map<String, String> = linkedMapOf(
        "win" to "Windows",
        "lin" to "Linux",
        "mac" to "Mac OS",
        "web" to "Website",
        "tdo" to "3DO",
        "ios" to "Apple iProduct",
        "and" to "Android",
        "bdp" to "Blu-ray Player",
        "dos" to "DOS",
        "dvd" to "DVD Player",
        "drc" to "Dreamcast",
        "nes" to "Famicom",
        "sfc" to "Super Famicom",
        "fm7" to "FM-7",
        "fm8" to "FM-8",
        "fmt" to "FM Towns",
        "gba" to "Game Boy Advance",
        "gbc" to "Game Boy Color",
        "msx" to "MSX",
        "nds" to "Nintendo DS",
        "swi" to "Nintendo Switch",
        "sw2" to "Nintendo Switch 2",
        "wii" to "Nintendo Wii",
        "wiu" to "Nintendo Wii U",
        "n3d" to "Nintendo 3DS",
        "p88" to "PC-88",
        "p98" to "PC-98",
        "pce" to "PC Engine",
        "pcf" to "PC-FX",
        "psp" to "PlayStation Portable",
        "ps1" to "PlayStation 1",
        "ps2" to "PlayStation 2",
        "ps3" to "PlayStation 3",
        "ps4" to "PlayStation 4",
        "ps5" to "PlayStation 5",
        "psv" to "PlayStation Vita",
        "smd" to "Sega Mega Drive",
        "scd" to "Sega Mega-CD",
        "sat" to "Sega Saturn",
        "vnd" to "VNDS",
        "x1s" to "Sharp X1",
        "x68" to "Sharp X68000",
        "xb1" to "Xbox",
        "xb3" to "Xbox 360",
        "xbo" to "Xbox One",
        "xxs" to "Xbox X/S",
        "mob" to "Other (mobile)",
        "oth" to "Other"
    )

    private val ASSET_BACKED_CODES = setOf(
        "and", "bdp", "drc", "dvd", "fm7", "fm8", "fmt", "ios", "lin", "mac",
        "n3d", "nds", "nes", "oth", "p88", "p98", "ps1", "ps2", "ps3", "ps4",
        "ps5", "psp", "psv", "swi", "vnd", "web", "wii", "win", "xb1", "xb3",
        "xbo", "xxs"
    )

    val PLAIN_ICONS = setOf(
        "bdp", "dvd", "n3d", "nds", "ps2", "ps3", "ps4", "ps5", "psp", "psv", "web", "wii"
    )

    fun getImgPath(key: String?): String {
        if (key != null && ASSET_BACKED_CODES.contains(key)) {
            return "assets/images/os/$key.png"
        }
        return "assets/images/os/oth.png"
    }

    fun isIconPlain(platformCode: String): Boolean = PLAIN_ICONS.contains(platformCode.lowercase())
}

data class DevStatusInfo(val title: String, val color: Color)

object DevStatusData {
    val DEVELOPMENT_STATUS: Map<Int, DevStatusInfo> = mapOf(
        -1 to DevStatusInfo("Unknown", Color.Gray),
        0 to DevStatusInfo("Finished", Color(0xFF4CAF50)),
        1 to DevStatusInfo("In Development", Color(0xFFFFEB3B)),
        2 to DevStatusInfo("Cancelled", Color(0xFFF44336))
    )

    fun get(status: Int?): DevStatusInfo = DEVELOPMENT_STATUS[status ?: -1] ?: DEVELOPMENT_STATUS[-1]!!
}

object VoteData {
    val VOTE_LABELS: Map<Int, String> = linkedMapOf(
        0 to "- (None)",
        1 to "1 (Worst ever)",
        2 to "2 (Awful)",
        3 to "3 (Bad)",
        4 to "4 (Weak)",
        5 to "5 (So-so)",
        6 to "6 (Decent)",
        7 to "7 (Good)",
        8 to "8 (Very good)",
        9 to "9 (Excellent)",
        10 to "10 (Masterpiece)"
    )
}

data class ChartStatItem(
    val code: String,
    val title: String,
    val count: Int,
    val color: Color
)

object DefaultChartStats {
    fun items(counts: Map<String, Int> = emptyMap()): List<ChartStatItem> = listOf(
        ChartStatItem("chars", "Characters", counts["chars"] ?: 136713, Color(0xFF14FF00)),
        ChartStatItem("releases", "Releases", counts["releases"] ?: 119899, Color(0xFFFFF000)),
        ChartStatItem("vn", "VNs", counts["vn"] ?: 50368, Color(0xFFFFFFFF)),
        ChartStatItem("staff", "Staffs", counts["staff"] ?: 37896, Color(0xFFFF00C8)),
        ChartStatItem("tags", "Tags", counts["tags"] ?: 37896, Color(0xFF8C00FF)),
        ChartStatItem("producers", "Producers", counts["producers"] ?: 21681, Color(0xFF00E6FF)),
        ChartStatItem("traits", "Traits", counts["traits"] ?: 3244, Color(0xFFFF7800))
    )
}

data class UserIdentity(
    val id: String,
    val username: String,
    val authToken: String
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("username", username)
        put("authToken", authToken)
    }

    companion object {
        fun fromJson(obj: JSONObject?): UserIdentity? {
            if (obj == null) return null
            val id = obj.optString("id", "")
            val username = obj.optString("username", "")
            val token = obj.optString("authToken", "")
            if (id.isBlank() || token.isBlank()) return null
            return UserIdentity(id = id, username = username, authToken = token)
        }
    }
}

data class SettingsState(
    val autoUpdate: Boolean = true,
    val themeCode: ThemeCode = ThemeCode.AGL,
    val showCoverCensor: Boolean = true,
    val showChart: Boolean = true,
    val fontSizeOffset: Float = 0.0f,
    val maxPreviewItem: Int = 20,
    val maxItemPerRowPortrait: Int = 2,
    val maxItemPerRowLandscape: Int = 4,
    val collectionStatusTabArrangement: List<String> = CollectionStatusCode.entries.map { it.statusName }
)

fun formatVnLength(lengthMinutes: Int?, lengthCode: Int? = null): String {
    if (lengthMinutes != null && lengthMinutes > 0) {
        val hours = lengthMinutes / 60
        val minutes = lengthMinutes % 60
        return if (hours > 0) {
            if (minutes > 0) "${hours}h ${minutes}m" else "${hours}h"
        } else {
            "${minutes}m"
        }
    }
    return when (lengthCode) {
        1 -> "< 2 hours"
        2 -> "2 - 10 hours"
        3 -> "10 - 30 hours"
        4 -> "30 - 50 hours"
        5 -> "> 50 hours"
        else -> "Unknown"
    }
}

fun formatRelationName(relationCode: String): String = when (relationCode.lowercase()) {
    "seq" -> "Sequel"
    "preq" -> "Prequel"
    "set" -> "Same setting"
    "alt" -> "Alternative version"
    "char" -> "Shares characters"
    "side" -> "Side story"
    "par" -> "Parent story"
    "ser" -> "Same series"
    "fan" -> "Fandisc"
    "orig" -> "Original game"
    else -> relationCode.replaceFirstChar { it.uppercase() }
}
