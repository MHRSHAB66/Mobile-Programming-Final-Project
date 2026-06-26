package com.example.project.ui.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project.domain.model.Playlist
import com.example.project.domain.model.PlaylistType
import com.example.project.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlaylistsUiState(
    val isLoading: Boolean = true,
    val global: List<Playlist> = emptyList(),
    val local: List<Playlist> = emptyList(),
    val user: List<Playlist> = emptyList(),
)

class PlaylistsViewModel(
    private val playlistRepository: PlaylistRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistsUiState())
    val uiState: StateFlow<PlaylistsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val all = playlistRepository.getAllPlaylists()
            _uiState.value = PlaylistsUiState(
                isLoading = false,
                global = all.filter { it.type == PlaylistType.GLOBAL },
                local = all.filter { it.type == PlaylistType.LOCAL },
                user = all.filter { it.type == PlaylistType.USER },
            )
        }
    }
}
