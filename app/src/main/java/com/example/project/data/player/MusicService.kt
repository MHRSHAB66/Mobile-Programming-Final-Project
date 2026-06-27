package com.example.project.data.player

import android.app.PendingIntent
import android.content.Intent
import android.media.AudioManager
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Background playback service. Hosts the ExoPlayer + [MediaSession] so audio keeps playing
 * when the user leaves the app, exposes system media-notification controls (play/pause/next/
 * previous) automatically via MediaSessionService, handles audio focus (pause/duck on
 * interruptions) and caches streamed audio to disk.
 */
@UnstableApi
class MusicService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        // Shorter connect/read timeouts (default is 8 s) so a dead/slow stream URL fails fast and
        // the player can fail over to the fallback in ~4 s instead of hanging ~8 s — this is the
        // main cause of the long delay before some songs start (issue #010).
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(4_000)
            .setReadTimeoutMs(4_000)
            .setAllowCrossProtocolRedirects(true)

        // Stream through a disk cache so re-listens / seeks don't re-download.
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(PlaybackCache.get(this))
            .setUpstreamDataSourceFactory(DefaultDataSource.Factory(this, httpDataSourceFactory))
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        // Reduce bufferForPlaybackMs so playback starts as soon as ~500 ms is buffered
        // (default is 2 500 ms), cutting the first-tap delay on stable connections.
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs             = */ 15_000,
                /* maxBufferMs             = */ 50_000,
                /* bufferForPlaybackMs     = */ 500,
                /* bufferForPlaybackAfterRebufferMs = */ 2_000,
            )
            .build()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory))
            .setLoadControl(loadControl)
            .build()

        // Assign an explicit audio session id and publish it so the Now Playing visualizer can
        // attach a real android.media.audiofx.Visualizer to this exact playback output (issue #012).
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val audioSessionId = audioManager.generateAudioSessionId()
        player.setAudioSessionId(audioSessionId)
        AudioSessionHolder.update(audioSessionId)

        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val sessionActivity = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        // Stop the service if nothing is (or will be) playing when the task is swiped away.
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        AudioSessionHolder.update(0)
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
