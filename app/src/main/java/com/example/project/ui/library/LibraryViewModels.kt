package com.example.project.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project.domain.model.Song
import com.example.project.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class LikedSongsViewModel(
    libraryRepository: LibraryRepository,
) : ViewModel() {
    val songs: StateFlow<List<Song>> = libraryRepository.observeLikedSongs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

class RecentlyPlayedViewModel(
    libraryRepository: LibraryRepository,
) : ViewModel() {
    val songs: StateFlow<List<Song>> = combine(
        libraryRepository.observeRecentlyPlayed(),
        libraryRepository.observeLikedIds(),
    ) { recent, likedIds ->
        recent.map { it.copy(isLiked = it.id in likedIds) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
