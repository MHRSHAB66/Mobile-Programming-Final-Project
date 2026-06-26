package com.example.project.domain.model

/** Origin of a playlist, used for the Playlists screen sections. */
enum class PlaylistType { GLOBAL, LOCAL, USER }

data class Playlist(
    val id: String,
    val title: String,
    val description: String,
    val coverImageUrl: String,
    val type: PlaylistType,
    val ownerName: String = "",
    val songIds: List<String> = emptyList(),
    val isPublic: Boolean = true,
)
