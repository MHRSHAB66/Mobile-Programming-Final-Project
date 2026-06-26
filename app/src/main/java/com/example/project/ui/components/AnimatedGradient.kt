package com.example.project.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * A living, slowly-shifting linear gradient built from brand colours. Used for hero headers and
 * accent surfaces to give the UI a premium, animated feel without hurting readability.
 */
@Composable
fun rememberAnimatedBrandGradient(
    colors: List<Color>,
    durationMillis: Int = 6000,
    span: Float = 800f,
): Brush {
    val transition = rememberInfiniteTransition(label = "animatedGradient")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "gradientShift",
    )
    return Brush.linearGradient(
        colors = colors,
        start = Offset(span * shift, 0f),
        end = Offset(span * shift + span, span),
    )
}

/** A soft, slowly-pulsing scale value for glow/halo effects. */
@Composable
fun rememberPulse(min: Float = 0.92f, max: Float = 1.08f, durationMillis: Int = 2600): Float {
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale by transition.animateFloat(
        initialValue = min,
        targetValue = max,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )
    return scale
}
