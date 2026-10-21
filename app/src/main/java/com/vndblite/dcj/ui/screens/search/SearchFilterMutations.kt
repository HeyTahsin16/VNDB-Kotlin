package com.vndblite.dcj.ui.screens.search

import com.vndblite.dcj.domain.model.DevStatus
import com.vndblite.dcj.domain.model.HomeSection
import com.vndblite.dcj.domain.model.QuickGenrePreset
import com.vndblite.dcj.domain.model.ReleaseWindow
import com.vndblite.dcj.domain.model.SortField
import com.vndblite.dcj.domain.model.SortOption
import com.vndblite.dcj.domain.model.VnSearchFilters

/**
 * Pure filter mutation functions for [SearchViewModel] (A4, A9).
 */
internal object SearchFilterMutations {

    fun toggleQuickGenre(
        current: VnSearchFilters,
        genre: QuickGenrePreset
    ): VnSearchFilters {
        val tag = genre.toTag()
        val exists = current.tags.any { it.id == tag.id }
        val nextTags = if (exists) {
            current.tags.filterNot { it.id == tag.id }
        } else {
            current.tags + tag
        }
        return current.copy(tags = nextTags)
    }

    fun removeChip(
        current: VnSearchFilters,
        chip: ActiveFilterChip
    ): VnSearchFilters = when (chip) {
        is ActiveFilterChip.TagChip ->
            current.copy(tags = current.tags.filterNot { it.id == chip.tag.id })
        is ActiveFilterChip.TraitChip ->
            current.copy(characterTraits = current.characterTraits.filterNot { it.id == chip.trait.id })
        is ActiveFilterChip.DeveloperChip ->
            current.copy(developers = current.developers.filterNot { it.id == chip.developer.id })
        is ActiveFilterChip.LanguageChip ->
            current.copy(languages = current.languages - chip.language)
        is ActiveFilterChip.OriginLanguageChip ->
            current.copy(originalLanguages = current.originalLanguages - chip.language)
        is ActiveFilterChip.PlatformChip ->
            current.copy(platforms = current.platforms - chip.platform)
        is ActiveFilterChip.DevStatusChip ->
            current.copy(devStatuses = current.devStatuses - chip.devStatus)
        is ActiveFilterChip.CharacterRoleChip ->
            current.copy(characterRoles = current.characterRoles - chip.role)
        is ActiveFilterChip.CharacterSexChip ->
            current.copy(characterSexes = current.characterSexes - chip.sex)
        ActiveFilterChip.ReleaseWindowChip ->
            current.copy(releaseWindow = ReleaseWindow.ANY)
    }

    fun filtersAndSortForHomeSection(section: HomeSection): Pair<VnSearchFilters, SortOption> =
        when (section) {
            HomeSection.LATEST_RELEASES ->
                VnSearchFilters(releaseWindow = ReleaseWindow.LATEST) to
                    SortOption(SortField.RELEASED, reverse = true)
            HomeSection.UPCOMING_RELEASES ->
                VnSearchFilters(releaseWindow = ReleaseWindow.UPCOMING) to
                    SortOption(SortField.RELEASED, reverse = false)
            HomeSection.TOP_RATED ->
                VnSearchFilters() to SortOption(SortField.RATING, reverse = true)
            HomeSection.TOP_VOTED ->
                VnSearchFilters() to SortOption(SortField.VOTE_COUNT, reverse = true)
            HomeSection.POPULAR_ONGOING ->
                VnSearchFilters(devStatuses = listOf(DevStatus.IN_DEVELOPMENT)) to
                    SortOption(SortField.VOTE_COUNT, reverse = true)
            HomeSection.COLLECTION ->
                VnSearchFilters() to SortOption.DEFAULT_REMOTE
        }
}
