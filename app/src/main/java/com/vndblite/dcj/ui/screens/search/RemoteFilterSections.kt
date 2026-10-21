package com.vndblite.dcj.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.vndblite.dcj.domain.model.DevStatus
import com.vndblite.dcj.domain.model.Language
import com.vndblite.dcj.domain.model.Platform
import com.vndblite.dcj.domain.model.VnSearchFilters
import com.vndblite.dcj.ui.components.AssetImage
import com.vndblite.dcj.ui.format.flagAssetUri
import com.vndblite.dcj.ui.format.iconAssetUri
import com.vndblite.dcj.ui.format.labelResId
import com.vndblite.dcj.ui.screens.characters.AndOrToggleButton
import com.vndblite.dcj.ui.theme.VndbThemeTokens
import com.vndblite.dcj.ui.theme.vndbFilterChipBorder
import com.vndblite.dcj.ui.theme.vndbFilterChipColors
import com.vndblite.dcj.ui.theme.vndbInputChipColors
import com.vndblite.dcj.ui.theme.vndbOutlinedTextFieldColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun <T> RemoteEntityAutocompleteSection(
    heading: String,
    hint: String,
    andOr: AndOr?,
    onToggleAndOr: () -> Unit,
    query: String,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    suggestions: List<T>,
    suggestionKey: (T) -> Any,
    suggestionLabel: (T) -> String,
    onAddSuggestion: (T) -> Unit,
    selectedItems: List<T>,
    selectedLabel: (T) -> String,
    onRemoveSelected: (T) -> Unit,
    inputTestTag: String? = null
) {
    val spec = VndbThemeTokens.spec
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = heading,
            style = MaterialTheme.typography.titleMedium,
            color = spec.accentColor,
            fontWeight = FontWeight.Bold
        )
        if (andOr != null) {
            AndOrToggleButton(
                mode = andOr,
                testTag = "${heading}_andor",
                onClick = onToggleAndOr
            )
        }
    }

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(hint) },
        singleLine = true,
        trailingIcon = {
            if (isSearching) {
                CircularProgressIndicator(
                    color = spec.accentColor,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp)
                )
            } else if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
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
            .then(if (inputTestTag != null) Modifier.testTag(inputTestTag) else Modifier)
    )

    if (suggestions.isNotEmpty()) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(suggestions, key = suggestionKey) { item ->
                AssistChip(
                    onClick = { onAddSuggestion(item) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = spec.accentColor,
                            modifier = Modifier.size(15.dp)
                        )
                    },
                    label = { Text(suggestionLabel(item)) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = spec.accentColor.copy(alpha = 0.16f),
                        labelColor = spec.onSurfaceColor
                    )
                )
            }
        }
    }

    if (selectedItems.isNotEmpty()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            selectedItems.forEach { item ->
                InputChip(
                    selected = true,
                    onClick = { onRemoveSelected(item) },
                    label = { Text(selectedLabel(item)) },
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RemoteMetadataSelectorsSection(
    draftFilters: VnSearchFilters,
    onUpdateFilters: (VnSearchFilters) -> Unit
) {
    val spec = VndbThemeTokens.spec

    Text(
        text = stringResource(R.string.search_sheet_dev_status_heading),
        style = MaterialTheme.typography.titleMedium,
        color = spec.accentColor,
        fontWeight = FontWeight.Bold
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        DevStatus.entries.forEach { ds ->
            val selected = ds in draftFilters.devStatuses
            FilterChip(
                selected = selected,
                onClick = {
                    val next = if (selected) draftFilters.devStatuses - ds else draftFilters.devStatuses + ds
                    onUpdateFilters(draftFilters.copy(devStatuses = next))
                },
                label = { Text(stringResource(ds.labelResId())) },
                colors = vndbFilterChipColors(spec),
                border = vndbFilterChipBorder(selected, spec)
            )
        }
    }

    Text(
        text = stringResource(R.string.search_sheet_origin_lang_heading),
        style = MaterialTheme.typography.titleMedium,
        color = spec.accentColor,
        fontWeight = FontWeight.Bold
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Language.originLanguages.forEach { lang ->
            val selected = lang in draftFilters.originalLanguages
            FilterChip(
                selected = selected,
                onClick = {
                    val next = if (selected) {
                        draftFilters.originalLanguages - lang
                    } else {
                        draftFilters.originalLanguages + lang
                    }
                    onUpdateFilters(draftFilters.copy(originalLanguages = next))
                },
                leadingIcon = {
                    AssetImage(
                        assetPath = lang.flagAssetUri(),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = { Text(stringResource(lang.labelResId())) },
                colors = vndbFilterChipColors(spec),
                border = vndbFilterChipBorder(selected, spec)
            )
        }
    }

    Text(
        text = stringResource(R.string.search_sheet_languages_heading),
        style = MaterialTheme.typography.titleMedium,
        color = spec.accentColor,
        fontWeight = FontWeight.Bold
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Language.entries.forEach { lang ->
            val selected = lang in draftFilters.languages
            FilterChip(
                selected = selected,
                onClick = {
                    val next = if (selected) draftFilters.languages - lang else draftFilters.languages + lang
                    onUpdateFilters(draftFilters.copy(languages = next))
                },
                leadingIcon = {
                    AssetImage(
                        assetPath = lang.flagAssetUri(),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = { Text(stringResource(lang.labelResId())) },
                colors = vndbFilterChipColors(spec),
                border = vndbFilterChipBorder(selected, spec)
            )
        }
    }

    Text(
        text = stringResource(R.string.search_sheet_platforms_heading),
        style = MaterialTheme.typography.titleMedium,
        color = spec.accentColor,
        fontWeight = FontWeight.Bold
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Platform.entries.forEach { platform ->
            val selected = platform in draftFilters.platforms
            FilterChip(
                selected = selected,
                onClick = {
                    val next = if (selected) draftFilters.platforms - platform else draftFilters.platforms + platform
                    onUpdateFilters(draftFilters.copy(platforms = next))
                },
                leadingIcon = {
                    AssetImage(
                        assetPath = platform.iconAssetUri(),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = { Text(stringResource(platform.labelResId())) },
                colors = vndbFilterChipColors(spec),
                border = vndbFilterChipBorder(selected, spec)
            )
        }
    }

    Text(
        text = stringResource(R.string.search_sheet_char_roles_heading),
        style = MaterialTheme.typography.titleMedium,
        color = spec.accentColor,
        fontWeight = FontWeight.Bold
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        CharacterRole.entries.forEach { role ->
            val selected = role in draftFilters.characterRoles
            FilterChip(
                selected = selected,
                onClick = {
                    val next = if (selected) {
                        draftFilters.characterRoles - role
                    } else {
                        draftFilters.characterRoles + role
                    }
                    onUpdateFilters(draftFilters.copy(characterRoles = next))
                },
                label = { Text(stringResource(role.labelResId())) },
                colors = vndbFilterChipColors(spec),
                border = vndbFilterChipBorder(selected, spec)
            )
        }
    }

    Text(
        text = stringResource(R.string.search_sheet_char_sex_heading),
        style = MaterialTheme.typography.titleMedium,
        color = spec.accentColor,
        fontWeight = FontWeight.Bold
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        CharacterSex.entries.forEach { sex ->
            val selected = sex in draftFilters.characterSexes
            FilterChip(
                selected = selected,
                onClick = {
                    val next = if (selected) {
                        draftFilters.characterSexes - sex
                    } else {
                        draftFilters.characterSexes + sex
                    }
                    onUpdateFilters(draftFilters.copy(characterSexes = next))
                },
                label = { Text(stringResource(sex.labelResId())) },
                colors = vndbFilterChipColors(spec),
                border = vndbFilterChipBorder(selected, spec)
            )
        }
    }
}
