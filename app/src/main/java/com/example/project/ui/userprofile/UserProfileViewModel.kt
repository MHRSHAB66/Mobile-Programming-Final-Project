package com.example.project.ui.userprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project.domain.model.Playlist
import com.example.project.domain.model.User
import com.example.project.domain.repository.ChatRepository
import com.example.project.domain.repository.ProfileRepository
import com.example.project.domain.repository.SettingsRepository
import com.example.project.domain.repository.SocialRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UserProfileUiState(
    val user: User? = null,
    val playlists: List<Playlist> = emptyList(),
    val isSelf: Boolean = false,
    val isOpeningChat: Boolean = false,
)

class UserProfileViewModel(
    private val userId: String,
    private val socialRepository: SocialRepository,
    private val profileRepository: ProfileRepository,
    private val chatRepository: ChatRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val baseUser = MutableStateFlow<User?>(null)
    private val playlists = MutableStateFlow<List<Playlist>>(emptyList())
    private val isOpeningChat = MutableStateFlow(false)

    val uiState: StateFlow<UserProfileUiState> = combine(
        baseUser,
        socialRepository.observeFollowedUsers(),
        playlists,
        settingsRepository.settings.map { it.currentUserId == userId },
        isOpeningChat,
    ) { user, followed, lists, isSelf, opening ->
        UserProfileUiState(
            user = user?.copy(isFollowed = followed.any { it.id == user.id }),
            playlists = lists,
            isSelf = isSelf,
            isOpeningChat = opening,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserProfileUiState())

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            reloadUser()

            val remotePlaylists = profileRepository.getUserPublicPlaylists(userId).getOrNull()
            playlists.value = remotePlaylists
                ?: socialRepository.getPublicPlaylists(userId)
        }
    }

    fun toggleFollow() {
        viewModelScope.launch {
            socialRepository.toggleFollow(userId)
            reloadUser()
        }
    }

    fun openMessage(onChatReady: (String) -> Unit) {
        viewModelScope.launch {
            isOpeningChat.value = true
            chatRepository.openDirectMessage(userId).onSuccess { onChatReady(it) }
            isOpeningChat.value = false
        }
    }

    private suspend fun reloadUser() {
        val remoteUser = profileRepository.getUser(userId).getOrNull()
        baseUser.value = remoteUser ?: socialRepository.getUser(userId)
    }
}
