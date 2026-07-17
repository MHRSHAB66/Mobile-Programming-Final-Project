package com.example.project.data.player

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer

/**
 * Real overlapping audio crossfade for the spec's "Crossfade" requirement (§6): the outgoing
 * track fades out over its final seconds while the incoming track is already fading in, so the
 * two are audibly mixed for the overlap window.
 *
 * A single [ExoPlayer] renders one audio stream and therefore cannot overlap two tracks, so this
 * manager drives a short-lived *secondary* player for the incoming track:
 *
 *  1. While the main (session) player is within [durationMs] of the end of the current item and a
 *     next item exists, a secondary player starts the next track at volume 0.
 *  2. Over the overlap window the main player ramps 1→0 and the secondary ramps 0→1 — the mix.
 *  3. When the main player gaplessly auto-advances to that same next item (at volume 0, silent),
 *     control is handed back: the main player is seeked to the secondary's position, restored to
 *     full volume, and the secondary is released. The session player stays the single source of
 *     truth for the notification, queue and visualizer throughout.
 *
 * Every failure path (secondary can't load, user skips/pauses/seeks, queue changes) aborts the
 * crossfade and restores normal single-player playback, so this never blocks or breaks playback.
 */
@UnstableApi
class CrossfadeManager(
    private val mainPlayer: ExoPlayer,
    private val secondaryPlayerFactory: () -> ExoPlayer,
    private val durationMs: Long = DEFAULT_DURATION_MS,
) {
    private val handler = Handler(Looper.getMainLooper())
    private var secondary: ExoPlayer? = null
    private var fading = false
    private var rampStartUptime = 0L

    private val mainListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
            if (!fading) return
            // The main player gaplessly reached the next item: hand control back to it. Any other
            // reason (user skip, new queue, repeat) means the crossfade target is no longer valid.
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) handoff() else abort()
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            // A manual seek during the overlap invalidates the timing assumptions.
            if (fading && reason == Player.DISCONTINUITY_REASON_SEEK) abort()
        }
    }

    private val ticker = object : Runnable {
        override fun run() {
            step()
            handler.postDelayed(this, TICK_MS)
        }
    }

    /** Begins monitoring playback. Safe to call once, from the service, after the player is built. */
    fun start() {
        if (durationMs <= 0) return
        mainPlayer.addListener(mainListener)
        handler.post(ticker)
    }

    private fun step() {
        val main = mainPlayer
        if (fading) {
            // A pause during the overlap should not leave the secondary playing on alone.
            if (!main.playWhenReady) { abort(); return }
            updateRamp()
            return
        }
        val eligible = main.playWhenReady &&
            main.hasNextMediaItem() &&
            main.repeatMode != Player.REPEAT_MODE_ONE
        if (!eligible) return
        val duration = main.duration
        val position = main.currentPosition
        // Skip tracks too short to crossfade cleanly; only trigger inside the tail window.
        if (duration == C.TIME_UNSET || duration <= durationMs * 2) return
        val remaining = duration - position
        if (remaining in 1..durationMs) beginCrossfade()
    }

    private fun beginCrossfade() {
        val nextIndex = mainPlayer.nextMediaItemIndex
        if (nextIndex == C.INDEX_UNSET) return
        val nextItem = mainPlayer.getMediaItemAt(nextIndex)
        try {
            val s = secondaryPlayerFactory().apply {
                volume = 0f
                setMediaItem(nextItem)
                playbackParameters = mainPlayer.playbackParameters
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        // Next track can't stream; drop the crossfade and let the main player
                        // advance normally (it has its own fallback-URL recovery).
                        Log.w(TAG, "Secondary player error, aborting crossfade: ${error.errorCodeName}")
                        abort()
                    }
                })
                prepare()
                playWhenReady = true
            }
            secondary = s
            fading = true
            rampStartUptime = SystemClock.uptimeMillis()
        } catch (e: Exception) {
            Log.w(TAG, "Could not start crossfade", e)
            releaseSecondary()
            fading = false
            mainPlayer.volume = 1f
        }
    }

    private fun updateRamp() {
        val s = secondary ?: return
        val t = ((SystemClock.uptimeMillis() - rampStartUptime).toFloat() / durationMs).coerceIn(0f, 1f)
        mainPlayer.volume = 1f - t
        s.volume = t
    }

    /** Main player reached the crossfaded track: continue it from where the secondary had got to. */
    private fun handoff() {
        val position = secondary?.currentPosition ?: 0L
        releaseSecondary()
        fading = false
        if (position > 0L) mainPlayer.seekTo(position)
        mainPlayer.volume = 1f
    }

    /** Cancel an in-flight crossfade and restore normal single-player playback. */
    private fun abort() {
        releaseSecondary()
        fading = false
        mainPlayer.volume = 1f
    }

    private fun releaseSecondary() {
        secondary?.release()
        secondary = null
    }

    fun release() {
        handler.removeCallbacks(ticker)
        mainPlayer.removeListener(mainListener)
        releaseSecondary()
    }

    companion object {
        private const val TAG = "CrossfadeManager"
        private const val TICK_MS = 100L
        /** Overlap window. Set to 0 to disable crossfade entirely (falls back to gapless). */
        const val DEFAULT_DURATION_MS = 5_000L
    }
}
