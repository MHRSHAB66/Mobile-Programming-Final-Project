package com.example.project.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.project.data.local.datastore.SettingsKeys
import com.example.project.domain.model.AppLanguage
import com.example.project.domain.model.FontSize
import com.example.project.domain.model.LOCAL_USER_ID
import com.example.project.domain.model.ThemeMode
import com.example.project.domain.model.UserSettings
import com.example.project.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val settings: Flow<UserSettings> = dataStore.data.map { prefs ->
        UserSettings(
            themeMode = prefs[SettingsKeys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            language = AppLanguage.fromTag(prefs[SettingsKeys.LANGUAGE]),
            fontSize = prefs[SettingsKeys.FONT_SIZE]?.let { runCatching { FontSize.valueOf(it) }.getOrNull() }
                ?: FontSize.NORMAL,
            isPremium = prefs[SettingsKeys.PREMIUM] ?: false,
            isLoggedIn = prefs[SettingsKeys.LOGGED_IN] ?: false,
            currentUserId = prefs[SettingsKeys.CURRENT_USER_ID] ?: LOCAL_USER_ID,
            displayName = prefs[SettingsKeys.USER_NAME],
            handle = prefs[SettingsKeys.USER_HANDLE],
            avatarUrl = prefs[SettingsKeys.USER_AVATAR],
        )
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[SettingsKeys.THEME] = mode.name }
    }

    override suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { it[SettingsKeys.LANGUAGE] = language.tag }
    }

    override suspend fun setFontSize(size: FontSize) {
        dataStore.edit { it[SettingsKeys.FONT_SIZE] = size.name }
    }

    override suspend fun setPremium(isPremium: Boolean) {
        dataStore.edit { it[SettingsKeys.PREMIUM] = isPremium }
    }

    override suspend fun setLoggedIn(isLoggedIn: Boolean) {
        dataStore.edit { it[SettingsKeys.LOGGED_IN] = isLoggedIn }
    }

    override suspend fun login(name: String, handle: String, avatarUrl: String) {
        dataStore.edit {
            it[SettingsKeys.LOGGED_IN] = true
            it[SettingsKeys.CURRENT_USER_ID] = LOCAL_USER_ID
            it[SettingsKeys.USER_NAME] = name
            it[SettingsKeys.USER_HANDLE] = handle
            it[SettingsKeys.USER_AVATAR] = avatarUrl
        }
    }

    override suspend fun logout() {
        // Single atomic write so the UI never observes a half-cleared state (prevents the
        // "stuck on logout" problem). Room data is intentionally preserved.
        dataStore.edit {
            it[SettingsKeys.LOGGED_IN] = false
            it[SettingsKeys.PREMIUM] = false
            it.remove(SettingsKeys.USER_NAME)
            it.remove(SettingsKeys.USER_HANDLE)
            it.remove(SettingsKeys.USER_AVATAR)
        }
    }
}
