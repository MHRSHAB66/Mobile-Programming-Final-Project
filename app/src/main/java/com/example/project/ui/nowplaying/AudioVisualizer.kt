package com.example.project.ui.nowplaying

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.sin

/**
 * Pure-Compose audio equalizer drawn on a [Canvas]. Three sine waves of different frequencies
 * are summed per bar to produce a complex, multi-band waveform that looks closer to a real
 * spectrum analyser than a single sine sweep.
 */
@Composable
fun AudioVisualizer(
    isPlaying: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    barCount: Int = 36,
) {
    val transition = rememberInfiniteTransition(label = "visualizer")

    // Three independent phase animators at different speeds to simulate bass/mid/treble bands.
    val phase1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "phase1",
    )
    val phase2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing)),
        label = "phase2",
    )
    val phase3 by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1700, easing = LinearEasing)),
        label = "phase3",
    )

    val amplitude by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.08f,
        animationSpec = tween(500),
        label = "amplitude",
    )

    Canvas(modifier = modifier) {
        val slot = size.width / barCount
        val barWidth = slot * 0.52f
        repeat(barCount) { i ->
            // Superimpose three waves: bass (low-freq), mid, treble (high-freq).
            val bass   = abs(sin(phase1 + i * 0.30f))           // slow, wide arches
            val mid    = abs(sin(phase2 + i * 0.55f + 1.0f))    // medium ripple
            val treble = abs(sin(phase3 + i * 0.85f + 2.3f))    // fast, narrow peaks
            val combined = (0.50f * bass + 0.30f * mid + 0.20f * treble)
            val height = (0.10f + 0.90f * combined) * size.height * amplitude
            val x = i * slot + (slot - barWidth) / 2f
            // Taller bars are slightly more opaque for a depth effect.
            val alpha = 0.55f + 0.45f * combined
            drawRoundRect(
                color = color.copy(alpha = alpha),
                topLeft = Offset(x, (size.height - height) / 2f),
                size = Size(barWidth, height),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
        }
    }
}
