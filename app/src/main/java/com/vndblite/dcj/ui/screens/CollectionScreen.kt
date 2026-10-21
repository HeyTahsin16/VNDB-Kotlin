package com.vndblite.dcj.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vndblite.dcj.data.model.*
import com.vndblite.dcj.ui.components.AssetImage
import com.vndblite.dcj.ui.components.CollectionRecordDialog
import com.vndblite.dcj.ui.components.VnItemGridCard
import com.vndblite.dcj.ui.theme.LocalAppThemeCode
import com.vndblite.dcj.ui.theme.vndbFilterChipBorder
import com.vndblite.dcj.ui.theme.vndbFilterChipColors
import com.vndblite.dcj.ui.theme.vndbOutlinedTextFieldColors
import com.vndblite.dcj.ui.viewmodel.CollectionUiState
import com.vndblite.dcj.ui.viewmodel.SyncUiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CollectionScreen(
    collectionState: CollectionUiState,
    syncState: SyncUiState,
    settings: SettingsState,
    isLandscape: Boolean,
    onToggleSearchActive: (Boolean) -> Unit,
    onUpdateSearchQuery: (String) -> Unit,
    onApplyLocalFilterAndSort: (FilterData, SortData) -> Unit,
    onToggleSelectVn: (String) -> Unit,
    onSelectAllInTab: (String) -> Unit,
    onClearSelection: () -> Unit,
    onSaveRecord: (VnDataPhase01, String, Int, String?, String?, String?) -> Unit,
    onRemoveRecord: (String) -> Unit,
    onBatchUpdateSelected: (String, Int?) -> Unit,
    onBatchDeleteSelected: () -> Unit,
    onOpenAuthScreen: () -> Unit,
    onSyncNow: () -> Unit,
    onVnClick: (VnDataPhase01) -> Unit
) {
    val theme = LocalAppThemeCode.current
    val orderedTabs = remember(settings.collectionStatusTabArrangement) {
        settings.collectionStatusTabArrangement.mapNotNull { name ->
            CollectionStatusCode.entries.firstOrNull { it.statusName.equals(name, ignoreCase = true) }
        }.ifEmpty { CollectionStatusCode.entries }
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val currentTabStatus = orderedTabs.getOrElse(selectedTabIndex) { CollectionStatusCode.PLAYING }
    val currentList = collectionState.recordsByStatus[currentTabStatus.statusName].orEmpty()

    var showLocalFilterSheet by remember { mutableStateOf(false) }
    var editingSingleVn by remember { mutableStateOf<VnDataPhase01?>(null) }
    var showBatchEditDialog by remember { mutableStateOf(false) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }

    val columns = if (isLandscape) settings.maxItemPerRowLandscape else settings.maxItemPerRowPortrait
    val labelCode = remember(collectionState.localSort.sort) {
        SortableCode.entries.firstOrNull { it.code == collectionState.localSort.sort } ?: SortableCode.RATING
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("collection_screen_root")
    ) {
        // Top Action Bar or Multi-Selection Bar
        Surface(
            color = theme.primary.copy(alpha = 0.90f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                if (collectionState.selectedVnIds.isNotEmpty()) {
                    // Multi-Selection Action Bar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onClearSelection) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel Selection", tint = theme.tertiary)
                            }
                            Text(
                                text = "${collectionState.selectedVnIds.size} selected",
                                style = MaterialTheme.typography.titleLarge,
                                color = theme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = { onSelectAllInTab(currentTabStatus.statusName) },
                                modifier = Modifier.testTag("collection_select_all_button")
                            ) {
                                Icon(Icons.Default.SelectAll, contentDescription = "Select All", tint = theme.tertiary)
                            }
                            IconButton(
                                onClick = { showBatchEditDialog = true },
                                modifier = Modifier.testTag("collection_batch_edit_button")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Selected", tint = theme.secondary)
                            }
                            IconButton(
                                onClick = { showBatchDeleteConfirm = true },
                                modifier = Modifier.testTag("collection_batch_delete_button")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Selected", tint = Color(0xFFFF5252))
                            }
                        }
                    }
                } else {
                    // Standard Collection AppBar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        if (collectionState.isSearchActive) {
                            OutlinedTextField(
                                value = collectionState.localFilter.search,
                                onValueChange = onUpdateSearchQuery,
                                placeholder = { Text("Filter local collection...") },
                                singleLine = true,
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = theme.secondary)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { onToggleSearchActive(false) }) {
                                        Icon(Icons.Default.Close, contentDescription = "Close Search", tint = theme.tertiary)
                                    }
                                },
                                colors = vndbOutlinedTextFieldColors(theme),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("collection_search_input")
                            )
                        } else {
                            Column {
                                Text(
                                    text = "My Collection",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = theme.secondary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (syncState.userIdentity != null) {
                                        "Synced: ${syncState.userIdentity.username} (${collectionState.rawRecords.size} VNs)"
                                    } else {
                                        "${collectionState.rawRecords.size} VNs stored locally"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = theme.tertiary.copy(alpha = 0.78f)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!collectionState.isSearchActive) {
                                IconButton(
                                    onClick = { onToggleSearchActive(true) },
                                    modifier = Modifier.testTag("collection_search_toggle_button")
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = "Search Collection", tint = theme.tertiary)
                                }
                            }

                            BadgedBox(
                                badge = {
                                    if (collectionState.localFilter.hasActiveLocalFilters) {
                                        Badge(containerColor = theme.secondary, contentColor = Color.Black) {
                                            Text("!")
                                        }
                                    }
                                }
                            ) {
                                IconButton(
                                    onClick = { showLocalFilterSheet = true },
                                    modifier = Modifier.testTag("collection_filter_button")
                                ) {
                                    Icon(Icons.Default.FilterList, contentDescription = "Local Sort & Filter", tint = theme.tertiary)
                                }
                            }

                            IconButton(
                                onClick = {
                                    if (syncState.userIdentity != null) {
                                        onSyncNow()
                                    } else {
                                        onOpenAuthScreen()
                                    }
                                },
                                modifier = Modifier.testTag("collection_sync_button")
                            ) {
                                if (syncState.isSyncing) {
                                    CircularProgressIndicator(
                                        color = theme.secondary,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = "Sync with VNDB",
                                        tint = if (syncState.userIdentity != null) theme.secondary else theme.tertiary
                                    )
                                }
                            }
                        }
                    }
                }

                // Scrollable Status Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex.coerceIn(0, orderedTabs.lastIndex.coerceAtLeast(0)),
                    containerColor = Color.Transparent,
                    contentColor = theme.secondary,
                    edgePadding = 12.dp
                ) {
                    orderedTabs.forEachIndexed { idx, status ->
                        val count = collectionState.recordsByStatus[status.statusName]?.size ?: 0
                        val selected = idx == selectedTabIndex
                        Tab(
                            selected = selected,
                            onClick = {
                                selectedTabIndex = idx
                                onClearSelection()
                            },
                            modifier = Modifier.testTag("collection_tab_${status.statusName}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                AssetImage(
                                    assetPath = status.imagePath,
                                    contentDescription = status.displayTitle,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${status.displayTitle} ($count)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) theme.secondary else theme.tertiary.copy(alpha = 0.75f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Sync Banner if statusMessage exists
        if (!syncState.statusMessage.isNullOrBlank()) {
            Surface(
                color = if (syncState.isError) Color(0xFFB71C1C).copy(alpha = 0.85f) else theme.secondary.copy(alpha = 0.2f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = syncState.statusMessage,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (syncState.isError) Color.White else theme.tertiary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }

        // Content Grid or Empty State
        if (collectionState.rawRecords.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = theme.primary.copy(alpha = 0.86f),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CollectionsBookmark,
                            contentDescription = null,
                            tint = theme.secondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Your Collection is Empty",
                            style = MaterialTheme.typography.titleLarge,
                            color = theme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Add visual novels from any detail page, or connect your VNDB account to sync your cloud ulist.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = theme.tertiary.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onOpenAuthScreen,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = theme.secondary,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier.testTag("collection_empty_sync_button")
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Connect VNDB Account", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else if (currentList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No visual novels in '${currentTabStatus.displayTitle}'.",
                    style = MaterialTheme.typography.titleMedium,
                    color = theme.tertiary.copy(alpha = 0.8f)
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns.coerceAtLeast(2)),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 100.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("collection_grid")
            ) {
                items(currentList, key = { it.id }) { vn ->
                    val isSelected = collectionState.selectedVnIds.contains(vn.id)
                    VnItemGridCard(
                        vn = vn,
                        labelCode = labelCode,
                        showCoverCensorGlobal = settings.showCoverCensor,
                        collectionRecord = collectionState.rawRecords[vn.id],
                        p2Data = collectionState.p2Cache[vn.id],
                        isSelected = isSelected,
                        onClick = {
                            if (collectionState.selectedVnIds.isNotEmpty()) {
                                onToggleSelectVn(vn.id)
                            } else {
                                onVnClick(vn)
                            }
                        },
                        onLongClick = {
                            onToggleSelectVn(vn.id)
                        }
                    )
                }
            }
        }
    }

    // Single Record Edit Dialog
    editingSingleVn?.let { vn ->
        CollectionRecordDialog(
            vn = vn,
            existingRecord = collectionState.rawRecords[vn.id],
            onDismiss = { editingSingleVn = null },
            onSave = { status, vote, notes, started, finished ->
                onSaveRecord(vn, status, vote, notes, started, finished)
                editingSingleVn = null
            },
            onRemove = {
                onRemoveRecord(vn.id)
                editingSingleVn = null
            }
        )
    }

    // Batch Edit Dialog
    if (showBatchEditDialog) {
        var batchStatus by remember { mutableStateOf(currentTabStatus.statusName) }
        var batchVote by remember { mutableStateOf<Int?>(null) }

        AlertDialog(
            onDismissRequest = { showBatchEditDialog = false },
            containerColor = theme.primary,
            titleContentColor = theme.secondary,
            textContentColor = theme.tertiary,
            title = {
                Text("Edit ${collectionState.selectedVnIds.size} Selected VNs")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Move to Status:", fontWeight = FontWeight.Bold)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CollectionStatusCode.entries.forEach { st ->
                            val isSelected = batchStatus == st.statusName
                            FilterChip(
                                selected = isSelected,
                                onClick = { batchStatus = st.statusName },
                                label = {
                                    Text(
                                        st.displayTitle,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = vndbFilterChipColors(theme),
                                border = vndbFilterChipBorder(isSelected, theme)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Set Vote (Optional):", fontWeight = FontWeight.Bold)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val isKeep = batchVote == null
                        FilterChip(
                            selected = isKeep,
                            onClick = { batchVote = null },
                            label = {
                                Text(
                                    "Keep Existing",
                                    fontWeight = if (isKeep) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = vndbFilterChipColors(theme),
                            border = vndbFilterChipBorder(isKeep, theme)
                        )
                        (1..10).forEach { v ->
                            val isV = batchVote == v
                            FilterChip(
                                selected = isV,
                                onClick = { batchVote = v },
                                label = {
                                    Text(
                                        "$v",
                                        fontWeight = if (isV) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = vndbFilterChipColors(theme),
                                border = vndbFilterChipBorder(isV, theme)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onBatchUpdateSelected(batchStatus, batchVote)
                        showBatchEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = theme.secondary, contentColor = Color.Black)
                ) {
                    Text("Update All", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchEditDialog = false }) {
                    Text("Cancel", color = theme.tertiary)
                }
            }
        )
    }

    // Batch Delete Confirmation
    if (showBatchDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteConfirm = false },
            containerColor = theme.primary,
            titleContentColor = Color(0xFFFF8A80),
            textContentColor = theme.tertiary,
            title = { Text("Remove ${collectionState.selectedVnIds.size} VNs?") },
            text = {
                Text("Are you sure you want to remove the selected visual novels from your collection?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onBatchDeleteSelected()
                        showBatchDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252), contentColor = Color.White)
                ) {
                    Text("Remove", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteConfirm = false }) {
                    Text("Cancel", color = theme.tertiary)
                }
            }
        )
    }

    // Local Sort & Filter Bottom Sheet
    if (showLocalFilterSheet) {
        LocalSortFilterSheet(
            initialFilter = collectionState.localFilter,
            initialSort = collectionState.localSort,
            onDismiss = { showLocalFilterSheet = false },
            onReset = {
                onApplyLocalFilterAndSort(FilterData.DEFAULT_LOCAL, SortData.DEFAULT_LOCAL)
                showLocalFilterSheet = false
            },
            onApply = { f, s ->
                onApplyLocalFilterAndSort(f, s)
                showLocalFilterSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun LocalSortFilterSheet(
    initialFilter: FilterData,
    initialSort: SortData,
    onDismiss: () -> Unit,
    onReset: () -> Unit,
    onApply: (FilterData, SortData) -> Unit
) {
    val theme = LocalAppThemeCode.current
    var draftFilter by remember { mutableStateOf(initialFilter) }
    var draftSort by remember { mutableStateOf(initialSort) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = theme.primary,
        contentColor = theme.tertiary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.82f)
                .padding(horizontal = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Local Collection Sort & Filter",
                    style = MaterialTheme.typography.titleLarge,
                    color = theme.secondary,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onReset) {
                        Text("Reset", color = Color(0xFFFF8A80))
                    }
                    Button(
                        onClick = { onApply(draftFilter, draftSort) },
                        colors = ButtonDefaults.buttonColors(containerColor = theme.secondary, contentColor = Color.Black)
                    ) {
                        Text("Apply", fontWeight = FontWeight.Bold)
                    }
                }
            }

            HorizontalDivider(color = theme.tertiary.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Sort Collection By",
                    style = MaterialTheme.typography.titleMedium,
                    color = theme.secondary,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SortableCode.localSortables.forEach { opt ->
                        val isChosen = draftSort.sort == opt.code
                        FilterChip(
                            selected = isChosen,
                            onClick = { draftSort = draftSort.copy(sort = opt.code) },
                            label = {
                                Text(
                                    opt.title,
                                    fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = vndbFilterChipColors(theme),
                            border = vndbFilterChipBorder(isChosen, theme)
                        )
                    }
                    FilterChip(
                        selected = draftSort.reverse,
                        onClick = { draftSort = draftSort.copy(reverse = !draftSort.reverse) },
                        label = {
                            Text(
                                if (draftSort.reverse) "Descending ↓" else "Ascending ↑",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = vndbFilterChipColors(theme),
                        border = vndbFilterChipBorder(draftSort.reverse, theme)
                    )
                }

                // Minimum Age Slider
                Text(
                    text = "Minimum Age Rating: ${if (draftFilter.minage == 0) "All Ages" else "${draftFilter.minage}+"}",
                    style = MaterialTheme.typography.titleMedium,
                    color = theme.secondary,
                    fontWeight = FontWeight.Bold
                )
                Slider(
                    value = draftFilter.minage.toFloat(),
                    onValueChange = { draftFilter = draftFilter.copy(minage = it.toInt()) },
                    valueRange = 0f..18f,
                    steps = 5,
                    colors = SliderDefaults.colors(
                        thumbColor = theme.secondary,
                        activeTrackColor = theme.secondary
                    )
                )

                // Development Status
                Text(
                    text = "Development Status",
                    style = MaterialTheme.typography.titleMedium,
                    color = theme.secondary,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0, 1, 2).forEach { code ->
                        val info = DevStatusData.get(code)
                        val selected = draftFilter.devstatus.contains(code)
                        FilterChip(
                            selected = selected,
                            onClick = {
                                val next = if (selected) draftFilter.devstatus - code else draftFilter.devstatus + code
                                draftFilter = draftFilter.copy(devstatus = next)
                            },
                            label = {
                                Text(
                                    info.title,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = vndbFilterChipColors(theme),
                            border = vndbFilterChipBorder(selected, theme)
                        )
                    }
                }

                // Origin Language
                Text(
                    text = "Original Language",
                    style = MaterialTheme.typography.titleMedium,
                    color = theme.secondary,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LangData.DEFINED_CODES_ORIGIN.forEach { (code, label) ->
                        val selected = draftFilter.olang.contains(code)
                        FilterChip(
                            selected = selected,
                            onClick = {
                                val next = if (selected) draftFilter.olang - code else draftFilter.olang + code
                                draftFilter = draftFilter.copy(olang = next)
                            },
                            label = {
                                Text(
                                    label,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = vndbFilterChipColors(theme),
                            border = vndbFilterChipBorder(selected, theme)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
