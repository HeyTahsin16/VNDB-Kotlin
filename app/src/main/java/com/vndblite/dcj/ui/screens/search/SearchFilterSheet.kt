package com.vndblite.dcj.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.vndblite.dcj.domain.model.DevStatus
import com.vndblite.dcj.domain.model.GenrePreset
import com.vndblite.dcj.domain.model.Language
import com.vndblite.dcj.domain.model.Platform
import com.vndblite.dcj.domain.model.Producer
import com.vndblite.dcj.domain.model.ReleaseWindow
import com.vndblite.dcj.domain.model.SortField
import com.vndblite.dcj.domain.model.SortOption
import com.vndblite.dcj.domain.model.Tag
import com.vndblite.dcj.domain.model.Trait
import com.vndblite.dcj.ui.components.AssetImage
import com.vndblite.dcj.ui.components.AssetPaths
import com.vndblite.dcj.ui.format.labelRes
import com.vndblite.dcj.ui.theme.LocalThemeSpec
import com.vndblite.dcj.ui.theme.vndbFilterChipBorder
import com.vndblite.dcj.ui.theme.vndbFilterChipColors
import com.vndblite.dcj.ui.theme.vndbInputChipColors
import com.vndblite.dcj.ui.theme.vndbOutlinedTextFieldColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchFilterSheet(
    uiState: SearchUiState,
    onAction: (SearchAction) -> Unit,
    onDismiss: () -> Unit
) {
    val themeSpec = LocalThemeSpec.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tagInput by rememberSaveable { mutableStateOf("") }
    var traitInput by rememberSaveable { mutableStateOf("") }
    var devInput by rememberSaveable { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = themeSpec.surfaceColor,
        contentColor = themeSpec.contentColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.search_filter_sheet_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = themeSpec.accentColor
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onAction(SearchAction.ResetFilters) },
                        modifier = Modifier.testTag("search_reset_filters_button")
                    ) {
                        Text(
                            text = stringResource(R.string.search_reset_filters),
                            color = themeSpec.contentColor
                        )
                    }
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = themeSpec.accentColor,
                            contentColor = themeSpec.onAccentColor
                        ),
                        modifier = Modifier.testTag("search_close_filters_button")
                    ) {
                        Text(
                            text = stringResource(R.string.action_close),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            SortControlsSection(
                currentSort = uiState.sort,
                onSelectSort = { field, reverse -> onAction(SearchAction.SelectSort(field, reverse)) }
            )

            HorizontalDivider(color = themeSpec.contentColor.copy(alpha = 0.2f))

            ReleaseWindowSection(
                selectedWindow = uiState.filters.releaseWindow,
                onSelectWindow = { onAction(SearchAction.SelectReleaseWindow(it)) }
            )

            TagFilterSection(
                tagInput = tagInput,
                onTagInputChange = {
                    tagInput = it
                    onAction(SearchAction.QueryTagSuggestions(it))
                },
                selectedTags = uiState.filters.tags,
                suggestions = uiState.tagSuggestions,
                andOr = uiState.filters.andOr,
                onToggleAndOr = { onAction(SearchAction.ToggleFilterAndOr(uiState.filters.andOr.toggle())) },
                onToggleTag = { onAction(SearchAction.ToggleTag(it)) }
            )

            TraitFilterSection(
                traitInput = traitInput,
                onTraitInputChange = {
                    traitInput = it
                    onAction(SearchAction.QueryTraitSuggestions(it))
                },
                selectedTraits = uiState.filters.characterTraits,
                suggestions = uiState.traitSuggestions,
                onToggleTrait = { onAction(SearchAction.ToggleTrait(it)) }
            )

            DeveloperFilterSection(
                devInput = devInput,
                onDevInputChange = {
                    devInput = it
                    onAction(SearchAction.QueryProducerSuggestions(it))
                },
                selectedProducers = uiState.filters.developers,
                suggestions = uiState.producerSuggestions,
                onToggleProducer = { onAction(SearchAction.ToggleProducer(it)) }
            )

            MetadataChipSection(
                filters = uiState.filters,
                onAction = onAction
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SortControlsSection(
    currentSort: SortOption,
    onSelectSort: (SortField, Boolean) -> Unit
) {
    val themeSpec = LocalThemeSpec.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.search_sort_cd),
            style = MaterialTheme.typography.titleMedium,
            color = themeSpec.accentColor
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SortField.remoteSortableFields.forEach { field ->
                val selected = currentSort.field == field
                FilterChip(
                    selected = selected,
                    onClick = { onSelectSort(field, currentSort.reverse) },
                    label = { Text(stringResource(field.labelRes())) },
                    colors = vndbFilterChipColors(),
                    border = vndbFilterChipBorder(selected = selected),
                    modifier = Modifier.testTag("search_sort_${field.code}")
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = currentSort.reverse,
                onClick = { onSelectSort(currentSort.field, true) },
                label = { Text(stringResource(R.string.sort_order_descending)) },
                colors = vndbFilterChipColors(),
                border = vndbFilterChipBorder(selected = currentSort.reverse)
            )
            FilterChip(
                selected = !currentSort.reverse,
                onClick = { onSelectSort(currentSort.field, false) },
                label = { Text(stringResource(R.string.sort_order_ascending)) },
                colors = vndbFilterChipColors(),
                border = vndbFilterChipBorder(selected = !currentSort.reverse)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReleaseWindowSection(
    selectedWindow: ReleaseWindow,
    onSelectWindow: (ReleaseWindow) -> Unit
) {
    val themeSpec = LocalThemeSpec.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.search_release_window_title),
            style = MaterialTheme.typography.titleMedium,
            color = themeSpec.accentColor
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReleaseWindow.entries.forEach { window ->
                val selected = selectedWindow == window
                FilterChip(
                    selected = selected,
                    onClick = { onSelectWindow(window) },
                    label = { Text(stringResource(window.labelRes())) },
                    colors = vndbFilterChipColors(),
                    border = vndbFilterChipBorder(selected = selected)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagFilterSection(
    tagInput: String,
    onTagInputChange: (String) -> Unit,
    selectedTags: List<Tag>,
    suggestions: List<Tag>,
    andOr: AndOr,
    onToggleAndOr: () -> Unit,
    onToggleTag: (Tag) -> Unit
) {
    val themeSpec = LocalThemeSpec.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.search_tags_title),
                style = MaterialTheme.typography.titleMedium,
                color = themeSpec.accentColor
            )
            FilterChip(
                selected = andOr == AndOr.AND,
                onClick = onToggleAndOr,
                label = {
                    Text(
                        if (andOr == AndOr.AND) {
                            stringResource(R.string.search_operator_and)
                        } else {
                            stringResource(R.string.search_operator_or)
                        }
                    )
                },
                colors = vndbFilterChipColors(),
                border = vndbFilterChipBorder(selected = true)
            )
        }
        OutlinedTextField(
            value = tagInput,
            onValueChange = onTagInputChange,
            label = { Text(stringResource(R.string.search_tags_hint)) },
            singleLine = true,
            colors = vndbOutlinedTextFieldColors(),
            modifier = Modifier.fillMaxWidth().testTag("search_tag_autocomplete_input")
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            GenrePreset.entries.forEach { preset ->
                val tag = preset.toTag()
                val selected = selectedTags.any { it.id == tag.id }
                FilterChip(
                    selected = selected,
                    onClick = { onToggleTag(tag) },
                    label = { Text(tag.name) },
                    colors = vndbFilterChipColors(),
                    border = vndbFilterChipBorder(selected = selected)
                )
            }
            suggestions.forEach { suggestion ->
                val selected = selectedTags.any { it.id == suggestion.id }
                FilterChip(
                    selected = selected,
                    onClick = { onToggleTag(suggestion) },
                    label = { Text(suggestion.name) },
                    colors = vndbFilterChipColors(),
                    border = vndbFilterChipBorder(selected = selected)
                )
            }
        }
        if (selectedTags.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                selectedTags.forEach { tag ->
                    InputChip(
                        selected = true,
                        onClick = { onToggleTag(tag) },
                        label = { Text(tag.name) },
                        trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = vndbInputChipColors()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TraitFilterSection(
    traitInput: String,
    onTraitInputChange: (String) -> Unit,
    selectedTraits: List<Trait>,
    suggestions: List<Trait>,
    onToggleTrait: (Trait) -> Unit
) {
    val themeSpec = LocalThemeSpec.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.search_traits_title),
            style = MaterialTheme.typography.titleMedium,
            color = themeSpec.accentColor
        )
        OutlinedTextField(
            value = traitInput,
            onValueChange = onTraitInputChange,
            label = { Text(stringResource(R.string.search_traits_hint)) },
            singleLine = true,
            colors = vndbOutlinedTextFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        if (suggestions.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                suggestions.forEach { trait ->
                    val selected = selectedTraits.any { it.id == trait.id }
                    val label = trait.groupName?.takeIf { it.isNotBlank() }?.let { "$it: ${trait.name}" } ?: trait.name
                    FilterChip(
                        selected = selected,
                        onClick = { onToggleTrait(trait) },
                        label = { Text(label) },
                        colors = vndbFilterChipColors(),
                        border = vndbFilterChipBorder(selected = selected)
                    )
                }
            }
        }
        if (selectedTraits.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                selectedTraits.forEach { trait ->
                    InputChip(
                        selected = true,
                        onClick = { onToggleTrait(trait) },
                        label = { Text(trait.name) },
                        trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = vndbInputChipColors()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeveloperFilterSection(
    devInput: String,
    onDevInputChange: (String) -> Unit,
    selectedProducers: List<Producer>,
    suggestions: List<Producer>,
    onToggleProducer: (Producer) -> Unit
) {
    val themeSpec = LocalThemeSpec.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.search_developers_title),
            style = MaterialTheme.typography.titleMedium,
            color = themeSpec.accentColor
        )
        OutlinedTextField(
            value = devInput,
            onValueChange = onDevInputChange,
            label = { Text(stringResource(R.string.search_developers_hint)) },
            singleLine = true,
            colors = vndbOutlinedTextFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        if (suggestions.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                suggestions.forEach { producer ->
                    val selected = selectedProducers.any { it.id == producer.id }
                    FilterChip(
                        selected = selected,
                        onClick = { onToggleProducer(producer) },
                        label = { Text(producer.name) },
                        colors = vndbFilterChipColors(),
                        border = vndbFilterChipBorder(selected = selected)
                    )
                }
            }
        }
        if (selectedProducers.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                selectedProducers.forEach { producer ->
                    InputChip(
                        selected = true,
                        onClick = { onToggleProducer(producer) },
                        label = { Text(producer.name) },
                        trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = vndbInputChipColors()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MetadataChipSection(
    filters: com.vndblite.dcj.domain.model.VnSearchFilters,
    onAction: (SearchAction) -> Unit
) {
    val themeSpec = LocalThemeSpec.current

    Text(
        text = stringResource(R.string.search_dev_status_title),
        style = MaterialTheme.typography.titleMedium,
        color = themeSpec.accentColor
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DevStatus.entries.forEach { status ->
            val selected = status in filters.devStatuses
            FilterChip(
                selected = selected,
                onClick = { onAction(SearchAction.ToggleDevStatus(status)) },
                label = { Text(stringResource(status.labelRes())) },
                colors = vndbFilterChipColors(),
                border = vndbFilterChipBorder(selected = selected)
            )
        }
    }

    Text(
        text = stringResource(R.string.search_origin_languages_title),
        style = MaterialTheme.typography.titleMedium,
        color = themeSpec.accentColor
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Language.originLanguages.forEach { lang ->
            val selected = lang in filters.originalLanguages
            FilterChip(
                selected = selected,
                onClick = { onAction(SearchAction.ToggleOriginalLanguage(lang)) },
                leadingIcon = {
                    AssetImage(
                        assetPath = AssetPaths.languageFlagPath(lang),
                        contentDescription = null,
                        modifier = Modifier.width(16.dp).height(11.dp)
                    )
                },
                label = { Text(stringResource(lang.labelRes())) },
                colors = vndbFilterChipColors(),
                border = vndbFilterChipBorder(selected = selected)
            )
        }
    }

    Text(
        text = stringResource(R.string.search_languages_title),
        style = MaterialTheme.typography.titleMedium,
        color = themeSpec.accentColor
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Language.entries.forEach { lang ->
            val selected = lang in filters.languages
            FilterChip(
                selected = selected,
                onClick = { onAction(SearchAction.ToggleLanguage(lang)) },
                leadingIcon = {
                    AssetImage(
                        assetPath = AssetPaths.languageFlagPath(lang),
                        contentDescription = null,
                        modifier = Modifier.width(16.dp).height(11.dp)
                    )
                },
                label = { Text(stringResource(lang.labelRes())) },
                colors = vndbFilterChipColors(),
                border = vndbFilterChipBorder(selected = selected)
            )
        }
    }

    Text(
        text = stringResource(R.string.search_platforms_title),
        style = MaterialTheme.typography.titleMedium,
        color = themeSpec.accentColor
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Platform.entries.filter { it.hasDedicatedAsset }.forEach { platform ->
            val selected = platform in filters.platforms
            FilterChip(
                selected = selected,
                onClick = { onAction(SearchAction.TogglePlatform(platform)) },
                leadingIcon = {
                    AssetImage(
                        assetPath = AssetPaths.platformIconPath(platform),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = { Text(stringResource(platform.labelRes())) },
                colors = vndbFilterChipColors(),
                border = vndbFilterChipBorder(selected = selected)
            )
        }
    }

    Text(
        text = stringResource(R.string.search_char_roles_title),
        style = MaterialTheme.typography.titleMedium,
        color = themeSpec.accentColor
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CharacterRole.entries.forEach { role ->
            val selected = role in filters.characterRoles
            FilterChip(
                selected = selected,
                onClick = { onAction(SearchAction.ToggleCharacterRole(role)) },
                label = { Text(stringResource(role.labelRes())) },
                colors = vndbFilterChipColors(),
                border = vndbFilterChipBorder(selected = selected)
            )
        }
    }

    Text(
        text = stringResource(R.string.search_char_sex_title),
        style = MaterialTheme.typography.titleMedium,
        color = themeSpec.accentColor
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CharacterSex.entries.forEach { sex ->
            val selected = sex in filters.characterSexes
            FilterChip(
                selected = selected,
                onClick = { onAction(SearchAction.ToggleCharacterSex(sex)) },
                label = { Text(stringResource(sex.labelRes())) },
                colors = vndbFilterChipColors(),
                border = vndbFilterChipBorder(selected = selected)
            )
        }
    }
}
