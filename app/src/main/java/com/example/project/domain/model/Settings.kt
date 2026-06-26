package com.example.project.domain.model

/** Theme selection persisted in DataStore and applied app-wide. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** App language. Drives both string resources and layout direction (RTL/LTR). */
enum class AppLanguage(val tag: String) {
    ENGLISH("en"),
    PERSIAN("fa");

    companion object {
        fun fromTag(tag: String?): AppLanguage =
            entries.firstOrNull { it.tag == tag } ?: ENGLISH
    }
}

/** Optional font scaling applied on top of the type scale. */
enum class FontSize(val scale: Float) {
    SMALL(0.9f),
    NORMAL(1.0f),
    LARGE(1.15f),
}

/** The internal id of the single local user slot (kept stable so chat/playlists keep working). */
const val LOCAL_USER_ID = "u0"

/** Aggregated user settings exposed by [com.example.project.domain.repository.SettingsRepository]. */
data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val fontSize: FontSize = FontSize.NORMAL,
    val isPremium: Boolean = false,
    // Starts logged out so the app opens on the (simulated) login/create-account screen.
    val isLoggedIn: Boolean = false,
    val currentUserId: String = LOCAL_USER_ID,
    // Custom identity from "create account"; null means use the demo defaults.
    val displayName: String? = null,
    val handle: String? = null,
    val avatarUrl: String? = null,
)
