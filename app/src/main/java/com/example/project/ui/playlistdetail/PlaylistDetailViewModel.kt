package com.example.project.ui.playlistdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.project.domain.model.Playlist
import com.example.project.domain.model.Song
import com.example.project.domain.repository.LibraryRepository
import com.example.project.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlaylistDetailViewModel(
    private val playlistId: String,
    private val playlistRepository: PlaylistRepository,
    libraryRepository: LibraryRepository,
) : ViewModel() {

    private val _header = MutableStateFlow<Playlist?>(null)
    val header: StateFlow<Playlist?> = _header.asStateFlow()

    // Full (decorated) list used for Play All / Shuffle.
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    // Paging-ready song stream rendered by the list.
    val pagedSongs: Flow<PagingData<Song>> =
        playlistRepository.getPlaylistSongsPaged(playlistId).cachedIn(viewModelScope)

    val likedIds: StateFlow<Set<String>> = libraryRepository.observeLikedIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            // Populates Room when online; Room paging still works offline afterwards.
            _header.value = playlistRepository.getPlaylist(playlistId)
            _songs.value = playlistRepository.getPlaylistSongs(playlistId)
        }
    }
}