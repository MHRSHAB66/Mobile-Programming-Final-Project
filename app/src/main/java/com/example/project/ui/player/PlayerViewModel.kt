package com.example.project.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project.R
import com.example.project.core.util.UiText
import com.example.project.domain.model.Conversation
import com.example.project.domain.model.PlaybackState
import com.example.project.domain.model.Playlist
import com.example.project.domain.model.Song
import com.example.project.domain.player.PlayerController
import com.example.project.domain.repository.ChatRepository
import com.example.project.domain.repository.DownloadRepository
import com.example.project.domain.repository.LibraryRepository
import com.example.project.domain.repository.MusicRepository
import com.example.project.domain.repository.PlaylistRepository
import com.example.project.domain.usecase.DownloadResult
import com.example.project.domain.usecase.DownloadSongUseCase
import com.example.project.domain.usecase.PlaySongsUseCase
import com.example.project.domain.usecase.ToggleLikeUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One-time effects surfaced by the player (snackbars, premium prompt). */
sealed interface PlayerEffect {
    data class Message(val text: UiText) : PlayerEffect
    data object NeedsPremium : PlayerEffect
}

/**
 * App-scoped ViewModel for playback and cross-screen song actions (play, like, download,
 * share, add to playlist).
 */
class PlayerViewModel(
    private val player: PlayerController,
    private val playSongs: PlaySongsUseCase,
    private val toggleLike: ToggleLikeUseCase,
    private val downloadSong: DownloadSongUseCase,
    private val chatRepository: ChatRepository,
    private val musicRepository: MusicRepository,
    private val downloadRepository: DownloadRepository,
    private val libraryRepository: LibraryRepository,
    private val playlistRepository: PlaylistRepository,
) : ViewModel() {

    val playbackState: StateFlow<PlaybackState> = player.state

    val conversations: StateFlow<List<Conversation>> = chatRepository.observeConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val downloadedIds: StateFlow<Set<String>> = downloadRepository.observeDownloadedIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val likedIds: StateFlow<Set<String>> = libraryRepository.observeLikedIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private val _myPlaylists = MutableStateFlow<List<Playlist>>(emptyList())
    val myPlaylists: StateFlow<List<Playlist>> = _myPlaylists.asStateFlow()

    private val _addToPlaylistSong = MutableStateFlow<Song?>(null)
    val addToPlaylistSong: StateFlow<Song?> = _addToPlaylistSong.asStateFlow()

    private val _effects = Channel<PlayerEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private val _sleepTimerSeconds = MutableStateFlow(0)
    val sleepTimerSeconds: StateFlow<Int> = _sleepTimerSeconds.asStateFlow()
    private var sleepJob: Job? = null

    init {
        viewModelScope.launch {
            var known: Set<String>? = null
            downloadRepository.observeDownloadedIds().collect { ids ->
                val previous = known
                if (previous != null && (ids - previous).isNotEmpty()) {
                    _effects.send(PlayerEffect.Message(UiText.from(R.string.download_complete)))
                }
                known = ids
            }
        }
    }

    fun playSong(song: Song) = playQueue(listOf(song), 0)

    fun playSongById(songId: String) {
        viewModelScope.launch {
            musicRepository.getSong(songId)?.let { playSongs(listOf(it), 0) }
        }
    }

    fun playQueue(songs: List<Song>, startIndex: Int) {
        viewModelScope.launch { playSongs(songs, startIndex) }
    }

    fun playShuffled(songs: List<Song>) {
        if (songs.isEmpty()) return
        playQueue(songs.shuffled(), 0)
    }

    fun togglePlayPause() = player.togglePlayPause()
    fun next() = player.next()
    fun previous() = player.previous()
    fun seekTo(positionMs: Long) = player.seekTo(positionMs)

    fun setSpeed(speed: Float) = player.setSpeed(speed)
    fun toggleShuffle() = player.toggleShuffle()
    fun cycleRepeatMode() = player.cycleRepeatMode()

    fun onToggleLike(song: Song) {
        viewModelScope.launch {
            val nowLiked = toggleLike(song)
            _effects.send(
                PlayerEffect.Message(
                    UiText.from(if (nowLiked) R.string.song_liked else R.string.song_unliked)
                )
            )
        }
    }

    fun onDownload(song: Song) {
        viewModelScope.launch {
            when (downloadSong(song)) {
                DownloadResult.Started ->
                    _effects.send(PlayerEffect.Message(UiText.from(R.string.download_started)))
                DownloadResult.NeedsPremium ->
                    _effects.send(PlayerEffect.NeedsPremium)
                DownloadResult.AlreadyDownloaded ->
                    _effects.send(PlayerEffect.Message(UiText.from(R.string.downloaded)))
            }
        }
    }

    fun shareSongToConversation(conversationId: String, song: Song) {
        viewModelScope.launch { chatRepository.shareSong(conversationId, song) }
    }

    fun openAddToPlaylist(song: Song) {
        _addToPlaylistSong.value = song
        viewModelScope.launch {
            _myPlaylists.value = playlistRepository.getMyPlaylists()
        }
    }

    fun dismissAddToPlaylist() {
        _addToPlaylistSong.value = null
    }

    fun addSongToPlaylist(playlistId: String) {
        val song = _addToPlaylistSong.value ?: return
        viewModelScope.launch {
            val result = playlistRepository.addSongToPlaylist(playlistId, song.id)
            _addToPlaylistSong.value = null
            _effects.send(
                PlayerEffect.Message(
                    UiText.from(
                        if (result.isSuccess) R.string.add_to_playlist_success
                        else R.string.add_to_playlist_error,
                    ),
                ),
            )
        }
    }

    fun setSleepTimer(totalSeconds: Int) {
        sleepJob?.cancel()
        _sleepTimerSeconds.value = totalSeconds
        if (totalSeconds <= 0) {
            viewModelScope.launch {
                _effects.send(PlayerEffect.Message(UiText.from(R.string.sleep_timer_cancelled)))
            }
            return
        }
        sleepJob = viewModelScope.launch {
            _effects.send(PlayerEffect.Message(UiText.from(R.string.sleep_timer_set)))
            delay(totalSeconds * 1_000L)
            player.stop()
            _sleepTimerSeconds.value = 0
            _effects.send(PlayerEffect.Message(UiText.from(R.string.sleep_timer_ended)))
        }
    }
}
