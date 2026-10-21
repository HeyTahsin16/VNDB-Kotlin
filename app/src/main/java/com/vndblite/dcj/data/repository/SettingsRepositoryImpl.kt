package com.vndblite.dcj.data.repository

import com.vndblite.dcj.data.local.prefs.SettingsStore
import com.vndblite.dcj.domain.model.AppSettings
import com.vndblite.dcj.domain.model.CollectionFilters
import com.vndblite.dcj.domain.model.SortOption
import com.vndblite.dcj.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Default [SettingsRepository] backed by [SettingsStore] and an in-memory session-scoped
 * NSFW reveal set (Section 5, A3, S1, F10).
 */
class SettingsRepositoryImpl(
    private val settingsStore: SettingsStore
) : SettingsRepository {

    private val _nsfwRevealedIdsFlow = MutableStateFlow<Set<String>>(emptySet())
    override val nsfwRevealedIdsFlow: StateFlow<Set<String>> = _nsfwRevealedIdsFlow.asStateFlow()

    override val settingsFlow: Flow<AppSettings> = settingsStore.settingsFlow
    override val collectionFiltersFlow: Flow<CollectionFilters> = settingsStore.collectionFiltersFlow
    override val collectionSortFlow: Flow<SortOption> = settingsStore.collectionSortFlow

    override fun toggleNsfwReveal(id: String) {
        val trimmed = id.trim()
        if (trimmed.isEmpty()) return
        _nsfwRevealedIdsFlow.update { current ->
            if (trimmed in current) current - trimmed else current + trimmed
        }
    }

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
