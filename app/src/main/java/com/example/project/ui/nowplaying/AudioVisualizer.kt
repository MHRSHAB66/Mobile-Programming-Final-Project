package com.example.project.ui.nowplaying

import android.media.audiofx.Visualizer
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Audio equalizer drawn on a [Canvas].
 *
 * When a real playback [audioSessionId] is available AND the user granted RECORD_AUDIO, an
 * [android.media.audiofx.Visualizer] is attached to that session and the bars are driven by the
 * REAL FFT of the music — so they actually rise on loud/busy passages and fall on quiet ones
 * (issue #012). Otherwise it falls back to a synthetic three-wave animation so the screen still
 * looks alive without the permission.
 */
@Composable
fun AudioVisualizer(
    isPlaying: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    barCount: Int = 36,
    audioSessionId: Int = 0,
    hasAudioPermission: Boolean = false,
) {
    val useRealData = isPlaying && hasAudioPermission && audioSessionId != 0

    if (useRealData) {
        RealtimeVisualizer(color, modifier, barCount, audioSessionId)
    } else {
        SyntheticVisualizer(isPlaying, color, modifier, barCount)
    }
}

/** Real spectrum: bars driven by the FFT of the live audio session. */
@Composable
private fun RealtimeVisualizer(
    color: Color,
    modifier: Modifier,
    barCount: Int,
    audioSessionId: Int,
) {
    // Per-bar magnitudes in 0..1, updated from the Visualizer capture callback.
    val magnitudes = remember(barCount) { mutableStateOf(FloatArray(barCount)) }

    DisposableEffect(audioSessionId, barCount) {
        // Light exponential smoothing so bars glide instead of flickering frame-to-frame.
        val smoothed = FloatArray(barCount)
        var visualizer: Visualizer? = null

        runCatching {
            visualizer = Visualizer(audioSessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(v: Visualizer?, waveform: ByteArray?, rate: Int) = Unit

                        override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, rate: Int) {
                            if (fft == null) return
                            val bins = fft.size / 2
                            if (bins < 2) return

                            // Magnitude per FFT bin.
                            val mags = FloatArray(bins)
                            for (i in 0 until bins) {
                                val re = fft[2 * i].toFloat()
                                val im = fft[2 * i + 1].toFloat()
                                mags[i] = sqrt(re * re + im * im)
                            }

                            // Spread bars across the spectrum on a LOG scale: bass carries far more
                            // energy than treble, so a linear split made only the left bars move.
                            // Log spacing + sqrt compression gives every bar a live frequency band.
                            val out = FloatArray(barCount)
                            var frameMax = 1f
                            val minBin = 1.0
                            val maxBin = bins.toDouble()
                            for (b in 0 until barCount) {
                                val lo = (minBin * (maxBin / minBin).pow(b.toDouble() / barCount))
                                    .toInt().coerceIn(1, bins - 1)
                                val hi = (minBin * (maxBin / minBin).pow((b + 1).toDouble() / barCount))
                                    .toInt().coerceIn(lo + 1, bins)
                                var sum = 0f
                                for (k in lo until hi) sum += mags[k]
                                val value = sqrt(sum / (hi - lo))
                                out[b] = value
                                if (value > frameMax) frameMax = value
                            }
                            // Normalise to this frame's peak, then smooth so bars glide.
                            for (b in 0 until barCount) {
                                val norm = (out[b] / frameMax).coerceIn(0f, 1f)
                                smoothed[b] = smoothed[b] * 0.55f + norm * 0.45f
                            }
                            magnitudes.value = smoothed.copyOf()
                        }
                    },
                    // Capture rate in milliHertz; half of max is a smooth ~10 Hz refresh.
                    Visualizer.getMaxCaptureRate() / 2,
                    /* waveform = */ false,
                    /* fft = */ true,
                )
                enabled = true
            }
        }

        onDispose {
            runCatching {
                visualizer?.enabled = false
                visualizer?.release()
            }
        }
    }

    Canvas(modifier = modifier) {
        val mags = magnitudes.value
        val slot = size.width / barCount
        val barWidth = slot * 0.52f
        for (i in 0 until barCount) {
            val mag = mags.getOrElse(i) { 0f }
            val height = (0.06f + 0.94f * mag) * size.height
            val x = i * slot + (slot - barWidth) / 2f
            drawRoundRect(
                color = color.copy(alpha = 0.55f + 0.45f * mag),
                topLeft = Offset(x, (size.height - height) / 2f),
                size = Size(barWidth, height),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
        }
    }
}

/** Decorative fallback: three superimposed sine waves (no permission / no live session). */
@Composable
private fun SyntheticVisualizer(
    isPlaying: Boolean,
    color: Color,
    modifier: Modifier,
    barCount: Int,
) {
    val transition = rememberInfiniteTransition(label = "visualizer")
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
            val bass = abs(sin(phase1 + i * 0.30f))
            val mid = abs(sin(phase2 + i * 0.55f + 1.0f))
            val treble = abs(sin(phase3 + i * 0.85f + 2.3f))
            val combined = (0.50f * bass + 0.30f * mid + 0.20f * treble)
            val height = (0.10f + 0.90f * combined) * size.height * amplitude
            val x = i * slot + (slot - barWidth) / 2f
            drawRoundRect(
                color = color.copy(alpha = 0.55f + 0.45f * combined),
                topLeft = Offset(x, (size.height - height) / 2f),
                size = Size(barWidth, height),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
        }
    }
}
