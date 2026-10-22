package com.vndblite.dcj.ui.screens.collection

import android.content.res.Configuration
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vndblite.dcj.R
import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.ui.components.AssetImage
import com.vndblite.dcj.ui.components.AssetPaths
import com.vndblite.dcj.ui.components.CollectionEntryDialog
import com.vndblite.dcj.ui.components.ConfirmDialog
import com.vndblite.dcj.ui.components.EmptyStateView
import com.vndblite.dcj.ui.components.VnGridCard
import com.vndblite.dcj.ui.format.labelRes
import com.vndblite.dcj.ui.format.toUiText
import com.vndblite.dcj.ui.theme.ColorTokens
import com.vndblite.dcj.ui.theme.LocalThemeSpec
import com.vndblite.dcj.ui.theme.vndbFilterChipBorder
import com.vndblite.dcj.ui.theme.vndbFilterChipColors
import com.vndblite.dcj.ui.theme.vndbOutlinedTextFieldColors

@Composable
fun CollectionScreen(
    uiState: CollectionUiState,
    onAction: (CollectionAction) -> Unit,
    onOpenVnDetail: (String) -> Unit,
    onOpenAuthSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val columns = if (isLandscape) {
        uiState.settings.maxItemPerRowLandscape.coerceIn(2, 10)
    } else {
        uiState.settings.maxItemPerRowPortrait.coerceIn(1, 6)
    }

    var showFilterSheet by rememberSaveable { mutableStateOf(false) }
    var showBatchEditDialog by rememberSaveable { mutableStateOf(false) }
    var showBatchDeleteConfirm by rememberSaveable { mutableStateOf(false) }

    val orderedTabs = uiState.settings.collectionStatusTabArrangement.ifEmpty {
        CollectionStatus.entries
    }
    val selectedTabIndex = orderedTabs.indexOf(uiState.selectedTab).coerceAtLeast(0)

    Column(modifier = modifier.fillMaxSize()) {
        if (uiState.isSelectionMode) {
            CollectionSelectionBar(
                selectedCount = uiState.selectedVnIds.size,
                onSelectAll = { onAction(CollectionAction.SelectAllInCurrentTab) },
                onBatchEdit = { showBatchEditDialog = true },
                onBatchDelete = { showBatchDeleteConfirm = true },
                onClearSelection = { onAction(CollectionAction.ClearSelection) }
            )
        } else {
            CollectionTopHeader(
                uiState = uiState,
                onToggleSearch = { onAction(CollectionAction.ToggleSearchVisibility) },
                onOpenFilters = { showFilterSheet = true },
                onSyncClick = {
                    if (uiState.activeAccount != null) {
                        onAction(CollectionAction.TriggerSync)
                    } else {
                        onOpenAuthSync()
                    }
                }
            )
        }

        if (uiState.syncProgress.isSyncing) {
            LinearProgressIndicator(
                color = LocalThemeSpec.current.accentColor,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (uiState.isSearchVisible) {
            OutlinedTextField(
                value = uiState.filters.searchQuery,
                onValueChange = { onAction(CollectionAction.UpdateSearchQuery(it)) },
                placeholder = { Text(stringResource(R.string.collection_search_hint)) },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.filters.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onAction(CollectionAction.UpdateSearchQuery("")) }) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.action_clear))
                        }
                    }
                },
                colors = vndbOutlinedTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .testTag("collection_search_input")
            )
        }

        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            edgePadding = 12.dp,
            containerColor = LocalThemeSpec.current.surfaceColor.copy(alpha = 0.85f),
            contentColor = LocalThemeSpec.current.accentColor
        ) {
            orderedTabs.forEach { status ->
                val count = uiState.statusCounts[status] ?: 0
                val isSelected = status == uiState.selectedTab
                Tab(
                    selected = isSelected,
                    onClick = { onAction(CollectionAction.SelectTab(status)) },
                    modifier = Modifier.testTag("collection_tab_${status.storageKey}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AssetImage(
                            assetPath = AssetPaths.collectionStatusIconPath(status),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${stringResource(status.labelRes())} ($count)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) {
                                LocalThemeSpec.current.accentColor
                            } else {
                                LocalThemeSpec.current.contentColor.copy(alpha = 0.8f)
                            }
                        )
                    }
                }
            }
        }

        if (uiState.items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyStateView(
                    message = stringResource(
                        R.string.collection_empty_tab,
                        stringResource(uiState.selectedTab.labelRes())
                    )
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("collection_items_grid")
            ) {
                items(uiState.items, key = { it.entry.vnId }) { item ->
                    val vnId = item.entry.vnId
                    val isSelected = vnId in uiState.selectedVnIds
                    VnGridCard(
                        vn = item.vn,
                        sortField = uiState.sort.field,
                        showCoverCensorGlobal = uiState.settings.showCoverCensor,
                        isNsfwRevealed = vnId in uiState.revealedNsfwIds,
                        onToggleNsfwReveal = { onAction(CollectionAction.ToggleNsfwReveal(vnId)) },
                        collectionEntry = item.entry,
                        details = item.details,
                        isSelected = isSelected,
                        onClick = {
                            if (uiState.isSelectionMode) {
                                onAction(CollectionAction.ToggleSelectVn(vnId))
                            } else {
                                onOpenVnDetail(vnId)
                            }
                        },
                        onLongClick = { onAction(CollectionAction.ToggleSelectVn(vnId)) }
                    )
                }
            }
        }
    }

    if (showFilterSheet) {
        CollectionFilterSheet(
            uiState = uiState,
            onAction = onAction,
            onDismiss = { showFilterSheet = false }
        )
    }

    uiState.editingItem?.let { item ->
        CollectionEntryDialog(
            vn = item.vn,
            existingEntry = item.entry,
            onDismiss = { onAction(CollectionAction.DismissEditDialog) },
            onSave = { draft -> onAction(CollectionAction.SaveEditedEntry(item.entry.vnId, draft)) },
            onRemove = { onAction(CollectionAction.RemoveEditedEntry(item.entry.vnId)) }
        )
    }

    if (showBatchEditDialog) {
        BatchEditCollectionDialog(
            selectedCount = uiState.selectedVnIds.size,
            initialStatus = uiState.selectedTab,
            onConfirm = { newStatus ->
                showBatchEditDialog = false
                onAction(
                    CollectionAction.BatchUpdateSelected(
                        newStatus = newStatus,
                        newVote = null,
                        updateVote = false
                    )
                )
            },
            onDismiss = { showBatchEditDialog = false }
        )
    }

    if (showBatchDeleteConfirm) {
        ConfirmDialog(
            title = stringResource(R.string.collection_batch_delete_title),
            message = stringResource(R.string.collection_batch_delete_message, uiState.selectedVnIds.size),
            confirmLabel = stringResource(R.string.action_delete),
            isDestructive = true,
            confirmTestTag = "confirm_batch_delete_button",
            onConfirm = {
                showBatchDeleteConfirm = false
                onAction(CollectionAction.BatchDeleteSelected)
            },
            onDismiss = { showBatchDeleteConfirm = false }
        )
    }
}

@Composable
private fun CollectionTopHeader(
    uiState: CollectionUiState,
    onToggleSearch: () -> Unit,
    onOpenFilters: () -> Unit,
    onSyncClick: () -> Unit
) {
    val themeSpec = LocalThemeSpec.current
    Surface(
        color = themeSpec.surfaceColor.copy(alpha = 0.85f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.tab_collection),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = themeSpec.accentColor
                    )
                    Text(
                        text = stringResource(R.string.collection_subtitle_count, uiState.totalCollectionCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = themeSpec.contentColor.copy(alpha = 0.85f)
                    )
                }
                IconButton(
                    onClick = onToggleSearch,
                    modifier = Modifier.testTag("collection_search_toggle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.tab_search),
                        tint = themeSpec.accentColor
                    )
                }
                IconButton(
                    onClick = onOpenFilters,
                    modifier = Modifier.testTag("collection_filter_button")
                ) {
                    BadgedBox(
                        badge = {
                            if (uiState.filters.hasActiveMetadataFilters) {
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
                IconButton(
                    onClick = onSyncClick,
                    enabled = !uiState.syncProgress.isSyncing,
                    modifier = Modifier.testTag("collection_sync_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = stringResource(R.string.auth_sync_now),
                        tint = themeSpec.accentColor
                    )
                }
            }

            uiState.syncError?.let { error ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = error.toUiText().asString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorTokens.DestructiveRed
                )
            }
        }
    }
}

@Composable
private fun CollectionSelectionBar(
    selectedCount: Int,
    onSelectAll: () -> Unit,
    onBatchEdit: () -> Unit,
    onBatchDelete: () -> Unit,
    onClearSelection: () -> Unit
) {
    val themeSpec = LocalThemeSpec.current
    Surface(
        color = themeSpec.surfaceColor.copy(alpha = 0.92f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClearSelection,
                modifier = Modifier.testTag("collection_clear_selection_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.action_clear_selection),
                    tint = themeSpec.contentColor
                )
            }
            Text(
                text = stringResource(R.string.collection_selection_count, selectedCount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = themeSpec.accentColor,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onSelectAll,
                modifier = Modifier.testTag("collection_select_all_button")
            ) {
                Icon(
                    imageVector = Icons.Default.SelectAll,
                    contentDescription = stringResource(R.string.action_select_all),
                    tint = themeSpec.accentColor
                )
            }
            IconButton(
                onClick = onBatchEdit,
                modifier = Modifier.testTag("collection_batch_edit_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.edit_collection_entry),
                    tint = themeSpec.accentColor
                )
            }
            IconButton(
                onClick = onBatchDelete,
                modifier = Modifier.testTag("collection_batch_delete_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.action_delete),
                    tint = ColorTokens.DestructiveRed
                )
            }
        }
    }
}

@Composable
private fun BatchEditCollectionDialog(
    selectedCount: Int,
    initialStatus: CollectionStatus,
    onConfirm: (CollectionStatus) -> Unit,
    onDismiss: () -> Unit
) {
    val themeSpec = LocalThemeSpec.current
    var chosenStatusKey by rememberSaveable { mutableStateOf(initialStatus.storageKey) }
    val chosenStatus = CollectionStatus.fromStorageKeyOrDefault(chosenStatusKey)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = themeSpec.surfaceColor,
        titleContentColor = themeSpec.contentColor,
        textContentColor = themeSpec.contentColor,
        title = {
            Text(
                text = stringResource(R.string.collection_batch_edit_title, selectedCount),
                style = MaterialTheme.typography.titleLarge,
                color = themeSpec.accentColor
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CollectionStatus.entries.forEach { status ->
                    val selected = status == chosenStatus
                    FilterChip(
                        selected = selected,
                        onClick = { chosenStatusKey = status.storageKey },
                        leadingIcon = {
                            AssetImage(
                                assetPath = AssetPaths.collectionStatusIconPath(status),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text(stringResource(status.labelRes())) },
                        colors = vndbFilterChipColors(),
                        border = vndbFilterChipBorder(selected = selected),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(chosenStatus) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = themeSpec.accentColor,
                    contentColor = themeSpec.onAccentColor
                )
            ) {
                Text(stringResource(R.string.action_save), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), color = themeSpec.contentColor)
            }
        }
    )
}
