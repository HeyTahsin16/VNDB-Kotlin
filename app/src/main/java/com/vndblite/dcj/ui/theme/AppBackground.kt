package com.vndblite.dcj.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.vndblite.dcj.domain.model.ThemeId
import com.vndblite.dcj.ui.components.AssetImage
import com.vndblite.dcj.ui.components.AssetPaths

/**
 * Dedicated background composable that renders the selected theme's surface color and
 * background illustration behind [content], decoupled from [VndbTheme] (U2, U4).
 */
@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    themeId: ThemeId = LocalThemeId.current,
    imageAlpha: Float = 0.88f,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AssetImage(
            assetPath = AssetPaths.themeBackground(themeId),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = imageAlpha,
            modifier = Modifier.fillMaxSize()
        )
        content()
    }
}
