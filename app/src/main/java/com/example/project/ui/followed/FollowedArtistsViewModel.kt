package com.example.project.ui.followed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project.R
import com.example.project.core.util.UiText
import com.example.project.domain.model.Artist
import com.example.project.domain.repository.MusicRepository
import com.example.project.domain.repository.SocialRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FollowedArtistsUiState(
    val artists: List<Artist> = emptyList(),
    val isLoading: Boolean = true,
    val error: UiText? = null,
)

class FollowedArtistsViewModel(
    private val musicRepository: MusicRepository,
    private val socialRepository: SocialRepository,
) : ViewModel() {

    private val allArtists = MutableStateFlow<List<Artist>>(emptyList())
    private val loading = MutableStateFlow(true)
    private val error = MutableStateFlow<UiText?>(null)

    val uiState: StateFlow<FollowedArtistsUiState> = combine(
        allArtists,
        socialRepository.observeFollowedArtistIds(),
        loading,
        error,
    ) { artists, followedIds, isLoading, currentError ->
        FollowedArtistsUiState(
            artists = artists
                .filter { it.id in followedIds }
                .map { it.copy(isFollowed = true) },
            isLoading = isLoading,
            error = currentError,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FollowedArtistsUiState(),
    )

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            loading.value = true
            error.value = null
            runCatching { musicRepository.getArtists() }
                .onSuccess { allArtists.value = it }
                .onFailure { error.value = UiText.from(R.string.error_generic) }
            loading.value = false
        }
    }

    fun unfollow(artistId: String) {
        viewModelScope.launch {
            socialRepository.toggleFollowArtist(artistId)
        }
    }
}
