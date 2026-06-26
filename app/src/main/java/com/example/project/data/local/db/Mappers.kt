package com.example.project.data.local.db

import com.example.project.domain.model.ChatMessage
import com.example.project.domain.model.DownloadItem
import com.example.project.domain.model.DownloadState
import com.example.project.domain.model.MessageStatus
import com.example.project.domain.model.Song

fun LikedSongEntity.toSong() = Song(
    id = songId, title = title, artistId = artistId, artistName = artistName,
    album = album, coverImageUrl = coverImageUrl, audioUrl = audioUrl,
    durationMs = durationMs, genre = genre, isLiked = true,
)

fun Song.toLikedEntity(likedAt: Long) = LikedSongEntity(
    songId = id, title = title, artistId = artistId, artistName = artistName,
    album = album, coverImageUrl = coverImageUrl, audioUrl = audioUrl,
    durationMs = durationMs, genre = genre, likedAt = likedAt,
)

fun RecentlyPlayedEntity.toSong() = Song(
    id = songId, title = title, artistId = artistId, artistName = artistName,
    album = album, coverImageUrl = coverImageUrl, audioUrl = audioUrl,
    durationMs = durationMs, genre = genre,
)

fun Song.toRecentEntity(playedAt: Long) = RecentlyPlayedEntity(
    songId = id, title = title, artistId = artistId, artistName = artistName,
    album = album, coverImageUrl = coverImageUrl, audioUrl = audioUrl,
    durationMs = durationMs, genre = genre, playedAt = playedAt,
)

fun Song.toDownloadEntity(state: DownloadState, progress: Int, localPath: String?, addedAt: Long) =
    DownloadEntity(
        songId = id, title = title, artistId = artistId, artistName = artistName,
        album = album, coverImageUrl = coverImageUrl, audioUrl = audioUrl,
        durationMs = durationMs, genre = genre,
        state = state.name, progress = progress, localPath = localPath, addedAt = addedAt,
    )

fun DownloadEntity.toSong() = Song(
    id = songId, title = title, artistId = artistId, artistName = artistName,
    album = album, coverImageUrl = coverImageUrl, audioUrl = audioUrl,
    durationMs = durationMs, genre = genre, localPath = localPath,
)

fun DownloadEntity.toDownloadItem() = DownloadItem(
    song = toSong(),
    state = runCatching { DownloadState.valueOf(state) }.getOrDefault(DownloadState.QUEUED),
    progress = progress,
    addedAt = addedAt,
)

fun ChatMessageEntity.toDomain() = ChatMessage(
    id = id, conversationId = conversationId, senderId = senderId, text = text,
    timestamp = timestamp,
    status = runCatching { MessageStatus.valueOf(status) }.getOrDefault(MessageStatus.SENT),
    isFromMe = isFromMe, sharedSongId = sharedSongId, sharedSongTitle = sharedSongTitle,
    sharedSongArtist = sharedSongArtist, sharedSongCover = sharedSongCover,
)

fun ChatMessage.toEntity() = ChatMessageEntity(
    id = id, conversationId = conversationId, senderId = senderId, text = text,
    timestamp = timestamp, status = status.name, isFromMe = isFromMe,
    sharedSongId = sharedSongId, sharedSongTitle = sharedSongTitle,
    sharedSongArtist = sharedSongArtist, sharedSongCover = sharedSongCover,
)
