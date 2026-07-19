package com.example.project.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project.domain.model.AppLanguage
import com.example.project.domain.model.FontSize
import com.example.project.domain.model.ThemeMode
import com.example.project.domain.model.UserSettings
import com.example.project.domain.repository.AuthRepository
import com.example.project.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    val settings: StateFlow<UserSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    fun setLanguage(language: AppLanguage) = viewModelScope.launch { settingsRepository.setLanguage(language) }
    fun setFontSize(size: FontSize) = viewModelScope.launch { settingsRepository.setFontSize(size) }

    // Calls backend logout (best-effort) then clears the local session.
    fun logout() = viewModelScope.launch { authRepository.logout() }
}
