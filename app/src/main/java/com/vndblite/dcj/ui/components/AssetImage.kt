package com.vndblite.dcj.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import coil.transform.Transformation
import com.vndblite.dcj.core.AppLog
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Single-pass bitmap downsample transformation used only on Android < 12 (API < 31) where
 * hardware RenderEffect (`Modifier.blur`) is unavailable. On API 31+, `Modifier.blur` is used alone.
 */
class NsfwBlurTransformation(
    private val downsampleFactor: Int = DEFAULT_DOWNSAMPLE_FACTOR
) : Transformation {
    override val cacheKey: String = "nsfw_blur_single_pass_$downsampleFactor"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val width = max(1, input.width)
        val height = max(1, input.height)
        val sampledWidth = max(4, width / downsampleFactor.coerceAtLeast(2))
        val sampledHeight = max(4, height / downsampleFactor.coerceAtLeast(2))

        val downsampled = Bitmap.createScaledBitmap(input, sampledWidth, sampledHeight, true)
        val output = Bitmap.createBitmap(width, height, input.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        canvas.drawBitmap(downsampled, null, Rect(0, 0, width, height), paint)
        if (downsampled != input && !downsampled.isRecycled) {
            downsampled.recycle()
        }
        return output
    }

    companion object {
        const val DEFAULT_DOWNSAMPLE_FACTOR: Int = 16
    }
}

@Composable
fun NsfwAsyncImage(
    imageUrl: String,
    contentDescription: String?,
    shouldBlur: Boolean,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    blurRadius: Dp = 24.dp,
    scrimAlpha: Float = 0.28f
) {
    val context = LocalContext.current
    val useHardwareBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val request = remember(imageUrl, shouldBlur, useHardwareBlur, context) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .crossfade(true)
            .apply {
                if (shouldBlur && !useHardwareBlur) {
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
                .then(
                    if (shouldBlur && useHardwareBlur) {
                        Modifier.blur(blurRadius)
                    } else {
                        Modifier
                    }
                )
        )
        if (shouldBlur && scrimAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = scrimAlpha))
            )
        }
    }
}

internal object AssetBitmapCache {
    private const val TAG = "AssetImage"
    private const val MAX_CACHE_BYTES: Int = 16 * 1024 * 1024 // 16 MiB byte-bounded cache
    private const val DEFAULT_MAX_DIMENSION_PX: Int = 1080

    private val loggedFailures = ConcurrentHashMap.newKeySet<String>()

    private val byteCache = object : LruCache<String, Bitmap>(MAX_CACHE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int =
            value.allocationByteCount.coerceAtLeast(1)
    }

    fun normalizePath(rawPath: String): String =
        rawPath.trim().removePrefix("file:///android_asset/").removePrefix("assets/").removePrefix("/")

    fun getCached(cleanPath: String, bucketPx: Int): ImageBitmap? {
        val key = cacheKey(cleanPath, bucketPx)
        return synchronized(byteCache) { byteCache.get(key) }?.asImageBitmap()
    }

    fun decodeSizeAware(
        context: Context,
        rawPath: String,
        reqWidthPx: Int,
        reqHeightPx: Int
    ): ImageBitmap? {
        val cleanPath = normalizePath(rawPath)
        if (cleanPath.isEmpty()) {
            logMissingOnce("<empty>", "Asset path was blank")
            return null
        }
        val targetMaxPx = max(reqWidthPx, reqHeightPx)
            .takeIf { it in 1..DEFAULT_MAX_DIMENSION_PX }
            ?: DEFAULT_MAX_DIMENSION_PX
        val bucketPx = quantizeBucket(targetMaxPx)
        val key = cacheKey(cleanPath, bucketPx)

        synchronized(byteCache) {
            byteCache.get(key)?.let { return it.asImageBitmap() }
        }

        return try {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.assets.open(cleanPath).use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }
            if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
                logMissingOnce(cleanPath, "Undecodable image bounds (${boundsOptions.outWidth}x${boundsOptions.outHeight})")
                return null
            }

            val sampleSize = calculateInSampleSize(
                outWidth = boundsOptions.outWidth,
                outHeight = boundsOptions.outHeight,
                reqWidth = bucketPx,
                reqHeight = bucketPx
            )
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = context.assets.open(cleanPath).use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            }
            if (decoded == null) {
                logMissingOnce(cleanPath, "BitmapFactory.decodeStream returned null")
                return null
            }
            synchronized(byteCache) {
                byteCache.put(key, decoded)
            }
            decoded.asImageBitmap()
        } catch (e: IOException) {
            logMissingOnce(cleanPath, "Missing or unreadable asset: ${e.message}", e)
            null
        } catch (e: IllegalArgumentException) {
            logMissingOnce(cleanPath, "Invalid bitmap argument: ${e.message}", e)
            null
        }
    }

    internal fun calculateInSampleSize(
        outWidth: Int,
        outHeight: Int,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        var inSampleSize = 1
        if (outHeight > reqHeight || outWidth > reqWidth) {
            val halfHeight = outHeight / 2
            val halfWidth = outWidth / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }

    private fun quantizeBucket(px: Int): Int = when {
        px <= 96 -> 96
        px <= 256 -> 256
        px <= 512 -> 512
        else -> DEFAULT_MAX_DIMENSION_PX
    }

    private fun cacheKey(cleanPath: String, bucketPx: Int): String = "$cleanPath@$bucketPx"

    private fun logMissingOnce(
        cleanPath: String,
        reason: String,
        throwable: Throwable? = null
    ) {
        if (loggedFailures.add(cleanPath)) {
            AppLog.w(TAG, "Failed to load bundled asset '$cleanPath': $reason", throwable)
        }
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
    val density = LocalDensity.current
    val cleanPath = remember(assetPath) { AssetBitmapCache.normalizePath(assetPath) }

    BoxWithConstraints(modifier = modifier) {
        val reqWidthPx = remember(maxWidth, density) {
            if (maxWidth.value.isFinite() && maxWidth > 0.dp) {
                with(density) { maxWidth.toPx().roundToInt() }.coerceAtLeast(1)
            } else {
                0
            }
        }
        val reqHeightPx = remember(maxHeight, density) {
            if (maxHeight.value.isFinite() && maxHeight > 0.dp) {
                with(density) { maxHeight.toPx().roundToInt() }.coerceAtLeast(1)
            } else {
                0
            }
        }
        val bucketPx = remember(reqWidthPx, reqHeightPx) {
            val maxDim = max(reqWidthPx, reqHeightPx)
            when {
                maxDim in 1..96 -> 96
                maxDim in 97..256 -> 256
                maxDim in 257..512 -> 512
                else -> 1080
            }
        }

        var bitmap by remember(cleanPath, bucketPx) {
            mutableStateOf(AssetBitmapCache.getCached(cleanPath, bucketPx))
        }
        var loadAttempted by remember(cleanPath, bucketPx) {
            mutableStateOf(bitmap != null)
        }

        LaunchedEffect(cleanPath, reqWidthPx, reqHeightPx) {
            if (bitmap == null) {
                bitmap = withContext(Dispatchers.IO) {
                    AssetBitmapCache.decodeSizeAware(
                        context = context,
                        rawPath = cleanPath,
                        reqWidthPx = reqWidthPx,
                        reqHeightPx = reqHeightPx
                    )
                }
                loadAttempted = true
            }
        }

        val currentBmp = bitmap
        if (currentBmp != null) {
            Image(
                bitmap = currentBmp,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                alpha = alpha,
                colorFilter = colorFilter
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(
                            alpha = if (loadAttempted) 0.28f else 0.14f
                        )
                    )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AssetImagePlaceholderPreview() {
    MaterialTheme {
        AssetImage(
            assetPath = "images/flags/ja.png",
            contentDescription = null,
            modifier = Modifier
        )
    }
}
