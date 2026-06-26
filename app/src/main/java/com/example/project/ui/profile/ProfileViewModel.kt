package com.example.project.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project.R
import com.example.project.core.util.UiText
import com.example.project.domain.model.Playlist
import com.example.project.domain.model.User
import com.example.project.domain.repository.PlaylistRepository
import com.example.project.domain.repository.SettingsRepository
import com.example.project.domain.repository.SocialRepository
import com.example.project.domain.usecase.UpgradeToPremiumUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: User? = null,
    val avatarUrl: String? = null,
    val isUpgrading: Boolean = false,
    val followedUsers: List<User> = emptyList(),
    val publicPlaylists: List<Playlist> = emptyList(),
)

sealed interface ProfileEffect {
    data class Message(val text: UiText) : ProfileEffect
}

class ProfileViewModel(
    private val socialRepository: SocialRepository,
    private val playlistRepository: PlaylistRepository,
    private val settingsRepository: SettingsRepository,
    private val upgradeToPremium: UpgradeToPremiumUseCase,
) : ViewModel() {

    private val avatarOverride = MutableStateFlow<String?>(null)
    private val isUpgrading = MutableStateFlow(false)
    private val publicPlaylists = MutableStateFlow<List<Playlist>>(emptyList())

    private val _effects = Channel<ProfileEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    val uiState: StateFlow<ProfileUiState> = combine(
        socialRepository.observeCurrentUser(),
        socialRepository.observeFollowedUsers(),
        avatarOverride,
        isUpgrading,
        publicPlaylists,
    ) { user, followed, avatar, upgrading, playlists ->
        ProfileUiState(
            user = user,
            avatarUrl = avatar ?: user.avatarUrl,
            isUpgrading = upgrading,
            followedUsers = followed,
            publicPlaylists = playlists,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    init {
        loadPublicPlaylists()
    }

    private fun loadPublicPlaylists() {
        viewModelScope.launch {
            publicPlaylists.value = socialRepository.getPublicPlaylists("u0")
        }
    }

    fun upgrade() {
        if (isUpgrading.value) return
        viewModelScope.launch {
            isUpgrading.value = true
            upgradeToPremium()
            isUpgrading.value = false
            _effects.send(ProfileEffect.Message(UiText.from(R.string.upgrade_success)))
        }
    }

    fun changeAvatar() {
        // Simulated avatar change: rotate through deterministic picsum seeds.
        val seed = (1..999).random()
        avatarOverride.value = "https://picsum.photos/seed/avatar$seed/400/400"
        viewModelScope.launch {
            _effects.send(ProfileEffect.Message(UiText.from(R.string.avatar_changed)))
        }
    }

    fun logout() {
        viewModelScope.launch {
            settingsRepository.logout()
            _effects.send(ProfileEffect.Message(UiText.from(R.string.logged_out)))
        }
    }
}
