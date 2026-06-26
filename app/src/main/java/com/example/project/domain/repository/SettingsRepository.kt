package com.example.project.domain.repository

import com.example.project.domain.model.AppLanguage
import com.example.project.domain.model.FontSize
import com.example.project.domain.model.ThemeMode
import com.example.project.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

/** App settings persisted in Preferences DataStore. */
interface SettingsRepository {
    val settings: Flow<UserSettings>

    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setLanguage(language: AppLanguage)
    suspend fun setFontSize(size: FontSize)
    suspend fun setPremium(isPremium: Boolean)
    suspend fun setLoggedIn(isLoggedIn: Boolean)

    /** Simulated sign-in: stores the chosen identity and marks the session as logged in (atomic). */
    suspend fun login(name: String, handle: String, avatarUrl: String)

    /** Simulated sign-out: clears the identity, resets premium, and logs out (atomic). */
    suspend fun logout()
}
