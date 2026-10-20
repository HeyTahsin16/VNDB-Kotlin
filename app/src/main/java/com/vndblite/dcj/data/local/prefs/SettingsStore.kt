package com.vndblite.dcj.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.vndblite.dcj.core.AppLog
import com.vndblite.dcj.data.local.db.entity.VnCacheEntity
import com.vndblite.dcj.domain.model.AppSettings
import com.vndblite.dcj.domain.model.CollectionFilters
import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.domain.model.DevStatus
import com.vndblite.dcj.domain.model.Language
import com.vndblite.dcj.domain.model.Platform
import com.vndblite.dcj.domain.model.SortField
import com.vndblite.dcj.domain.model.SortOption
import com.vndblite.dcj.domain.model.ThemeId
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Reactive preferences store backed by Jetpack DataStore (Section 2, S1, S3, A12).
 * Note: the dead `autoUpdate` preference is intentionally excluded (A12).
 */
interface SettingsStore {
    val settingsFlow: Flow<AppSettings>
    val collectionFiltersFlow: Flow<CollectionFilters>
    val collectionSortFlow: Flow<SortOption>
    val activeAccountIdFlow: Flow<String?>

    suspend fun getSettingsSnapshot(): AppSettings
    suspend fun saveSettings(settings: AppSettings)
    suspend fun updateSettings(transform: (AppSettings) -> AppSettings)
    suspend fun resetSettingsToDefaults()

    suspend fun saveCollectionFilters(filters: CollectionFilters)
    suspend fun saveCollectionSort(sort: SortOption)

    suspend fun getActiveAccountId(): String?
    suspend fun setActiveAccountId(accountId: String?)

    suspend fun isLegacyMigrationCompleted(): Boolean
    suspend fun markLegacyMigrationCompleted()
}

class DataStoreSettingsStore(
    private val dataStore: DataStore<Preferences>
) : SettingsStore {

    private val safePreferencesFlow: Flow<Preferences> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) {
                AppLog.w(TAG, "Failed to read DataStore preferences; emitting defaults", throwable)
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }

    override val settingsFlow: Flow<AppSettings> = safePreferencesFlow.map(::mapPreferencesToSettings)

    override val collectionFiltersFlow: Flow<CollectionFilters> =
        safePreferencesFlow.map(::mapPreferencesToCollectionFilters)

    override val collectionSortFlow: Flow<SortOption> =
        safePreferencesFlow.map(::mapPreferencesToCollectionSort)

    override val activeAccountIdFlow: Flow<String?> = safePreferencesFlow.map { prefs ->
        prefs[KEY_ACTIVE_ACCOUNT_ID]?.takeIf { it.isNotBlank() }
    }

    override suspend fun getSettingsSnapshot(): AppSettings = settingsFlow.first()

    override suspend fun saveSettings(settings: AppSettings) {
        dataStore.edit { prefs ->
            writeSettingsToPreferences(prefs, settings)
        }
    }

    override suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { prefs ->
            val current = mapPreferencesToSettings(prefs)
            val updated = transform(current)
            writeSettingsToPreferences(prefs, updated)
        }
    }

    override suspend fun resetSettingsToDefaults() {
        saveSettings(AppSettings())
    }

    override suspend fun saveCollectionFilters(filters: CollectionFilters) {
        dataStore.edit { prefs ->
            prefs[KEY_FILTER_QUERY] = filters.searchQuery
            val status = filters.status
            if (status != null) {
                prefs[KEY_FILTER_STATUS_ID] = status.labelId
            } else {
                prefs.remove(KEY_FILTER_STATUS_ID)
            }
            val minAge = filters.minAge
            if (minAge != null && minAge > 0) {
                prefs[KEY_FILTER_MIN_AGE] = minAge
            } else {
                prefs.remove(KEY_FILTER_MIN_AGE)
            }
            prefs[KEY_FILTER_LANGUAGES] = VnCacheEntity.encodePipeList(filters.languages.map { it.code })
            prefs[KEY_FILTER_DEV_STATUSES] = filters.devStatuses.joinToString(",") { it.apiCode.toString() }
            prefs[KEY_FILTER_ORIG_LANGUAGES] = VnCacheEntity.encodePipeList(filters.originalLanguages.map { it.code })
            prefs[KEY_FILTER_PLATFORMS] = VnCacheEntity.encodePipeList(filters.platforms.map { it.code })
        }
    }

    override suspend fun saveCollectionSort(sort: SortOption) {
        dataStore.edit { prefs ->
            prefs[KEY_SORT_FIELD] = sort.field.code
            prefs[KEY_SORT_REVERSE] = sort.reverse
        }
    }

    override suspend fun getActiveAccountId(): String? = activeAccountIdFlow.first()

    override suspend fun setActiveAccountId(accountId: String?) {
        dataStore.edit { prefs ->
            val cleaned = accountId?.trim().orEmpty()
            if (cleaned.isEmpty()) {
                prefs.remove(KEY_ACTIVE_ACCOUNT_ID)
            } else {
                prefs[KEY_ACTIVE_ACCOUNT_ID] = cleaned
            }
        }
    }

    override suspend fun isLegacyMigrationCompleted(): Boolean =
        safePreferencesFlow.map { prefs -> prefs[KEY_LEGACY_MIGRATION_DONE] ?: false }.first()

    override suspend fun markLegacyMigrationCompleted() {
        dataStore.edit { prefs ->
            prefs[KEY_LEGACY_MIGRATION_DONE] = true
        }
    }

    private fun mapPreferencesToSettings(prefs: Preferences): AppSettings {
        val themeId = ThemeId.fromCode(prefs[KEY_THEME_CODE])
        val blurNsfw = prefs[KEY_BLUR_NSFW] ?: true
        val showChart = prefs[KEY_SHOW_CHART] ?: true
        val fontScaleOffset = (prefs[KEY_FONT_SCALE_OFFSET] ?: 0.0f).coerceIn(-4.0f, 8.0f)
        val maxPreview = (prefs[KEY_MAX_PREVIEW_ITEMS] ?: 20).coerceIn(5, 50)
        val portraitCols = (prefs[KEY_GRID_COLS_PORTRAIT] ?: 2).coerceIn(1, 6)
        val landscapeCols = (prefs[KEY_GRID_COLS_LANDSCAPE] ?: 4).coerceIn(2, 10)
        val statusOrderRaw = prefs[KEY_STATUS_TAB_ORDER]
        val parsedOrder = statusOrderRaw
            ?.split(',')
            ?.mapNotNull { token -> token.trim().toIntOrNull()?.let(CollectionStatus::fromLabelId) }
            ?.distinct()
            .orEmpty()
        val fullStatusOrder = if (parsedOrder.isEmpty()) {
            CollectionStatus.entries
        } else {
            (parsedOrder + CollectionStatus.entries).distinct()
        }
        return AppSettings(
            themeId = themeId,
            showCoverCensor = blurNsfw,
            showChart = showChart,
            fontSizeOffset = fontScaleOffset,
            maxPreviewItem = maxPreview,
            maxItemPerRowPortrait = portraitCols,
            maxItemPerRowLandscape = landscapeCols,
            collectionStatusTabArrangement = fullStatusOrder
        )
    }

    private fun writeSettingsToPreferences(
        prefs: androidx.datastore.preferences.core.MutablePreferences,
        settings: AppSettings
    ) {
        prefs[KEY_THEME_CODE] = settings.themeId.code
        prefs[KEY_BLUR_NSFW] = settings.showCoverCensor
        prefs[KEY_SHOW_CHART] = settings.showChart
        prefs[KEY_FONT_SCALE_OFFSET] = settings.fontSizeOffset
        prefs[KEY_MAX_PREVIEW_ITEMS] = settings.maxPreviewItem
        prefs[KEY_GRID_COLS_PORTRAIT] = settings.maxItemPerRowPortrait
        prefs[KEY_GRID_COLS_LANDSCAPE] = settings.maxItemPerRowLandscape
        val orderIds = (settings.collectionStatusTabArrangement + CollectionStatus.entries)
            .distinct()
            .joinToString(",") { status -> status.labelId.toString() }
        prefs[KEY_STATUS_TAB_ORDER] = orderIds
    }

    private fun mapPreferencesToCollectionFilters(prefs: Preferences): CollectionFilters {
        val query = prefs[KEY_FILTER_QUERY].orEmpty()
        val status = prefs[KEY_FILTER_STATUS_ID]?.let(CollectionStatus::fromLabelId)
        val minAge = prefs[KEY_FILTER_MIN_AGE]?.takeIf { it > 0 }
        val languages = VnCacheEntity.decodePipeList(prefs[KEY_FILTER_LANGUAGES].orEmpty())
            .mapNotNull(Language::fromCode)
        val devStatuses = prefs[KEY_FILTER_DEV_STATUSES]
            ?.split(',')
            ?.mapNotNull { it.trim().toIntOrNull()?.let(DevStatus::fromApiCode) }
            .orEmpty()
        val origLanguages = VnCacheEntity.decodePipeList(prefs[KEY_FILTER_ORIG_LANGUAGES].orEmpty())
            .mapNotNull(Language::fromCode)
        val platforms = VnCacheEntity.decodePipeList(prefs[KEY_FILTER_PLATFORMS].orEmpty())
            .mapNotNull(Platform::fromCode)
        return CollectionFilters(
            searchQuery = query,
            status = status,
            minAge = minAge,
            languages = languages,
            devStatuses = devStatuses,
            originalLanguages = origLanguages,
            platforms = platforms
        )
    }

    private fun mapPreferencesToCollectionSort(prefs: Preferences): SortOption {
        val field = SortField.fromCode(prefs[KEY_SORT_FIELD], default = SortField.TITLE)
        val reverse = prefs[KEY_SORT_REVERSE] ?: false
        return SortOption(field = field, reverse = reverse)
    }

    companion object {
        private const val TAG = "SettingsStore"
        const val DATASTORE_FILE_NAME = "vndb_settings"

        private val KEY_THEME_CODE = stringPreferencesKey("theme_code")
        private val KEY_BLUR_NSFW = booleanPreferencesKey("blur_nsfw")
        private val KEY_SHOW_CHART = booleanPreferencesKey("show_statistics_chart")
        private val KEY_FONT_SCALE_OFFSET = floatPreferencesKey("font_scale_offset")
        private val KEY_MAX_PREVIEW_ITEMS = intPreferencesKey("max_home_preview_items")
        private val KEY_GRID_COLS_PORTRAIT = intPreferencesKey("grid_cols_portrait")
        private val KEY_GRID_COLS_LANDSCAPE = intPreferencesKey("grid_cols_landscape")
        private val KEY_STATUS_TAB_ORDER = stringPreferencesKey("collection_status_tab_order")

        private val KEY_FILTER_QUERY = stringPreferencesKey("collection_filter_query")
        private val KEY_FILTER_STATUS_ID = intPreferencesKey("collection_filter_status_id")
        private val KEY_FILTER_MIN_AGE = intPreferencesKey("collection_filter_min_age")
        private val KEY_FILTER_LANGUAGES = stringPreferencesKey("collection_filter_languages")
        private val KEY_FILTER_DEV_STATUSES = stringPreferencesKey("collection_filter_dev_statuses")
        private val KEY_FILTER_ORIG_LANGUAGES = stringPreferencesKey("collection_filter_orig_languages")
        private val KEY_FILTER_PLATFORMS = stringPreferencesKey("collection_filter_platforms")

        private val KEY_SORT_FIELD = stringPreferencesKey("collection_sort_field")
        private val KEY_SORT_REVERSE = booleanPreferencesKey("collection_sort_reverse")

        private val KEY_ACTIVE_ACCOUNT_ID = stringPreferencesKey("active_account_id")
        private val KEY_LEGACY_MIGRATION_DONE = booleanPreferencesKey("legacy_migration_done")

        fun create(context: Context, scope: CoroutineScope): DataStoreSettingsStore {
            val dataStore = PreferenceDataStoreFactory.create(
                scope = scope,
                produceFile = { context.applicationContext.preferencesDataStoreFile(DATASTORE_FILE_NAME) }
            )
            return DataStoreSettingsStore(dataStore)
        }
    }
}
