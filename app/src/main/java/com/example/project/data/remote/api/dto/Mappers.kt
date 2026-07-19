package com.example.project.data.remote.api.dto

import com.example.project.domain.model.Playlist
import com.example.project.domain.model.PlaylistType
import com.example.project.domain.model.User

fun UserDto.toDomainUser(): User = User(
    id = id,
    displayName = displayName,
    handle = if (handle.startsWith("@")) handle else "@$handle",
    avatarUrl = avatarUrl.orEmpty(),
    isPremium = isPremium,
)

fun PlaylistDto.toDomainPlaylist(): Playlist = Playlist(
    id = id,
    title = title,
    description = "",
    coverImageUrl = coverImageUrl.orEmpty(),
    type = when (category.lowercase()) {
        "world", "featured", "global" -> PlaylistType.GLOBAL
        "local" -> PlaylistType.LOCAL
        else -> PlaylistType.USER
    },
    isPublic = isPublic,
)
