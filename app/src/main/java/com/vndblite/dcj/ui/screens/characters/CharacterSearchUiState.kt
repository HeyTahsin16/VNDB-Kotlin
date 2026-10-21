package com.vndblite.dcj.ui.screens.characters

import androidx.compose.runtime.Immutable
import com.vndblite.dcj.core.UiText
import com.vndblite.dcj.domain.model.Character
import com.vndblite.dcj.domain.model.CharacterRole
import com.vndblite.dcj.domain.model.CharacterSearchFilters
import com.vndblite.dcj.domain.model.CharacterSex
import com.vndblite.dcj.domain.model.Trait
import com.vndblite.dcj.domain.model.VnSummary

@Immutable
data class CharacterSearchUiState(
    val filters: CharacterSearchFilters = CharacterSearchFilters(),
    val traitQuery: String = "",
    val traitSuggestions: List<Trait> = emptyList(),
    val isSearchingTraits: Boolean = false,
    val characters: List<Character> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = false,
    val currentPage: Int = 1,
    val error: UiText? = null,
    val showCoverCensor: Boolean = true,
    val nsfwRevealedIds: Set<String> = emptySet()
)

sealed interface CharacterSearchAction {
    data class UpdateNameQuery(val query: String) : CharacterSearchAction
    data class UpdateTraitAutocompleteQuery(val query: String) : CharacterSearchAction
    data class ToggleTrait(val trait: Trait) : CharacterSearchAction
    data class RemoveTrait(val traitId: String) : CharacterSearchAction
    data object ToggleTraitAndOr : CharacterSearchAction
    data class ToggleRole(val role: CharacterRole) : CharacterSearchAction
    data object ToggleRoleAndOr : CharacterSearchAction
    data class ToggleSex(val sex: CharacterSex) : CharacterSearchAction
    data object ToggleSexAndOr : CharacterSearchAction
    data object ClearAllFilters : CharacterSearchAction
    data object ExecuteSearchNow : CharacterSearchAction
    data object LoadNextPage : CharacterSearchAction
    data object Retry : CharacterSearchAction
    data class ToggleNsfwReveal(val id: String) : CharacterSearchAction
    data class FilterVnsByTrait(val trait: Trait) : CharacterSearchAction
    data class OpenVnDetail(val vn: VnSummary) : CharacterSearchAction
}
