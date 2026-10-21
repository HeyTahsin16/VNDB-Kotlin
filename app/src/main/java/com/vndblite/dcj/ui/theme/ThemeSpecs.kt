package com.vndblite.dcj.ui.theme

import androidx.annotation.StringRes
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.vndblite.dcj.R
import com.vndblite.dcj.domain.model.ThemeId
import com.vndblite.dcj.ui.components.AssetPaths
import kotlin.math.max
import kotlin.math.min

/**
 * WCAG 2.x relative luminance crossover point (~0.179) where white foreground text
 * achieves higher contrast than black foreground text (B7, F7, U2).
 */
const val WCAG_DARK_SURFACE_LUMINANCE_THRESHOLD: Float = 0.179f

/**
 * Minimum WCAG AA contrast ratio required for body text on theme surfaces (U2).
 */
const val MIN_WCAG_AA_CONTRAST_RATIO: Float = 4.5f

/**
 * Presentation specification for a [ThemeId] (D1, F7, B7, U2, A8).
 * Keeps Compose [Color], string resource IDs, and asset URIs out of the pure domain model.
 */
@Immutable
data class ThemeSpec(
    val id: ThemeId,
    val displayName: String,
    @StringRes val displayNameResId: Int,
    val backgroundAssetUri: String,
    val surfaceColor: Color,
    val accentColor: Color,
    val onSurfaceColor: Color
) {
    /**
     * Single source of truth for whether this theme's surface is dark, derived from
     * its opaque surface relative luminance (B7, F7).
     */
    val hasDarkSurface: Boolean =
        surfaceColor.copy(alpha = 1f).luminance() < WCAG_DARK_SURFACE_LUMINANCE_THRESHOLD

    val backgroundAssetPath: String
        get() = backgroundAssetUri

    val paletteLabel: String
        get() = if (hasDarkSurface) "Dark Palette" else "Light Palette"

    @get:StringRes
    val paletteLabelResId: Int
        get() = if (hasDarkSurface) R.string.settings_palette_dark else R.string.settings_palette_light

    /**
     * Computes the WCAG 2.x contrast ratio between [onSurfaceColor] and [surfaceColor] (U2).
     */
    fun surfaceTextContrastRatio(): Float =
        ThemeSpecs.calculateContrastRatio(foreground = onSurfaceColor, background = surfaceColor)

    /**
     * Computes the WCAG 2.x contrast ratio between `onSurfaceVariant` (composited over [surfaceColor])
     * and [surfaceColor] (U2).
     */
    fun surfaceVariantTextContrastRatio(): Float {
        val variantAlpha = if (hasDarkSurface) 0.90f else 0.88f
        val composited = onSurfaceColor.copy(alpha = variantAlpha).compositeOver(surfaceColor.copy(alpha = 1f))
        return ThemeSpecs.calculateContrastRatio(foreground = composited, background = surfaceColor)
    }
}

/**
 * Registry of all 11 VNDB Kotlin theme specifications and the single unified [ColorScheme] builder (D1, F7, B7, U2, A9).
 */
object ThemeSpecs {

    private val DarkOnSurface = Color(0xFFFFFFFF)
    private val LightOnSurface = Color(0xFF000000)

    val all: List<ThemeSpec> = listOf(
        ThemeSpec(
            id = ThemeId.AGL,
            displayName = "Angel Serenade",
            displayNameResId = R.string.theme_name_agl,
            backgroundAssetUri = AssetPaths.themeBackground(ThemeId.AGL),
            surfaceColor = Color(0xFF325064),
            accentColor = Color(0xFF8CDCFF),
            onSurfaceColor = DarkOnSurface
        ),
        ThemeSpec(
            id = ThemeId.AIR,
            displayName = "AIR",
            displayNameResId = R.string.theme_name_air,
            backgroundAssetUri = AssetPaths.themeBackground(ThemeId.AIR),
            surfaceColor = Color(0xFFB4DCFF),
            accentColor = Color(0xFF5082E1),
            onSurfaceColor = LightOnSurface
        ),
        ThemeSpec(
            id = ThemeId.EV17,
            displayName = "Ever17",
            displayNameResId = R.string.theme_name_ev17,
            backgroundAssetUri = AssetPaths.themeBackground(ThemeId.EV17),
            surfaceColor = Color(0xFF6EC8D2),
            accentColor = Color(0xFFF0AA64),
            onSurfaceColor = LightOnSurface
        ),
        ThemeSpec(
            id = ThemeId.FATE,
            displayName = "Fate/Stay Night",
            displayNameResId = R.string.theme_name_fate,
            backgroundAssetUri = AssetPaths.themeBackground(ThemeId.FATE),
            surfaceColor = Color(0xFF463232),
            accentColor = Color(0xFFD26446),
            onSurfaceColor = DarkOnSurface
        ),
        ThemeSpec(
            id = ThemeId.CARNVL,
            displayName = "Gekkou No Carnevale",
            displayNameResId = R.string.theme_name_carnvl,
            backgroundAssetUri = AssetPaths.themeBackground(ThemeId.CARNVL),
            surfaceColor = Color(0xFF141414),
            accentColor = Color(0xFFBEBEBE),
            onSurfaceColor = DarkOnSurface
        ),
        ThemeSpec(
            id = ThemeId.HGRSHI,
            displayName = "Higurashi No Naku Koro Ni",
            displayNameResId = R.string.theme_name_hgrshi,
            backgroundAssetUri = AssetPaths.themeBackground(ThemeId.HGRSHI),
            surfaceColor = Color(0xFFF5B4AF),
            accentColor = Color(0xFFDCDCDC),
            onSurfaceColor = LightOnSurface
        ),
        ThemeSpec(
            id = ThemeId.TSUKHME,
            displayName = "Tsukihime",
            displayNameResId = R.string.theme_name_tsukhme,
            backgroundAssetUri = AssetPaths.themeBackground(ThemeId.TSUKHME),
            surfaceColor = Color(0xFF645050),
            accentColor = Color(0xFFFA4646),
            onSurfaceColor = DarkOnSurface
        ),
        ThemeSpec(
            id = ThemeId.TOHO,
            displayName = "Touhou",
            displayNameResId = R.string.theme_name_toho,
            backgroundAssetUri = AssetPaths.themeBackground(ThemeId.TOHO),
            surfaceColor = Color(0xFFAFAFAF),
            accentColor = Color(0xFFDCD7D7),
            onSurfaceColor = LightOnSurface
        ),
        ThemeSpec(
            id = ThemeId.SEINRKN,
            displayName = "Seinarukana",
            displayNameResId = R.string.theme_name_seinrkn,
            backgroundAssetUri = AssetPaths.themeBackground(ThemeId.SEINRKN),
            surfaceColor = Color(0xFFFFF0F0),
            accentColor = Color(0xFFFF4696),
            onSurfaceColor = LightOnSurface
        ),
        ThemeSpec(
            id = ThemeId.LILBUSTS,
            displayName = "Little Busters!",
            displayNameResId = R.string.theme_name_lilbusts,
            backgroundAssetUri = AssetPaths.themeBackground(ThemeId.LILBUSTS),
            surfaceColor = Color(0xFFFAC8FA),
            accentColor = Color(0xFFF050D7),
            onSurfaceColor = LightOnSurface
        ),
        ThemeSpec(
            id = ThemeId.SAYA,
            displayName = "Saya No Uta",
            displayNameResId = R.string.theme_name_saya,
            backgroundAssetUri = AssetPaths.themeBackground(ThemeId.SAYA),
            surfaceColor = Color(0xFF326450),
            accentColor = Color(0xFFF0B48C),
            onSurfaceColor = DarkOnSurface
        )
    )

    private val byId: Map<ThemeId, ThemeSpec> = all.associateBy { it.id }

    fun forId(themeId: ThemeId): ThemeSpec = byId[themeId] ?: all.first()

    /**
     * Single function building both dark and light Material 3 [ColorScheme]s from [spec]
     * without duplicating property assignments (U2, A9).
     */
    fun buildColorScheme(spec: ThemeSpec): ColorScheme {
        val surface = spec.surfaceColor
        val accent = spec.accentColor
        val onSurface = spec.onSurfaceColor
        val surfaceVariantAlpha = if (spec.hasDarkSurface) 0.14f else 0.12f
        val onSurfaceVariantAlpha = if (spec.hasDarkSurface) 0.90f else 0.88f

        val baseScheme = if (spec.hasDarkSurface) darkColorScheme() else lightColorScheme()
        return baseScheme.copy(
            primary = accent,
            onPrimary = LightOnSurface,
            primaryContainer = accent.copy(alpha = 0.25f),
            onPrimaryContainer = onSurface,
            secondary = accent,
            onSecondary = LightOnSurface,
            secondaryContainer = accent,
            onSecondaryContainer = LightOnSurface,
            tertiary = onSurface,
            onTertiary = surface,
            background = surface,
            onBackground = onSurface,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = onSurface.copy(alpha = surfaceVariantAlpha),
            onSurfaceVariant = onSurface.copy(alpha = onSurfaceVariantAlpha),
            outline = onSurface.copy(alpha = 0.55f),
            outlineVariant = onSurface.copy(alpha = 0.30f),
            surfaceContainerLowest = surface,
            surfaceContainerLow = surface,
            surfaceContainer = surface,
            surfaceContainerHigh = surface,
            surfaceContainerHighest = surface
        )
    }

    /**
     * Calculates the WCAG 2.x contrast ratio `(L_lighter + 0.05) / (L_darker + 0.05)` (U2).
     */
    fun calculateContrastRatio(foreground: Color, background: Color): Float {
        val lum1 = foreground.copy(alpha = 1f).luminance()
        val lum2 = background.copy(alpha = 1f).luminance()
        val lighter = max(lum1, lum2)
        val darker = min(lum1, lum2)
        return (lighter + 0.05f) / (darker + 0.05f)
    }
}

val LocalThemeId = staticCompositionLocalOf { ThemeId.DEFAULT }
val LocalThemeSpec = staticCompositionLocalOf { ThemeSpecs.forId(ThemeId.DEFAULT) }
