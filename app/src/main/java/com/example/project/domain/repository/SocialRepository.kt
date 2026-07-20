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
    fun observeFollowedArtistIds(): Flow<Set<String>>
    suspend fun getFollowers(userId: String): List<User>
    suspend fun getFollowing(userId: String): List<User>

    /** Returns the new followed state after the toggle. */
    suspend fun toggleFollow(userId: String): Boolean

    /** Returns the new followed state after the toggle. */
    suspend fun toggleFollowArtist(artistId: String): Boolean

    suspend fun refreshFollowing()

    /** Clears in-memory follow/profile stats (call on logout / account switch). */
    fun clearSocialCache()

    suspend fun getPublicPlaylists(userId: String): List<Playlist>
}
