package com.example.project.data.remote.api.dto

import com.squareup.moshi.Json

data class RegisterRequestDto(
    @Json(name = "display_name") val displayName: String,
    val handle: String,
    val password: String,
)

data class LoginRequestDto(
    val handle: String,
    val password: String,
)

data class TokenResponseDto(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "token_type") val tokenType: String = "bearer",
    val user: UserDto,
)

data class UserDto(
    val id: String,
    val handle: String,
    @Json(name = "display_name") val displayName: String,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    @Json(name = "is_premium") val isPremium: Boolean = false,
    @Json(name = "created_at") val createdAt: String? = null,
)

data class MessageResponseDto(
    val detail: String,
)
