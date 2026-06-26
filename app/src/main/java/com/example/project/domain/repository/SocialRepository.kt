package com.example.project.domain.repository

import com.example.project.domain.model.Playlist
import com.example.project.domain.model.User
import kotlinx.coroutines.flow.Flow

/** Users, following relationships, and friends' public playlists. */
interface SocialRepository {
    fun observeCurrentUser(): Flow<User>
    suspend fun searchUsers(query: String): List<User>
    suspend fun getUser(id: String): User?

    fun observeFollowedUsers(): Flow<List<User>>
    suspend fun toggleFollow(userId: String): Boolean

    suspend fun getPublicPlaylists(userId: String): List<Playlist>
}
