package com.vndblite.dcj.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.vndblite.dcj.ui.components.AssetImage

/**
 * Full-screen visual novel theme background container with subtle readability scrim (D1).
 */
@Composable
fun AppBackground(
    themeSpec: ThemeSpec = LocalThemeSpec.current,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(themeSpec.surfaceColor)
    ) {
        AssetImage(
            assetPath = themeSpec.backgroundAssetPath,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = 0.88f
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(themeSpec.surfaceColor.copy(alpha = 0.22f))
        )
        content()
    }
}
