package com.example.project.ui.navigation

import androidx.compose.runtime.Immutable

/** Bundles the avatar + the three shared top-bar actions so main screens forward one object. */
@Immutable
data class TopBarActions(
    val avatarUrl: String?,
    val onProfileClick: () -> Unit,
    val onNotificationsClick: () -> Unit,
    val onSettingsClick: () -> Unit,
)
