package com.vndblite.dcj.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Centralized color tokens for VNDB Kotlin (D1).
 * Raw hex colors, pure black, and pure white are defined exclusively inside `ui/theme/`.
 */
object ColorTokens {
    val PureWhite = Color(0xFFFFFFFF)
    val PureBlack = Color(0xFF000000)
    val Transparent = Color(0x00000000)

    // Theme surface & accent palettes
    val AglSurface = Color(0xFF325064)
    val AglAccent = Color(0xFF8CDCFF)

    val AirSurface = Color(0xFFB4DCFF)
    val AirAccent = Color(0xFF5082E1)

    val Ev17Surface = Color(0xFF6EC8D2)
    val Ev17Accent = Color(0xFFF0AA64)

    val FateSurface = Color(0xFF463232)
    val FateAccent = Color(0xFFD26446)

    val CarnvlSurface = Color(0xFF141414)
    val CarnvlAccent = Color(0xFFBEBEBE)

    val HgrshiSurface = Color(0xFFF5B4AF)
    val HgrshiAccent = Color(0xFFDCDCDC)

    val TsukhmeSurface = Color(0xFF645050)
    val TsukhmeAccent = Color(0xFFFA4646)

    val TohoSurface = Color(0xFFAFAFAF)
    val TohoAccent = Color(0xFFDCD7D7)

    val SeinrknSurface = Color(0xFFFFF0F0)
    val SeinrknAccent = Color(0xFFFF4696)

    val LilbustsSurface = Color(0xFFFAC8FA)
    val LilbustsAccent = Color(0xFFF050D7)

    val SayaSurface = Color(0xFF326450)
    val SayaAccent = Color(0xFFF0B48C)

    // Collection status badge colors
    val StatusPlaying = Color(0xFF00C3FF)
    val StatusFinished = Color(0xFF14E614)
    val StatusStalled = Color(0xFFFA9600)
    val StatusDropped = Color(0xFFFF3C32)
    val StatusWishlist = Color(0xFFFFE600)
    val StatusBlacklist = Color(0xFFBE3737)

    // Development status colors
    val DevFinished = Color(0xFF4CAF50)
    val DevInDevelopment = Color(0xFFFFEB3B)
    val DevCancelled = Color(0xFFF44336)
    val DevUnknown = Color(0xFF9E9E9E)

    // Semantic accents & overlays
    val RatingStarGold = Color(0xFFFFD54F)
    val NsfwWarningTint = Color(0xFFFF8A80)
    val DestructiveRed = Color(0xFFFF5252)
    val SuccessGreen = Color(0xFF69F0AE)
    val WarningAmber = Color(0xFFFFD740)

    val ScrimDarkLight = Color(0xB8000000)
    val ScrimDarkHeavy = Color(0xEB000000)
    val CardOverlayText = Color(0xFFFFFFFF)
    val CardOverlaySubtext = Color(0xE6FFFFFF)
    val BadgeDarkText = Color(0xFF000000)
}
