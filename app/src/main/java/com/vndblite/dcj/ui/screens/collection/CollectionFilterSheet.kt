package com.vndblite.dcj.ui.screens.collection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vndblite.dcj.R
import com.vndblite.dcj.domain.model.DevStatus
import com.vndblite.dcj.domain.model.Language
import com.vndblite.dcj.domain.model.Platform
import com.vndblite.dcj.domain.model.SortField
import com.vndblite.dcj.ui.components.AssetImage
import com.vndblite.dcj.ui.components.AssetPaths
import com.vndblite.dcj.ui.format.labelRes
import com.vndblite.dcj.ui.theme.LocalThemeSpec
import com.vndblite.dcj.ui.theme.vndbFilterChipBorder
import com.vndblite.dcj.ui.theme.vndbFilterChipColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CollectionFilterSheet(
    uiState: CollectionUiState,
    onAction: (CollectionAction) -> Unit,
    onDismiss: () -> Unit
) {
    val themeSpec = LocalThemeSpec.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = themeSpec.surfaceColor,
        contentColor = themeSpec.contentColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.search_filter_sheet_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = themeSpec.accentColor
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onAction(CollectionAction.ResetFilters) },
                        modifier = Modifier.testTag("collection_reset_filters_button")
                    ) {
                        Text(
                            text = stringResource(R.string.search_reset_filters),
                            color = themeSpec.contentColor
                        )
                    }
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = themeSpec.accentColor,
                            contentColor = themeSpec.onAccentColor
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.action_close),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.search_sort_cd),
                style = MaterialTheme.typography.titleMedium,
                color = themeSpec.accentColor
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SortField.localSortableFields.forEach { field ->
                    val selected = uiState.sort.field == field
                    FilterChip(
                        selected = selected,
                        onClick = { onAction(CollectionAction.SelectSort(field, uiState.sort.reverse)) },
                        label = { Text(stringResource(field.labelRes())) },
                        colors = vndbFilterChipColors(),
                        border = vndbFilterChipBorder(selected = selected),
                        modifier = Modifier.testTag("collection_sort_${field.code}")
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !uiState.sort.reverse,
                    onClick = { onAction(CollectionAction.SelectSort(uiState.sort.field, false)) },
                    label = { Text(stringResource(R.string.sort_order_ascending)) },
                    colors = vndbFilterChipColors(),
                    border = vndbFilterChipBorder(selected = !uiState.sort.reverse)
                )
                FilterChip(
                    selected = uiState.sort.reverse,
                    onClick = { onAction(CollectionAction.SelectSort(uiState.sort.field, true)) },
                    label = { Text(stringResource(R.string.sort_order_descending)) },
                    colors = vndbFilterChipColors(),
                    border = vndbFilterChipBorder(selected = uiState.sort.reverse)
                )
            }

            HorizontalDivider(color = themeSpec.contentColor.copy(alpha = 0.2f))

            Text(
                text = stringResource(R.string.collection_filter_min_age),
                style = MaterialTheme.typography.titleMedium,
                color = themeSpec.accentColor
            )
            val ageChoices = listOf(
                null to R.string.collection_filter_min_age_any,
                12 to R.string.collection_filter_min_age_12,
                15 to R.string.collection_filter_min_age_15,
                18 to R.string.collection_filter_min_age_18
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ageChoices.forEach { (age, labelRes) ->
                    val selected = uiState.filters.minAge == age
                    FilterChip(
                        selected = selected,
                        onClick = { onAction(CollectionAction.SelectMinAge(age)) },
                        label = { Text(stringResource(labelRes)) },
                        colors = vndbFilterChipColors(),
                        border = vndbFilterChipBorder(selected = selected)
                    )
                }
            }

            Text(
                text = stringResource(R.string.search_dev_status_title),
                style = MaterialTheme.typography.titleMedium,
                color = themeSpec.accentColor
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DevStatus.entries.forEach { status ->
                    val selected = status in uiState.filters.devStatuses
                    FilterChip(
                        selected = selected,
                        onClick = { onAction(CollectionAction.ToggleDevStatus(status)) },
                        label = { Text(stringResource(status.labelRes())) },
                        colors = vndbFilterChipColors(),
                        border = vndbFilterChipBorder(selected = selected)
                    )
                }
            }

            Text(
                text = stringResource(R.string.search_origin_languages_title),
                style = MaterialTheme.typography.titleMedium,
                color = themeSpec.accentColor
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Language.originLanguages.forEach { lang ->
                    val selected = lang in uiState.filters.originalLanguages
                    FilterChip(
                        selected = selected,
                        onClick = { onAction(CollectionAction.ToggleOriginalLanguage(lang)) },
                        leadingIcon = {
                            AssetImage(
                                assetPath = AssetPaths.languageFlagPath(lang),
                                contentDescription = null,
                                modifier = Modifier.width(16.dp).height(11.dp)
                            )
                        },
                        label = { Text(stringResource(lang.labelRes())) },
                        colors = vndbFilterChipColors(),
                        border = vndbFilterChipBorder(selected = selected)
                    )
                }
            }

            Text(
                text = stringResource(R.string.search_languages_title),
                style = MaterialTheme.typography.titleMedium,
                color = themeSpec.accentColor
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Language.entries.forEach { lang ->
                    val selected = lang in uiState.filters.languages
                    FilterChip(
                        selected = selected,
                        onClick = { onAction(CollectionAction.ToggleLanguage(lang)) },
                        leadingIcon = {
                            AssetImage(
                                assetPath = AssetPaths.languageFlagPath(lang),
                                contentDescription = null,
                                modifier = Modifier.width(16.dp).height(11.dp)
                            )
                        },
                        label = { Text(stringResource(lang.labelRes())) },
                        colors = vndbFilterChipColors(),
                        border = vndbFilterChipBorder(selected = selected)
                    )
                }
            }

            Text(
                text = stringResource(R.string.search_platforms_title),
                style = MaterialTheme.typography.titleMedium,
                color = themeSpec.accentColor
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Platform.entries.filter { it.hasDedicatedAsset }.forEach { platform ->
                    val selected = platform in uiState.filters.platforms
                    FilterChip(
                        selected = selected,
                        onClick = { onAction(CollectionAction.TogglePlatform(platform)) },
                        leadingIcon = {
                            AssetImage(
                                assetPath = AssetPaths.platformIconPath(platform),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text(stringResource(platform.labelRes())) },
                        colors = vndbFilterChipColors(),
                        border = vndbFilterChipBorder(selected = selected)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
