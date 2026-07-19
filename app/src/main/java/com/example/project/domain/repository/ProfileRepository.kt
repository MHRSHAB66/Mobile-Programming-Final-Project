package com.example.project.domain.repository

import com.example.project.domain.model.Playlist
import com.example.project.domain.model.User

/** Profile & premium against the Melodify backend (with local DataStore sync). */
interface ProfileRepository {
    /** Pull `/me` into DataStore. No-op success when offline / demo (no token). */
    suspend fun refreshProfile(): Result<Unit>

    suspend fun setAvatarUrl(avatarUrl: String): Result<Unit>

    /** Upload a device image as the avatar (`POST /me/avatar`). */
    suspend fun uploadAvatar(bytes: ByteArray, mimeType: String): Result<Unit>

    suspend fun upgradePremium(): Result<Unit>

    suspend fun getUser(userId: String): Result<User>

    suspend fun getUserPublicPlaylists(userId: String): Result<List<Playlist>>
}
