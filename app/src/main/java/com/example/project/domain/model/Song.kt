package com.example.project.domain.model

/**
 * A single, verified, highly-available sample MP3 (Google-hosted ExoPlayer test media) used as
 * a safe fallback for every song. If a primary [Song.audioUrl] fails to load, the player retries
 * with this URL before skipping — so a track is always playable during the demo.
 */
const val DEFAULT_FALLBACK_AUDIO_URL = "https://storage.googleapis.com/exoplayer-test-media-0/play.mp3"

/**
 * Core music item. Mirrors the required metadata (id, title, artist_name,
 * cover_image_url, audio_url) plus a few fields the UI needs.
 *
 * Songs come from the local/mock catalogue ([com.example.project.data.mock.MockData]), NOT a
 * real backend API. [audioUrl] is a public sample stream; [fallbackAudioUrl] is a reliable
 * backup stream; [localPath] is non-null when an offline copy exists. The player chooses:
 * downloaded local file → primary stream → fallback stream.
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
