package com.example.project.ui.artist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.project.domain.model.Artist
import com.example.project.domain.model.Song
import com.example.project.domain.repository.LibraryRepository
import com.example.project.domain.repository.MusicRepository
import com.example.project.domain.repository.SocialRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ArtistUiState(
    val artist: Artist? = null,
    val songs: List<Song> = emptyList(),
    val likedIds: Set<String> = emptySet(),
    val isFollowed: Boolean = false,
)

class ArtistViewModel(
    private val artistId: String,
    private val musicRepository: MusicRepository,
    libraryRepository: LibraryRepository,
    private val socialRepository: SocialRepository,
) : ViewModel() {

    private val artist = MutableStateFlow<Artist?>(null)
    private val baseSongs = MutableStateFlow<List<Song>>(emptyList())

    val uiState: StateFlow<ArtistUiState> = combine(
        artist,
        baseSongs,
        socialRepository.observeFollowedArtistIds(),
        libraryRepository.observeLikedIds(),
    ) { artist, songs, followedIds, likedIds ->
        val followed = artistId in followedIds
        ArtistUiState(
            artist = artist?.copy(isFollowed = followed),
            songs = songs.map { it.copy(isLiked = it.id in likedIds) },
            likedIds = likedIds,
            isFollowed = followed,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ArtistUiState())

    val pagedSongs: Flow<PagingData<Song>> =
        musicRepository.getArtistSongsPaged(artistId).cachedIn(viewModelScope)

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            artist.value = musicRepository.getArtist(artistId)
            // Full list only for Play All / queue — list UI uses Paging 3.
            baseSongs.value = musicRepository.getArtistSongs(artistId)
            socialRepository.refreshFollowing()
        }
    }

    fun toggleFollow() {
        viewModelScope.launch {
            val wasFollowed = uiState.value.isFollowed
            artist.value = artist.value?.let { current ->
                current.copy(
                    followers = (current.followers + if (wasFollowed) -1 else 1).coerceAtLeast(0),
                )
            }
            socialRepository.toggleFollowArtist(artistId)
            musicRepository.getArtist(artistId)?.let { refreshed ->
                artist.value = refreshed
            }
        }
    }
}
