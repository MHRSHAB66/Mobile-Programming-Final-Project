package com.example.project.data.remote.api.dto

import com.squareup.moshi.Json

data class UpdateProfileRequestDto(
    @Json(name = "display_name") val displayName: String? = null,
    val handle: String? = null,
)

data class AvatarUrlRequestDto(
    @Json(name = "avatar_url") val avatarUrl: String,
)

data class PlaylistDto(
    val id: String,
    val title: String,
    @Json(name = "cover_image_url") val coverImageUrl: String? = null,
    val category: String,
    @Json(name = "is_public") val isPublic: Boolean = true,
    @Json(name = "owner_user_id") val ownerUserId: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
)
