package com.example.project.domain.player

import com.example.project.domain.model.PlaybackState
import com.example.project.domain.model.Song
import kotlinx.coroutines.flow.StateFlow

/**
 * Abstraction over the media player. The implementation connects to the Media3
 * [MediaSessionService] via a MediaController, so the UI never touches ExoPlayer directly.
 */
interface PlayerController {
    val state: StateFlow<PlaybackState>

    /** Plays [queue] starting at [startIndex]; sources are already resolved (local vs. stream). */
    fun play(queue: List<Song>, startIndex: Int)
    fun playSingle(song: Song)
    fun togglePlayPause()
    fun next()
    fun previous()
    fun seekTo(positionMs: Long)
    fun setSpeed(speed: Float)
    fun stop()
    fun release()
}
