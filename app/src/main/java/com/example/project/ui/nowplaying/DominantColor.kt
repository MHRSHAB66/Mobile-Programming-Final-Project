package com.example.project.ui.nowplaying

import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Extracts the dominant/vibrant colour from cover art via the Palette API for the Now Playing
 * gradient. Falls back to a provided colour when extraction isn't possible, so the screen is
 * always stable even if the image fails to load.
 */
@Composable
fun rememberDominantColor(url: String?, fallback: Color): State<Color> {
    val context = LocalContext.current
    val colorState = remember(url) { mutableStateOf(fallback) }

    LaunchedEffect(url, fallback) {
        if (url.isNullOrBlank()) {
            colorState.value = fallback
            return@LaunchedEffect
        }
        val extracted = withContext(Dispatchers.IO) {
            runCatching {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .allowHardware(false)
                    .size(128)
                    .build()
                val result = context.imageLoader.execute(request)
                val bitmap = (result as? SuccessResult)?.drawable as? BitmapDrawable
                bitmap?.bitmap?.let { bmp ->
                    val palette = Palette.from(bmp).generate()
                    (palette.vibrantSwatch ?: palette.mutedSwatch ?: palette.dominantSwatch)?.rgb
                }
            }.getOrNull()
        }
        if (extracted != null) colorState.value = Color(extracted)
    }
    return colorState
}
