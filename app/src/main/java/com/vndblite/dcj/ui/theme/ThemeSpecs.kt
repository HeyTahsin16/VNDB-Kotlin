package com.vndblite.dcj.ui.theme

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.vndblite.dcj.R
import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.domain.model.DevStatus
import com.vndblite.dcj.domain.model.ThemeId
import com.vndblite.dcj.ui.components.AssetPaths
import kotlin.math.max
import kotlin.math.min

/**
 * WCAG 2.x relative luminance crossover point (~0.179) where white foreground text
 * achieves higher contrast than black foreground text (B7, F7).
 */
const val WCAG_DARK_SURFACE_LUMINANCE_THRESHOLD: Float = 0.179f

data class ThemeSpec(
    val themeId: ThemeId,
    val backgroundAssetPath: String,
    val surfaceColor: Color,
    val accentColor: Color,
    val contentColor: Color
) {
    val hasDarkSurface: Boolean
        get() = surfaceColor.copy(alpha = 1f).luminance() < WCAG_DARK_SURFACE_LUMINANCE_THRESHOLD

    val onAccentColor: Color
        get() = if (accentColor.copy(alpha = 1f).luminance() < WCAG_DARK_SURFACE_LUMINANCE_THRESHOLD) {
            ColorTokens.PureWhite
        } else {
            ColorTokens.PureBlack
        }

    @get:StringRes
    val paletteLabelRes: Int
        get() = if (hasDarkSurface) R.string.theme_palette_dark else R.string.theme_palette_light

    fun surfaceContentContrastRatio(): Float =
        contrastRatio(surfaceColor.copy(alpha = 1f), contentColor.copy(alpha = 1f))

    companion object {
        fun contrastRatio(first: Color, second: Color): Float {
            val l1 = first.luminance()
            val l2 = second.luminance()
            val lighter = max(l1, l2)
            val darker = min(l1, l2)
            return (lighter + 0.05f) / (darker + 0.05f)
        }
    }
}

object ThemeSpecs {
    private val specsById: Map<ThemeId, ThemeSpec> = mapOf(
        ThemeId.AGL to ThemeSpec(
            themeId = ThemeId.AGL,
            backgroundAssetPath = AssetPaths.themeBackgroundPath(ThemeId.AGL),
            surfaceColor = ColorTokens.AglSurface,
            accentColor = ColorTokens.AglAccent,
            contentColor = ColorTokens.PureWhite
        ),
        ThemeId.AIR to ThemeSpec(
            themeId = ThemeId.AIR,
            backgroundAssetPath = AssetPaths.themeBackgroundPath(ThemeId.AIR),
            surfaceColor = ColorTokens.AirSurface,
            accentColor = ColorTokens.AirAccent,
            contentColor = ColorTokens.PureBlack
        ),
        ThemeId.EV17 to ThemeSpec(
            themeId = ThemeId.EV17,
            backgroundAssetPath = AssetPaths.themeBackgroundPath(ThemeId.EV17),
            surfaceColor = ColorTokens.Ev17Surface,
            accentColor = ColorTokens.Ev17Accent,
            contentColor = ColorTokens.PureBlack
        ),
        ThemeId.FATE to ThemeSpec(
            themeId = ThemeId.FATE,
            backgroundAssetPath = AssetPaths.themeBackgroundPath(ThemeId.FATE),
            surfaceColor = ColorTokens.FateSurface,
            accentColor = ColorTokens.FateAccent,
            contentColor = ColorTokens.PureWhite
        ),
        ThemeId.CARNVL to ThemeSpec(
            themeId = ThemeId.CARNVL,
            backgroundAssetPath = AssetPaths.themeBackgroundPath(ThemeId.CARNVL),
            surfaceColor = ColorTokens.CarnvlSurface,
            accentColor = ColorTokens.CarnvlAccent,
            contentColor = ColorTokens.PureWhite
        ),
        ThemeId.HGRSHI to ThemeSpec(
            themeId = ThemeId.HGRSHI,
            backgroundAssetPath = AssetPaths.themeBackgroundPath(ThemeId.HGRSHI),
            surfaceColor = ColorTokens.HgrshiSurface,
            accentColor = ColorTokens.HgrshiAccent,
            contentColor = ColorTokens.PureBlack
        ),
        ThemeId.TSUKHME to ThemeSpec(
            themeId = ThemeId.TSUKHME,
            backgroundAssetPath = AssetPaths.themeBackgroundPath(ThemeId.TSUKHME),
            surfaceColor = ColorTokens.TsukhmeSurface,
            accentColor = ColorTokens.TsukhmeAccent,
            contentColor = ColorTokens.PureWhite
        ),
        ThemeId.TOHO to ThemeSpec(
            themeId = ThemeId.TOHO,
            backgroundAssetPath = AssetPaths.themeBackgroundPath(ThemeId.TOHO),
            surfaceColor = ColorTokens.TohoSurface,
            accentColor = ColorTokens.TohoAccent,
            contentColor = ColorTokens.PureBlack
        ),
        ThemeId.SEINRKN to ThemeSpec(
            themeId = ThemeId.SEINRKN,
            backgroundAssetPath = AssetPaths.themeBackgroundPath(ThemeId.SEINRKN),
            surfaceColor = ColorTokens.SeinrknSurface,
            accentColor = ColorTokens.SeinrknAccent,
            contentColor = ColorTokens.PureBlack
        ),
        ThemeId.LILBUSTS to ThemeSpec(
            themeId = ThemeId.LILBUSTS,
            backgroundAssetPath = AssetPaths.themeBackgroundPath(ThemeId.LILBUSTS),
            surfaceColor = ColorTokens.LilbustsSurface,
            accentColor = ColorTokens.LilbustsAccent,
            contentColor = ColorTokens.PureBlack
        ),
        ThemeId.SAYA to ThemeSpec(
            themeId = ThemeId.SAYA,
            backgroundAssetPath = AssetPaths.themeBackgroundPath(ThemeId.SAYA),
            surfaceColor = ColorTokens.SayaSurface,
            accentColor = ColorTokens.SayaAccent,
            contentColor = ColorTokens.PureWhite
        )
    )

    val all: List<ThemeSpec> = ThemeId.entries.map { forId(it) }

    fun forId(themeId: ThemeId): ThemeSpec =
        specsById[themeId] ?: specsById.getValue(ThemeId.DEFAULT)
}

fun ThemeId.toThemeSpec(): ThemeSpec = ThemeSpecs.forId(this)

fun CollectionStatus.badgeColor(): Color = when (this) {
    CollectionStatus.PLAYING -> ColorTokens.StatusPlaying
    CollectionStatus.FINISHED -> ColorTokens.StatusFinished
    CollectionStatus.STALLED -> ColorTokens.StatusStalled
    CollectionStatus.DROPPED -> ColorTokens.StatusDropped
    CollectionStatus.WISHLIST -> ColorTokens.StatusWishlist
    CollectionStatus.BLACKLIST -> ColorTokens.StatusBlacklist
}

fun DevStatus?.badgeColor(): Color = when (this) {
    DevStatus.FINISHED -> ColorTokens.DevFinished
    DevStatus.IN_DEVELOPMENT -> ColorTokens.DevInDevelopment
    DevStatus.CANCELLED -> ColorTokens.DevCancelled
    null -> ColorTokens.DevUnknown
}
