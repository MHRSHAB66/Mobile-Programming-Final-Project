package com.example.project.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project.R
import com.example.project.core.util.UiText
import com.example.project.data.remote.api.AuthException
import com.example.project.domain.model.Playlist
import com.example.project.domain.model.User
import com.example.project.domain.repository.AuthRepository
import com.example.project.domain.repository.ProfileRepository
import com.example.project.domain.repository.SettingsRepository
import com.example.project.domain.repository.SocialRepository
import com.example.project.domain.usecase.UpgradeToPremiumUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: User? = null,
    val avatarUrl: String? = null,
    val isUpgrading: Boolean = false,
    val isChangingAvatar: Boolean = false,
    val followedUsers: List<User> = emptyList(),
    val publicPlaylists: List<Playlist> = emptyList(),
)

sealed interface ProfileEffect {
    data class Message(val text: UiText) : ProfileEffect
}

class ProfileViewModel(
    private val socialRepository: SocialRepository,
    private val profileRepository: ProfileRepository,
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository,
    private val upgradeToPremium: UpgradeToPremiumUseCase,
) : ViewModel() {

    private val isUpgrading = MutableStateFlow(false)
    private val isChangingAvatar = MutableStateFlow(false)
    private val publicPlaylists = MutableStateFlow<List<Playlist>>(emptyList())

    private val _effects = Channel<ProfileEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    val uiState: StateFlow<ProfileUiState> = combine(
        socialRepository.observeCurrentUser(),
        socialRepository.observeFollowedUsers(),
        isUpgrading,
        isChangingAvatar,
        publicPlaylists,
    ) { user, followed, upgrading, changingAvatar, playlists ->
        ProfileUiState(
            user = user,
            avatarUrl = user.avatarUrl.takeIf { it.isNotBlank() },
            isUpgrading = upgrading,
            isChangingAvatar = changingAvatar,
            followedUsers = followed,
            publicPlaylists = playlists,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    init {
        viewModelScope.launch {
            settingsRepository.settings
                .map { it.currentUserId }
                .distinctUntilChanged()
                .collect {
                    socialRepository.refreshFollowing()
                    profileRepository.refreshProfile()
                    loadPublicPlaylists()
                }
        }
    }

    private suspend fun loadPublicPlaylists() {
        val userId = settingsRepository.settings.first().currentUserId
        val remote = profileRepository.getUserPublicPlaylists(userId)
        publicPlaylists.value = remote.getOrElse {
            socialRepository.getPublicPlaylists(userId)
        }
    }

    fun upgrade() {
        if (isUpgrading.value) return
        viewModelScope.launch {
            isUpgrading.value = true
            val result = upgradeToPremium()
            isUpgrading.value = false
            result
                .onSuccess {
                    _effects.send(ProfileEffect.Message(UiText.from(R.string.upgrade_success)))
                }
                .onFailure { error ->
                    val text = (error as? AuthException)?.uiText
                        ?: UiText.from(R.string.upgrade_failed)
                    _effects.send(ProfileEffect.Message(text))
                }
        }
    }

    fun onAvatarImageSelected(bytes: ByteArray, mimeType: String) {
        if (isChangingAvatar.value) return
        viewModelScope.launch {
            isChangingAvatar.value = true
            val result = profileRepository.uploadAvatar(bytes, mimeType)
            isChangingAvatar.value = false
            result
                .onSuccess {
                    _effects.send(ProfileEffect.Message(UiText.from(R.string.avatar_changed)))
                }
                .onFailure { error ->
                    val text = when (error) {
                        is com.example.project.data.repository.AvatarTooLargeException ->
                            UiText.from(R.string.avatar_too_large)
                        is AuthException -> error.uiText
                        else -> UiText.from(R.string.avatar_change_failed)
                    }
                    _effects.send(ProfileEffect.Message(text))
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _effects.send(ProfileEffect.Message(UiText.from(R.string.logged_out)))
        }
    }
}
