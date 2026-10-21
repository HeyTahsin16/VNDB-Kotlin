package com.vndblite.dcj.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import coil.transform.Transformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

/**
 * Bitmap-level NSFW blur transformation that works across all Android API levels (API 24-36),
 * ensuring NSFW covers, screenshots, and character images are blurred even on devices where
 * RenderEffect (Modifier.blur) is unavailable or subtle.
 */
class NsfwBlurTransformation(
    private val downsampleFactor: Int = 14
) : Transformation {
    override val cacheKey: String = "nsfw_blur_v1_$downsampleFactor"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val w = max(1, input.width)
        val h = max(1, input.height)
        val smallW = max(4, w / downsampleFactor)
        val smallH = max(4, h / downsampleFactor)
        val midW = max(2, smallW / 2)
        val midH = max(2, smallH / 2)

        val pass1 = Bitmap.createScaledBitmap(input, smallW, smallH, true)
        val pass2 = Bitmap.createScaledBitmap(pass1, midW, midH, true)
        val pass3 = Bitmap.createScaledBitmap(pass2, smallW, smallH, true)
        val output = Bitmap.createBitmap(w, h, input.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        canvas.drawBitmap(
            pass3,
            null,
            android.graphics.Rect(0, 0, w, h),
            paint
        )
        if (pass1 != input && !pass1.isRecycled) pass1.recycle()
        if (pass2 != pass1 && !pass2.isRecycled) pass2.recycle()
        if (pass3 != pass2 && !pass3.isRecycled) pass3.recycle()
        return output
    }
}

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
    val request = remember(imageUrl, shouldBlur, context) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .crossfade(true)
            .apply {
                if (shouldBlur) {
                    transformations(NsfwBlurTransformation())
                }
            }
            .build()
    }

    Box(modifier = modifier) {
        AsyncImage(
            model = request,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = Modifier
                .fillMaxSize()
                .then(if (shouldBlur) Modifier.blur(blurRadius) else Modifier)
        )
        if (shouldBlur && scrimAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha))
            )
        }
    }
}

private object AssetBitmapCache {
    private val cache = LruCache<String, ImageBitmap>(120)

    fun get(path: String): ImageBitmap? = cache.get(path)

    fun loadSync(context: Context, rawPath: String): ImageBitmap? {
        val cleanPath = rawPath.removePrefix("assets/").removePrefix("/")
        cache.get(cleanPath)?.let { return it }
        return runCatching {
            context.assets.open(cleanPath).use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()?.also { bmp ->
                    cache.put(cleanPath, bmp)
                }
            }
        }.getOrNull()
    }
}

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
    val cleanPath = remember(assetPath) { assetPath.removePrefix("assets/").removePrefix("/") }
    var bitmap by remember(cleanPath) { mutableStateOf(AssetBitmapCache.get(cleanPath)) }

    LaunchedEffect(cleanPath) {
        if (bitmap == null) {
            bitmap = withContext(Dispatchers.IO) {
                AssetBitmapCache.loadSync(context, cleanPath)
            }
        }
    }

    val currentBmp = bitmap
    if (currentBmp != null) {
        Image(
            bitmap = currentBmp,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
            alpha = alpha,
            colorFilter = colorFilter
        )
    } else {
        Box(modifier = modifier)
    }
}
