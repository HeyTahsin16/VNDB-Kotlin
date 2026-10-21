package com.vndblite.dcj.domain.repository

import com.vndblite.dcj.domain.model.AppSettings
import com.vndblite.dcj.domain.model.CollectionFilters
import com.vndblite.dcj.domain.model.SortOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository contract for reactive user settings, local collection filter/sort preferences,
 * and session-scoped NSFW cover reveal state (Section 5, A3, S1, F10).
 */
interface SettingsRepository {
    val settingsFlow: Flow<AppSettings>
    val collectionFiltersFlow: Flow<CollectionFilters>
    val collectionSortFlow: Flow<SortOption>

    /**
     * Session-scoped set of VN/character/screenshot IDs whose NSFW cover blur has been toggled
     * open by the user so cards do not re-blur when scrolling away (F10).
     */
    val nsfwRevealedIdsFlow: StateFlow<Set<String>>

    fun toggleNsfwReveal(id: String)

    suspend fun getSettingsSnapshot(): AppSettings

    suspend fun saveSettings(settings: AppSettings)

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings)

    suspend fun resetSettingsToDefaults()

    suspend fun saveCollectionFilters(filters: CollectionFilters)

    suspend fun saveCollectionSort(sort: SortOption)
}
