package com.vndblite.dcj.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.vndblite.dcj.data.model.ThemeCode
import com.vndblite.dcj.domain.model.ThemeId

val LocalThemeSpec = compositionLocalOf { ThemeSpecs.forId(ThemeId.DEFAULT) }
val LocalAppThemeCode = compositionLocalOf { ThemeCode.AGL }
val LocalFontScaleOffset = compositionLocalOf { 0f }

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun vndbFilterChipColors(spec: ThemeSpec = LocalThemeSpec.current): SelectableChipColors {
    return FilterChipDefaults.filterChipColors(
        containerColor = spec.contentColor.copy(alpha = 0.12f),
        labelColor = spec.contentColor,
        iconColor = spec.accentColor,
        selectedContainerColor = spec.accentColor,
        selectedLabelColor = spec.onAccentColor,
        selectedLeadingIconColor = spec.onAccentColor,
        selectedTrailingIconColor = spec.onAccentColor
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun vndbFilterChipColors(theme: ThemeCode): SelectableChipColors =
    vndbFilterChipColors(ThemeId.fromCode(theme.code).toThemeSpec())

@Composable
fun vndbFilterChipBorder(
    selected: Boolean,
    spec: ThemeSpec = LocalThemeSpec.current
): BorderStroke {
    return BorderStroke(
        width = 1.dp,
        color = if (selected) spec.accentColor else spec.contentColor.copy(alpha = 0.50f)
    )
}

@Composable
fun vndbFilterChipBorder(
    selected: Boolean,
    theme: ThemeCode
): BorderStroke = vndbFilterChipBorder(
    selected = selected,
    spec = ThemeId.fromCode(theme.code).toThemeSpec()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun vndbInputChipColors(spec: ThemeSpec = LocalThemeSpec.current): SelectableChipColors {
    return InputChipDefaults.inputChipColors(
        containerColor = spec.contentColor.copy(alpha = 0.14f),
        labelColor = spec.contentColor,
        leadingIconColor = spec.accentColor,
        trailingIconColor = spec.contentColor,
        selectedContainerColor = spec.accentColor.copy(alpha = 0.28f),
        selectedLabelColor = spec.contentColor,
        selectedLeadingIconColor = spec.accentColor,
        selectedTrailingIconColor = spec.contentColor
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun vndbInputChipColors(theme: ThemeCode): SelectableChipColors =
    vndbInputChipColors(ThemeId.fromCode(theme.code).toThemeSpec())

@Composable
fun vndbOutlinedTextFieldColors(spec: ThemeSpec = LocalThemeSpec.current): TextFieldColors {
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = spec.contentColor,
        unfocusedTextColor = spec.contentColor,
        focusedContainerColor = spec.contentColor.copy(alpha = 0.12f),
        unfocusedContainerColor = spec.contentColor.copy(alpha = 0.08f),
        cursorColor = spec.accentColor,
        focusedBorderColor = spec.accentColor,
        unfocusedBorderColor = spec.contentColor.copy(alpha = 0.55f),
        focusedLabelColor = spec.accentColor,
        unfocusedLabelColor = spec.contentColor.copy(alpha = 0.88f),
        focusedPlaceholderColor = spec.contentColor.copy(alpha = 0.68f),
        unfocusedPlaceholderColor = spec.contentColor.copy(alpha = 0.68f),
        focusedLeadingIconColor = spec.accentColor,
        unfocusedLeadingIconColor = spec.accentColor,
        focusedTrailingIconColor = spec.contentColor,
        unfocusedTrailingIconColor = spec.contentColor
    )
}

@Composable
fun vndbOutlinedTextFieldColors(theme: ThemeCode): TextFieldColors =
    vndbOutlinedTextFieldColors(ThemeId.fromCode(theme.code).toThemeSpec())

fun ThemeSpec.toMaterialColorScheme(): ColorScheme = if (hasDarkSurface) {
    darkColorScheme(
        primary = accentColor,
        onPrimary = onAccentColor,
        primaryContainer = accentColor.copy(alpha = 0.25f),
        onPrimaryContainer = contentColor,
        secondary = accentColor,
        onSecondary = onAccentColor,
        secondaryContainer = accentColor,
        onSecondaryContainer = onAccentColor,
        tertiary = contentColor,
        onTertiary = surfaceColor,
        background = surfaceColor,
        onBackground = contentColor,
        surface = surfaceColor,
        onSurface = contentColor,
        surfaceVariant = contentColor.copy(alpha = 0.14f),
        onSurfaceVariant = contentColor.copy(alpha = 0.90f),
        outline = contentColor.copy(alpha = 0.55f),
        outlineVariant = contentColor.copy(alpha = 0.30f),
        surfaceContainerLowest = surfaceColor,
        surfaceContainerLow = surfaceColor,
        surfaceContainer = surfaceColor,
        surfaceContainerHigh = surfaceColor,
        surfaceContainerHighest = surfaceColor
    )
} else {
    lightColorScheme(
        primary = accentColor,
        onPrimary = onAccentColor,
        primaryContainer = accentColor.copy(alpha = 0.25f),
        onPrimaryContainer = contentColor,
        secondary = accentColor,
        onSecondary = onAccentColor,
        secondaryContainer = accentColor,
        onSecondaryContainer = onAccentColor,
        tertiary = contentColor,
        onTertiary = surfaceColor,
        background = surfaceColor,
        onBackground = contentColor,
        surface = surfaceColor,
        onSurface = contentColor,
        surfaceVariant = contentColor.copy(alpha = 0.12f),
        onSurfaceVariant = contentColor.copy(alpha = 0.88f),
        outline = contentColor.copy(alpha = 0.55f),
        outlineVariant = contentColor.copy(alpha = 0.30f),
        surfaceContainerLowest = surfaceColor,
        surfaceContainerLow = surfaceColor,
        surfaceContainer = surfaceColor,
        surfaceContainerHigh = surfaceColor,
        surfaceContainerHighest = surfaceColor
    )
}

@Composable
fun VndbTheme(
    themeId: ThemeId = ThemeId.DEFAULT,
    fontSizeOffset: Float = 0f,
    content: @Composable () -> Unit
) {
    val themeSpec = themeId.toThemeSpec()
    val legacyCode = ThemeCode.fromCode(themeId.code)
    val isDarkSurface = themeSpec.hasDarkSurface

    val view = LocalView.current
    if (!LocalInspectionMode.current && !view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !isDarkSurface
            insetsController.isAppearanceLightNavigationBars = !isDarkSurface
        }
    }

    val colorScheme = themeSpec.toMaterialColorScheme()
    val typography = buildVndbTypography(fontSizeOffset)

    CompositionLocalProvider(
        LocalThemeSpec provides themeSpec,
        LocalAppThemeCode provides legacyCode,
        LocalFontScaleOffset provides fontSizeOffset
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography
        ) {
            AppBackground(themeSpec = themeSpec) {
                content()
            }
        }
    }
}

@Composable
fun VndbTheme(
    themeCode: ThemeCode,
    fontSizeOffset: Float = 0f,
    content: @Composable () -> Unit
) {
    VndbTheme(
        themeId = ThemeId.fromCode(themeCode.code),
        fontSizeOffset = fontSizeOffset,
        content = content
    )
}
