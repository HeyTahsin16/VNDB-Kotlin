package com.vndblite.dcj.ui.screens.characters

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vndblite.dcj.R
import com.vndblite.dcj.domain.model.Character
import com.vndblite.dcj.domain.model.CharacterRole
import com.vndblite.dcj.domain.model.CharacterSex
import com.vndblite.dcj.domain.model.PopularTraitPreset
import com.vndblite.dcj.ui.components.EmptyStateView
import com.vndblite.dcj.ui.components.ErrorStateView
import com.vndblite.dcj.ui.components.LoadingStateView
import com.vndblite.dcj.ui.components.NsfwAsyncImage
import com.vndblite.dcj.ui.format.labelRes
import com.vndblite.dcj.ui.theme.ColorTokens
import com.vndblite.dcj.ui.theme.LocalThemeSpec
import com.vndblite.dcj.ui.theme.vndbFilterChipBorder
import com.vndblite.dcj.ui.theme.vndbFilterChipColors
import com.vndblite.dcj.ui.theme.vndbInputChipColors
import com.vndblite.dcj.ui.theme.vndbOutlinedTextFieldColors

@Composable
fun CharacterSearchScreen(
    uiState: CharacterSearchUiState,
    onAction: (CharacterSearchAction) -> Unit,
    onOpenVnDetail: (String) -> Unit,
    onSwitchToVnSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeSpec = LocalThemeSpec.current
    val focusManager = LocalFocusManager.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val columns = if (isLandscape) {
        uiState.settings.maxItemPerRowLandscape.coerceIn(2, 10)
    } else {
        uiState.settings.maxItemPerRowPortrait.coerceIn(1, 6)
    }

    val gridState = rememberLazyGridState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val total = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && lastVisible >= total - 4
        }
    }

    LaunchedEffect(shouldLoadMore, uiState.hasMore, uiState.isLoading, uiState.isAppending) {
        if (shouldLoadMore && uiState.hasMore && !uiState.isLoading && !uiState.isAppending) {
            onAction(CharacterSearchAction.LoadNextPage)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        CharacterSearchHeader(
            uiState = uiState,
            onAction = onAction,
            onSubmit = {
                focusManager.clearFocus()
                onAction(CharacterSearchAction.SubmitSearch)
            },
            onSwitchToVnSearch = onSwitchToVnSearch
        )

        when {
            uiState.isLoading && uiState.results.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingStateView()
                }
            }
            uiState.error != null && uiState.results.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ErrorStateView(
                        error = uiState.error,
                        onRetry = { onAction(CharacterSearchAction.Retry) }
                    )
                }
            }
            uiState.results.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyStateView(message = stringResource(R.string.no_results))
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    state = gridState,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("character_results_grid")
                ) {
                    items(uiState.results, key = { it.id }) { character ->
                        CharacterGridCard(
                            character = character,
                            showCoverCensor = uiState.settings.showCoverCensor,
                            isNsfwRevealed = character.id in uiState.revealedNsfwIds,
                            onClick = { onAction(CharacterSearchAction.SelectCharacter(character)) }
                        )
                    }

                    if (uiState.isAppending) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            LoadingStateView()
                        }
                    } else if (uiState.hasMore) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Button(
                                    onClick = { onAction(CharacterSearchAction.LoadNextPage) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = themeSpec.accentColor,
                                        contentColor = themeSpec.onAccentColor
                                    )
                                ) {
                                    Text(
                                        text = stringResource(R.string.action_load_more),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    uiState.selectedCharacter?.let { selected ->
        CharacterBioDialog(
            character = selected,
            showCoverCensor = uiState.settings.showCoverCensor,
            isNsfwRevealed = selected.id in uiState.revealedNsfwIds,
            onToggleNsfwReveal = { onAction(CharacterSearchAction.ToggleNsfwReveal(selected.id)) },
            onSelectTrait = { trait -> onAction(CharacterSearchAction.ToggleTrait(trait)) },
            onOpenVnDetail = onOpenVnDetail,
            onDismiss = { onAction(CharacterSearchAction.DismissCharacterBio) }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CharacterSearchHeader(
    uiState: CharacterSearchUiState,
    onAction: (CharacterSearchAction) -> Unit,
    onSubmit: () -> Unit,
    onSwitchToVnSearch: () -> Unit
) {
    var traitQuery by rememberSaveable { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = false,
                onClick = onSwitchToVnSearch,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = { Text(stringResource(R.string.search_mode_vns)) },
                colors = vndbFilterChipColors(),
                border = vndbFilterChipBorder(selected = false),
                modifier = Modifier.testTag("char_search_mode_vn_chip")
            )
            FilterChip(
                selected = true,
                onClick = {},
                leadingIcon = { Icon(Icons.Default.PersonSearch, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = { Text(stringResource(R.string.search_mode_characters)) },
                colors = vndbFilterChipColors(),
                border = vndbFilterChipBorder(selected = true),
                modifier = Modifier.testTag("char_search_mode_characters_chip")
            )
        }

        OutlinedTextField(
            value = uiState.filters.searchQuery,
            onValueChange = { onAction(CharacterSearchAction.UpdateQuery(it)) },
            placeholder = { Text(stringResource(R.string.search_character_hint)) },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (uiState.filters.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onAction(CharacterSearchAction.UpdateQuery("")) }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = stringResource(R.string.search_clear_query_cd)
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
            colors = vndbOutlinedTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("character_search_query_input")
        )

        OutlinedTextField(
            value = traitQuery,
            onValueChange = {
                traitQuery = it
                onAction(CharacterSearchAction.QueryTraitSuggestions(it))
            },
            placeholder = { Text(stringResource(R.string.search_traits_hint)) },
            singleLine = true,
            colors = vndbOutlinedTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("character_trait_autocomplete_input")
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PopularTraitPreset.entries.take(6).forEach { preset ->
                val trait = preset.toTrait()
                val selected = uiState.filters.traits.any { it.id == trait.id }
                FilterChip(
                    selected = selected,
                    onClick = { onAction(CharacterSearchAction.ToggleTrait(trait)) },
                    label = { Text(trait.name) },
                    colors = vndbFilterChipColors(),
                    border = vndbFilterChipBorder(selected = selected)
                )
            }
            uiState.traitSuggestions.take(8).forEach { suggestion ->
                val selected = uiState.filters.traits.any { it.id == suggestion.id }
                FilterChip(
                    selected = selected,
                    onClick = { onAction(CharacterSearchAction.ToggleTrait(suggestion)) },
                    label = { Text(suggestion.name) },
                    colors = vndbFilterChipColors(),
                    border = vndbFilterChipBorder(selected = selected)
                )
            }
            CharacterRole.entries.forEach { role ->
                val selected = role in uiState.filters.roles
                FilterChip(
                    selected = selected,
                    onClick = { onAction(CharacterSearchAction.ToggleRole(role)) },
                    label = { Text(stringResource(role.labelRes())) },
                    colors = vndbFilterChipColors(),
                    border = vndbFilterChipBorder(selected = selected)
                )
            }
            CharacterSex.entries.forEach { sex ->
                val selected = sex in uiState.filters.sexes
                FilterChip(
                    selected = selected,
                    onClick = { onAction(CharacterSearchAction.ToggleSex(sex)) },
                    label = { Text(stringResource(sex.labelRes())) },
                    colors = vndbFilterChipColors(),
                    border = vndbFilterChipBorder(selected = selected)
                )
            }
        }

        if (uiState.filters.traits.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                uiState.filters.traits.forEach { trait ->
                    InputChip(
                        selected = true,
                        onClick = { onAction(CharacterSearchAction.ToggleTrait(trait)) },
                        label = { Text(trait.name) },
                        trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = vndbInputChipColors()
                    )
                }
            }
        }
    }
}

@Composable
fun CharacterGridCard(
    character: Character,
    showCoverCensor: Boolean,
    isNsfwRevealed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeSpec = LocalThemeSpec.current
    val isExplicit = character.image?.isExplicit == true
    val shouldBlur = isExplicit && showCoverCensor && !isNsfwRevealed

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.72f)
            .clip(RoundedCornerShape(14.dp))
            .semantics(mergeDescendants = true) { role = Role.Button }
            .clickable(onClick = onClick)
            .testTag("character_card_${character.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = themeSpec.surfaceColor.copy(alpha = 0.85f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val imageUrl = character.image?.previewUrl.orEmpty()
            if (imageUrl.isNotBlank()) {
                NsfwAsyncImage(
                    imageUrl = imageUrl,
                    contentDescription = character.name,
                    shouldBlur = shouldBlur,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(themeSpec.surfaceColor.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = themeSpec.contentColor.copy(alpha = 0.5f),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                ColorTokens.Transparent,
                                ColorTokens.ScrimDarkLight,
                                ColorTokens.ScrimDarkHeavy
                            )
                        )
                    )
                    .padding(horizontal = 9.dp, vertical = 8.dp)
            ) {
                Text(
                    text = character.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ColorTokens.CardOverlayText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val firstVn = character.vns.firstOrNull()?.vnTitle
                if (!firstVn.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = firstVn,
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorTokens.CardOverlaySubtext,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
