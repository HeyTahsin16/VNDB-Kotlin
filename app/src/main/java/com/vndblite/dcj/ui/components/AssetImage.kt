package com.vndblite.dcj.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import coil.transform.Transformation
import com.vndblite.dcj.domain.model.ThemeId
import com.vndblite.dcj.ui.theme.VndbTheme
import com.vndblite.dcj.ui.theme.VndbThemeTokens
import kotlin.math.max

/**
 * Single-pass downsample/upscale Coil [Transformation] used ONLY on Android API < 31
 * where hardware `RenderEffect` (`Modifier.blur`) is unavailable (U3).
 */
class SinglePassDownsampleBlurTransformation(
    private val downsampleFactor: Int = 14
) : Transformation {

    override val cacheKey: String = "nsfw_single_downsample_v2_$downsampleFactor"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val w = max(1, input.width)
        val h = max(1, input.height)
        val smallW = max(4, w / downsampleFactor)
        val smallH = max(4, h / downsampleFactor)

        val downsampled = Bitmap.createScaledBitmap(input, smallW, smallH, true)
        val output = Bitmap.createBitmap(w, h, input.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        canvas.drawBitmap(downsampled, null, Rect(0, 0, w, h), paint)
        if (downsampled != input && !downsampled.isRecycled) {
            downsampled.recycle()
        }
        return output
    }
}

@VisibleForTesting
internal fun shouldUseHardwareModifierBlur(sdkInt: Int = Build.VERSION.SDK_INT): Boolean =
    sdkInt >= Build.VERSION_CODES.S

/**
 * Displays a remote image with optional NSFW censoring using **exactly one** blur mechanism (U3):
 * - `Modifier.blur` on API >= 31 (`Build.VERSION_CODES.S`)
 * - [SinglePassDownsampleBlurTransformation] on API < 31
 * Plus a dark scrim overlay when [shouldBlur] is `true`.
 */
@Composable
fun NsfwAsyncImage(
    imageUrl: String,
    contentDescription: String?,
    shouldBlur: Boolean,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    blurRadius: Dp = 26.dp,
    scrimAlpha: Float = 0.36f
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val useModifierBlur = remember { shouldUseHardwareModifierBlur() }
    val useCoilBlurTransformation = shouldBlur && !useModifierBlur
    val semanticColors = VndbThemeTokens.colors

    val request = remember(imageUrl, useCoilBlurTransformation, context) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .crossfade(true)
            .apply {
                if (useCoilBlurTransformation) {
                    transformations(SinglePassDownsampleBlurTransformation())
                }
            }
            .build()
    }

    Box(modifier = modifier) {
        if (isPreview) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        } else {
            AsyncImage(
                model = request,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (shouldBlur && useModifierBlur) {
                            Modifier.blur(blurRadius)
                        } else {
                            Modifier
                        }
                    )
            )
        }
        if (shouldBlur && scrimAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(semanticColors.nsfwScrim.copy(alpha = scrimAlpha))
            )
        }
    }
}

/**
 * Loads a bundled asset (`file:///android_asset/...`) through Coil with size-aware decoding,
 * replacing the full-resolution `AssetBitmapCache` `LruCache` (U4).
 */
@Composable
fun AssetImage(
    assetPath: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = 1f,
    colorFilter: ColorFilter? = null
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val assetUri = remember(assetPath) { AssetPaths.toCoilAssetUri(assetPath) }

    if (isPreview) {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
        )
        return
    }

    val request = remember(assetUri, context) {
        ImageRequest.Builder(context)
            .data(assetUri)
            .crossfade(false)
            .build()
    }

    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        alpha = alpha,
        colorFilter = colorFilter
    )
}

@Preview(name = "NsfwAsyncImage Blurred - Dark Theme", showBackground = true)
@Composable
private fun NsfwAsyncImageBlurredPreview() {
    VndbTheme(themeId = ThemeId.AGL) {
        NsfwAsyncImage(
            imageUrl = "https://t.vndb.org/cv/00/17.jpg",
            contentDescription = null,
            shouldBlur = true,
            modifier = Modifier.size(width = 120.dp, height = 170.dp)
        )
    }
}

@Preview(name = "AssetImage - Light Theme", showBackground = true)
@Composable
private fun AssetImagePreview() {
    VndbTheme(themeId = ThemeId.AIR) {
        AssetImage(
            assetPath = AssetPaths.APP_ICON,
            contentDescription = null,
            modifier = Modifier.size(48.dp)
        )
    }
}
