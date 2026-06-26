package com.example.project.ui.followed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project.domain.model.User
import com.example.project.domain.repository.SocialRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FollowedViewModel(
    private val socialRepository: SocialRepository,
) : ViewModel() {

    val users: StateFlow<List<User>> = socialRepository.observeFollowedUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun unfollow(userId: String) {
        viewModelScope.launch { socialRepository.toggleFollow(userId) }
    }
}
