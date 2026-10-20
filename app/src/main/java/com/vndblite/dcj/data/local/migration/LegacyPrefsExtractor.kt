package com.vndblite.dcj.data.local.migration

import android.content.SharedPreferences
import com.vndblite.dcj.core.AppLog
import com.vndblite.dcj.data.local.db.entity.CollectionEntryEntity
import com.vndblite.dcj.data.local.db.entity.HomeSectionCacheEntity
import com.vndblite.dcj.data.local.db.entity.QuarantinedRecordEntity
import com.vndblite.dcj.data.local.db.entity.QuoteCacheEntity
import com.vndblite.dcj.data.local.db.entity.SiteStatsCacheEntity
import com.vndblite.dcj.data.local.db.entity.VnCacheEntity

/**
 * Extracts and validates legacy SharedPreferences entries while recording any corrupt
 * payloads into [quarantined] for `quarantined_records` persistence (S6).
 */
internal object LegacyPrefsExtractor {
    private const val TAG = "LegacyPrefsExtractor"

    fun parseCollection(
        legacyPrefs: SharedPreferences,
        ctx: LegacyRecordContext,
        quarantined: MutableList<QuarantinedRecordEntity>
    ): List<CollectionEntryEntity> {
        val rawSet = legacyPrefs.getStringSet(LegacyPrefsMigrator.KEY_SAVED_COLLECTION, emptySet()).orEmpty()
        val results = mutableListOf<CollectionEntryEntity>()
        for (rawRecord in rawSet) {
            try {
                results += LegacyJsonParsers.parseCollectionRecord(rawRecord, ctx)
            } catch (e: Exception) {
                AppLog.w(TAG, "Quarantining corrupt legacy collection record", e)
                quarantined += QuarantinedRecordEntity(
                    sourceKey = LegacyPrefsMigrator.KEY_SAVED_COLLECTION,
                    rawPayload = LegacyJsonParsers.redactSecrets(rawRecord),
                    errorReason = e.message ?: "Unparsable collection record JSON",
                    quarantinedAtEpochSeconds = ctx.nowEpochSeconds
                )
            }
        }
        return results
    }

    fun parseVnCache(
        legacyPrefs: SharedPreferences,
        allKeys: Set<String>,
        nowEpochSeconds: Long,
        quarantined: MutableList<QuarantinedRecordEntity>
    ): List<VnCacheEntity> {
        val vnIds = allKeys.mapNotNull { key ->
            when {
                key.startsWith(LegacyPrefsMigrator.PREFIX_P1) -> key.removePrefix(LegacyPrefsMigrator.PREFIX_P1)
                key.startsWith(LegacyPrefsMigrator.PREFIX_P2) -> key.removePrefix(LegacyPrefsMigrator.PREFIX_P2)
                key.startsWith(LegacyPrefsMigrator.PREFIX_P3) -> key.removePrefix(LegacyPrefsMigrator.PREFIX_P3)
                else -> null
            }
        }.filter { it.isNotBlank() }.toSortedSet()

        val entities = mutableListOf<VnCacheEntity>()
        for (vnId in vnIds) {
            val p1Raw = legacyPrefs.getString("${LegacyPrefsMigrator.PREFIX_P1}$vnId", null)
            val p2Raw = legacyPrefs.getString("${LegacyPrefsMigrator.PREFIX_P2}$vnId", null)
            val p3Raw = legacyPrefs.getString("${LegacyPrefsMigrator.PREFIX_P3}$vnId", null)
            try {
                entities += LegacyJsonParsers.parseVnCacheFromPhases(
                    vnId = vnId,
                    p1Raw = p1Raw,
                    p2Raw = p2Raw,
                    p3Raw = p3Raw,
                    nowEpochSeconds = nowEpochSeconds
                )
            } catch (e: Exception) {
                AppLog.w(TAG, "Quarantining corrupt legacy VN cache entry for $vnId", e)
                val combinedRaw = buildString {
                    if (p1Raw != null) append("P1=").append(p1Raw).append('\n')
                    if (p2Raw != null) append("P2=").append(p2Raw).append('\n')
                    if (p3Raw != null) append("P3=").append(p3Raw)
                }
                quarantined += QuarantinedRecordEntity(
                    sourceKey = "${LegacyPrefsMigrator.PREFIX_P1}$vnId",
                    rawPayload = combinedRaw,
                    errorReason = e.message ?: "Unparsable VN cache phase JSON",
                    quarantinedAtEpochSeconds = nowEpochSeconds
                )
            }
        }
        return entities
    }

    fun parseHomeSections(
        legacyPrefs: SharedPreferences,
        allKeys: Set<String>,
        nowEpochSeconds: Long,
        quarantined: MutableList<QuarantinedRecordEntity>
    ): List<HomeSectionCacheEntity> {
        val homeKeys = allKeys.filter { it.startsWith(LegacyPrefsMigrator.PREFIX_HOME_PREVIEW) }.sorted()
        return homeKeys.mapNotNull { key ->
            val raw = legacyPrefs.getString(key, null) ?: return@mapNotNull null
            val sectionCode = key.removePrefix(LegacyPrefsMigrator.PREFIX_HOME_PREVIEW)
            try {
                val ids = LegacyJsonParsers.parseStringArray(raw)
                HomeSectionCacheEntity.create(
                    sectionCode = sectionCode,
                    vnIds = ids,
                    fetchedAtEpochSeconds = nowEpochSeconds
                )
            } catch (e: Exception) {
                AppLog.w(TAG, "Quarantining corrupt legacy home preview cache for $key", e)
                quarantined += QuarantinedRecordEntity(
                    sourceKey = key,
                    rawPayload = raw,
                    errorReason = e.message ?: "Unparsable home section ID array",
                    quarantinedAtEpochSeconds = nowEpochSeconds
                )
                null
            }
        }
    }

    fun parseQuotes(
        legacyPrefs: SharedPreferences,
        nowEpochSeconds: Long,
        quarantined: MutableList<QuarantinedRecordEntity>
    ): List<QuoteCacheEntity> =
        legacyPrefs.getString(LegacyPrefsMigrator.KEY_QUOTES_CACHE, null)?.let { raw ->
            try {
                LegacyJsonParsers.parseQuotesArray(raw, nowEpochSeconds)
            } catch (e: Exception) {
                AppLog.w(TAG, "Quarantining corrupt legacy quotes cache", e)
                quarantined += QuarantinedRecordEntity(
                    sourceKey = LegacyPrefsMigrator.KEY_QUOTES_CACHE,
                    rawPayload = raw,
                    errorReason = e.message ?: "Unparsable quotes JSON",
                    quarantinedAtEpochSeconds = nowEpochSeconds
                )
                emptyList()
            }
        }.orEmpty()

    fun parseStats(
        legacyPrefs: SharedPreferences,
        nowEpochSeconds: Long,
        quarantined: MutableList<QuarantinedRecordEntity>
    ): SiteStatsCacheEntity? =
        legacyPrefs.getString(LegacyPrefsMigrator.KEY_VNDB_STATS, null)?.let { raw ->
            try {
                LegacyJsonParsers.parseSiteStats(raw, nowEpochSeconds)
            } catch (e: Exception) {
                AppLog.w(TAG, "Quarantining corrupt legacy stats cache", e)
                quarantined += QuarantinedRecordEntity(
                    sourceKey = LegacyPrefsMigrator.KEY_VNDB_STATS,
                    rawPayload = raw,
                    errorReason = e.message ?: "Unparsable stats JSON",
                    quarantinedAtEpochSeconds = nowEpochSeconds
                )
                null
            }
        }
}
