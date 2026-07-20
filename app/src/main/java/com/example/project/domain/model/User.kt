package com.example.project.domain.model

data class User(
    val id: String,
    val displayName: String,
    val handle: String,
    val avatarUrl: String,
    val isPremium: Boolean = false,
    val followers: Int = 0,
    val followingCount: Int = 0,
    val isFollowed: Boolean = false,
    val isOnline: Boolean = false,
    val publicPlaylistIds: List<String> = emptyList(),
)
