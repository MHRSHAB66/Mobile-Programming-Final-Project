package com.example.project.data.repository

import com.example.project.data.mock.MockData
import com.example.project.domain.model.Playlist
import com.example.project.domain.model.User
import com.example.project.domain.repository.SettingsRepository
import com.example.project.domain.repository.SocialRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * In-memory social graph. Follow state lives in a StateFlow so the UI updates reactively;
 * a real backend would replace this with remote calls + a websocket/presence feed.
 */
class SocialRepositoryImpl(
    private val settingsRepository: SettingsRepository,
) : SocialRepository {

    private val followedIds = MutableStateFlow(
        MockData.users.filter { it.isFollowed }.map { it.id }.toSet()
    )

    override fun observeCurrentUser(): Flow<User> =
        settingsRepository.settings.map { settings ->
            // Reflect the signed-in identity (custom "create account" name/handle/avatar) while
            // keeping the stable local id so chat, follows and playlists keep working.
            MockData.currentUser.copy(
                id = settings.currentUserId,
                displayName = settings.displayName ?: MockData.currentUser.displayName,
                handle = settings.handle ?: MockData.currentUser.handle,
                avatarUrl = settings.avatarUrl?.takeIf { it.isNotBlank() }
                    ?: MockData.currentUser.avatarUrl,
                isPremium = settings.isPremium,
            )
        }

    override suspend fun searchUsers(query: String): List<User> = withContext(Dispatchers.IO) {
        val q = query.trim().lowercase()
        val followed = followedIds.value
        MockData.users
            .filter { it.displayName.lowercase().contains(q) || it.handle.lowercase().contains(q) }
            .map { it.copy(isFollowed = it.id in followed) }
    }

    override suspend fun getUser(id: String): User? = withContext(Dispatchers.IO) {
        MockData.userById(id)?.copy(isFollowed = id in followedIds.value)
    }

    override fun observeFollowedUsers(): Flow<List<User>> =
        followedIds.map { ids ->
            MockData.users.filter { it.id in ids }.map { it.copy(isFollowed = true) }
        }

    override suspend fun toggleFollow(userId: String): Boolean {
        val current = followedIds.value
        return if (userId in current) {
            followedIds.value = current - userId
            false
        } else {
            followedIds.value = current + userId
            true
        }
    }

    override suspend fun getPublicPlaylists(userId: String): List<Playlist> =
        withContext(Dispatchers.IO) {
            val user = MockData.userById(userId) ?: return@withContext emptyList()
            user.publicPlaylistIds.mapNotNull { MockData.playlistById(it) }
        }

    /** Combined signal kept for potential future presence/relationship streams. */
    val relationshipSignals: Flow<Unit> =
        combine(followedIds, settingsRepository.settings) { _, _ -> Unit }
}
