package com.vndblite.dcj.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.domain.model.DevStatus

/**
 * Semantic color tokens for VNDB Kotlin so no screen or component outside `ui/theme/`
 * uses raw `Color(0x...)` or `Color.Black`/`Color.White` literals (U2, A8).
 */
@Immutable
data class VndbSemanticColors(
    val statusPlaying: Color = Color(0xFF81C784),
    val statusFinished: Color = Color(0xFF64B5F6),
    val statusStalled: Color = Color(0xFFFFB74D),
    val statusDropped: Color = Color(0xFFE57373),
    val statusWishlist: Color = Color(0xFFCE93D8),
    val statusBlacklist: Color = Color(0xFF9E9E9E),
    val onStatusBadge: Color = Color(0xFF000000),
    val devFinished: Color = Color(0xFF81C784),
    val devInDevelopment: Color = Color(0xFFFFB74D),
    val devCancelled: Color = Color(0xFFE57373),
    val starGold: Color = Color(0xFFFFD54F),
    val danger: Color = Color(0xFFFF5252),
    val onDanger: Color = Color(0xFFFFFFFF),
    val success: Color = Color(0xFF69F0AE),
    val warning: Color = Color(0xFFFFB74D),
    val onAccentButton: Color = Color(0xFF000000),
    val nsfwBadgeTint: Color = Color(0xFFFF8A80),
    val nsfwBadgeContainer: Color = Color(0xB8000000),
    val nsfwBadgeContent: Color = Color(0xFFFFFFFF),
    val nsfwScrim: Color = Color(0x5C000000),
    val cardGradientStart: Color = Color(0x00000000),
    val cardGradientMiddle: Color = Color(0xB8000000),
    val cardGradientEnd: Color = Color(0xEB000000),
    val cardOverlayText: Color = Color(0xFFFFFFFF),
    val cardOverlaySubtleText: Color = Color(0xE6FFFFFF),
    val modalScrim: Color = Color(0x99000000),
    val transparent: Color = Color(0x00000000)
) {
    fun statusColor(status: CollectionStatus): Color = when (status) {
        CollectionStatus.PLAYING -> statusPlaying
        CollectionStatus.FINISHED -> statusFinished
        CollectionStatus.STALLED -> statusStalled
        CollectionStatus.DROPPED -> statusDropped
        CollectionStatus.WISHLIST -> statusWishlist
        CollectionStatus.BLACKLIST -> statusBlacklist
    }

    fun devStatusColor(devStatus: DevStatus): Color = when (devStatus) {
        DevStatus.FINISHED -> devFinished
        DevStatus.IN_DEVELOPMENT -> devInDevelopment
        DevStatus.CANCELLED -> devCancelled
    }
}

val DefaultVndbSemanticColors = VndbSemanticColors()

val LocalVndbSemanticColors = staticCompositionLocalOf { DefaultVndbSemanticColors }

object VndbThemeTokens {
    val colors: VndbSemanticColors
        @Composable
        @ReadOnlyComposable
        get() = LocalVndbSemanticColors.current

    val themeSpec: ThemeSpec
        @Composable
        @ReadOnlyComposable
        get() = LocalThemeSpec.current
}
