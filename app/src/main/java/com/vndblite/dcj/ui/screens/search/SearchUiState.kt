package com.vndblite.dcj.ui.screens.search

import com.vndblite.dcj.core.ApiError
import com.vndblite.dcj.domain.model.AndOr
import com.vndblite.dcj.domain.model.AppSettings
import com.vndblite.dcj.domain.model.CharacterRole
import com.vndblite.dcj.domain.model.CharacterSex
import com.vndblite.dcj.domain.model.CollectionEntry
import com.vndblite.dcj.domain.model.DevStatus
import com.vndblite.dcj.domain.model.Language
import com.vndblite.dcj.domain.model.Platform
import com.vndblite.dcj.domain.model.Producer
import com.vndblite.dcj.domain.model.ReleaseWindow
import com.vndblite.dcj.domain.model.SortField
import com.vndblite.dcj.domain.model.SortOption
import com.vndblite.dcj.domain.model.Tag
import com.vndblite.dcj.domain.model.Trait
import com.vndblite.dcj.domain.model.VnSearchFilters
import com.vndblite.dcj.domain.model.VnSummary

data class SearchUiState(
    val settings: AppSettings = AppSettings(),
    val filters: VnSearchFilters = VnSearchFilters(),
    val sort: SortOption = SortOption.DEFAULT_REMOTE,
    val results: List<VnSummary> = emptyList(),
    val page: Int = 1,
    val hasMore: Boolean = false,
    val isLoading: Boolean = false,
    val isAppending: Boolean = false,
    val error: ApiError? = null,
    val collectionByVnId: Map<String, CollectionEntry> = emptyMap(),
    val tagSuggestions: List<Tag> = emptyList(),
    val traitSuggestions: List<Trait> = emptyList(),
    val producerSuggestions: List<Producer> = emptyList(),
    val revealedNsfwIds: Set<String> = emptySet()
)

sealed interface SearchAction {
    data class UpdateQuery(val query: String) : SearchAction
    data object SubmitSearch : SearchAction
    data object Retry : SearchAction
    data object LoadNextPage : SearchAction
    data class ApplyFiltersAndSort(
        val filters: VnSearchFilters,
        val sort: SortOption
    ) : SearchAction
    data object ResetFilters : SearchAction
    data class ToggleTag(val tag: Tag) : SearchAction
    data class ToggleTrait(val trait: Trait) : SearchAction
    data class ToggleProducer(val producer: Producer) : SearchAction
    data class ToggleLanguage(val language: Language) : SearchAction
    data class ToggleOriginalLanguage(val language: Language) : SearchAction
    data class TogglePlatform(val platform: Platform) : SearchAction
    data class ToggleDevStatus(val devStatus: DevStatus) : SearchAction
    data class ToggleCharacterRole(val role: CharacterRole) : SearchAction
    data class ToggleCharacterSex(val sex: CharacterSex) : SearchAction
    data class SelectReleaseWindow(val window: ReleaseWindow) : SearchAction
    data class SelectSort(val field: SortField, val reverse: Boolean) : SearchAction
    data class ToggleFilterAndOr(val andOr: AndOr) : SearchAction
    data class QueryTagSuggestions(val query: String) : SearchAction
    data class QueryTraitSuggestions(val query: String) : SearchAction
    data class QueryProducerSuggestions(val query: String) : SearchAction
    data class ToggleNsfwReveal(val id: String) : SearchAction
}
