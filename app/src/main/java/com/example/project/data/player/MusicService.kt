package com.example.project.data.player

import android.app.PendingIntent
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.example.project.R
import com.example.project.domain.repository.LibraryRepository
import com.example.project.domain.repository.MusicRepository
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Background playback service. Hosts the ExoPlayer + [MediaSession] so audio keeps playing
 * when the user leaves the app, exposes system media-notification controls (play/pause/next/
 * previous) automatically via MediaSessionService, handles audio focus (pause/duck on
 * interruptions) and caches streamed audio to disk.
 */
@UnstableApi
class MusicService : MediaSessionService(), KoinComponent {

    private var mediaSession: MediaSession? = null
    private var crossfadeManager: CrossfadeManager? = null

    // Liked-songs are the single source of truth in Room; the notification Like action and the
    // in-app heart both read/write through this, so they stay in sync (issue #002).
    private val libraryRepository: LibraryRepository by inject()
    private val musicRepository: MusicRepository by inject()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var likedIds: Set<String> = emptySet()

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

        // Overlapping crossfade (spec §6): a short-lived secondary player renders the incoming
        // track so it can be mixed with the outgoing one's tail. The secondary must NOT grab audio
        // focus (the main player already holds it). It shares the disk-cache *factory* (safe — each
        // CacheDataSource it creates is independent) but gets its OWN default LoadControl: a
        // DefaultLoadControl owns a stateful Allocator and cannot be shared between two ExoPlayers
        // (doing so fails with ERROR_CODE_FAILED_RUNTIME_CHECK).
        crossfadeManager = CrossfadeManager(
            mainPlayer = player,
            secondaryPlayerFactory = {
                ExoPlayer.Builder(this)
                    .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ false)
                    .setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory))
                    .build()
            },
        ).also { it.start() }

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
            .setCallback(LikeAwareCallback())
            .build()

        // Refresh the notification's Like button whenever the liked set changes or the track does.
        serviceScope.launch {
            libraryRepository.observeLikedIds().collect { ids ->
                likedIds = ids
                updateCustomLayout()
            }
        }
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = updateCustomLayout()
        })
        updateCustomLayout()
    }

    /** The single custom notification button: Like/Unlike, reflecting the current song's state. */
    private fun likeButton(): CommandButton {
        val currentId = mediaSession?.player?.currentMediaItem?.mediaId
        val liked = currentId != null && currentId in likedIds
        return CommandButton.Builder()
            .setDisplayName(getString(if (liked) R.string.cd_unlike else R.string.cd_like))
            .setIconResId(if (liked) R.drawable.ic_favorite else R.drawable.ic_favorite_border)
            .setSessionCommand(SessionCommand(ACTION_TOGGLE_LIKE, Bundle.EMPTY))
            .build()
    }

    private fun updateCustomLayout() {
        mediaSession?.setCustomLayout(listOf(likeButton()))
    }

    /** Grants the custom Like command and handles taps on it from the notification. */
    private inner class LikeAwareCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(SessionCommand(ACTION_TOGGLE_LIKE, Bundle.EMPTY))
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == ACTION_TOGGLE_LIKE) {
                val currentId = session.player.currentMediaItem?.mediaId
                if (currentId != null) {
                    // Toggle in Room; the observeLikedIds collector then refreshes the button icon,
                    // and the in-app heart (also driven by liked ids) updates too.
                    serviceScope.launch {
                        musicRepository.getSong(currentId)?.let { libraryRepository.toggleLike(it) }
                    }
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }
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
        serviceScope.cancel()
        crossfadeManager?.release()
        crossfadeManager = null
        AudioSessionHolder.update(0)
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    private companion object {
        const val ACTION_TOGGLE_LIKE = "com.example.project.TOGGLE_LIKE"
    }
}
