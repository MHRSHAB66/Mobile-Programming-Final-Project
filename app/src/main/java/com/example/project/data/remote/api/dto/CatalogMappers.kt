package com.example.project.data.remote.api.dto

import com.example.project.data.remote.api.ApiConfig
import com.example.project.domain.model.Artist
import com.example.project.domain.model.Playlist
import com.example.project.domain.model.PlaylistType
import com.example.project.domain.model.Song

/** @see ApiConfig.rewriteUrl */
fun rewriteBackendUrl(url: String?): String = ApiConfig.rewriteUrl(url)

fun SongDto.toDomainSong(): Song = Song(
    id = id,
    title = title,
    artistId = artistId,
    artistName = artistName,
    album = album.orEmpty(),
    coverImageUrl = rewriteBackendUrl(coverImageUrl),
    audioUrl = rewriteBackendUrl(audioUrl),
    durationMs = durationMs ?: 0L,
    genre = genre.orEmpty(),
)

fun ArtistDto.toDomainArtist(): Artist = Artist(
    id = id,
    name = name,
    imageUrl = rewriteBackendUrl(imageUrl),
    followers = followers,
    bio = bio.orEmpty(),
)

fun CatalogPlaylistDto.toDomainPlaylist(): Playlist = Playlist(
    id = id,
    title = title,
    description = description.orEmpty(),
    coverImageUrl = rewriteBackendUrl(coverImageUrl),
    type = when (category.lowercase()) {
        "world", "featured", "global" -> PlaylistType.GLOBAL
        "local" -> PlaylistType.LOCAL
        else -> PlaylistType.USER
    },
    isPublic = isPublic,
)
