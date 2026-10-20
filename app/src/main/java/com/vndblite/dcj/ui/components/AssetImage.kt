package com.vndblite.dcj.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
