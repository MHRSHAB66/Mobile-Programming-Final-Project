package com.example.project.domain.model

/**
 * A single, verified sample MP3 used as a player-level fallback when a song's primary
 * [Song.audioUrl] fails to load. The player retries this URL before skipping the track.
 */
const val DEFAULT_FALLBACK_AUDIO_URL = "https://storage.googleapis.com/exoplayer-test-media-0/Jazz_In_Paris.mp3"

/**
 * Core music item from the Melodify catalogue API. [audioUrl] is the stream from the backend;
 * [fallbackAudioUrl] is a reliable backup stream; [localPath] is set when an offline download
 * exists. The player chooses: local file → primary stream → fallback stream.
 */
data class Song(
    val id: String,
    val title: String,
    val artistId: String,
    val artistName: String,
    val album: String,
    val coverImageUrl: String,
    val audioUrl: String,
    val durationMs: Long,
    val genre: String = "",
    val isLiked: Boolean = false,
    val localPath: String? = null,
    val fallbackAudioUrl: String = DEFAULT_FALLBACK_AUDIO_URL,
) {
    val isDownloaded: Boolean get() = localPath != null

    /** Resolved playback source: local file when available, otherwise the stream URL. */
    val playbackUri: String get() = localPath ?: audioUrl
}
