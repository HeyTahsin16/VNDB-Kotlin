package com.vndblite.dcj.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vndblite.dcj.R
import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.domain.model.ThemeId
import com.vndblite.dcj.ui.components.AssetImage
import com.vndblite.dcj.ui.components.AssetPaths
import com.vndblite.dcj.ui.components.ConfirmDialog
import com.vndblite.dcj.ui.format.displayNameRes
import com.vndblite.dcj.ui.format.labelRes
import com.vndblite.dcj.ui.theme.ColorTokens
import com.vndblite.dcj.ui.theme.LocalThemeSpec
import com.vndblite.dcj.ui.theme.ThemeSpecs
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeSpec = LocalThemeSpec.current
    val settings = uiState.settings

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                        tint = themeSpec.accentColor
                    )
                }
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = themeSpec.accentColor
                )
            }
        }

        item {
            ThemeSelectorCard(
                selectedTheme = settings.themeId,
                onSelectTheme = { onAction(SettingsAction.SelectTheme(it)) }
            )
        }

        item {
            DisplaySettingsCard(
                settings = settings,
                onAction = onAction
            )
        }

        item {
            LayoutSettingsCard(
                settings = settings,
                onAction = onAction
            )
        }

        item {
            TabOrderCard(
                tabs = settings.collectionStatusTabArrangement.ifEmpty { CollectionStatus.entries },
                onMoveTab = { status, dir -> onAction(SettingsAction.MoveTab(status, dir)) }
            )
        }

        item {
            MaintenanceCard(onAction = onAction)
        }
    }

    if (uiState.showClearCacheConfirm) {
        ConfirmDialog(
            title = stringResource(R.string.settings_clear_cache_confirm_title),
            message = stringResource(R.string.settings_clear_cache_confirm_msg),
            confirmLabel = stringResource(R.string.action_clear),
            onConfirm = { onAction(SettingsAction.ConfirmClearCache) },
            onDismiss = { onAction(SettingsAction.DismissClearCache) }
        )
    }

    if (uiState.showClearCollectionConfirm) {
        ConfirmDialog(
            title = stringResource(R.string.settings_clear_collection_confirm_title),
            message = stringResource(R.string.settings_clear_collection_confirm_msg),
            confirmLabel = stringResource(R.string.action_clear),
            isDestructive = true,
            onConfirm = { onAction(SettingsAction.ConfirmClearCollection) },
            onDismiss = { onAction(SettingsAction.DismissClearCollection) }
        )
    }

    if (uiState.showResetDefaultsConfirm) {
        ConfirmDialog(
            title = stringResource(R.string.settings_reset_defaults_confirm_title),
            message = stringResource(R.string.settings_reset_defaults_confirm_msg),
            confirmLabel = stringResource(R.string.action_reset),
            isDestructive = true,
            onConfirm = { onAction(SettingsAction.ConfirmResetDefaults) },
            onDismiss = { onAction(SettingsAction.DismissResetDefaults) }
        )
    }
}

@Composable
private fun ThemeSelectorCard(
    selectedTheme: ThemeId,
    onSelectTheme: (ThemeId) -> Unit
) {
    val themeSpec = LocalThemeSpec.current
    Surface(
        color = themeSpec.surfaceColor.copy(alpha = 0.85f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_section_theme),
                style = MaterialTheme.typography.titleMedium,
                color = themeSpec.accentColor
            )
            ThemeSpecs.all.forEach { candidateSpec ->
                val isSelected = candidateSpec.themeId == selectedTheme
                Surface(
                    color = candidateSpec.surfaceColor,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) candidateSpec.accentColor else candidateSpec.contentColor.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            role = Role.RadioButton
                            selected = isSelected
                        }
                        .clickable { onSelectTheme(candidateSpec.themeId) }
                        .testTag("theme_option_${candidateSpec.themeId.code}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(candidateSpec.themeId.displayNameRes()),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = candidateSpec.contentColor
                            )
                            Text(
                                text = stringResource(candidateSpec.paletteLabelRes),
                                style = MaterialTheme.typography.labelSmall,
                                color = candidateSpec.accentColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DisplaySettingsCard(
    settings: com.vndblite.dcj.domain.model.AppSettings,
    onAction: (SettingsAction) -> Unit
) {
    val themeSpec = LocalThemeSpec.current
    Surface(
        color = themeSpec.surfaceColor.copy(alpha = 0.85f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_section_display),
                style = MaterialTheme.typography.titleMedium,
                color = themeSpec.accentColor
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_nsfw_blur_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = themeSpec.contentColor
                    )
                    Text(
                        text = stringResource(R.string.settings_nsfw_blur_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = themeSpec.contentColor.copy(alpha = 0.8f)
                    )
                }
                Switch(
                    checked = settings.showCoverCensor,
                    onCheckedChange = { onAction(SettingsAction.SetCoverCensor(it)) },
                    modifier = Modifier.testTag("settings_nsfw_blur_switch")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_show_chart_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = themeSpec.contentColor
                    )
                    Text(
                        text = stringResource(R.string.settings_show_chart_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = themeSpec.contentColor.copy(alpha = 0.8f)
                    )
                }
                Switch(
                    checked = settings.showChart,
                    onCheckedChange = { onAction(SettingsAction.SetShowChart(it)) },
                    modifier = Modifier.testTag("settings_show_chart_switch")
                )
            }

            Column {
                Text(
                    text = stringResource(R.string.settings_font_scale_title, settings.fontSizeOffset),
                    style = MaterialTheme.typography.bodyMedium,
                    color = themeSpec.contentColor
                )
                Slider(
                    value = settings.fontSizeOffset,
                    onValueChange = { onAction(SettingsAction.SetFontSizeOffset((it * 2f).roundToInt() / 2f)) },
                    valueRange = -4f..8f,
                    colors = SliderDefaults.colors(
                        thumbColor = themeSpec.accentColor,
                        activeTrackColor = themeSpec.accentColor
                    )
                )
            }
        }
    }
}

@Composable
private fun LayoutSettingsCard(
    settings: com.vndblite.dcj.domain.model.AppSettings,
    onAction: (SettingsAction) -> Unit
) {
    val themeSpec = LocalThemeSpec.current
    Surface(
        color = themeSpec.surfaceColor.copy(alpha = 0.85f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_section_layout),
                style = MaterialTheme.typography.titleMedium,
                color = themeSpec.accentColor
            )
            Text(
                text = stringResource(R.string.settings_max_preview_title, settings.maxPreviewItem),
                style = MaterialTheme.typography.bodyMedium,
                color = themeSpec.contentColor
            )
            Slider(
                value = settings.maxPreviewItem.toFloat(),
                onValueChange = { onAction(SettingsAction.SetMaxPreviewItem(it.roundToInt())) },
                valueRange = 5f..50f,
                colors = SliderDefaults.colors(thumbColor = themeSpec.accentColor, activeTrackColor = themeSpec.accentColor)
            )
            Text(
                text = stringResource(R.string.settings_portrait_cols_title, settings.maxItemPerRowPortrait),
                style = MaterialTheme.typography.bodyMedium,
                color = themeSpec.contentColor
            )
            Slider(
                value = settings.maxItemPerRowPortrait.toFloat(),
                onValueChange = { onAction(SettingsAction.SetPortraitColumns(it.roundToInt())) },
                valueRange = 1f..6f,
                steps = 4,
                colors = SliderDefaults.colors(thumbColor = themeSpec.accentColor, activeTrackColor = themeSpec.accentColor)
            )
            Text(
                text = stringResource(R.string.settings_landscape_cols_title, settings.maxItemPerRowLandscape),
                style = MaterialTheme.typography.bodyMedium,
                color = themeSpec.contentColor
            )
            Slider(
                value = settings.maxItemPerRowLandscape.toFloat(),
                onValueChange = { onAction(SettingsAction.SetLandscapeColumns(it.roundToInt())) },
                valueRange = 2f..10f,
                steps = 7,
                colors = SliderDefaults.colors(thumbColor = themeSpec.accentColor, activeTrackColor = themeSpec.accentColor)
            )
        }
    }
}

@Composable
private fun TabOrderCard(
    tabs: List<CollectionStatus>,
    onMoveTab: (CollectionStatus, Int) -> Unit
) {
    val themeSpec = LocalThemeSpec.current
    Surface(
        color = themeSpec.surfaceColor.copy(alpha = 0.85f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_section_tabs),
                style = MaterialTheme.typography.titleMedium,
                color = themeSpec.accentColor
            )
            tabs.forEachIndexed { index, status ->
                val statusLabel = stringResource(status.labelRes())
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssetImage(
                        assetPath = AssetPaths.collectionStatusIconPath(status),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = themeSpec.contentColor,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { onMoveTab(status, -1) },
                        enabled = index > 0,
                        modifier = Modifier.testTag("move_tab_up_${status.storageKey}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = stringResource(R.string.settings_move_tab_up_cd, statusLabel),
                            tint = themeSpec.accentColor
                        )
                    }
                    IconButton(
                        onClick = { onMoveTab(status, 1) },
                        enabled = index < tabs.lastIndex,
                        modifier = Modifier.testTag("move_tab_down_${status.storageKey}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = stringResource(R.string.settings_move_tab_down_cd, statusLabel),
                            tint = themeSpec.accentColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MaintenanceCard(
    onAction: (SettingsAction) -> Unit
) {
    val themeSpec = LocalThemeSpec.current
    Surface(
        color = themeSpec.surfaceColor.copy(alpha = 0.85f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_section_data),
                style = MaterialTheme.typography.titleMedium,
                color = themeSpec.accentColor
            )
            OutlinedButton(
                onClick = { onAction(SettingsAction.RequestClearCache) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_clear_cache_button")
            ) {
                Text(stringResource(R.string.settings_clear_cache_button), color = themeSpec.contentColor)
            }
            OutlinedButton(
                onClick = { onAction(SettingsAction.RequestClearCollection) },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorTokens.DestructiveRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_clear_collection_button")
            ) {
                Text(stringResource(R.string.settings_clear_collection_button))
            }
            OutlinedButton(
                onClick = { onAction(SettingsAction.RequestResetDefaults) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_reset_defaults_button")
            ) {
                Text(stringResource(R.string.settings_reset_defaults_button), color = themeSpec.contentColor)
            }
        }
    }
}
