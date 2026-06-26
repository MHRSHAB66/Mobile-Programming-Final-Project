package com.example.project.data.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.example.project.domain.model.RepeatMode
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.project.domain.model.PlaybackState
import com.example.project.domain.model.Song
import com.example.project.domain.player.PlayerController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Connects the app to [MusicService] through a Media3 [MediaController] and projects the
 * player into a [StateFlow] for the mini player and Now Playing screen. The UI never touches
 * ExoPlayer directly. All controller access happens on the main looper.
 */
class PlayerControllerImpl(
    private val context: Context,
) : PlayerController {

    private val handler = Handler(Looper.getMainLooper())
    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private var currentQueue: List<Song> = emptyList()
    private val pending = ArrayDeque<(MediaController) -> Unit>()

    // Songs whose primary URL already failed and were switched to the fallback URL.
    private val triedFallback = mutableSetOf<String>()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = refreshState()

        /**
         * Graceful recovery: if a track fails to load (bad/slow URL), first retry it with the
         * song's reliable fallback URL; if that also fails, skip to the next track. The player
         * never gets stuck or crashes on a single unplayable source.
         */
        override fun onPlayerError(error: PlaybackException) {
            val c = controller ?: return
            val index = c.currentMediaItemIndex
            val song = currentQueue.getOrNull(index)
            Log.w(TAG, "Playback error for '${song?.title}': ${error.errorCodeName}")
            when {
                song != null && song.id !in triedFallback -> {
                    triedFallback.add(song.id)
                    c.replaceMediaItem(index, song.toMediaItem(useFallback = true))
                    c.prepare()
                    c.play()
                }
                c.hasNextMediaItem() -> {
                    c.seekToNextMediaItem()
                    c.prepare()
                    c.play()
                }
                else -> Log.w(TAG, "No fallback or next track available; stopping gracefully.")
            }
        }
    }

    private val ticker = object : Runnable {
        override fun run() {
            controller?.let { c ->
                _state.update { it.copy(positionMs = c.currentPosition.coerceAtLeast(0L)) }
            }
            handler.postDelayed(this, 500)
        }
    }

    init {
        handler.post { initController() }
    }

    private fun initController() {
        val token = SessionToken(context, ComponentName(context, MusicService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            controller = future.get().also { c ->
                c.addListener(listener)
                while (pending.isNotEmpty()) pending.removeFirst().invoke(c)
                refreshState()
            }
            handler.post(ticker)
        }, ContextCompat.getMainExecutor(context))
    }

    private fun action(block: (MediaController) -> Unit) {
        val c = controller
        if (c != null) handler.post { block(c) } else pending.add(block)
    }

    private fun Song.toMediaItem(useFallback: Boolean = false): MediaItem = MediaItem.Builder()
        .setMediaId(id)
        .setUri(if (useFallback) fallbackAudioUrl else playbackUri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artistName)
                .setAlbumTitle(album)
                .setArtworkUri(Uri.parse(coverImageUrl))
                .build()
        )
        .build()

    private fun refreshState() {
        val c = controller ?: return
        val currentId = c.currentMediaItem?.mediaId
        val song = currentQueue.firstOrNull { it.id == currentId }
        val duration = c.duration.let { if (it > 0) it else song?.durationMs ?: 0L }
        _state.update {
            it.copy(
                currentSong = song,
                queue = currentQueue,
                isPlaying = c.isPlaying,
                isBuffering = c.playbackState == Player.STATE_BUFFERING,
                durationMs = duration,
                speed = c.playbackParameters.speed,
                hasNext = c.hasNextMediaItem(),
                hasPrevious = c.hasPreviousMediaItem(),
                isShuffled = c.shuffleModeEnabled,
                repeatMode = when (c.repeatMode) {
                    Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                    Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                    else -> RepeatMode.OFF
                },
            )
        }
    }

    override fun play(queue: List<Song>, startIndex: Int) {
        currentQueue = queue
        triedFallback.clear()
        val items = queue.map { it.toMediaItem() }
        action { c ->
            c.setMediaItems(items, startIndex.coerceIn(0, items.lastIndex), 0L)
            c.prepare()
            c.play()
        }
    }

    override fun playSingle(song: Song) = play(listOf(song), 0)

    override fun togglePlayPause() = action { c ->
        if (c.isPlaying) c.pause() else c.play()
    }

    override fun next() = action { c -> if (c.hasNextMediaItem()) c.seekToNextMediaItem() }

    override fun previous() = action { c ->
        if (c.currentPosition > 3000 || !c.hasPreviousMediaItem()) c.seekTo(0)
        else c.seekToPreviousMediaItem()
    }

    override fun seekTo(positionMs: Long) {
        // Optimistically update position in state so the UI slider snaps immediately
        // instead of waiting for the next ExoPlayer event tick.
        _state.update { it.copy(positionMs = positionMs) }
        action { c -> c.seekTo(positionMs) }
    }

    override fun setSpeed(speed: Float) = action { c -> c.setPlaybackSpeed(speed) }

    override fun toggleShuffle() = action { c ->
        c.shuffleModeEnabled = !c.shuffleModeEnabled
        refreshState()
    }

    override fun cycleRepeatMode() = action { c ->
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        refreshState()
    }

    override fun stop() = action { c ->
        c.stop()
        c.clearMediaItems()
    }

    override fun release() {
        handler.removeCallbacks(ticker)
        controller?.release()
        controller = null
    }

    private companion object {
        const val TAG = "PlayerController"
    }
}
