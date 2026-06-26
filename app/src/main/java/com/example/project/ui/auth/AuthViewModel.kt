package com.example.project.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project.data.mock.MockData
import com.example.project.domain.repository.SettingsRepository
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Simulated authentication. There is no real auth backend — "create account / login" simply
 * stores the chosen identity in DataStore and flips the session to logged-in. The root of the
 * app observes that flag and swaps between the Auth screen and the main app.
 */
class AuthViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    fun login(name: String, handle: String) {
        viewModelScope.launch {
            val cleanName = name.trim().ifBlank { "Guest" }
            val rawHandle = handle.trim().removePrefix("@")
                .ifBlank { cleanName.lowercase().replace(" ", "") }
            val cleanHandle = "@$rawHandle"
            val avatar = "https://picsum.photos/seed/user${abs(cleanHandle.hashCode())}/400/400"
            settingsRepository.login(cleanName, cleanHandle, avatar)
        }
    }

    /** Quick path for the demo: sign in as the prebuilt sample user. */
    fun continueAsDemo() {
        viewModelScope.launch {
            settingsRepository.login(
                name = MockData.currentUser.displayName,
                handle = MockData.currentUser.handle,
                avatarUrl = MockData.currentUser.avatarUrl,
            )
        }
    }
}
