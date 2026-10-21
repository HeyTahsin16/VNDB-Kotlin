package com.vndblite.dcj.data.remote

import com.vndblite.dcj.core.AppInfo
import com.vndblite.dcj.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class VndbApiService {

    companion object {
        const val BASE_URL = "https://api.vndb.org/kana"
        const val P1_FIELDS =
            "title, image.url, image.sexual, image.violence, image.thumbnail, length, rating, votecount, olang, released, description"
        const val P2A_FIELDS =
            "aliases, length_minutes, devstatus, developers.name, screenshots.id, screenshots.sexual, screenshots.violence, tags.id, tags.spoiler, tags.name, tags.rating, platforms, languages"
        const val P2B_FIELDS = "minage, languages.mtl, languages.lang"
        const val P3_FIELDS =
            "extlinks.url, relations.id, relations.title, relations.relation, relations.relation_official, relations.image.url, relations.image.sexual, relations.image.violence, relations.image.thumbnail, relations.length, relations.votecount, relations.olang, relations.released, relations.rating, relations.description"
        const val USER_VNS_FIELDS =
            "id, vote, vn.title, added, started, finished, labels.label, lastmod, notes"
        const val CHARACTER_FIELDS =
            "id, name, original, aliases, description, image.url, image.sexual, image.violence, blood_type, height, weight, bust, waist, hips, cup, age, birthday, sex, vns.id, vns.title, vns.role, vns.spoiler, traits.id, traits.name, traits.group_name, traits.spoiler"
        const val TRAIT_FIELDS =
            "id, name, aliases, description, group_id, group_name, char_count"
        const val QUOTE_FIELDS =
            "id, quote, score, vn.id, vn.title, vn.image.url, vn.image.sexual, vn.image.violence, vn.image.thumbnail, vn.length, vn.rating, vn.votecount, vn.olang, vn.released, vn.description, character.id, character.name, character.original"

        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private suspend fun postJson(
        endpoint: String,
        bodyObj: JSONObject,
        authToken: String? = null
    ): JSONObject = withContext(Dispatchers.IO) {
        val reqBuilder = Request.Builder()
            .url("$BASE_URL$endpoint")
            .post(bodyObj.toString().toRequestBody(JSON_MEDIA))
            .header("User-Agent", AppInfo.userAgent)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
        if (!authToken.isNullOrBlank()) {
            reqBuilder.header("Authorization", "token $authToken")
        }
        client.newCall(reqBuilder.build()).execute().use { resp ->
            val text = resp.body?.string() ?: ""
            if (!resp.isSuccessful) {
                throw IOException("HTTP ${resp.code}: $text")
            }
            if (text.isBlank()) JSONObject() else JSONObject(text)
        }
    }

    private suspend fun getJson(
        url: String,
        authToken: String? = null
    ): JSONObject = withContext(Dispatchers.IO) {
        val reqBuilder = Request.Builder()
            .url(url)
            .get()
            .header("User-Agent", AppInfo.userAgent)
            .header("Accept", "application/json")
        if (!authToken.isNullOrBlank()) {
            reqBuilder.header("Authorization", "token $authToken")
        }
        client.newCall(reqBuilder.build()).execute().use { resp ->
            val text = resp.body?.string() ?: ""
            if (!resp.isSuccessful) {
                throw IOException("HTTP ${resp.code}: $text")
            }
            if (text.isBlank()) JSONObject() else JSONObject(text)
        }
    }

    private suspend fun getHtml(url: String): String = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url(url)
            .get()
            .header("User-Agent", AppInfo.userAgent)
            .header("Accept", "text/html,application/xhtml+xml")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw IOException("HTTP ${resp.code}")
            }
            resp.body?.string() ?: ""
        }
    }

    data class PaginatedResult<T>(
        val items: List<T>,
        val more: Boolean
    )

    suspend fun searchVn(
        filterData: FilterData,
        sortData: SortData,
        page: Int = 1,
        results: Int = 24
    ): PaginatedResult<VnDataPhase01> {
        val body = JSONObject().apply {
            put("fields", P1_FIELDS)
            put("filters", filterData.toVndbFilterJsonArray())
            if (sortData.sort.isNotBlank()) {
                put("sort", sortData.sort)
            }
            put("reverse", sortData.reverse)
            put("page", page)
            put("results", results)
        }
        val json = postJson("/vn", body)
        val arr = json.optJSONArray("results") ?: JSONArray()
        val list = mutableListOf<VnDataPhase01>()
        for (i in 0 until arr.length()) {
            arr.optJSONObject(i)?.let { list.add(VnDataPhase01.fromJson(it)) }
        }
        return PaginatedResult(items = list, more = json.optBoolean("more", false))
    }

    suspend fun fetchRandomHighRatedVn(): VnDataPhase01 {
        val ratingMin = Random.nextInt(80, 90)
        val body = JSONObject().apply {
            put("sort", SortableCode.RATING.code)
            put("filters", JSONArray().put("rating").put(">=").put(ratingMin))
            put("fields", P1_FIELDS)
            put("results", 12)
        }
        val json = postJson("/vn", body)
        val arr = json.optJSONArray("results") ?: JSONArray()
        if (arr.length() == 0) {
            throw IOException("No random VN returned")
        }
        val pickIdx = Random.nextInt(arr.length())
        return VnDataPhase01.fromJson(arr.getJSONObject(pickIdx))
    }

    suspend fun fetchP1ById(vnId: String): VnDataPhase01? {
        val body = JSONObject().apply {
            put("filters", JSONArray().put("id").put("=").put(vnId))
            put("fields", P1_FIELDS)
        }
        val json = postJson("/vn", body)
        val arr = json.optJSONArray("results")
        if (arr == null || arr.length() == 0) return null
        return VnDataPhase01.fromJson(arr.getJSONObject(0))
    }

    suspend fun fetchP2ById(vnId: String): VnDataPhase02? {
        val bodyA = JSONObject().apply {
            put("filters", JSONArray().put("id").put("=").put(vnId))
            put("fields", P2A_FIELDS)
        }
        val jsonA = postJson("/vn", bodyA)
        val arrA = jsonA.optJSONArray("results")
        if (arrA == null || arrA.length() == 0) return null
        val p2Raw = arrA.getJSONObject(0)

        val bodyB = JSONObject().apply {
            put(
                "filters",
                JSONArray().put("vn").put("=").put(JSONArray().put("id").put("=").put(vnId))
            )
            put("fields", P2B_FIELDS)
            put("results", 100)
        }
        val jsonB = runCatching { postJson("/release", bodyB) }.getOrNull()
        val (mtlLangs, minAges) = VnDataPhase02.processAdditionalP2(jsonB?.optJSONArray("results"))
        p2Raw.put("mtl", JSONArray(mtlLangs))
        p2Raw.put("minage", JSONArray(minAges))
        return VnDataPhase02.fromJson(p2Raw)
    }

    suspend fun fetchP3ById(vnId: String): Pair<VnDataPhase03, List<VnDataPhase01>>? {
        val body = JSONObject().apply {
            put("filters", JSONArray().put("id").put("=").put(vnId))
            put("fields", P3_FIELDS)
        }
        val json = postJson("/vn", body)
        val arr = json.optJSONArray("results")
        if (arr == null || arr.length() == 0) return null
        val raw = arr.getJSONObject(0)
        val p3 = VnDataPhase03.fromJson(raw)

        val relatedP1s = mutableListOf<VnDataPhase01>()
        val relArr = raw.optJSONArray("relations")
        if (relArr != null) {
            for (i in 0 until relArr.length()) {
                val relObj = relArr.optJSONObject(i) ?: continue
                if (relObj.has("title")) {
                    relatedP1s.add(VnDataPhase01.fromJson(relObj))
                }
            }
        }
        return p3 to relatedP1s
    }

    suspend fun fetchCharactersForVn(vnId: String): List<VnCharacter> {
        val body = JSONObject().apply {
            put(
                "filters",
                JSONArray().put("vn").put("=").put(JSONArray().put("id").put("=").put(vnId))
            )
            put("fields", CHARACTER_FIELDS)
            put("results", 50)
        }
        val json = postJson("/character", body)
        val arr = json.optJSONArray("results") ?: JSONArray()
        val list = mutableListOf<VnCharacter>()
        for (i in 0 until arr.length()) {
            arr.optJSONObject(i)?.let { list.add(VnCharacter.fromJson(it)) }
        }
        return list
    }

    suspend fun searchTraits(query: String): List<VnTrait> {
        val body = JSONObject().apply {
            put("filters", JSONArray().put("search").put("=").put(query.trim()))
            put("fields", TRAIT_FIELDS)
            put("results", 30)
        }
        val json = postJson("/trait", body)
        val arr = json.optJSONArray("results") ?: JSONArray()
        val list = mutableListOf<VnTrait>()
        for (i in 0 until arr.length()) {
            arr.optJSONObject(i)?.let { list.add(VnTrait.fromJson(it)) }
        }
        return list
    }

    suspend fun searchCharacters(
        query: String,
        traits: List<VnTrait>,
        traitAndOr: String = "and",
        roles: List<String> = emptyList(),
        roleAndOr: String = "or",
        sexes: List<String> = emptyList(),
        sexAndOr: String = "or",
        overallAndOr: String = "and",
        page: Int = 1,
        results: Int = 24
    ): PaginatedResult<VnCharacter> {
        val filterArr = JSONArray().apply {
            put(overallAndOr)
            put(JSONArray().put("search").put("=").put(query.trim()))
            if (roles.size == 1) {
                put(JSONArray().put("role").put("=").put(roles[0]))
            } else if (roles.size > 1) {
                val roleGroup = JSONArray().put(roleAndOr)
                roles.forEach { r ->
                    roleGroup.put(JSONArray().put("role").put("=").put(r))
                }
                put(roleGroup)
            }
            if (sexes.size == 1) {
                put(JSONArray().put("sex").put("=").put(sexes[0]))
            } else if (sexes.size > 1) {
                val sexGroup = JSONArray().put(sexAndOr)
                sexes.forEach { s ->
                    sexGroup.put(JSONArray().put("sex").put("=").put(s))
                }
                put(sexGroup)
            }
            if (traits.size == 1) {
                put(JSONArray().put("trait").put("=").put(traits[0].id))
            } else if (traits.size > 1) {
                val traitGroup = JSONArray().put(traitAndOr)
                traits.forEach { tr ->
                    traitGroup.put(JSONArray().put("trait").put("=").put(tr.id))
                }
                put(traitGroup)
            }
        }
        val body = JSONObject().apply {
            put("filters", filterArr)
            put("fields", CHARACTER_FIELDS)
            put("page", page)
            put("results", results)
        }
        val json = postJson("/character", body)
        val arr = json.optJSONArray("results") ?: JSONArray()
        val list = mutableListOf<VnCharacter>()
        for (i in 0 until arr.length()) {
            arr.optJSONObject(i)?.let { list.add(VnCharacter.fromJson(it)) }
        }
        return PaginatedResult(items = list, more = json.optBoolean("more", false))
    }

    suspend fun fetchRandomQuote(): VnQuote? {
        val body = JSONObject().apply {
            put("filters", JSONArray().put("random").put("=").put(1))
            put("fields", QUOTE_FIELDS)
            put("results", 1)
        }
        val json = postJson("/quote", body)
        val arr = json.optJSONArray("results")
        if (arr == null || arr.length() == 0) return null
        val obj = arr.optJSONObject(0) ?: return null
        return VnQuote.fromJson(obj).takeIf { it.quote.isNotBlank() }
    }

    suspend fun fetchTopQuotes(results: Int = 8): List<VnQuote> {
        val body = JSONObject().apply {
            put("filters", JSONArray().put("id").put("!=").put("q0"))
            put("sort", "score")
            put("reverse", true)
            put("fields", QUOTE_FIELDS)
            put("results", results)
        }
        val json = postJson("/quote", body)
        val arr = json.optJSONArray("results") ?: JSONArray()
        val list = mutableListOf<VnQuote>()
        for (i in 0 until arr.length()) {
            arr.optJSONObject(i)?.let { obj ->
                val q = VnQuote.fromJson(obj)
                if (q.quote.isNotBlank()) list.add(q)
            }
        }
        return list
    }

    suspend fun fetchHomeQuotes(randomCount: Int = 3, topCount: Int = 6): List<VnQuote> = coroutineScope {
        val randomDeferreds = (1..randomCount).map {
            async { runCatching { fetchRandomQuote() }.getOrNull() }
        }
        val topDeferred = async {
            runCatching { fetchTopQuotes(topCount) }.getOrDefault(emptyList())
        }
        val randomQuotes = randomDeferreds.awaitAll().filterNotNull()
        val topQuotes = topDeferred.await()
        (randomQuotes + topQuotes).distinctBy { it.id }
    }

    suspend fun fetchQuotesForVn(vnId: String, results: Int = 8): List<VnQuote> {
        val body = JSONObject().apply {
            put("filters", JSONArray().put("vn").put("=").put(JSONArray().put("id").put("=").put(vnId)))
            put("sort", "score")
            put("reverse", true)
            put("fields", QUOTE_FIELDS)
            put("results", results)
        }
        val json = postJson("/quote", body)
        val arr = json.optJSONArray("results") ?: JSONArray()
        val list = mutableListOf<VnQuote>()
        for (i in 0 until arr.length()) {
            arr.optJSONObject(i)?.let { obj ->
                val q = VnQuote.fromJson(obj)
                if (q.quote.isNotBlank()) list.add(q)
            }
        }
        return list
    }

    suspend fun fetchReviewsForVn(vnId: String): List<VnReview> {
        val cleanId = vnId.trim()
        if (cleanId.isBlank()) return emptyList()
        val html = getHtml("https://vndb.org/$cleanId/reviews")
        return VnReview.parseReviewsFromVndbHtml(html)
    }

    suspend fun fetchFullReviewText(reviewId: String): String? {
        val cleanId = reviewId.trim()
        if (cleanId.isBlank()) return null
        val html = getHtml("https://vndb.org/$cleanId")
        return VnReview.parseFullReviewFromHtml(html)
    }

    suspend fun searchTags(query: String): List<VnTag> {
        val body = JSONObject().apply {
            put("filters", JSONArray().put("search").put("=").put(query.trim()))
            put("fields", "id, name")
            put("results", 25)
        }
        val json = postJson("/tag", body)
        val arr = json.optJSONArray("results") ?: JSONArray()
        val list = mutableListOf<VnTag>()
        for (i in 0 until arr.length()) {
            arr.optJSONObject(i)?.let { list.add(VnTag.fromJson(it)) }
        }
        return list
    }

    suspend fun searchDevelopers(query: String): List<Developer> {
        val body = JSONObject().apply {
            put("filters", JSONArray().put("search").put("=").put(query.trim()))
            put("fields", "id, name")
            put("results", 25)
        }
        val json = postJson("/producer", body)
        val arr = json.optJSONArray("results") ?: JSONArray()
        val list = mutableListOf<Developer>()
        for (i in 0 until arr.length()) {
            arr.optJSONObject(i)?.let { list.add(Developer.fromJson(it)) }
        }
        return list
    }

    suspend fun fetchStats(): Map<String, Int> {
        val json = getJson("$BASE_URL/stats")
        val map = mutableMapOf<String, Int>()
        val keys = listOf("chars", "producers", "releases", "staff", "tags", "traits", "vn")
        keys.forEach { k ->
            if (json.has(k)) {
                map[k] = json.optInt(k, 0)
            }
        }
        return map
    }

    suspend fun authenticate(token: String): UserIdentity {
        val json = getJson("$BASE_URL/authinfo", authToken = token.trim())
        val id = json.optString("id", "")
        val username = json.optString("username", "")
        if (id.isBlank()) throw IOException("Invalid token response")
        return UserIdentity(id = id, username = username, authToken = token.trim())
    }

    suspend fun fetchAllCloudUlist(userId: String): List<JSONObject> {
        val allResults = mutableListOf<JSONObject>()
        var page = 1
        while (true) {
            val body = JSONObject().apply {
                put("user", userId)
                put("sort", SortableCode.ADDED.code)
                put("fields", USER_VNS_FIELDS)
                put("results", 24)
                put("page", page)
            }
            val json = postJson("/ulist", body)
            val arr = json.optJSONArray("results") ?: JSONArray()
            for (i in 0 until arr.length()) {
                arr.optJSONObject(i)?.let { allResults.add(it) }
            }
            val more = json.optBoolean("more", false)
            if (!more) break
            page++
        }
        return allResults
    }

    suspend fun patchCloudRecord(
        localRecord: VnRecord,
        statusId: Int,
        authToken: String
    ) = withContext(Dispatchers.IO) {
        delay(600)
        val body = JSONObject().apply {
            put("labels", JSONArray().put(statusId))
            if (localRecord.vote != 0) {
                put("vote", localRecord.vote * 10)
            }
            put("notes", localRecord.notes ?: "")
            localRecord.started?.takeIf { it.length >= 10 && it != "null" }?.let {
                put("started", it.take(10))
            }
            localRecord.finished?.takeIf { it.length >= 10 && it != "null" }?.let {
                put("finished", it.take(10))
            }
        }
        val req = Request.Builder()
            .url("$BASE_URL/ulist/${localRecord.id}")
            .patch(body.toString().toRequestBody(JSON_MEDIA))
            .header("User-Agent", AppInfo.userAgent)
            .header("Content-Type", "application/json")
            .header("Authorization", "token $authToken")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw IOException("PATCH ulist failed: ${resp.code}")
            }
        }
    }

    suspend fun deleteCloudRecord(vnId: String, authToken: String) = withContext(Dispatchers.IO) {
        delay(600)
        val req = Request.Builder()
            .url("$BASE_URL/ulist/$vnId")
            .delete()
            .header("User-Agent", AppInfo.userAgent)
            .header("Content-Type", "application/json")
            .header("Authorization", "token $authToken")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful && resp.code != 404) {
                throw IOException("DELETE ulist failed: ${resp.code}")
            }
        }
    }
}
