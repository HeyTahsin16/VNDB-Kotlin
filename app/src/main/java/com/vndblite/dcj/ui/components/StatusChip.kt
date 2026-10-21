package com.vndblite.dcj.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vndblite.dcj.R
import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.domain.model.ThemeId
import com.vndblite.dcj.ui.format.badgeColor
import com.vndblite.dcj.ui.format.displayLabel
import com.vndblite.dcj.ui.format.iconAssetUri
import com.vndblite.dcj.ui.theme.VndbTheme

/**
 * Accessible selectable chip for choosing a [CollectionStatus] with `Role.RadioButton`
 * or `Role.Tab` semantics and a minimum 48.dp touch target (U1, U2, U6).
 *
 * Preserves `testTag("status_option_${status.storageKey}")` (Rule 1).
 */
@Composable
fun StatusOptionChip(
    status: CollectionStatus,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    role: Role = Role.RadioButton,
    countBadge: Int? = null
) {
    val statusColor = status.badgeColor()
    val label = status.displayLabel()
    val onSurface = MaterialTheme.colorScheme.onSurface

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) statusColor.copy(alpha = 0.28f) else onSurface.copy(alpha = 0.08f),
        border = if (selected) BorderStroke(1.5.dp, statusColor) else null,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = role
            )
            .testTag("status_option_${status.storageKey}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
        ) {
            AssetImage(
                assetPath = status.iconAssetUri(),
                contentDescription = null, // Decorative next to visible status label (U1)
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (countBadge != null) "$label ($countBadge)" else label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = onSurface
            )
        }
    }
}

@Composable
fun StatusChip(
    status: CollectionStatus,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    StatusOptionChip(
        status = status,
        selected = selected,
        onClick = onClick,
        modifier = modifier
    )
}

/**
 * Accessible tab chip for the Collection screen's status filter bar, including an "All" option (U1, U2, U6).
 */
@Composable
fun StatusFilterTabChip(
    status: CollectionStatus?,
    selected: Boolean,
    count: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (status != null) {
        StatusOptionChip(
            status = status,
            selected = selected,
            onClick = onClick,
            role = Role.Tab,
            countBadge = count,
            modifier = modifier
        )
    } else {
        val allLabel = stringResource(R.string.collection_all_statuses)
        val accent = MaterialTheme.colorScheme.primary
        val onSurface = MaterialTheme.colorScheme.onSurface
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (selected) accent.copy(alpha = 0.28f) else onSurface.copy(alpha = 0.08f),
            border = if (selected) BorderStroke(1.5.dp, accent) else null,
            modifier = modifier
                .minimumInteractiveComponentSize()
                .selectable(
                    selected = selected,
                    onClick = onClick,
                    role = Role.Tab
                )
                .testTag("status_option_all")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = if (count != null) "$allLabel ($count)" else allLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = onSurface
                )
            }
        }
    }
}

/**
 * 2-column x 3-row radio group grid of [CollectionStatus] options (U1, U2, U6).
 */
@Composable
fun StatusSelectionGrid(
    selectedStatus: CollectionStatus,
    onStatusSelected: (CollectionStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CollectionStatus.entries.chunked(2).forEach { rowStatuses ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowStatuses.forEach { status ->
                    StatusOptionChip(
                        status = status,
                        selected = status == selectedStatus,
                        onClick = { onStatusSelected(status) },
                        role = Role.RadioButton,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Preview(name = "StatusSelectionGrid - Dark Theme", showBackground = true)
@Composable
private fun StatusSelectionGridDarkPreview() {
    VndbTheme(themeId = ThemeId.FATE, includeBackground = true) {
        StatusSelectionGrid(
            selectedStatus = CollectionStatus.PLAYING,
            onStatusSelected = {},
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Preview(name = "StatusSelectionGrid - Light Theme", showBackground = true)
@Composable
private fun StatusSelectionGridLightPreview() {
    VndbTheme(themeId = ThemeId.AIR, includeBackground = true) {
        StatusSelectionGrid(
            selectedStatus = CollectionStatus.FINISHED,
            onStatusSelected = {},
            modifier = Modifier.padding(12.dp)
        )
    }
}
