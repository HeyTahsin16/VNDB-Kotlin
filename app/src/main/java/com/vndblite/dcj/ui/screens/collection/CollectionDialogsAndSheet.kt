package com.vndblite.dcj.ui.screens.collection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vndblite.dcj.R
import com.vndblite.dcj.domain.model.CollectionFilters
import com.vndblite.dcj.domain.model.SortField
import com.vndblite.dcj.domain.model.SortOption
import com.vndblite.dcj.ui.format.displayLabel
import com.vndblite.dcj.ui.theme.VndbThemeTokens
import com.vndblite.dcj.ui.theme.vndbFilterChipBorder
import com.vndblite.dcj.ui.theme.vndbFilterChipColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun CollectionFilterSheet(
    initialFilters: CollectionFilters,
    initialSort: SortOption,
    onDismiss: () -> Unit,
    onApply: (CollectionFilters, SortOption) -> Unit
) {
    val spec = VndbThemeTokens.spec
    val semantic = VndbThemeTokens.colors
    var draftFilters by remember(initialFilters) { mutableStateOf(initialFilters) }
    var draftSort by remember(initialSort) { mutableStateOf(initialSort) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = spec.surfaceColor,
        contentColor = spec.onSurfaceColor,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = spec.onSurfaceColor.copy(alpha = 0.5f))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.collection_filter_sheet_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = spec.accentColor,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            draftFilters = CollectionFilters(searchQuery = draftFilters.searchQuery)
                            draftSort = SortOption.DEFAULT_LOCAL
                        }
                    ) {
                        Text(stringResource(R.string.action_reset), color = semantic.danger)
                    }
                    Button(
                        onClick = { onApply(draftFilters, draftSort) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = spec.accentColor,
                            contentColor = semantic.onAccentButton
                        ),
                        modifier = Modifier.testTag("apply_local_filter_button")
                    ) {
                        Text(stringResource(R.string.action_apply), fontWeight = FontWeight.Bold)
                    }
                }
            }

            HorizontalDivider(
                color = spec.onSurfaceColor.copy(alpha = 0.25f),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                CollectionSortSection(
                    draftSort = draftSort,
                    onUpdateSort = { draftSort = it }
                )

                CollectionMetadataFiltersSection(
                    draftFilters = draftFilters,
                    onUpdateFilters = { draftFilters = it }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CollectionSortSection(
    draftSort: SortOption,
    onUpdateSort: (SortOption) -> Unit
) {
    val spec = VndbThemeTokens.spec
    Text(
        text = stringResource(R.string.collection_filter_sort_heading),
        style = MaterialTheme.typography.titleMedium,
        color = spec.accentColor,
        fontWeight = FontWeight.Bold
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SortField.localSortableFields.forEach { field ->
            val selected = draftSort.field == field
            FilterChip(
                selected = selected,
                onClick = { onUpdateSort(draftSort.copy(field = field)) },
                label = {
                    Text(
                        text = field.displayLabel(),
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                leadingIcon = if (selected) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null,
                colors = vndbFilterChipColors(spec),
                border = vndbFilterChipBorder(selected, spec)
            )
        }
        FilterChip(
            selected = draftSort.reverse,
            onClick = { onUpdateSort(draftSort.copy(reverse = !draftSort.reverse)) },
            label = {
                Text(
                    text = stringResource(
                        if (draftSort.reverse) R.string.order_descending_chip
                        else R.string.order_ascending_chip
                    ),
                    fontWeight = FontWeight.Bold
                )
            },
            colors = vndbFilterChipColors(spec),
            border = vndbFilterChipBorder(draftSort.reverse, spec)
        )
    }
}
