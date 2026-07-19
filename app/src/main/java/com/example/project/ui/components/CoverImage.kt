package com.example.project.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * Cover art that keeps loading visually neutral and shows a meaningful fallback when
 * the URL is missing or loading fails.
 */
@Composable
fun CoverImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    cornerRadius: Int = 12,
) {
    ThemedAsyncImage(
        url = url,
        contentDescription = contentDescription,
        fallbackIcon = Icons.Rounded.MusicNote,
        shape = RoundedCornerShape(cornerRadius.dp),
        modifier = modifier,
    )
}

/**
 * Circular user/artist image with a person fallback for null, blank, or failed URLs.
 */
@Composable
fun CircleImage(
    url: String?,
    contentDescription: String?,
    sizeDp: Int,
    modifier: Modifier = Modifier,
) {
    ThemedAsyncImage(
        url = url,
        contentDescription = contentDescription,
        fallbackIcon = Icons.Rounded.Person,
        shape = CircleShape,
        modifier = modifier.size(sizeDp.dp),
    )
}

@Composable
private fun ThemedAsyncImage(
    url: String?,
    contentDescription: String?,
    fallbackIcon: ImageVector,
    shape: Shape,
    modifier: Modifier = Modifier,
) {
    val hasImageUrl = !url.isNullOrBlank()
    var loadFailed by remember(url) { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (hasImageUrl) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .crossfade(true)
                    .build(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                onLoading = { loadFailed = false },
                onSuccess = { loadFailed = false },
                onError = { loadFailed = true },
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (!hasImageUrl || loadFailed) {
            ImageFallback(
                icon = fallbackIcon,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun ImageFallback(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(
            brush = Brush.linearGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.secondaryContainer,
                ),
            ),
        ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
            modifier = Modifier.fillMaxSize(0.42f),
        )
    }
}
