package com.vndblite.dcj.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vndblite.dcj.R
import com.vndblite.dcj.domain.model.AppSettings
import com.vndblite.dcj.domain.model.ThemeId
import com.vndblite.dcj.ui.components.AssetImage
import com.vndblite.dcj.ui.theme.ThemeSpec
import com.vndblite.dcj.ui.theme.ThemeSpecs
import com.vndblite.dcj.ui.theme.VndbThemeTokens
import java.util.Locale
import kotlin.math.roundToInt

@Composable
internal fun SettingsFeedbackBanner(
    message: String,
    onDismiss: () -> Unit
) {
    val spec = VndbThemeTokens.spec
    Surface(
        color = spec.accentColor.copy(alpha = 0.2f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, spec.accentColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = spec.onSurfaceColor,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.action_close),
                    tint = spec.onSurfaceColor
                )
            }
        }
    }
}

@Composable
internal fun SettingsThemePickerCard(
    selectedTheme: ThemeId,
    onSelectTheme: (ThemeId) -> Unit
) {
    val spec = VndbThemeTokens.spec
    Surface(
        color = spec.surfaceColor.copy(alpha = 0.85f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.settings_theme_section),
                style = MaterialTheme.typography.titleLarge,
                color = spec.accentColor,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeSpecs.all.forEach { candidateSpec ->
                    ThemeOptionRow(
                        candidate = candidateSpec,
                        isSelected = selectedTheme == candidateSpec.id,
                        onSelect = { onSelectTheme(candidateSpec.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeOptionRow(
    candidate: ThemeSpec,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val activeSpec = VndbThemeTokens.spec
    Surface(
        color = if (isSelected) {
            activeSpec.accentColor.copy(alpha = 0.22f)
        } else {
            activeSpec.onSurfaceColor.copy(alpha = 0.08f)
        },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) activeSpec.accentColor else activeSpec.onSurfaceColor.copy(alpha = 0.2f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { selected = isSelected }
            .clickable(role = Role.RadioButton, onClick = onSelect)
            .testTag("theme_option_${candidate.id.code}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AssetImage(
                    assetPath = candidate.backgroundAssetPath,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Column {
                    Text(
                        text = stringResource(candidate.displayNameResId),
                        style = MaterialTheme.typography.titleMedium,
                        color = activeSpec.onSurfaceColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(candidate.paletteLabelResId),
                        style = MaterialTheme.typography.labelSmall,
                        color = activeSpec.onSurfaceColor.copy(alpha = 0.72f)
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ColorSwatchCircle(color = candidate.surfaceColor, borderColor = activeSpec.onSurfaceColor)
                ColorSwatchCircle(color = candidate.accentColor, borderColor = activeSpec.onSurfaceColor)
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = activeSpec.accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorSwatchCircle(
    color: androidx.compose.ui.graphics.Color,
    borderColor: androidx.compose.ui.graphics.Color
) {
    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, borderColor.copy(alpha = 0.5f), CircleShape)
    )
}

@Composable
internal fun SettingsDisplayAndLayoutCard(
    settings: AppSettings,
    onAction: (SettingsAction) -> Unit
) {
    val spec = VndbThemeTokens.spec
    Surface(
        color = spec.surfaceColor.copy(alpha = 0.85f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_display_content_heading),
                style = MaterialTheme.typography.titleLarge,
                color = spec.accentColor,
                fontWeight = FontWeight.Bold
            )

            SettingsToggleRow(
                title = stringResource(R.string.settings_blur_nsfw_title),
                subtitle = stringResource(R.string.settings_blur_nsfw_subtitle),
                checked = settings.showCoverCensor,
                onCheckedChange = { onAction(SettingsAction.ToggleNsfwBlur(it)) },
                switchTestTag = "settings_nsfw_switch"
            )

            HorizontalDivider(color = spec.onSurfaceColor.copy(alpha = 0.2f))

            SettingsToggleRow(
                title = stringResource(R.string.settings_show_chart_title),
                subtitle = stringResource(R.string.settings_show_chart_subtitle),
                checked = settings.showChart,
                onCheckedChange = { onAction(SettingsAction.ToggleShowChart(it)) },
                switchTestTag = "settings_chart_switch"
            )

            HorizontalDivider(color = spec.onSurfaceColor.copy(alpha = 0.2f))

            val formattedOffset = String.format(Locale.US, "%+.1f", settings.fontSizeOffset)
            SettingsSliderRow(
                label = stringResource(R.string.settings_font_scale_value, formattedOffset),
                value = settings.fontSizeOffset,
                valueRange = -2f..6f,
                steps = 7,
                onValueChange = { onAction(SettingsAction.UpdateFontScaleOffset(it)) }
            )

            SettingsSliderRow(
                label = stringResource(R.string.settings_max_preview_value, settings.maxPreviewItem),
                value = settings.maxPreviewItem.toFloat(),
                valueRange = 5f..40f,
                steps = 6,
                onValueChange = { onAction(SettingsAction.UpdateMaxPreviewItems(it.roundToInt())) }
            )

            SettingsSliderRow(
                label = stringResource(R.string.settings_grid_portrait_value, settings.maxItemPerRowPortrait),
                value = settings.maxItemPerRowPortrait.toFloat(),
                valueRange = 1f..4f,
                steps = 2,
                onValueChange = { onAction(SettingsAction.UpdatePortraitColumns(it.roundToInt())) }
            )

            SettingsSliderRow(
                label = stringResource(R.string.settings_grid_landscape_value, settings.maxItemPerRowLandscape),
                value = settings.maxItemPerRowLandscape.toFloat(),
                valueRange = 2f..6f,
                steps = 3,
                onValueChange = { onAction(SettingsAction.UpdateLandscapeColumns(it.roundToInt())) }
            )
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    switchTestTag: String
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
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = spec.onSurfaceColor,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = spec.onSurfaceColor.copy(alpha = 0.72f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = semantic.onAccentButton,
                checkedTrackColor = spec.accentColor
            ),
            modifier = Modifier.testTag(switchTestTag)
        )
    }
}

@Composable
private fun SettingsSliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit
) {
    val spec = VndbThemeTokens.spec
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = spec.onSurfaceColor,
            fontWeight = FontWeight.SemiBold
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = spec.accentColor,
                activeTrackColor = spec.accentColor
            )
        )
    }
}
