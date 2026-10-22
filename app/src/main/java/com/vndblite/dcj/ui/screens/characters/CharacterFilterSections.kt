package com.vndblite.dcj.ui.screens.characters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vndblite.dcj.R
import com.vndblite.dcj.domain.model.AndOr
import com.vndblite.dcj.domain.model.CharacterRole
import com.vndblite.dcj.domain.model.CharacterSex
import com.vndblite.dcj.domain.model.PopularTraitPreset
import com.vndblite.dcj.ui.format.displayLabel
import com.vndblite.dcj.ui.format.labelResId
import com.vndblite.dcj.ui.theme.VndbThemeTokens
import com.vndblite.dcj.ui.theme.vndbFilterChipBorder
import com.vndblite.dcj.ui.theme.vndbFilterChipColors
import com.vndblite.dcj.ui.theme.vndbInputChipColors
import com.vndblite.dcj.ui.theme.vndbOutlinedTextFieldColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CharacterTraitAutocompleteSection(
    state: CharacterSearchUiState,
    onAction: (CharacterSearchAction) -> Unit
) {
    val spec = VndbThemeTokens.spec

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.char_traits_heading),
            style = MaterialTheme.typography.titleMedium,
            color = spec.accentColor,
            fontWeight = FontWeight.Bold
        )
        AndOrToggleButton(
            mode = state.filters.traitAndOr,
            testTag = "char_trait_andor_toggle",
            onClick = { onAction(CharacterSearchAction.ToggleTraitAndOr) }
        )
    }

    OutlinedTextField(
        value = state.traitQuery,
        onValueChange = { onAction(CharacterSearchAction.UpdateTraitAutocompleteQuery(it)) },
        placeholder = { Text(stringResource(R.string.char_trait_input_hint)) },
        singleLine = true,
        trailingIcon = {
            if (state.isSearchingTraits) {
                CircularProgressIndicator(
                    color = spec.accentColor,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp)
                )
            } else if (state.traitQuery.isNotEmpty()) {
                IconButton(
                    onClick = { onAction(CharacterSearchAction.UpdateTraitAutocompleteQuery("")) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.action_clear_cd),
                        tint = spec.onSurfaceColor
                    )
                }
            }
        },
        shape = RoundedCornerShape(12.dp),
        colors = vndbOutlinedTextFieldColors(spec),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("char_trait_input")
    )

    if (state.traitSuggestions.isNotEmpty()) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(state.traitSuggestions, key = { it.id }) { suggestion ->
                AssistChip(
                    onClick = { onAction(CharacterSearchAction.ToggleTrait(suggestion)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = spec.accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text(suggestion.displayLabel()) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = spec.accentColor.copy(alpha = 0.16f),
                        labelColor = spec.onSurfaceColor
                    )
                )
            }
        }
    }

    if (state.filters.traits.isNotEmpty()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            state.filters.traits.forEach { trait ->
                InputChip(
                    selected = true,
                    onClick = { onAction(CharacterSearchAction.RemoveTrait(trait.id)) },
                    label = { Text(trait.displayLabel()) },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.action_remove),
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    colors = vndbInputChipColors(spec)
                )
            }
        }
    }
}

@Composable
internal fun CharacterPopularTraitsRow(
    state: CharacterSearchUiState,
    onAction: (CharacterSearchAction) -> Unit
) {
    val spec = VndbThemeTokens.spec
    Text(
        text = stringResource(R.string.char_popular_traits_label),
        style = MaterialTheme.typography.labelMedium,
        color = spec.onSurfaceColor.copy(alpha = 0.78f)
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(PopularTraitPreset.entries, key = { it.traitId }) { preset ->
            val trait = preset.toTrait()
            val selected = state.filters.traits.any { it.id == trait.id }
            FilterChip(
                selected = selected,
                onClick = { onAction(CharacterSearchAction.ToggleTrait(trait)) },
                label = {
                    Text(
                        text = preset.defaultName,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                leadingIcon = if (selected) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(15.dp)) }
                } else null,
                colors = vndbFilterChipColors(spec),
                border = vndbFilterChipBorder(selected, spec),
                modifier = Modifier.testTag("popular_trait_${preset.traitId}")
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CharacterRoleAndSexSection(
    state: CharacterSearchUiState,
    onAction: (CharacterSearchAction) -> Unit
) {
    val spec = VndbThemeTokens.spec
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.char_roles_heading),
            style = MaterialTheme.typography.titleMedium,
            color = spec.accentColor,
            fontWeight = FontWeight.Bold
        )
        AndOrToggleButton(
            mode = state.filters.roleAndOr,
            testTag = "char_role_andor_toggle",
            onClick = { onAction(CharacterSearchAction.ToggleRoleAndOr) }
        )
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        CharacterRole.entries.forEach { role ->
            val selected = role in state.filters.roles
            FilterChip(
                selected = selected,
                onClick = { onAction(CharacterSearchAction.ToggleRole(role)) },
                label = { Text(stringResource(role.labelResId())) },
                colors = vndbFilterChipColors(spec),
                border = vndbFilterChipBorder(selected, spec),
                modifier = Modifier.testTag("char_role_${role.apiValue}")
            )
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.char_sex_heading),
            style = MaterialTheme.typography.titleMedium,
            color = spec.accentColor,
            fontWeight = FontWeight.Bold
        )
        AndOrToggleButton(
            mode = state.filters.sexAndOr,
            testTag = "char_sex_andor_toggle",
            onClick = { onAction(CharacterSearchAction.ToggleSexAndOr) }
        )
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        CharacterSex.entries.forEach { sex ->
            val selected = sex in state.filters.sexes
            FilterChip(
                selected = selected,
                onClick = { onAction(CharacterSearchAction.ToggleSex(sex)) },
                label = { Text(stringResource(sex.labelResId())) },
                colors = vndbFilterChipColors(spec),
                border = vndbFilterChipBorder(selected, spec),
                modifier = Modifier.testTag("char_sex_${sex.apiValue}")
            )
        }
    }
}

@Composable
internal fun AndOrToggleButton(
    mode: AndOr,
    testTag: String,
    onClick: () -> Unit
) {
    val spec = VndbThemeTokens.spec
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
        modifier = Modifier.testTag(testTag)
    ) {
        Text(
            text = stringResource(
                if (mode == AndOr.AND) R.string.filter_logic_and else R.string.filter_logic_or
            ),
            style = MaterialTheme.typography.labelSmall,
            color = spec.accentColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
internal fun CharacterLoadMoreFooter(
    isLoadingMore: Boolean,
    onLoadMore: () -> Unit
) {
    val spec = VndbThemeTokens.spec
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoadingMore) {
            CircularProgressIndicator(color = spec.accentColor, modifier = Modifier.size(28.dp))
        } else {
            OutlinedButton(onClick = onLoadMore) {
                Text(stringResource(R.string.search_load_more), color = spec.accentColor)
            }
        }
    }
}
