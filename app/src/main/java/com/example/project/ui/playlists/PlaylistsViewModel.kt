package com.example.project.ui.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project.R
import com.example.project.core.util.UiText
import com.example.project.domain.model.Playlist
import com.example.project.domain.model.PlaylistType
import com.example.project.domain.repository.PlaylistRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlaylistsUiState(
    val isLoading: Boolean = true,
    val global: List<Playlist> = emptyList(),
    val local: List<Playlist> = emptyList(),
    val user: List<Playlist> = emptyList(),
    val isCreating: Boolean = false,
)

sealed interface PlaylistsEffect {
    data class Message(val text: UiText) : PlaylistsEffect
}

class PlaylistsViewModel(
    private val playlistRepository: PlaylistRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistsUiState())
    val uiState: StateFlow<PlaylistsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<PlaylistsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val catalog = playlistRepository.getAllPlaylists()
            val mine = playlistRepository.getMyPlaylists()
            _uiState.value = PlaylistsUiState(
                isLoading = false,
                global = catalog.filter { it.type == PlaylistType.GLOBAL },
                local = catalog.filter { it.type == PlaylistType.LOCAL },
                user = mine.ifEmpty { catalog.filter { it.type == PlaylistType.USER } },
            )
        }
    }

    fun createPlaylist(title: String, isPublic: Boolean) {
        val trimmed = title.trim()
        if (trimmed.isEmpty() || _uiState.value.isCreating) return
        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true) }
            val result = playlistRepository.createPlaylist(trimmed, isPublic)
            _uiState.update { it.copy(isCreating = false) }
            result
                .onSuccess {
                    _effects.send(PlaylistsEffect.Message(UiText.from(R.string.playlists_create_success)))
                    load()
                }
                .onFailure {
                    _effects.send(PlaylistsEffect.Message(UiText.from(R.string.playlists_create_error)))
                }
        }
    }
}
