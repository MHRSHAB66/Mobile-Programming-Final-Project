package com.example.project.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.project.R
import kotlinx.coroutines.launch

/**
 * Animated like (heart) toggle. The icon flips instantly on tap with a bouncy "pop" — it keeps
 * an optimistic local state so it reacts immediately even when the source song snapshot isn't
 * reactive, and re-syncs whenever the real [isLiked] value changes.
 */
@Composable
fun LikeButton(
    isLiked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    likedTint: Color = Color(0xFFE53935),
    unlikedTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    var liked by remember { mutableStateOf(isLiked) }
    LaunchedEffect(isLiked) { liked = isLiked }

    val scope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }

    IconButton(
        onClick = {
            liked = !liked
            onToggle()
            scope.launch {
                // Quick overshoot then a springy settle for a satisfying "pop".
                scale.animateTo(1.4f, tween(durationMillis = 120))
                scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            }
        },
        modifier = modifier,
    ) {
        Icon(
            imageVector = if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = stringResource(if (liked) R.string.cd_unlike else R.string.cd_like),
            tint = if (liked) likedTint else unlikedTint,
            modifier = Modifier
                .size(iconSize)
                .scale(scale.value),
        )
    }
}
