package com.example.project.ui.artist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project.domain.model.Artist
import com.example.project.domain.model.Song
import com.example.project.domain.repository.LibraryRepository
import com.example.project.domain.repository.MusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ArtistUiState(
    val artist: Artist? = null,
    val songs: List<Song> = emptyList(),
    val isFollowed: Boolean = false,
)

class ArtistViewModel(
    private val artistId: String,
    private val musicRepository: MusicRepository,
    libraryRepository: LibraryRepository,
) : ViewModel() {

    private val artist = MutableStateFlow<Artist?>(null)
    private val baseSongs = MutableStateFlow<List<Song>>(emptyList())
    private val followed = MutableStateFlow(false)

    val uiState: StateFlow<ArtistUiState> = combine(
        artist, baseSongs, followed, libraryRepository.observeLikedIds()
    ) { artist, songs, isFollowed, likedIds ->
        ArtistUiState(
            artist = artist?.copy(isFollowed = isFollowed),
            songs = songs.map { it.copy(isLiked = it.id in likedIds) },
            isFollowed = isFollowed,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ArtistUiState())

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val loaded = musicRepository.getArtist(artistId)
            artist.value = loaded
            followed.value = loaded?.isFollowed ?: false
            baseSongs.value = musicRepository.getArtistSongs(artistId)
        }
    }

    fun toggleFollow() {
        followed.value = !followed.value
    }
}
