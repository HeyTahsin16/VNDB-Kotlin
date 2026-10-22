package com.vndblite.dcj.ui.screens.characters

import com.vndblite.dcj.core.ApiError
import com.vndblite.dcj.domain.model.AndOr
import com.vndblite.dcj.domain.model.AppSettings
import com.vndblite.dcj.domain.model.Character
import com.vndblite.dcj.domain.model.CharacterRole
import com.vndblite.dcj.domain.model.CharacterSearchFilters
import com.vndblite.dcj.domain.model.CharacterSex
import com.vndblite.dcj.domain.model.Trait

data class CharacterSearchUiState(
    val settings: AppSettings = AppSettings(),
    val filters: CharacterSearchFilters = CharacterSearchFilters(),
    val results: List<Character> = emptyList(),
    val page: Int = 1,
    val hasMore: Boolean = false,
    val isLoading: Boolean = false,
    val isAppending: Boolean = false,
    val error: ApiError? = null,
    val traitSuggestions: List<Trait> = emptyList(),
    val selectedCharacter: Character? = null,
    val revealedNsfwIds: Set<String> = emptySet()
)

sealed interface CharacterSearchAction {
    data class UpdateQuery(val query: String) : CharacterSearchAction
    data object SubmitSearch : CharacterSearchAction
    data object Retry : CharacterSearchAction
    data object LoadNextPage : CharacterSearchAction
    data class ToggleTrait(val trait: Trait) : CharacterSearchAction
    data class ToggleRole(val role: CharacterRole) : CharacterSearchAction
    data class ToggleSex(val sex: CharacterSex) : CharacterSearchAction
    data class ToggleTraitAndOr(val andOr: AndOr) : CharacterSearchAction
    data object ResetFilters : CharacterSearchAction
    data class QueryTraitSuggestions(val query: String) : CharacterSearchAction
    data class SelectCharacter(val character: Character) : CharacterSearchAction
    data object DismissCharacterBio : CharacterSearchAction
    data class ToggleNsfwReveal(val id: String) : CharacterSearchAction
}
