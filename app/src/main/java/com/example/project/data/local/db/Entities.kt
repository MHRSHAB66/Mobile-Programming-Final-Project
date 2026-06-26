package com.example.project.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Liked songs — full snapshot so the library works offline. */
@Entity(tableName = "liked_songs")
data class LikedSongEntity(
    @PrimaryKey val songId: String,
    val title: String,
    val artistId: String,
    val artistName: String,
    val album: String,
    val coverImageUrl: String,
    val audioUrl: String,
    val durationMs: Long,
    val genre: String,
    val likedAt: Long,
)

/** Recently played history. PK on songId so replays move the song to the top. */
@Entity(tableName = "recently_played")
data class RecentlyPlayedEntity(
    @PrimaryKey val songId: String,
    val title: String,
    val artistId: String,
    val artistName: String,
    val album: String,
    val coverImageUrl: String,
    val audioUrl: String,
    val durationMs: Long,
    val genre: String,
    val playedAt: Long,
)

/** Downloaded songs and their offline file path / status. */
@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val songId: String,
    val title: String,
    val artistId: String,
    val artistName: String,
    val album: String,
    val coverImageUrl: String,
    val audioUrl: String,
    val durationMs: Long,
    val genre: String,
    val state: String,
    val progress: Int,
    val localPath: String?,
    val addedAt: Long,
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val query: String,
    val createdAt: Long,
)

/** Offline cache of chat messages. */
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val text: String,
    val timestamp: Long,
    val status: String,
    val isFromMe: Boolean,
    val sharedSongId: String?,
    val sharedSongTitle: String?,
    val sharedSongArtist: String?,
    val sharedSongCover: String?,
)
