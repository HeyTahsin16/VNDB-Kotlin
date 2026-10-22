package com.vndblite.dcj.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vndblite.dcj.R
import com.vndblite.dcj.domain.model.ReleaseWindow
import com.vndblite.dcj.domain.model.SortField
import com.vndblite.dcj.domain.model.SortOption
import com.vndblite.dcj.domain.model.VnSearchFilters
import com.vndblite.dcj.ui.format.displayLabel
import com.vndblite.dcj.ui.format.labelResId
import com.vndblite.dcj.ui.theme.VndbThemeTokens
import com.vndblite.dcj.ui.theme.vndbFilterChipBorder
import com.vndblite.dcj.ui.theme.vndbFilterChipColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RemoteFilterSheet(
    initialFilters: VnSearchFilters,
    initialSort: SortOption,
    autocomplete: FilterSheetAutocompleteState,
    onAction: (SearchAction) -> Unit,
    onDismiss: () -> Unit,
    onApply: (VnSearchFilters, SortOption) -> Unit
) {
    val spec = VndbThemeTokens.spec
    val semantic = VndbThemeTokens.colors
    var draftFilters by remember(initialFilters) { mutableStateOf(initialFilters) }
    var draftSort by remember(initialSort) { mutableStateOf(initialSort) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = spec.surfaceColor,
        contentColor = spec.onSurfaceColor,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = spec.onSurfaceColor.copy(alpha = 0.5f))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.search_sheet_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = spec.accentColor,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            draftFilters = VnSearchFilters(searchQuery = draftFilters.searchQuery)
                            draftSort = SortOption.DEFAULT_REMOTE
                        }
                    ) {
                        Text(stringResource(R.string.action_reset), color = semantic.danger)
                    }
                    Button(
                        onClick = { onApply(draftFilters, draftSort) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = spec.accentColor,
                            contentColor = semantic.onAccentButton
                        ),
                        modifier = Modifier.testTag("apply_remote_filter_button")
                    ) {
                        Text(stringResource(R.string.action_apply), fontWeight = FontWeight.Bold)
                    }
                }
            }

            HorizontalDivider(
                color = spec.onSurfaceColor.copy(alpha = 0.25f),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                RemoteSortAndReleaseSection(
                    draftSort = draftSort,
                    draftFilters = draftFilters,
                    onUpdateSort = { draftSort = it },
                    onUpdateFilters = { draftFilters = it }
                )

                RemoteEntityAutocompleteSection(
                    heading = stringResource(R.string.search_sheet_tags_heading),
                    hint = stringResource(R.string.search_sheet_tag_input_hint),
                    andOr = draftFilters.andOr,
                    onToggleAndOr = { draftFilters = draftFilters.copy(andOr = draftFilters.andOr.toggled()) },
                    query = autocomplete.tagQuery,
                    isSearching = autocomplete.isSearchingTags,
                    onQueryChange = { onAction(SearchAction.UpdateTagAutocompleteQuery(it)) },
                    suggestions = autocomplete.tagSuggestions,
                    suggestionKey = { it.id },
                    suggestionLabel = { it.name },
                    onAddSuggestion = { tag ->
                        if (draftFilters.tags.none { it.id == tag.id }) {
                            draftFilters = draftFilters.copy(tags = draftFilters.tags + tag)
                        }
                        onAction(SearchAction.UpdateTagAutocompleteQuery(""))
                    },
                    selectedItems = draftFilters.tags,
                    selectedLabel = { it.name },
                    onRemoveSelected = { tag ->
                        draftFilters = draftFilters.copy(tags = draftFilters.tags.filterNot { it.id == tag.id })
                    }
                )

                RemoteEntityAutocompleteSection(
                    heading = stringResource(R.string.search_sheet_traits_heading),
                    hint = stringResource(R.string.search_sheet_trait_input_hint),
                    andOr = draftFilters.traitAndOr,
                    onToggleAndOr = { draftFilters = draftFilters.copy(traitAndOr = draftFilters.traitAndOr.toggled()) },
                    query = autocomplete.traitQuery,
                    isSearching = autocomplete.isSearchingTraits,
                    onQueryChange = { onAction(SearchAction.UpdateTraitAutocompleteQuery(it)) },
                    suggestions = autocomplete.traitSuggestions,
                    suggestionKey = { it.id },
                    suggestionLabel = { it.displayLabel() },
                    onAddSuggestion = { trait ->
                        if (draftFilters.characterTraits.none { it.id == trait.id }) {
                            draftFilters = draftFilters.copy(characterTraits = draftFilters.characterTraits + trait)
                        }
                        onAction(SearchAction.UpdateTraitAutocompleteQuery(""))
                    },
                    selectedItems = draftFilters.characterTraits,
                    selectedLabel = { it.displayLabel() },
                    onRemoveSelected = { trait ->
                        draftFilters = draftFilters.copy(
                            characterTraits = draftFilters.characterTraits.filterNot { it.id == trait.id }
                        )
                    },
                    inputTestTag = "filter_sheet_trait_input"
                )

                RemoteEntityAutocompleteSection(
                    heading = stringResource(R.string.search_sheet_developers_heading),
                    hint = stringResource(R.string.search_sheet_developer_input_hint),
                    andOr = null,
                    onToggleAndOr = {},
                    query = autocomplete.developerQuery,
                    isSearching = autocomplete.isSearchingDevelopers,
                    onQueryChange = { onAction(SearchAction.UpdateDeveloperAutocompleteQuery(it)) },
                    suggestions = autocomplete.developerSuggestions,
                    suggestionKey = { it.id },
                    suggestionLabel = { it.name },
                    onAddSuggestion = { dev ->
                        if (draftFilters.developers.none { it.id == dev.id }) {
                            draftFilters = draftFilters.copy(developers = draftFilters.developers + dev)
                        }
                        onAction(SearchAction.UpdateDeveloperAutocompleteQuery(""))
                    },
                    selectedItems = draftFilters.developers,
                    selectedLabel = { it.name },
                    onRemoveSelected = { dev ->
                        draftFilters = draftFilters.copy(
                            developers = draftFilters.developers.filterNot { it.id == dev.id }
                        )
                    }
                )

                RemoteMetadataSelectorsSection(
                    draftFilters = draftFilters,
                    onUpdateFilters = { draftFilters = it }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RemoteSortAndReleaseSection(
    draftSort: SortOption,
    draftFilters: VnSearchFilters,
    onUpdateSort: (SortOption) -> Unit,
    onUpdateFilters: (VnSearchFilters) -> Unit
) {
    val spec = VndbThemeTokens.spec
    Text(
        text = stringResource(R.string.search_sheet_sort_heading),
        style = MaterialTheme.typography.titleMedium,
        color = spec.accentColor,
        fontWeight = FontWeight.Bold
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SortField.remoteSortableFields.forEach { field ->
            val selected = draftSort.field == field
            FilterChip(
                selected = selected,
                onClick = { onUpdateSort(draftSort.copy(field = field)) },
                label = { Text(field.displayLabel()) },
                leadingIcon = if (selected) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(15.dp)) }
                } else null,
                colors = vndbFilterChipColors(spec),
                border = vndbFilterChipBorder(selected, spec)
            )
        }
        FilterChip(
            selected = draftSort.reverse,
            onClick = { onUpdateSort(draftSort.copy(reverse = !draftSort.reverse)) },
            label = {
                Text(
                    text = stringResource(
                        if (draftSort.reverse) R.string.order_descending_chip
                        else R.string.order_ascending_chip
                    ),
                    fontWeight = FontWeight.Bold
                )
            },
            colors = vndbFilterChipColors(spec),
            border = vndbFilterChipBorder(draftSort.reverse, spec)
        )
    }

    Text(
        text = stringResource(R.string.search_sheet_release_window_heading),
        style = MaterialTheme.typography.titleMedium,
        color = spec.accentColor,
        fontWeight = FontWeight.Bold
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ReleaseWindow.entries.forEach { window ->
            val selected = draftFilters.releaseWindow == window
            FilterChip(
                selected = selected,
                onClick = { onUpdateFilters(draftFilters.copy(releaseWindow = window)) },
                label = { Text(stringResource(window.labelResId())) },
                colors = vndbFilterChipColors(spec),
                border = vndbFilterChipBorder(selected, spec)
            )
        }
    }
}
