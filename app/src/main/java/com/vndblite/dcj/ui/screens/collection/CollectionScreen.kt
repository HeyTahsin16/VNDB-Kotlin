package com.vndblite.dcj.ui.screens.collection

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vndblite.dcj.LocalAppContainer
import com.vndblite.dcj.R
import com.vndblite.dcj.core.UiText
import com.vndblite.dcj.domain.model.CollectionItemWithVn
import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.domain.model.ThemeId
import com.vndblite.dcj.domain.model.VnSummary
import com.vndblite.dcj.ui.components.CollectionEntryDialog
import com.vndblite.dcj.ui.components.ConfirmDialog
import com.vndblite.dcj.ui.components.ConfirmDialogSpec
import com.vndblite.dcj.ui.components.PreviewData
import com.vndblite.dcj.ui.format.isWideLayout
import com.vndblite.dcj.ui.theme.VndbTheme

@Composable
fun CollectionScreen(
    onOpenAuthScreen: () -> Unit,
    onOpenVnDetail: (VnSummary) -> Unit,
    viewModel: CollectionViewModel = viewModel(
        factory = CollectionViewModel.factory(LocalAppContainer.current)
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val wide = isWideLayout()
    CollectionContent(
        state = state,
        isWide = wide,
        onAction = { action ->
            when (action) {
                CollectionAction.OpenAuthScreen -> onOpenAuthScreen()
                is CollectionAction.OpenVnDetail -> onOpenVnDetail(action.vn)
                else -> viewModel.onAction(action)
            }
        }
    )
}

@Composable
fun CollectionContent(
    state: CollectionUiState,
    isWide: Boolean,
    onAction: (CollectionAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val orderedStatuses = state.settings.collectionStatusTabArrangement.ifEmpty {
        CollectionStatus.entries
    }
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val activeStatus = orderedStatuses.getOrElse(selectedTabIndex) { CollectionStatus.PLAYING }
    val itemsInTab = state.itemsByStatus[activeStatus].orEmpty()

    var showFilterSheet by rememberSaveable { mutableStateOf(false) }
    var editingVnId by rememberSaveable { mutableStateOf<String?>(null) }
    var showBatchEditDialog by rememberSaveable { mutableStateOf(false) }
    var showBatchDeleteConfirm by rememberSaveable { mutableStateOf(false) }

    val isSelectionMode = state.selectedVnIds.isNotEmpty()
    val cols = if (isWide) state.settings.maxItemPerRowLandscape else state.settings.maxItemPerRowPortrait

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        if (isSelectionMode) {
            CollectionSelectionBar(
                selectedCount = state.selectedVnIds.size,
                onClearSelection = { onAction(CollectionAction.ClearSelection) },
                onSelectAllInTab = { onAction(CollectionAction.SelectAllInStatus(activeStatus)) },
                onOpenBatchEdit = { showBatchEditDialog = true },
                onRequestBatchDelete = { showBatchDeleteConfirm = true }
            )
        } else {
            CollectionHeaderBar(
                state = state,
                onToggleSearch = { onAction(CollectionAction.SetSearchActive(!state.isSearchActive)) },
                onOpenFilterSheet = { showFilterSheet = true },
                onSyncOrOpenAuth = {
                    if (state.activeAccount != null) {
                        onAction(CollectionAction.SyncNow)
                    } else {
                        onAction(CollectionAction.OpenAuthScreen)
                    }
                }
            )
        }

        AnimatedVisibility(visible = state.isSearchActive) {
            CollectionSearchField(
                query = state.filters.searchQuery,
                onQueryChange = { onAction(CollectionAction.UpdateSearchQuery(it)) }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        CollectionStatusTabsRow(
            orderedStatuses = orderedStatuses,
            itemsByStatus = state.itemsByStatus,
            selectedTabIndex = selectedTabIndex,
            onSelectTab = { selectedTabIndex = it }
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (itemsInTab.isEmpty()) {
            CollectionEmptyView(hasTotalEntries = state.totalCount > 0)
        } else {
            CollectionGrid(
                items = itemsInTab,
                columns = cols.coerceAtLeast(1),
                state = state,
                isSelectionMode = isSelectionMode,
                onAction = onAction,
                onEditEntry = { vn -> editingVnId = vn.id }
            )
        }
    }

    if (showFilterSheet) {
        CollectionFilterSheet(
            initialFilters = state.filters,
            initialSort = state.sort,
            onDismiss = { showFilterSheet = false },
            onApply = { f, s ->
                showFilterSheet = false
                onAction(CollectionAction.ApplyFiltersAndSort(f, s))
            }
        )
    }

    val editingVn = editingVnId?.let { targetId ->
        itemsInTab.firstOrNull { it.vn.id == targetId }?.vn
            ?: state.itemsByStatus.values.flatten().firstOrNull { it.vn.id == targetId }?.vn
    }
    if (editingVn != null) {
        CollectionEntryDialog(
            vnTitle = editingVn.title,
            existingEntry = state.allEntriesMap[editingVn.id],
            isAccountSynced = state.isAccountSynced,
            onSave = { payload ->
                editingVnId = null
                onAction(CollectionAction.SaveEntry(editingVn, payload))
            },
            onRemove = {
                editingVnId = null
                onAction(CollectionAction.RemoveEntry(editingVn.id))
            },
            onDismiss = { editingVnId = null }
        )
    }

    if (showBatchEditDialog) {
        BatchEditCollectionDialog(
            selectedCount = state.selectedVnIds.size,
            defaultStatus = activeStatus,
            onDismiss = { showBatchEditDialog = false },
            onConfirm = { newStatus, newVote ->
                showBatchEditDialog = false
                onAction(CollectionAction.BatchUpdateSelected(newStatus, newVote))
            }
        )
    }

    if (showBatchDeleteConfirm) {
        val count = state.selectedVnIds.size
        val msgRes = if (state.isAccountSynced) {
            R.plurals.dialog_remove_batch_message_synced
        } else {
            R.plurals.dialog_remove_batch_message_local
        }
        ConfirmDialog(
            spec = ConfirmDialogSpec(
                title = UiText.StringResource(R.string.dialog_remove_batch_title),
                message = UiText.PluralResource(msgRes, count, count),
                confirmLabel = UiText.StringResource(R.string.collection_batch_delete),
                isDestructive = true
            ),
            onConfirm = {
                showBatchDeleteConfirm = false
                onAction(CollectionAction.BatchDeleteSelected)
            },
            onDismiss = { showBatchDeleteConfirm = false }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CollectionContentPreview() {
    VndbTheme(themeId = ThemeId.AGL, includeBackground = true) {
        CollectionContent(
            state = CollectionUiState(
                itemsByStatus = mapOf(
                    CollectionStatus.PLAYING to listOf(
                        CollectionItemWithVn(
                            entry = PreviewData.sampleCollectionEntry,
                            vn = PreviewData.sampleVnSummary
                        )
                    )
                ),
                allEntriesMap = mapOf(PreviewData.sampleVnSummary.id to PreviewData.sampleCollectionEntry),
                totalCount = 1
            ),
            isWide = false,
            onAction = {}
        )
    }
}
