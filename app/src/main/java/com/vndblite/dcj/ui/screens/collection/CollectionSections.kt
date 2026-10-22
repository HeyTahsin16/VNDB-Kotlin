package com.vndblite.dcj.ui.screens.collection

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vndblite.dcj.R
import com.vndblite.dcj.domain.model.CollectionItemWithVn
import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.domain.model.VnSummary
import com.vndblite.dcj.ui.components.AssetImage
import com.vndblite.dcj.ui.components.VnGridCard
import com.vndblite.dcj.ui.components.VnGridCardDisplayState
import com.vndblite.dcj.ui.format.displayLabel
import com.vndblite.dcj.ui.format.iconAssetUri
import com.vndblite.dcj.ui.theme.VndbThemeTokens
import com.vndblite.dcj.ui.theme.vndbFilterChipBorder
import com.vndblite.dcj.ui.theme.vndbFilterChipColors
import com.vndblite.dcj.ui.theme.vndbOutlinedTextFieldColors

@Composable
internal fun CollectionHeaderBar(
    state: CollectionUiState,
    onToggleSearch: () -> Unit,
    onOpenFilterSheet: () -> Unit,
    onSyncOrOpenAuth: () -> Unit
) {
    val spec = VndbThemeTokens.spec
    val semantic = VndbThemeTokens.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.collection_title),
                style = MaterialTheme.typography.displayMedium,
                color = spec.accentColor,
                fontWeight = FontWeight.Bold
            )
            val account = state.activeAccount
            Text(
                text = if (account != null && state.isAccountSynced) {
                    stringResource(R.string.collection_subtitle_synced_user, state.totalCount, account.username)
                } else {
                    stringResource(R.string.collection_subtitle_entries_only, state.totalCount)
                },
                style = MaterialTheme.typography.bodySmall,
                color = spec.onSurfaceColor.copy(alpha = 0.8f)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(
                onClick = onToggleSearch,
                modifier = Modifier.testTag("collection_search_toggle")
            ) {
                Icon(
                    imageVector = if (state.isSearchActive) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = stringResource(R.string.collection_search_cd),
                    tint = spec.accentColor
                )
            }

            IconButton(
                onClick = onOpenFilterSheet,
                modifier = Modifier.testTag("collection_filter_button")
            ) {
                BadgedBox(
                    badge = {
                        if (state.filters.hasActiveMetadataFilters) {
                            Badge(containerColor = spec.accentColor)
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = stringResource(R.string.collection_filter_cd),
                        tint = spec.accentColor
                    )
                }
            }

            IconButton(
                onClick = onSyncOrOpenAuth,
                modifier = Modifier.testTag("collection_sync_button")
            ) {
                if (state.isSyncRunning) {
                    CircularProgressIndicator(
                        color = spec.accentColor,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        imageVector = if (state.isAccountSynced) Icons.Default.CloudDone else Icons.Default.CloudSync,
                        contentDescription = stringResource(R.string.collection_sync_cd),
                        tint = if (state.isAccountSynced) semantic.statusFinished else spec.accentColor
                    )
                }
            }
        }
    }
}

@Composable
internal fun CollectionSelectionBar(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onSelectAllInTab: () -> Unit,
    onOpenBatchEdit: () -> Unit,
    onRequestBatchDelete: () -> Unit
) {
    val spec = VndbThemeTokens.spec
    val semantic = VndbThemeTokens.colors
    Surface(
        color = spec.accentColor.copy(alpha = 0.22f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, spec.accentColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClearSelection) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.collection_clear_selection),
                        tint = spec.onSurfaceColor
                    )
                }
                Text(
                    text = stringResource(R.string.collection_selected_count, selectedCount),
                    style = MaterialTheme.typography.titleMedium,
                    color = spec.accentColor,
                    fontWeight = FontWeight.Bold
                )
            }
            Row {
                IconButton(onClick = onSelectAllInTab) {
                    Icon(
                        imageVector = Icons.Default.SelectAll,
                        contentDescription = stringResource(R.string.collection_select_all_cd),
                        tint = spec.accentColor
                    )
                }
                IconButton(onClick = onOpenBatchEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.collection_batch_edit_cd),
                        tint = spec.accentColor
                    )
                }
                IconButton(onClick = onRequestBatchDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.collection_batch_delete_cd),
                        tint = semantic.danger
                    )
                }
            }
        }
    }
}

@Composable
internal fun CollectionSearchField(
    query: String,
    onQueryChange: (String) -> Unit
) {
    val spec = VndbThemeTokens.spec
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(R.string.collection_search_hint)) },
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
        shape = RoundedCornerShape(14.dp),
        colors = vndbOutlinedTextFieldColors(spec),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .testTag("collection_search_input")
    )
}

@Composable
internal fun CollectionStatusTabsRow(
    orderedStatuses: List<CollectionStatus>,
    itemsByStatus: Map<CollectionStatus, List<CollectionItemWithVn>>,
    selectedTabIndex: Int,
    onSelectTab: (Int) -> Unit
) {
    val spec = VndbThemeTokens.spec
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(orderedStatuses, key = { _, st -> st.storageKey }) { index, status ->
            val count = itemsByStatus[status]?.size ?: 0
            val selected = index == selectedTabIndex
            FilterChip(
                selected = selected,
                onClick = { onSelectTab(index) },
                leadingIcon = {
                    AssetImage(
                        assetPath = status.iconAssetUri(),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = {
                    Text(
                        text = stringResource(
                            R.string.collection_tab_count_format,
                            status.displayLabel(),
                            count
                        ),
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = vndbFilterChipColors(spec),
                border = vndbFilterChipBorder(selected, spec),
                modifier = Modifier.testTag("collection_status_tab_${status.storageKey}")
            )
        }
    }
}

@Composable
internal fun CollectionEmptyView(hasTotalEntries: Boolean) {
    val spec = VndbThemeTokens.spec
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = null,
                tint = spec.onSurfaceColor.copy(alpha = 0.45f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(
                    if (hasTotalEntries) R.string.collection_empty_filtered_title
                    else R.string.collection_empty_title
                ),
                style = MaterialTheme.typography.titleMedium,
                color = spec.onSurfaceColor.copy(alpha = 0.85f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(
                    if (hasTotalEntries) R.string.collection_empty_filtered_subtitle
                    else R.string.collection_empty_subtitle
                ),
                style = MaterialTheme.typography.bodySmall,
                color = spec.onSurfaceColor.copy(alpha = 0.65f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}

@Composable
internal fun CollectionGrid(
    items: List<CollectionItemWithVn>,
    columns: Int,
    state: CollectionUiState,
    isSelectionMode: Boolean,
    onAction: (CollectionAction) -> Unit,
    onEditEntry: (VnSummary) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(items, key = { it.vn.id }) { item ->
            val vn = item.vn
            val isSelected = vn.id in state.selectedVnIds
            VnGridCard(
                vn = vn,
                onClick = {
                    if (isSelectionMode) {
                        onAction(CollectionAction.ToggleSelectVn(vn.id))
                    } else {
                        onAction(CollectionAction.OpenVnDetail(vn))
                    }
                },
                onLongClick = { onAction(CollectionAction.ToggleSelectVn(vn.id)) },
                onStatusBadgeClick = { onEditEntry(vn) },
                displayState = VnGridCardDisplayState(
                    subtitleSortField = state.sort.field,
                    collectionEntry = item.entry,
                    isSelected = isSelected,
                    blurExplicitCover = state.settings.showCoverCensor,
                    isNsfwRevealed = vn.id in state.nsfwRevealedIds
                ),
                onToggleNsfwReveal = { onAction(CollectionAction.ToggleNsfwReveal(vn.id)) }
            )
        }
    }
}
