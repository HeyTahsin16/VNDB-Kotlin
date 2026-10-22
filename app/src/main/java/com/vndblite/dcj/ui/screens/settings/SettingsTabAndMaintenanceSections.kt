package com.vndblite.dcj.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vndblite.dcj.R
import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.ui.components.AssetImage
import com.vndblite.dcj.ui.format.badgeColor
import com.vndblite.dcj.ui.format.displayLabel
import com.vndblite.dcj.ui.format.iconAssetUri
import com.vndblite.dcj.ui.theme.VndbThemeTokens

@Composable
internal fun SettingsTabOrderCard(
    orderedStatuses: List<CollectionStatus>,
    onMoveStatus: (CollectionStatus, Int) -> Unit
) {
    val spec = VndbThemeTokens.spec
    Surface(
        color = spec.surfaceColor.copy(alpha = 0.85f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_tab_order_heading),
                style = MaterialTheme.typography.titleLarge,
                color = spec.accentColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.settings_tab_order_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = spec.onSurfaceColor.copy(alpha = 0.75f)
            )

            orderedStatuses.forEachIndexed { idx, status ->
                Surface(
                    color = spec.onSurfaceColor.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AssetImage(
                                assetPath = status.iconAssetUri(),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "${idx + 1}. ${status.displayLabel()}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = status.badgeColor(),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row {
                            IconButton(
                                onClick = { onMoveStatus(status, -1) },
                                enabled = idx > 0
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = stringResource(R.string.settings_move_up_cd),
                                    tint = if (idx > 0) spec.accentColor else spec.onSurfaceColor.copy(alpha = 0.3f)
                                )
                            }
                            IconButton(
                                onClick = { onMoveStatus(status, 1) },
                                enabled = idx < orderedStatuses.lastIndex
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = stringResource(R.string.settings_move_down_cd),
                                    tint = if (idx < orderedStatuses.lastIndex) spec.accentColor else spec.onSurfaceColor.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun SettingsMaintenanceCard(
    onRequestClearCache: () -> Unit,
    onRequestResetSettings: () -> Unit,
    onRequestClearCollection: () -> Unit
) {
    val spec = VndbThemeTokens.spec
    val semantic = VndbThemeTokens.colors
    Surface(
        color = spec.surfaceColor.copy(alpha = 0.85f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_data_maintenance_heading),
                style = MaterialTheme.typography.titleLarge,
                color = spec.accentColor,
                fontWeight = FontWeight.Bold
            )

            OutlinedButton(
                onClick = onRequestClearCache,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CleaningServices, contentDescription = null, tint = spec.accentColor)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.settings_clear_cache_button), color = spec.accentColor)
            }

            OutlinedButton(
                onClick = onRequestResetSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null, tint = spec.accentColor)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.settings_reset_defaults_button), color = spec.accentColor)
            }

            OutlinedButton(
                onClick = onRequestClearCollection,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = semantic.danger),
                border = BorderStroke(1.dp, semantic.danger.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = semantic.danger)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.settings_clear_collection_button), color = semantic.danger)
            }
        }
    }
}
