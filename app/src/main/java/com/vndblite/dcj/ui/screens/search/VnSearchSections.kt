package com.vndblite.dcj.ui.screens.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.vndblite.dcj.R
import com.vndblite.dcj.domain.model.QuickGenrePreset
import com.vndblite.dcj.ui.components.AssetImage
import com.vndblite.dcj.ui.format.displayLabel
import com.vndblite.dcj.ui.format.genrePresetImageUri
import com.vndblite.dcj.ui.format.labelResId
import com.vndblite.dcj.ui.theme.VndbThemeTokens
import com.vndblite.dcj.ui.theme.vndbInputChipColors
import com.vndblite.dcj.ui.theme.vndbOutlinedTextFieldColors

@Composable
internal fun VnSearchQueryBar(
    query: String,
    hasActiveFilters: Boolean,
    onQueryChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onOpenFilterSheet: () -> Unit
) {
    val spec = VndbThemeTokens.spec
    val keyboard = LocalSoftwareKeyboardController.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text(stringResource(R.string.search_vn_hint)) },
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = spec.accentColor)
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.action_clear_cd),
                            tint = spec.onSurfaceColor
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    keyboard?.hide()
                    onSearchSubmit()
                }
            ),
            shape = RoundedCornerShape(14.dp),
            colors = vndbOutlinedTextFieldColors(spec),
            modifier = Modifier
                .weight(1f)
                .testTag("search_query_input")
        )

        Surface(
            color = spec.surfaceColor.copy(alpha = 0.85f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(
                width = 1.dp,
                color = if (hasActiveFilters) spec.accentColor else spec.onSurfaceColor.copy(alpha = 0.35f)
            )
        ) {
            IconButton(
                onClick = onOpenFilterSheet,
                modifier = Modifier.testTag("search_filter_button")
            ) {
                BadgedBox(
                    badge = {
                        if (hasActiveFilters) {
                            Badge(containerColor = spec.accentColor)
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = stringResource(R.string.search_filter_cd),
                        tint = spec.accentColor
                    )
                }
            }
        }
    }
}

@Composable
internal fun QuickGenresRow(
    selectedTagIds: Set<String>,
    onToggleGenre: (QuickGenrePreset) -> Unit
) {
    val spec = VndbThemeTokens.spec
    val semantic = VndbThemeTokens.colors

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(QuickGenrePreset.entries, key = { it.tagId }) { genre ->
            val selected = genre.tagId in selectedTagIds
            Box(
                modifier = Modifier
                    .width(104.dp)
                    .height(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .selectable(
                        selected = selected,
                        role = Role.Checkbox,
                        onClick = { onToggleGenre(genre) }
                    )
                    .testTag("quick_genre_${genre.tagId}")
            ) {
                AssetImage(
                    assetPath = genre.genrePresetImageUri(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (selected) spec.accentColor.copy(alpha = 0.65f)
                            else semantic.scrimHeavy
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(genre.labelResId()),
                        style = MaterialTheme.typography.labelLarge,
                        color = semantic.cardOverlayText,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
internal fun ActiveFilterChipsRow(
    chips: List<ActiveFilterChip>,
    onRemoveChip: (ActiveFilterChip) -> Unit,
    onClearAll: () -> Unit
) {
    val spec = VndbThemeTokens.spec
    val semantic = VndbThemeTokens.colors

    LazyRow(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(chips, key = { it.key }) { chip ->
            val label = when (chip) {
                is ActiveFilterChip.TagChip -> chip.tag.name
                is ActiveFilterChip.TraitChip -> chip.trait.displayLabel()
                is ActiveFilterChip.DeveloperChip -> chip.developer.name
                is ActiveFilterChip.LanguageChip -> stringResource(chip.language.labelResId())
                is ActiveFilterChip.OriginLanguageChip -> stringResource(chip.language.labelResId())
                is ActiveFilterChip.PlatformChip -> stringResource(chip.platform.labelResId())
                is ActiveFilterChip.DevStatusChip -> stringResource(chip.devStatus.labelResId())
                is ActiveFilterChip.CharacterRoleChip -> stringResource(chip.role.labelResId())
                is ActiveFilterChip.CharacterSexChip -> stringResource(chip.sex.labelResId())
                ActiveFilterChip.ReleaseWindowChip -> stringResource(R.string.search_sheet_release_window_heading)
            }
            InputChip(
                selected = true,
                onClick = { onRemoveChip(chip) },
                label = { Text(label) },
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
        item(key = "clear_all_active_filters") {
            TextButton(onClick = onClearAll) {
                Text(stringResource(R.string.search_clear_filters), color = semantic.danger)
            }
        }
    }
}

@Composable
internal fun VnLoadMoreFooter(
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
