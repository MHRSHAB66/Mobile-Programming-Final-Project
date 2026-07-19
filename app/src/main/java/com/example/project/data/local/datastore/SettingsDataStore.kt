package com.example.project.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

/** Single Preferences DataStore instance for the whole app. */
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

object SettingsKeys {
    val THEME = stringPreferencesKey("theme_mode")
    val LANGUAGE = stringPreferencesKey("language")
    val FONT_SIZE = stringPreferencesKey("font_size")
    val PREMIUM = booleanPreferencesKey("is_premium")
    val LOGGED_IN = booleanPreferencesKey("is_logged_in")

    // Simulated account: identity of the locally signed-in user.
    val CURRENT_USER_ID = stringPreferencesKey("current_user_id")
    val USER_NAME = stringPreferencesKey("user_name")
    val USER_HANDLE = stringPreferencesKey("user_handle")
    val USER_AVATAR = stringPreferencesKey("user_avatar")
    val ACCESS_TOKEN = stringPreferencesKey("access_token")
}
