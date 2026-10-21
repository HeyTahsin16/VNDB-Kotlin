package com.vndblite.dcj.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.vndblite.dcj.domain.model.ThemeId

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun vndbFilterChipColors(
    spec: ThemeSpec = LocalThemeSpec.current
): SelectableChipColors {
    val semantic = LocalVndbSemanticColors.current
    return FilterChipDefaults.filterChipColors(
        containerColor = spec.onSurfaceColor.copy(alpha = 0.12f),
        labelColor = spec.onSurfaceColor,
        iconColor = spec.accentColor,
        selectedContainerColor = spec.accentColor,
        selectedLabelColor = semantic.onAccentButton,
        selectedLeadingIconColor = semantic.onAccentButton,
        selectedTrailingIconColor = semantic.onAccentButton
    )
}

@Composable
fun vndbFilterChipBorder(
    selected: Boolean,
    spec: ThemeSpec = LocalThemeSpec.current
): BorderStroke {
    return BorderStroke(
        width = 1.dp,
        color = if (selected) spec.accentColor else spec.onSurfaceColor.copy(alpha = 0.50f)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun vndbInputChipColors(
    spec: ThemeSpec = LocalThemeSpec.current
): SelectableChipColors {
    return InputChipDefaults.inputChipColors(
        containerColor = spec.onSurfaceColor.copy(alpha = 0.14f),
        labelColor = spec.onSurfaceColor,
        leadingIconColor = spec.accentColor,
        trailingIconColor = spec.onSurfaceColor,
        selectedContainerColor = spec.accentColor.copy(alpha = 0.28f),
        selectedLabelColor = spec.onSurfaceColor,
        selectedLeadingIconColor = spec.accentColor,
        selectedTrailingIconColor = spec.onSurfaceColor
    )
}

@Composable
fun vndbOutlinedTextFieldColors(
    spec: ThemeSpec = LocalThemeSpec.current
): TextFieldColors {
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = spec.onSurfaceColor,
        unfocusedTextColor = spec.onSurfaceColor,
        focusedContainerColor = spec.onSurfaceColor.copy(alpha = 0.12f),
        unfocusedContainerColor = spec.onSurfaceColor.copy(alpha = 0.08f),
        cursorColor = spec.accentColor,
        focusedBorderColor = spec.accentColor,
        unfocusedBorderColor = spec.onSurfaceColor.copy(alpha = 0.55f),
        focusedLabelColor = spec.accentColor,
        unfocusedLabelColor = spec.onSurfaceColor.copy(alpha = 0.88f),
        focusedPlaceholderColor = spec.onSurfaceColor.copy(alpha = 0.68f),
        unfocusedPlaceholderColor = spec.onSurfaceColor.copy(alpha = 0.68f),
        focusedLeadingIconColor = spec.accentColor,
        unfocusedLeadingIconColor = spec.accentColor,
        focusedTrailingIconColor = spec.onSurfaceColor,
        unfocusedTrailingIconColor = spec.onSurfaceColor
    )
}

/**
 * Primary Material 3 theme wrapper for VNDB Kotlin (R10, B7, F7, U2, A12).
 *
 * - Uses the single [ThemeSpecs.buildColorScheme] builder and 15-style [buildVndbTypography].
 * - Updates status and navigation bar icon appearance dynamically from [ThemeSpec.hasDarkSurface] (B7).
 * - Provides semantic color tokens via [LocalVndbSemanticColors].
 * - Does not draw the background image directly unless [includeBackground] is requested;
 *   screens and the app shell use the separate [AppBackground] composable (U2).
 */
@Composable
fun VndbTheme(
    themeId: ThemeId = ThemeId.DEFAULT,
    fontSizeOffset: Float = 0f,
    includeBackground: Boolean = false,
    content: @Composable () -> Unit
) {
    val spec = remember(themeId) { ThemeSpecs.forId(themeId) }
    val colorScheme = remember(spec) { ThemeSpecs.buildColorScheme(spec) }
    val typography = remember(fontSizeOffset) { buildVndbTypography(fontSizeOffset) }

    val view = LocalView.current
    if (!LocalInspectionMode.current && !view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !spec.hasDarkSurface
            insetsController.isAppearanceLightNavigationBars = !spec.hasDarkSurface
        }
    }

    CompositionLocalProvider(
        LocalThemeId provides themeId,
        LocalThemeSpec provides spec,
        LocalVndbSemanticColors provides DefaultVndbSemanticColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography
        ) {
            if (includeBackground) {
                AppBackground(themeId = themeId) {
                    content()
                }
            } else {
                content()
            }
        }
    }
}
