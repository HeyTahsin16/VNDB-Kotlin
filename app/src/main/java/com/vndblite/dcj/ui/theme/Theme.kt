package com.vndblite.dcj.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.vndblite.dcj.data.model.ThemeCode
import com.vndblite.dcj.ui.components.AssetImage

val LocalAppThemeCode = compositionLocalOf { ThemeCode.AGL }
val LocalFontScaleOffset = compositionLocalOf { 0f }

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun vndbFilterChipColors(theme: ThemeCode = LocalAppThemeCode.current): SelectableChipColors {
    return FilterChipDefaults.filterChipColors(
        containerColor = theme.tertiary.copy(alpha = 0.12f),
        labelColor = theme.tertiary,
        iconColor = theme.secondary,
        selectedContainerColor = theme.secondary,
        selectedLabelColor = Color.Black,
        selectedLeadingIconColor = Color.Black,
        selectedTrailingIconColor = Color.Black
    )
}

@Composable
fun vndbFilterChipBorder(
    selected: Boolean,
    theme: ThemeCode = LocalAppThemeCode.current
): BorderStroke {
    return BorderStroke(
        width = 1.dp,
        color = if (selected) theme.secondary else theme.tertiary.copy(alpha = 0.50f)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun vndbInputChipColors(theme: ThemeCode = LocalAppThemeCode.current): SelectableChipColors {
    return InputChipDefaults.inputChipColors(
        containerColor = theme.tertiary.copy(alpha = 0.14f),
        labelColor = theme.tertiary,
        leadingIconColor = theme.secondary,
        trailingIconColor = theme.tertiary,
        selectedContainerColor = theme.secondary.copy(alpha = 0.28f),
        selectedLabelColor = theme.tertiary,
        selectedLeadingIconColor = theme.secondary,
        selectedTrailingIconColor = theme.tertiary
    )
}

@Composable
fun vndbOutlinedTextFieldColors(theme: ThemeCode = LocalAppThemeCode.current): TextFieldColors {
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = theme.tertiary,
        unfocusedTextColor = theme.tertiary,
        focusedContainerColor = theme.tertiary.copy(alpha = 0.12f),
        unfocusedContainerColor = theme.tertiary.copy(alpha = 0.08f),
        cursorColor = theme.secondary,
        focusedBorderColor = theme.secondary,
        unfocusedBorderColor = theme.tertiary.copy(alpha = 0.55f),
        focusedLabelColor = theme.secondary,
        unfocusedLabelColor = theme.tertiary.copy(alpha = 0.88f),
        focusedPlaceholderColor = theme.tertiary.copy(alpha = 0.68f),
        unfocusedPlaceholderColor = theme.tertiary.copy(alpha = 0.68f),
        focusedLeadingIconColor = theme.secondary,
        unfocusedLeadingIconColor = theme.secondary,
        focusedTrailingIconColor = theme.tertiary,
        unfocusedTrailingIconColor = theme.tertiary
    )
}

@Composable
fun VndbTheme(
    themeCode: ThemeCode,
    fontSizeOffset: Float = 0f,
    content: @Composable () -> Unit
) {
    val isDarkSurface = themeCode.hasDarkSurface

    val view = LocalView.current
    if (!LocalInspectionMode.current && !view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !isDarkSurface
            insetsController.isAppearanceLightNavigationBars = !isDarkSurface
        }
    }

    val colorScheme = if (isDarkSurface) {
        darkColorScheme(
            primary = themeCode.secondary,
            onPrimary = Color.Black,
            primaryContainer = themeCode.secondary.copy(alpha = 0.25f),
            onPrimaryContainer = themeCode.tertiary,
            secondary = themeCode.secondary,
            onSecondary = Color.Black,
            secondaryContainer = themeCode.secondary,
            onSecondaryContainer = Color.Black,
            tertiary = themeCode.tertiary,
            onTertiary = themeCode.primary,
            background = themeCode.primary,
            onBackground = themeCode.tertiary,
            surface = themeCode.primary,
            onSurface = themeCode.tertiary,
            surfaceVariant = themeCode.tertiary.copy(alpha = 0.14f),
            onSurfaceVariant = themeCode.tertiary.copy(alpha = 0.90f),
            outline = themeCode.tertiary.copy(alpha = 0.55f),
            outlineVariant = themeCode.tertiary.copy(alpha = 0.30f),
            surfaceContainerLowest = themeCode.primary,
            surfaceContainerLow = themeCode.primary,
            surfaceContainer = themeCode.primary,
            surfaceContainerHigh = themeCode.primary,
            surfaceContainerHighest = themeCode.primary
        )
    } else {
        lightColorScheme(
            primary = themeCode.secondary,
            onPrimary = Color.Black,
            primaryContainer = themeCode.secondary.copy(alpha = 0.25f),
            onPrimaryContainer = themeCode.tertiary,
            secondary = themeCode.secondary,
            onSecondary = Color.Black,
            secondaryContainer = themeCode.secondary,
            onSecondaryContainer = Color.Black,
            tertiary = themeCode.tertiary,
            onTertiary = themeCode.primary,
            background = themeCode.primary,
            onBackground = themeCode.tertiary,
            surface = themeCode.primary,
            onSurface = themeCode.tertiary,
            surfaceVariant = themeCode.tertiary.copy(alpha = 0.12f),
            onSurfaceVariant = themeCode.tertiary.copy(alpha = 0.88f),
            outline = themeCode.tertiary.copy(alpha = 0.55f),
            outlineVariant = themeCode.tertiary.copy(alpha = 0.30f),
            surfaceContainerLowest = themeCode.primary,
            surfaceContainerLow = themeCode.primary,
            surfaceContainer = themeCode.primary,
            surfaceContainerHigh = themeCode.primary,
            surfaceContainerHighest = themeCode.primary
        )
    }

    val delta = fontSizeOffset
    val typography = Typography(
        headlineMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = (22f + delta).sp
        ),
        titleLarge = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = (18f + delta).sp
        ),
        titleMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            fontSize = (15f + delta).sp
        ),
        bodyLarge = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = (14f + delta).sp
        ),
        bodyMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = (13f + delta).sp
        ),
        labelLarge = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            fontSize = (13f + delta).sp
        ),
        labelMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = (11.5f + delta).sp
        ),
        labelSmall = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = (10f + delta).sp
        )
    )

    CompositionLocalProvider(
        LocalAppThemeCode provides themeCode,
        LocalFontScaleOffset provides fontSizeOffset
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AssetImage(
                    assetPath = themeCode.backgroundImgPath,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    alpha = 0.88f
                )
                content()
            }
        }
    }
}
