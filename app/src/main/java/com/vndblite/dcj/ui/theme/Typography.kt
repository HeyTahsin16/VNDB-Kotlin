package com.vndblite.dcj.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Builds a complete 15-style Material 3 [Typography] scaled by [fontSizeOffset] (D1).
 */
fun buildVndbTypography(fontSizeOffset: Float = 0f): Typography {
    val d = fontSizeOffset.coerceIn(-4f, 8f)
    fun scaledSp(base: Float) = (base + d).coerceAtLeast(8f).sp

    return Typography(
        displayLarge = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = scaledSp(34f),
            lineHeight = scaledSp(40f)
        ),
        displayMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = scaledSp(28f),
            lineHeight = scaledSp(34f)
        ),
        displaySmall = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = scaledSp(24f),
            lineHeight = scaledSp(30f)
        ),
        headlineLarge = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = scaledSp(24f),
            lineHeight = scaledSp(30f)
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
            lineHeight = scaledSp(25f)
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
            fontSize = scaledSp(14.5f),
            lineHeight = scaledSp(21f)
        ),
        bodyMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = scaledSp(13f),
            lineHeight = scaledSp(19f)
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
