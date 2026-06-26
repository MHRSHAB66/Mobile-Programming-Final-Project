package com.example.project.domain.repository

import com.example.project.domain.model.Song
import kotlinx.coroutines.flow.Flow

/** Liked songs and recently played history, persisted in Room. */
interface LibraryRepository {
    fun observeLikedSongs(): Flow<List<Song>>
    fun observeLikedIds(): Flow<Set<String>>
    suspend fun isLiked(songId: String): Boolean
    suspend fun toggleLike(song: Song): Boolean

    fun observeRecentlyPlayed(): Flow<List<Song>>
    suspend fun addRecentlyPlayed(song: Song)
}
