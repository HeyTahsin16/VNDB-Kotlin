package com.vndblite.dcj.domain.repository

import com.vndblite.dcj.domain.model.AppSettings
import com.vndblite.dcj.domain.model.CollectionFilters
import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.domain.model.SortOption
import com.vndblite.dcj.domain.model.ThemeId
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
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

    suspend fun setThemeId(themeId: ThemeId) = updateSettings { it.copy(themeId = themeId) }
    suspend fun setShowCoverCensor(enabled: Boolean) = updateSettings { it.copy(showCoverCensor = enabled) }
    suspend fun setShowChart(enabled: Boolean) = updateSettings { it.copy(showChart = enabled) }
    suspend fun setFontSizeOffset(offset: Float) = updateSettings { it.copy(fontSizeOffset = offset) }
    suspend fun setMaxPreviewItem(count: Int) = updateSettings { it.copy(maxPreviewItem = count) }
    suspend fun setMaxItemPerRowPortrait(count: Int) = updateSettings { it.copy(maxItemPerRowPortrait = count) }
    suspend fun setMaxItemPerRowLandscape(count: Int) = updateSettings { it.copy(maxItemPerRowLandscape = count) }
    suspend fun setCollectionStatusTabArrangement(arrangement: List<CollectionStatus>) =
        updateSettings { it.copy(collectionStatusTabArrangement = arrangement) }
    suspend fun resetToDefaults() = resetSettingsToDefaults()
}
