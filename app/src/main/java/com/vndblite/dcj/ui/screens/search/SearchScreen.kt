package com.vndblite.dcj.ui.screens.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vndblite.dcj.LocalAppContainer
import com.vndblite.dcj.R
import com.vndblite.dcj.domain.model.ThemeId
import com.vndblite.dcj.domain.model.VnSummary
import com.vndblite.dcj.ui.components.EmptyStateView
import com.vndblite.dcj.ui.components.ErrorStateView
import com.vndblite.dcj.ui.components.LoadingStateView
import com.vndblite.dcj.ui.components.PreviewData
import com.vndblite.dcj.ui.components.VnGridCard
import com.vndblite.dcj.ui.components.VnGridCardDisplayState
import com.vndblite.dcj.ui.format.isWideLayout
import com.vndblite.dcj.ui.navigation.SearchNavCommand
import com.vndblite.dcj.ui.screens.characters.CharacterExplorerContent
import com.vndblite.dcj.ui.screens.characters.CharacterSearchAction
import com.vndblite.dcj.ui.screens.characters.CharacterSearchUiState
import com.vndblite.dcj.ui.screens.characters.CharacterSearchViewModel
import com.vndblite.dcj.ui.theme.VndbTheme
import com.vndblite.dcj.ui.theme.VndbThemeTokens

@Composable
fun SearchScreen(
    onOpenVnDetail: (VnSummary) -> Unit,
    searchViewModel: SearchViewModel = viewModel(
        factory = SearchViewModel.factory(LocalAppContainer.current)
    ),
    characterViewModel: CharacterSearchViewModel = viewModel(
        factory = CharacterSearchViewModel.factory(LocalAppContainer.current)
    )
) {
    val container = LocalAppContainer.current
    val pendingCommand by container.searchCommandBus.pendingCommand.collectAsStateWithLifecycle()

    LaunchedEffect(pendingCommand) {
        when (val cmd = container.searchCommandBus.consume()) {
            is SearchNavCommand.FilterVnsByTag ->
                searchViewModel.onAction(SearchAction.ApplySingleTagFilter(cmd.tag))
            is SearchNavCommand.FilterVnsByTrait ->
                searchViewModel.onAction(SearchAction.ApplySingleTraitFilter(cmd.trait))
            is SearchNavCommand.ApplyHomeSection ->
                searchViewModel.onAction(SearchAction.ApplyHomeSectionPreset(cmd.section))
            is SearchNavCommand.OpenCharacterExplorer -> {
                searchViewModel.onAction(SearchAction.SelectMode(SearchMode.CHARACTERS))
                characterViewModel.applyPresetTrait(cmd.presetTrait)
            }
            null -> Unit
        }
    }

    val vnState by searchViewModel.uiState.collectAsStateWithLifecycle()
    val charState by characterViewModel.uiState.collectAsStateWithLifecycle()
    val wide = isWideLayout()

    SearchContent(
        state = vnState,
        characterState = charState,
        isWide = wide,
        onAction = { action ->
            when (action) {
                is SearchAction.OpenVnDetail -> onOpenVnDetail(action.vn)
                else -> searchViewModel.onAction(action)
            }
        },
        onCharacterAction = { charAction ->
            when (charAction) {
                is CharacterSearchAction.OpenVnDetail -> onOpenVnDetail(charAction.vn)
                is CharacterSearchAction.FilterVnsByTrait ->
                    searchViewModel.onAction(SearchAction.ApplySingleTraitFilter(charAction.trait))
                else -> characterViewModel.onAction(charAction)
            }
        }
    )
}

@Composable
fun SearchContent(
    state: SearchUiState,
    characterState: CharacterSearchUiState,
    isWide: Boolean,
    onAction: (SearchAction) -> Unit,
    onCharacterAction: (CharacterSearchAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var showFilterSheet by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        SearchModeTabSwitcher(
            selectedMode = state.mode,
            onSelectMode = { onAction(SearchAction.SelectMode(it)) },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        when (state.mode) {
            SearchMode.VISUAL_NOVELS -> {
                VnSearchModeBody(
                    state = state,
                    isWide = isWide,
                    onOpenFilterSheet = { showFilterSheet = true },
                    onAction = onAction
                )
            }
            SearchMode.CHARACTERS -> {
                CharacterExplorerContent(
                    state = characterState,
                    onAction = onCharacterAction
                )
            }
        }
    }

    if (showFilterSheet) {
        RemoteFilterSheet(
            initialFilters = state.filters,
            initialSort = state.sort,
            autocomplete = state.autocomplete,
            onAction = onAction,
            onDismiss = { showFilterSheet = false },
            onApply = { filters, sort ->
                showFilterSheet = false
                onAction(SearchAction.ApplyFiltersAndSort(filters, sort))
            }
        )
    }
}

@Composable
private fun SearchModeTabSwitcher(
    selectedMode: SearchMode,
    onSelectMode: (SearchMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val spec = VndbThemeTokens.spec
    val semantic = VndbThemeTokens.colors
    Surface(
        color = spec.surfaceColor.copy(alpha = 0.85f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SearchModeTabButton(
                label = stringResource(R.string.search_mode_vns),
                icon = Icons.Default.MenuBook,
                selected = selectedMode == SearchMode.VISUAL_NOVELS,
                testTag = "search_mode_vns",
                onClick = { onSelectMode(SearchMode.VISUAL_NOVELS) },
                spec = spec,
                onAccentColor = semantic.onAccentButton,
                modifier = Modifier.weight(1f)
            )
            SearchModeTabButton(
                label = stringResource(R.string.search_mode_characters),
                icon = Icons.Default.PersonSearch,
                selected = selectedMode == SearchMode.CHARACTERS,
                testTag = "search_mode_characters",
                onClick = { onSelectMode(SearchMode.CHARACTERS) },
                spec = spec,
                onAccentColor = semantic.onAccentButton,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SearchModeTabButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
    spec: com.vndblite.dcj.ui.theme.ThemeSpec,
    onAccentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) spec.accentColor else spec.surfaceColor.copy(alpha = 0f),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) onAccentColor else spec.onSurfaceColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) onAccentColor else spec.onSurfaceColor,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun VnSearchModeBody(
    state: SearchUiState,
    isWide: Boolean,
    onOpenFilterSheet: () -> Unit,
    onAction: (SearchAction) -> Unit
) {
    val cols = if (isWide) {
        state.settings.maxItemPerRowLandscape
    } else {
        state.settings.maxItemPerRowPortrait
    }.coerceAtLeast(1)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        VnSearchQueryBar(
            query = state.filters.searchQuery,
            hasActiveFilters = state.filters.hasActiveFilters,
            onQueryChange = { onAction(SearchAction.UpdateQuery(it)) },
            onSearchSubmit = { onAction(SearchAction.ExecuteSearchNow) },
            onOpenFilterSheet = onOpenFilterSheet
        )

        Spacer(modifier = Modifier.height(8.dp))

        QuickGenresRow(
            selectedTagIds = state.filters.tags.map { it.id }.toSet(),
            onToggleGenre = { onAction(SearchAction.ToggleQuickGenre(it)) }
        )

        val chips = state.activeFilterChips
        if (chips.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            ActiveFilterChipsRow(
                chips = chips,
                onRemoveChip = { onAction(SearchAction.RemoveChip(it)) },
                onClearAll = { onAction(SearchAction.ClearAllFilters) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        when {
            state.isLoading -> LoadingStateView()
            state.error != null && state.results.isEmpty() -> {
                ErrorStateView(
                    message = state.error,
                    onRetry = { onAction(SearchAction.Retry) }
                )
            }
            state.results.isEmpty() -> {
                EmptyStateView(title = stringResource(R.string.search_empty_results))
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(cols),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.results, key = { it.id }) { vn ->
                        VnGridCard(
                            vn = vn,
                            onClick = { onAction(SearchAction.OpenVnDetail(vn)) },
                            displayState = VnGridCardDisplayState(
                                subtitleSortField = state.sort.field,
                                collectionEntry = state.collectionEntries[vn.id],
                                blurExplicitCover = state.settings.showCoverCensor,
                                isNsfwRevealed = vn.id in state.nsfwRevealedIds
                            ),
                            onToggleNsfwReveal = {
                                onAction(SearchAction.ToggleNsfwReveal(vn.id))
                            }
                        )
                    }

                    if (state.canLoadMore) {
                        item(
                            key = "vn_load_more",
                            span = { GridItemSpan(maxLineSpan) }
                        ) {
                            VnLoadMoreFooter(
                                isLoadingMore = state.isLoadingMore,
                                onLoadMore = { onAction(SearchAction.LoadNextPage) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchContentPreview() {
    VndbTheme(themeId = ThemeId.AGL, includeBackground = true) {
        SearchContent(
            state = SearchUiState(
                results = listOf(PreviewData.sampleVnSummary)
            ),
            characterState = CharacterSearchUiState(),
            isWide = false,
            onAction = {},
            onCharacterAction = {}
        )
    }
}
