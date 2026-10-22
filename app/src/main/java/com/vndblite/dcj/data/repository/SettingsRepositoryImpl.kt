package com.vndblite.dcj.data.repository

import com.vndblite.dcj.data.local.prefs.SettingsStore
import com.vndblite.dcj.domain.model.AppSettings
import com.vndblite.dcj.domain.model.CollectionFilters
import com.vndblite.dcj.domain.model.SortOption
import com.vndblite.dcj.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class SettingsRepositoryImpl(
    private val settingsStore: SettingsStore
) : SettingsRepository {

    override val settingsFlow: Flow<AppSettings>
        get() = settingsStore.settingsFlow

    override val collectionFiltersFlow: Flow<CollectionFilters>
        get() = settingsStore.collectionFiltersFlow

    override val collectionSortFlow: Flow<SortOption>
        get() = settingsStore.collectionSortFlow

    override val activeAccountIdFlow: Flow<String?>
        get() = settingsStore.activeAccountIdFlow

    override suspend fun getSettingsSnapshot(): AppSettings =
        settingsStore.getSettingsSnapshot()

    override suspend fun saveSettings(settings: AppSettings) {
        settingsStore.saveSettings(settings)
    }

    override suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        settingsStore.updateSettings(transform)
    }

    override suspend fun resetSettingsToDefaults() {
        settingsStore.resetSettingsToDefaults()
    }

    override suspend fun saveCollectionFilters(filters: CollectionFilters) {
        settingsStore.saveCollectionFilters(filters)
    }

    override suspend fun saveCollectionSort(sort: SortOption) {
        settingsStore.saveCollectionSort(sort)
    }
}
