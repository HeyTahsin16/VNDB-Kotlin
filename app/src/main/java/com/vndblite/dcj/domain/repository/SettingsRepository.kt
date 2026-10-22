package com.vndblite.dcj.domain.repository

import com.vndblite.dcj.domain.model.AppSettings
import com.vndblite.dcj.domain.model.CollectionFilters
import com.vndblite.dcj.domain.model.SortOption
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
}
