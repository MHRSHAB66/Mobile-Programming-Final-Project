package com.example.project.ui.userprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project.domain.model.Playlist
import com.example.project.domain.model.User
import com.example.project.domain.repository.ProfileRepository
import com.example.project.domain.repository.SocialRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UserProfileUiState(
    val user: User? = null,
    val playlists: List<Playlist> = emptyList(),
)

class UserProfileViewModel(
    private val userId: String,
    private val socialRepository: SocialRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val baseUser = MutableStateFlow<User?>(null)
    private val playlists = MutableStateFlow<List<Playlist>>(emptyList())

    val uiState: StateFlow<UserProfileUiState> = combine(
        baseUser, socialRepository.observeFollowedUsers(), playlists
    ) { user, followed, lists ->
        UserProfileUiState(
            user = user?.copy(isFollowed = followed.any { it.id == user.id }),
            playlists = lists,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserProfileUiState())

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val remoteUser = profileRepository.getUser(userId).getOrNull()
            baseUser.value = remoteUser ?: socialRepository.getUser(userId)

            val remotePlaylists = profileRepository.getUserPublicPlaylists(userId).getOrNull()
            playlists.value = remotePlaylists
                ?: socialRepository.getPublicPlaylists(userId)
        }
    }

    fun toggleFollow() {
        viewModelScope.launch { socialRepository.toggleFollow(userId) }
    }
}
