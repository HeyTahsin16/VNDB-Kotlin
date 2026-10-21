package com.vndblite.dcj.data.local

import android.content.Context
import android.content.SharedPreferences
import com.vndblite.dcj.data.model.*
import org.json.JSONArray
import org.json.JSONObject

class LocalStorageManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "VndbKotlinPreferences"
        const val P1 = "P01_"
        const val P2 = "P02_"
        const val P3 = "P03_"
        const val HOME_PREVIEW_CACHE_KEY = "HOME_PREVIEW_"
        const val SAVED_COLLECTION_SECTIONS = "SAVED_COLLECTION_SECTIONS"
        const val COLLECTION_FILTER_CONF = "COLLECTION_FILTER_CONF"
        const val COLLECTION_SORT_CONF = "COLLECTION_SORT_CONF"
        const val VN_RECORDS_TO_BE_REMOVED = "VN_RECORDS_TO_BE_REMOVED"
        const val VN_RECORDS_ADDED_BY_APP = "VN_RECORDS_ADDED_BY_APP"
        const val COLLECTION_ARRANGEMENT = "COLLECTION_ARRANGEMENT"
        const val USER_IDENTITY = "USER_IDENTITY"
        const val USER_SYNCED = "USER_SYNCED"
        const val VNDB_STATS = "VNDB_STATS"
        const val AUTO_UPDATE_CHECK_CONF = "AUTO_UPDATE_CHECK_CONF"
        const val THEME_CONF = "THEME_CONF"
        const val COVER_CENSOR_CONF = "NSFW_COVER_CENSOR_DEFAULT_ON"
        const val SHOW_CHART_CONF = "SHOW_CHART_CONF"
        const val CUSTOM_FONT_SIZE_CONF = "CUSTOM_FONT_SIZE_CONF"
        const val MAX_PREVIEW_ITEMS_CONF = "MAX_PREVIEW_ITEMS_CONF"
        const val MAX_ITEMS_PER_ROW_PORTRAIT_CONF = "MAX_ITEMS_PER_ROW_PORTRAIT_CONF"
        const val MAX_ITEMS_PER_ROW_LANDSCAPE_CONF = "MAX_ITEMS_PER_ROW_LANDSCAPE_CONF"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // --- VN Phases ---
    fun hasP1(vnId: String): Boolean = prefs.contains("$P1$vnId")
    fun hasP2(vnId: String): Boolean = prefs.contains("$P2$vnId")
    fun hasP3(vnId: String): Boolean = prefs.contains("$P3$vnId")

    fun getP1(vnId: String): VnDataPhase01? {
        val raw = prefs.getString("$P1$vnId", null) ?: return null
        return runCatching { VnDataPhase01.fromJson(JSONObject(raw)) }.getOrNull()
    }

    fun getP2(vnId: String): VnDataPhase02? {
        val raw = prefs.getString("$P2$vnId", null) ?: return null
        return runCatching { VnDataPhase02.fromJson(JSONObject(raw)) }.getOrNull()
    }

    fun getP3(vnId: String): VnDataPhase03? {
        val raw = prefs.getString("$P3$vnId", null) ?: return null
        return runCatching { VnDataPhase03.fromJson(JSONObject(raw)) }.getOrNull()
    }

    fun saveP1(p1: VnDataPhase01) {
        prefs.edit().putString("$P1${p1.id}", p1.toJson().toString()).apply()
    }

    fun saveP2(p2: VnDataPhase02) {
        prefs.edit().putString("$P2${p2.id}", p2.toJson().toString()).apply()
    }

    fun saveP3(p3: VnDataPhase03) {
        prefs.edit().putString("$P3${p3.id}", p3.toJson().toString()).apply()
    }

    fun removeVnPhase(phase: Int, vnId: String) {
        val prefix = when (phase) {
            1 -> P1
            2 -> P2
            3 -> P3
            else -> return
        }
        prefs.edit().remove("$prefix$vnId").apply()
    }

    // --- Collection Records ---
    fun getAllRecords(): List<VnRecord> {
        val set = prefs.getStringSet(SAVED_COLLECTION_SECTIONS, emptySet()) ?: emptySet()
        return set.mapNotNull { raw ->
            runCatching { VnRecord.fromJson(JSONObject(raw)) }.getOrNull()
        }
    }

    fun getRecordById(vnId: String): VnRecord? =
        getAllRecords().firstOrNull { it.id == vnId }

    fun saveRecord(record: VnRecord) {
        val existing = getAllRecords().filterNot { it.id == record.id }.toMutableList()
        existing.add(record)
        val serialized = existing.map { it.toJson().toString() }.toSet()
        prefs.edit().putStringSet(SAVED_COLLECTION_SECTIONS, serialized).apply()
    }

    fun removeRecord(vnId: String) {
        val remaining = getAllRecords().filterNot { it.id == vnId }
        val serialized = remaining.map { it.toJson().toString() }.toSet()
        prefs.edit().putStringSet(SAVED_COLLECTION_SECTIONS, serialized).apply()
    }

    fun clearAllRecords(markForSyncRemoval: Boolean) {
        if (markForSyncRemoval) {
            val allIds = getAllRecords().map { it.id }
            val toRemove = getVnToBeRemovedWhenSync().toMutableSet()
            toRemove.addAll(allIds)
            prefs.edit().putStringSet(VN_RECORDS_TO_BE_REMOVED, toRemove).apply()
        } else {
            cleanVnToBeRemovedWhenSync()
        }
        cleanAddedViaAppNotBySync()
        prefs.edit().remove(SAVED_COLLECTION_SECTIONS).apply()
    }

    // --- Sync Tracking ---
    fun getVnToBeRemovedWhenSync(): Set<String> =
        prefs.getStringSet(VN_RECORDS_TO_BE_REMOVED, emptySet()) ?: emptySet()

    fun addVnToBeRemovedWhenSync(vnId: String) {
        val s = getVnToBeRemovedWhenSync().toMutableSet()
        s.add(vnId)
        prefs.edit().putStringSet(VN_RECORDS_TO_BE_REMOVED, s).apply()
    }

    fun cleanVnToBeRemovedWhenSync() {
        prefs.edit().remove(VN_RECORDS_TO_BE_REMOVED).apply()
    }

    fun getAddedViaAppNotBySync(): Set<String> =
        prefs.getStringSet(VN_RECORDS_ADDED_BY_APP, emptySet()) ?: emptySet()

    fun addAddedViaAppNotBySync(vnId: String) {
        val s = getAddedViaAppNotBySync().toMutableSet()
        s.add(vnId)
        prefs.edit().putStringSet(VN_RECORDS_ADDED_BY_APP, s).apply()
    }

    fun removeAddedViaAppNotBySync(vnId: String) {
        val s = getAddedViaAppNotBySync().toMutableSet()
        s.remove(vnId)
        prefs.edit().putStringSet(VN_RECORDS_ADDED_BY_APP, s).apply()
    }

    fun cleanAddedViaAppNotBySync() {
        prefs.edit().remove(VN_RECORDS_ADDED_BY_APP).apply()
    }

    // --- Home Preview Cache ---
    fun saveHomePreviewIds(section: HomeSectionsCode, ids: List<String>) {
        prefs.edit().putString(
            "$HOME_PREVIEW_CACHE_KEY${section.name}",
            JSONArray(ids).toString()
        ).apply()
    }

    fun getHomePreviewIds(section: HomeSectionsCode): List<String> {
        val raw = prefs.getString("$HOME_PREVIEW_CACHE_KEY${section.name}", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            List(arr.length()) { idx -> arr.optString(idx) }.filter { it.isNotBlank() }
        }.getOrDefault(emptyList())
    }

    // --- User Identity & Sync ---
    fun getUserIdentity(): UserIdentity? {
        val raw = prefs.getString(USER_IDENTITY, null) ?: return null
        return runCatching { UserIdentity.fromJson(JSONObject(raw)) }.getOrNull()
    }

    fun saveUserIdentity(identity: UserIdentity?) {
        if (identity == null) {
            prefs.edit().remove(USER_IDENTITY).remove(USER_SYNCED).apply()
        } else {
            prefs.edit().putString(USER_IDENTITY, identity.toJson().toString()).apply()
        }
    }

    var isUserSynced: Boolean
        get() = prefs.getBoolean(USER_SYNCED, false)
        set(value) = prefs.edit().putBoolean(USER_SYNCED, value).apply()

    // --- VNDB Stats Cache ---
    fun saveStats(stats: Map<String, Int>) {
        val obj = JSONObject()
        stats.forEach { (k, v) -> obj.put(k, v) }
        prefs.edit().putString(VNDB_STATS, obj.toString()).apply()
    }

    fun getStats(): Map<String, Int> {
        val raw = prefs.getString(VNDB_STATS, null) ?: return emptyMap()
        return runCatching {
            val obj = JSONObject(raw)
            val map = mutableMapOf<String, Int>()
            obj.keys().forEach { key -> map[key] = obj.optInt(key, 0) }
            map
        }.getOrDefault(emptyMap())
    }

    // --- VNDB Quotes Cache ---
    fun saveQuotes(quotes: List<VnQuote>) {
        val arr = JSONArray()
        quotes.forEach { arr.put(it.toJson()) }
        prefs.edit().putString("VNDB_QUOTES_CACHE", arr.toString()).apply()
    }

    fun getQuotes(): List<VnQuote> {
        val raw = prefs.getString("VNDB_QUOTES_CACHE", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            val list = mutableListOf<VnQuote>()
            for (i in 0 until arr.length()) {
                arr.optJSONObject(i)?.let { list.add(VnQuote.fromJson(it)) }
            }
            list
        }.getOrDefault(emptyList())
    }

    // --- Local Collection Sort & Filter ---
    fun getLocalFilter(): FilterData {
        val raw = prefs.getString(COLLECTION_FILTER_CONF, null) ?: return FilterData.DEFAULT_LOCAL
        return runCatching { FilterData.fromJson(JSONObject(raw)) }.getOrDefault(FilterData.DEFAULT_LOCAL)
    }

    fun saveLocalFilter(filterData: FilterData) {
        prefs.edit().putString(COLLECTION_FILTER_CONF, filterData.toJson().toString()).apply()
    }

    fun getLocalSort(): SortData {
        val raw = prefs.getString(COLLECTION_SORT_CONF, null) ?: return SortData.DEFAULT_LOCAL
        return runCatching { SortData.fromJson(JSONObject(raw)) }.getOrDefault(SortData.DEFAULT_LOCAL)
    }

    fun saveLocalSort(sortData: SortData) {
        prefs.edit().putString(COLLECTION_SORT_CONF, sortData.toJson().toString()).apply()
    }

    // --- Settings ---
    fun getSettings(): SettingsState {
        val autoUpdate = prefs.getBoolean(AUTO_UPDATE_CHECK_CONF, true)
        val themeCode = ThemeCode.fromCode(prefs.getString(THEME_CONF, ThemeCode.AGL.code))
        val showCoverCensor = prefs.getBoolean(COVER_CENSOR_CONF, true)
        val showChart = prefs.getBoolean(SHOW_CHART_CONF, true)
        val fontSizeOffset = prefs.getFloat(CUSTOM_FONT_SIZE_CONF, 0.0f)
        val maxPreview = prefs.getInt(MAX_PREVIEW_ITEMS_CONF, 20)
        val portraitRow = prefs.getInt(MAX_ITEMS_PER_ROW_PORTRAIT_CONF, 2)
        val landscapeRow = prefs.getInt(MAX_ITEMS_PER_ROW_LANDSCAPE_CONF, 4)
        val arrangementRaw = prefs.getString(COLLECTION_ARRANGEMENT, null)
        val arrangement = if (arrangementRaw != null) {
            runCatching {
                val arr = JSONArray(arrangementRaw)
                List(arr.length()) { arr.optString(it) }.filter { it.isNotBlank() }
            }.getOrDefault(CollectionStatusCode.entries.map { it.statusName })
        } else {
            CollectionStatusCode.entries.map { it.statusName }
        }
        return SettingsState(
            autoUpdate = autoUpdate,
            themeCode = themeCode,
            showCoverCensor = showCoverCensor,
            showChart = showChart,
            fontSizeOffset = fontSizeOffset,
            maxPreviewItem = maxPreview,
            maxItemPerRowPortrait = portraitRow,
            maxItemPerRowLandscape = landscapeRow,
            collectionStatusTabArrangement = arrangement.ifEmpty {
                CollectionStatusCode.entries.map { it.statusName }
            }
        )
    }

    fun saveSettings(state: SettingsState) {
        prefs.edit()
            .putBoolean(AUTO_UPDATE_CHECK_CONF, state.autoUpdate)
            .putString(THEME_CONF, state.themeCode.code)
            .putBoolean(COVER_CENSOR_CONF, state.showCoverCensor)
            .putBoolean(SHOW_CHART_CONF, state.showChart)
            .putFloat(CUSTOM_FONT_SIZE_CONF, state.fontSizeOffset)
            .putInt(MAX_PREVIEW_ITEMS_CONF, state.maxPreviewItem)
            .putInt(MAX_ITEMS_PER_ROW_PORTRAIT_CONF, state.maxItemPerRowPortrait)
            .putInt(MAX_ITEMS_PER_ROW_LANDSCAPE_CONF, state.maxItemPerRowLandscape)
            .putString(COLLECTION_ARRANGEMENT, JSONArray(state.collectionStatusTabArrangement).toString())
            .apply()
    }

    fun resetAllData() {
        prefs.edit().clear().apply()
    }
}
