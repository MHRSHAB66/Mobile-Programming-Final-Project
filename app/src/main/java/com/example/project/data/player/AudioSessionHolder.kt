package com.example.project.data.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Bridges the ExoPlayer audio session id (created inside [MusicService]) to the UI so the Now
 * Playing audio visualizer can attach an [android.media.audiofx.Visualizer] to the REAL playback
 * output and react to the actual music.
 *
 * [MusicService] and the Compose UI run in the same process, so a simple in-memory holder is
 * enough — no IPC needed. 0 means "no active session yet".
 */
object AudioSessionHolder {
    private val _sessionId = MutableStateFlow(0)
    val sessionId: StateFlow<Int> = _sessionId.asStateFlow()

    fun update(id: Int) {
        _sessionId.value = id
    }
}
