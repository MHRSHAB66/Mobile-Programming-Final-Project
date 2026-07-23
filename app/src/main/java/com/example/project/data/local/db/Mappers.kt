package com.example.project.data.local.db

import com.example.project.domain.model.Artist
import com.example.project.domain.model.ChatMessage
import com.example.project.domain.model.DownloadItem
import com.example.project.domain.model.DownloadState
import com.example.project.domain.model.MessageStatus
import com.example.project.domain.model.Playlist
import com.example.project.domain.model.PlaylistType
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

fun Song.toDownloadEntity(userId: String, state: DownloadState, progress: Int, localPath: String?, addedAt: Long) =
    DownloadEntity(
        songId = id, userId = userId, title = title, artistId = artistId, artistName = artistName,
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

fun Song.toCachedEntity(cachedAt: Long, popularity: Int = 0) = CachedSongEntity(
    id = id,
    title = title,
    artistId = artistId,
    artistName = artistName,
    album = album,
    coverImageUrl = coverImageUrl,
    audioUrl = audioUrl,
    durationMs = durationMs,
    genre = genre,
    popularity = popularity,
    cachedAt = cachedAt,
)

fun CachedSongEntity.toSong() = Song(
    id = id,
    title = title,
    artistId = artistId,
    artistName = artistName,
    album = album,
    coverImageUrl = coverImageUrl,
    audioUrl = audioUrl,
    durationMs = durationMs,
    genre = genre,
)

fun Artist.toCachedEntity(cachedAt: Long) = CachedArtistEntity(
    id = id,
    name = name,
    imageUrl = imageUrl,
    followers = followers,
    bio = bio,
    cachedAt = cachedAt,
)

fun CachedArtistEntity.toArtist() = Artist(
    id = id,
    name = name,
    imageUrl = imageUrl,
    followers = followers,
    bio = bio,
)

fun Playlist.toCachedEntity(cachedAt: Long) = CachedPlaylistEntity(
    id = id,
    title = title,
    description = description,
    coverImageUrl = coverImageUrl,
    type = type.name,
    isPublic = isPublic,
    cachedAt = cachedAt,
)

fun CachedPlaylistEntity.toPlaylist() = Playlist(
    id = id,
    title = title,
    description = description,
    coverImageUrl = coverImageUrl,
    type = runCatching { PlaylistType.valueOf(type) }.getOrDefault(PlaylistType.USER),
    isPublic = isPublic,
)
