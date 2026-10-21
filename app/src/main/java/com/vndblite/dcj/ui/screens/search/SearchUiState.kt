package com.vndblite.dcj.ui.screens.search

import androidx.compose.runtime.Immutable
import com.vndblite.dcj.core.UiText
import com.vndblite.dcj.domain.model.AppSettings
import com.vndblite.dcj.domain.model.CharacterRole
import com.vndblite.dcj.domain.model.CharacterSex
import com.vndblite.dcj.domain.model.CollectionEntry
import com.vndblite.dcj.domain.model.DevStatus
import com.vndblite.dcj.domain.model.HomeSection
import com.vndblite.dcj.domain.model.Language
import com.vndblite.dcj.domain.model.Platform
import com.vndblite.dcj.domain.model.Producer
import com.vndblite.dcj.domain.model.QuickGenrePreset
import com.vndblite.dcj.domain.model.SortOption
import com.vndblite.dcj.domain.model.Tag
import com.vndblite.dcj.domain.model.Trait
import com.vndblite.dcj.domain.model.VnSearchFilters
import com.vndblite.dcj.domain.model.VnSummary

enum class SearchMode {
    VISUAL_NOVELS,
    CHARACTERS
}

/**
 * Type-safe filter chip descriptor replacing the legacy 10-positional-nullable-argument
 * `removeSingleFilterChip(...)` method (A4).
 */
@Immutable
sealed interface ActiveFilterChip {
    val key: String

    data class TagChip(val tag: Tag) : ActiveFilterChip {
        override val key: String = "tag_${tag.id}"
    }

    data class TraitChip(val trait: Trait) : ActiveFilterChip {
        override val key: String = "trait_${trait.id}"
    }

    data class DeveloperChip(val developer: Producer) : ActiveFilterChip {
        override val key: String = "dev_${developer.id}"
    }

    data class LanguageChip(val language: Language) : ActiveFilterChip {
        override val key: String = "lang_${language.code}"
    }

    data class OriginLanguageChip(val language: Language) : ActiveFilterChip {
        override val key: String = "olang_${language.code}"
    }

    data class PlatformChip(val platform: Platform) : ActiveFilterChip {
        override val key: String = "plat_${platform.code}"
    }

    data class DevStatusChip(val devStatus: DevStatus) : ActiveFilterChip {
        override val key: String = "devstatus_${devStatus.apiValue}"
    }

    data class CharacterRoleChip(val role: CharacterRole) : ActiveFilterChip {
        override val key: String = "crole_${role.apiValue}"
    }

    data class CharacterSexChip(val sex: CharacterSex) : ActiveFilterChip {
        override val key: String = "csex_${sex.apiValue}"
    }

    data object ReleaseWindowChip : ActiveFilterChip {
        override val key: String = "release_window"
    }
}

@Immutable
data class FilterSheetAutocompleteState(
    val tagQuery: String = "",
    val tagSuggestions: List<Tag> = emptyList(),
    val isSearchingTags: Boolean = false,
    val traitQuery: String = "",
    val traitSuggestions: List<Trait> = emptyList(),
    val isSearchingTraits: Boolean = false,
    val developerQuery: String = "",
    val developerSuggestions: List<Producer> = emptyList(),
    val isSearchingDevelopers: Boolean = false
)

@Immutable
data class SearchUiState(
    val mode: SearchMode = SearchMode.VISUAL_NOVELS,
    val filters: VnSearchFilters = VnSearchFilters(),
    val sort: SortOption = SortOption.DEFAULT_REMOTE,
    val results: List<VnSummary> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = false,
    val currentPage: Int = 1,
    val error: UiText? = null,
    val autocomplete: FilterSheetAutocompleteState = FilterSheetAutocompleteState(),
    val collectionEntries: Map<String, CollectionEntry> = emptyMap(),
    val settings: AppSettings = AppSettings(),
    val nsfwRevealedIds: Set<String> = emptySet()
) {
    val activeFilterChips: List<ActiveFilterChip>
        get() = buildList {
            if (filters.releaseWindow != com.vndblite.dcj.domain.model.ReleaseWindow.ANY) {
                add(ActiveFilterChip.ReleaseWindowChip)
            }
            filters.tags.forEach { add(ActiveFilterChip.TagChip(it)) }
            filters.characterTraits.forEach { add(ActiveFilterChip.TraitChip(it)) }
            filters.developers.forEach { add(ActiveFilterChip.DeveloperChip(it)) }
            filters.languages.forEach { add(ActiveFilterChip.LanguageChip(it)) }
            filters.originalLanguages.forEach { add(ActiveFilterChip.OriginLanguageChip(it)) }
            filters.platforms.forEach { add(ActiveFilterChip.PlatformChip(it)) }
            filters.devStatuses.forEach { add(ActiveFilterChip.DevStatusChip(it)) }
            filters.characterRoles.forEach { add(ActiveFilterChip.CharacterRoleChip(it)) }
            filters.characterSexes.forEach { add(ActiveFilterChip.CharacterSexChip(it)) }
        }
}

sealed interface SearchAction {
    data class SelectMode(val mode: SearchMode) : SearchAction
    data class UpdateQuery(val query: String) : SearchAction
    data object ExecuteSearchNow : SearchAction
    data class ToggleQuickGenre(val genre: QuickGenrePreset) : SearchAction
    data class ApplyFiltersAndSort(val filters: VnSearchFilters, val sort: SortOption) : SearchAction
    data class RemoveChip(val chip: ActiveFilterChip) : SearchAction
    data object ClearAllFilters : SearchAction
    data class ApplyHomeSectionPreset(val section: HomeSection) : SearchAction
    data class ApplySingleTagFilter(val tag: Tag) : SearchAction
    data class ApplySingleTraitFilter(val trait: Trait) : SearchAction
    data class UpdateTagAutocompleteQuery(val query: String) : SearchAction
    data class UpdateTraitAutocompleteQuery(val query: String) : SearchAction
    data class UpdateDeveloperAutocompleteQuery(val query: String) : SearchAction
    data object LoadNextPage : SearchAction
    data object Retry : SearchAction
    data class ToggleNsfwReveal(val vnId: String) : SearchAction
    data class OpenVnDetail(val vn: VnSummary) : SearchAction
}
