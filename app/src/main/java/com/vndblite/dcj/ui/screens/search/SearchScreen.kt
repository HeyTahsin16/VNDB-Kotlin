package com.vndblite.dcj.ui.screens.search

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.vndblite.dcj.R
import com.vndblite.dcj.ui.components.EmptyStateView
import com.vndblite.dcj.ui.components.ErrorStateView
import com.vndblite.dcj.ui.components.LoadingStateView
import com.vndblite.dcj.ui.components.VnGridCard
import com.vndblite.dcj.ui.theme.LocalThemeSpec
import com.vndblite.dcj.ui.theme.vndbFilterChipBorder
import com.vndblite.dcj.ui.theme.vndbFilterChipColors
import com.vndblite.dcj.ui.theme.vndbOutlinedTextFieldColors

@Composable
fun SearchScreen(
    uiState: SearchUiState,
    onAction: (SearchAction) -> Unit,
    onOpenVnDetail: (String) -> Unit,
    onSwitchToCharacterExplorer: () -> Unit,
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

    var showFilterSheet by rememberSaveable { mutableStateOf(false) }
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
            onAction(SearchAction.LoadNextPage)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        SearchTopControls(
            query = uiState.filters.searchQuery,
            hasActiveFilters = uiState.filters.hasActiveFilters,
            onQueryChange = { onAction(SearchAction.UpdateQuery(it)) },
            onSubmitSearch = {
                focusManager.clearFocus()
                onAction(SearchAction.SubmitSearch)
            },
            onOpenFilters = { showFilterSheet = true },
            onSwitchToCharacterExplorer = onSwitchToCharacterExplorer
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
                        onRetry = { onAction(SearchAction.Retry) }
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
                        .testTag("search_results_grid")
                ) {
                    items(uiState.results, key = { it.id }) { vn ->
                        VnGridCard(
                            vn = vn,
                            sortField = uiState.sort.field,
                            showCoverCensorGlobal = uiState.settings.showCoverCensor,
                            isNsfwRevealed = vn.id in uiState.revealedNsfwIds,
                            onToggleNsfwReveal = { onAction(SearchAction.ToggleNsfwReveal(vn.id)) },
                            collectionEntry = uiState.collectionByVnId[vn.id],
                            onClick = { onOpenVnDetail(vn.id) }
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
                                    onClick = { onAction(SearchAction.LoadNextPage) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = themeSpec.accentColor,
                                        contentColor = themeSpec.onAccentColor
                                    ),
                                    modifier = Modifier.testTag("search_load_more_button")
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

    if (showFilterSheet) {
        SearchFilterSheet(
            uiState = uiState,
            onAction = onAction,
            onDismiss = { showFilterSheet = false }
        )
    }
}

@Composable
private fun SearchTopControls(
    query: String,
    hasActiveFilters: Boolean,
    onQueryChange: (String) -> Unit,
    onSubmitSearch: () -> Unit,
    onOpenFilters: () -> Unit,
    onSwitchToCharacterExplorer: () -> Unit
) {
    val themeSpec = LocalThemeSpec.current
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
                selected = true,
                onClick = {},
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = { Text(stringResource(R.string.search_mode_vns)) },
                colors = vndbFilterChipColors(),
                border = vndbFilterChipBorder(selected = true),
                modifier = Modifier.testTag("search_mode_vn_chip")
            )
            FilterChip(
                selected = false,
                onClick = onSwitchToCharacterExplorer,
                leadingIcon = { Icon(Icons.Default.PersonSearch, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = { Text(stringResource(R.string.search_mode_characters)) },
                colors = vndbFilterChipColors(),
                border = vndbFilterChipBorder(selected = false),
                modifier = Modifier.testTag("search_mode_characters_chip")
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text(stringResource(R.string.search_vn_hint)) },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = { onQueryChange("") },
                            modifier = Modifier.testTag("search_clear_query_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = stringResource(R.string.search_clear_query_cd)
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSubmitSearch() }),
                colors = vndbOutlinedTextFieldColors(),
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_query_input")
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onOpenFilters,
                modifier = Modifier.testTag("search_filter_button")
            ) {
                BadgedBox(
                    badge = {
                        if (hasActiveFilters) {
                            Badge(containerColor = themeSpec.accentColor)
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = stringResource(R.string.search_filter_cd),
                        tint = themeSpec.accentColor
                    )
                }
            }
        }
    }
}
