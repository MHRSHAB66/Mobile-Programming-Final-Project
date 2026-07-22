package com.example.project.domain.repository

import com.example.project.domain.model.Song
import kotlinx.coroutines.flow.Flow

/** Liked songs and recently played history. Likes sync with the Melodify API and cache in Room. */
interface LibraryRepository {
    fun observeLikedSongs(): Flow<List<Song>>
    fun observeLikedIds(): Flow<Set<String>>
    suspend fun isLiked(songId: String): Boolean
    suspend fun toggleLike(song: Song): Boolean
    suspend fun refreshLikes()
    suspend fun clearLikesCache()

    fun observeRecentlyPlayed(): Flow<List<Song>>
    suspend fun addRecentlyPlayed(song: Song)
}
