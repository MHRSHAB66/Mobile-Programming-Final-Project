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

/**
 * Extracts ALL prominent colours from the album art (vibrant, muted, dark vibrant, light muted)
 * so the Now Playing background can animate between them as a living multi-colour gradient.
 * Returns at least two colours so the gradient always has something to animate between.
 */
@Composable
fun rememberAlbumColors(url: String?, fallback: Color): State<List<Color>> {
    val context = LocalContext.current
    val colorsState = remember(url) { mutableStateOf(listOf(fallback, fallback)) }

    LaunchedEffect(url, fallback) {
        if (url.isNullOrBlank()) {
            colorsState.value = listOf(fallback, fallback)
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
                    listOfNotNull(
                        palette.vibrantSwatch?.rgb,
                        palette.lightVibrantSwatch?.rgb,
                        palette.darkVibrantSwatch?.rgb,
                        palette.mutedSwatch?.rgb,
                        palette.lightMutedSwatch?.rgb,
                        palette.darkMutedSwatch?.rgb,
                    ).map { Color(it) }
                }
            }.getOrNull()
        }
        if (!extracted.isNullOrEmpty()) {
            // Always provide at least 2 colours for the gradient animator.
            colorsState.value = if (extracted.size >= 2) extracted else extracted + extracted
        }
    }
    return colorsState
}
