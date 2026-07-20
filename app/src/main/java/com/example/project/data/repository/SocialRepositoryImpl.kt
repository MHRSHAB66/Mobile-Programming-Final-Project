package com.example.project.data.repository

import com.example.project.data.remote.api.AuthApi
import com.example.project.data.remote.api.CatalogApi
import com.example.project.data.remote.api.SocialApi
import com.example.project.data.remote.api.dto.toDomainPlaylist
import com.example.project.data.remote.api.dto.toDomainUser
import com.example.project.domain.model.Playlist
import com.example.project.domain.model.User
import com.example.project.domain.repository.SettingsRepository
import com.example.project.domain.repository.SocialRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext

/**
 * Social graph backed by Melodify FastAPI. Follow lists are cached in memory and refreshed
 * from `/me/following` (and artists) so Followed / profile screens stay reactive.
 */
class SocialRepositoryImpl(
    private val socialApi: SocialApi,
    private val authApi: AuthApi,
    private val catalogApi: CatalogApi,
    private val settingsRepository: SettingsRepository,
) : SocialRepository {

    private val followedUsers = MutableStateFlow<List<User>>(emptyList())
    private val followedArtistIds = MutableStateFlow<Set<String>>(emptySet())
    private val currentUserRemote = MutableStateFlow<User?>(null)

    override fun observeCurrentUser(): Flow<User> =
        combine(settingsRepository.settings, currentUserRemote, followedUsers) { settings, remote, following ->
            val remoteForSession = remote?.takeIf { it.id == settings.currentUserId }
            User(
                id = settings.currentUserId,
                displayName = settings.displayName.orEmpty()
                    .ifBlank { remoteForSession?.displayName ?: "User" },
                handle = settings.handle.orEmpty()
                    .ifBlank { remoteForSession?.handle ?: "@user" },
                avatarUrl = settings.avatarUrl.orEmpty()
                    .ifBlank { remoteForSession?.avatarUrl.orEmpty() },
                isPremium = settings.isPremium,
                followers = remoteForSession?.followers ?: 0,
                followingCount = remoteForSession?.followingCount ?: following.size,
            )
        }

    override suspend fun searchUsers(query: String): List<User> = withContext(Dispatchers.IO) {
        runCatching {
            catalogApi.search(query = query.trim(), type = "user", page = 1, limit = 40)
                .users
                .map { it.toDomainUser() }
        }.getOrDefault(emptyList())
    }

    override suspend fun getUser(id: String): User? = withContext(Dispatchers.IO) {
        runCatching { authApi.getUser(id).toDomainUser() }.getOrNull()
    }

    override fun observeFollowedUsers(): Flow<List<User>> =
        followedUsers.onStart { refreshFollowing() }

    override fun observeFollowedArtistIds(): Flow<Set<String>> =
        followedArtistIds.onStart { refreshFollowing() }

    override suspend fun getFollowers(userId: String): List<User> = withContext(Dispatchers.IO) {
        runCatching {
            socialApi.getUserFollowers(userId).map { it.toDomainUser() }
        }.getOrDefault(emptyList())
    }

    override suspend fun getFollowing(userId: String): List<User> = withContext(Dispatchers.IO) {
        runCatching {
            val currentUserId = settingsRepository.settings.map { it.currentUserId }.first()
            if (userId == currentUserId) {
                socialApi.getFollowing().map { it.toDomainUser().copy(isFollowed = true) }
            } else {
                socialApi.getUserFollowing(userId).map { it.toDomainUser() }
            }
        }.getOrDefault(emptyList())
    }

    override suspend fun toggleFollow(userId: String): Boolean = withContext(Dispatchers.IO) {
        val currentlyFollowed = followedUsers.value.any { it.id == userId }
        runCatching {
            if (currentlyFollowed) {
                socialApi.unfollowUser(userId)
                followedUsers.value = followedUsers.value.filterNot { it.id == userId }
                refreshCurrentUser()
                false
            } else {
                socialApi.followUser(userId)
                val user = runCatching { authApi.getUser(userId).toDomainUser() }.getOrNull()
                    ?.copy(isFollowed = true)
                if (user != null) {
                    followedUsers.value = listOf(user) + followedUsers.value.filterNot { it.id == userId }
                } else {
                    refreshFollowing()
                }
                refreshCurrentUser()
                true
            }
        }.getOrElse {
            currentlyFollowed
        }
    }

    override suspend fun toggleFollowArtist(artistId: String): Boolean =
        withContext(Dispatchers.IO) {
            val currentlyFollowed = artistId in followedArtistIds.value
            runCatching {
                if (currentlyFollowed) {
                    socialApi.unfollowArtist(artistId)
                    followedArtistIds.value = followedArtistIds.value - artistId
                    false
                } else {
                    socialApi.followArtist(artistId)
                    followedArtistIds.value = followedArtistIds.value + artistId
                    true
                }
            }.getOrElse { currentlyFollowed }
        }

    override suspend fun refreshFollowing() = withContext(Dispatchers.IO) {
        runCatching {
            followedUsers.value = socialApi.getFollowing().map { it.toDomainUser().copy(isFollowed = true) }
        }
        runCatching {
            followedArtistIds.value = socialApi.getFollowingArtists().map { it.id }.toSet()
        }
        refreshCurrentUser()
        Unit
    }

    override fun clearSocialCache() {
        followedUsers.value = emptyList()
        followedArtistIds.value = emptySet()
        currentUserRemote.value = null
    }

    override suspend fun getPublicPlaylists(userId: String): List<Playlist> =
        withContext(Dispatchers.IO) {
            runCatching {
                authApi.getUserPlaylists(userId).map { it.toDomainPlaylist() }
            }.getOrDefault(emptyList())
        }

    private suspend fun refreshCurrentUser() {
        runCatching {
            currentUserRemote.value = authApi.me().toDomainUser()
        }
    }
}
