package com.example.project.ui.followed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project.domain.model.User
import com.example.project.domain.repository.SocialRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ConnectionsUiState(
    val users: List<User> = emptyList(),
    val isFollowersMode: Boolean = false,
)

class ConnectionsViewModel(
    private val userId: String,
    private val mode: String,
    private val socialRepository: SocialRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ConnectionsUiState(isFollowersMode = mode == MODE_FOLLOWERS)
    )
    val uiState: StateFlow<ConnectionsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun toggleFollow(targetUserId: String) {
        viewModelScope.launch {
            socialRepository.toggleFollow(targetUserId)
            load()
        }
    }

    fun refresh() {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val users = if (mode == MODE_FOLLOWERS) {
                socialRepository.getFollowers(userId)
            } else {
                socialRepository.getFollowing(userId)
            }
            _uiState.value = ConnectionsUiState(
                users = users,
                isFollowersMode = mode == MODE_FOLLOWERS,
            )
        }
    }

    companion object {
        const val MODE_FOLLOWERS = "followers"
        const val MODE_FOLLOWING = "following"
    }
}
