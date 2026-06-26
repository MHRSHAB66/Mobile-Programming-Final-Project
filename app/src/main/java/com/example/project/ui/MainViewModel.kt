package com.example.project.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project.domain.model.UserSettings
import com.example.project.domain.repository.SettingsRepository
import com.example.project.domain.repository.SocialRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Shell-level state: the current user's avatar (for the shared top bar) and app settings. */
class MainViewModel(
    socialRepository: SocialRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val avatarUrl: StateFlow<String?> = socialRepository.observeCurrentUser()
        .map { it.avatarUrl }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val settings: StateFlow<UserSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserSettings())
}
