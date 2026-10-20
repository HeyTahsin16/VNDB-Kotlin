package com.vndblite.dcj.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.math.max

/**
 * Builds the complete Material 3 [Typography] with all 15 text styles defined and scaled
 * by the user's [fontSizeOffset] (U2).
 */
fun buildVndbTypography(fontSizeOffset: Float = 0f): Typography {
    val delta = fontSizeOffset.coerceIn(-4f, 8f)
    fun scaledSp(baseSp: Float) = max(8f, baseSp + delta).sp

    return Typography(
        displayLarge = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = scaledSp(36f),
            lineHeight = scaledSp(44f)
        ),
        displayMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = scaledSp(30f),
            lineHeight = scaledSp(38f)
        ),
        displaySmall = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = scaledSp(26f),
            lineHeight = scaledSp(34f)
        ),
        headlineLarge = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = scaledSp(24f),
            lineHeight = scaledSp(32f)
        ),
        headlineMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = scaledSp(22f),
            lineHeight = scaledSp(28f)
        ),
        headlineSmall = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            fontSize = scaledSp(19f),
            lineHeight = scaledSp(26f)
        ),
        titleLarge = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = scaledSp(18f),
            lineHeight = scaledSp(24f)
        ),
        titleMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            fontSize = scaledSp(15f),
            lineHeight = scaledSp(21f)
        ),
        titleSmall = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            fontSize = scaledSp(13.5f),
            lineHeight = scaledSp(19f)
        ),
        bodyLarge = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = scaledSp(14f),
            lineHeight = scaledSp(20f)
        ),
        bodyMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = scaledSp(13f),
            lineHeight = scaledSp(18.5f)
        ),
        bodySmall = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = scaledSp(12f),
            lineHeight = scaledSp(17f)
        ),
        labelLarge = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            fontSize = scaledSp(13f),
            lineHeight = scaledSp(18f)
        ),
        labelMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = scaledSp(11.5f),
            lineHeight = scaledSp(16f)
        ),
        labelSmall = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = scaledSp(10f),
            lineHeight = scaledSp(14f)
        )
    )
}
