package com.example.project.data.remote.api.dto

import com.squareup.moshi.Json

data class SongDto(
    val id: String,
    val title: String,
    @Json(name = "artist_id") val artistId: String,
    @Json(name = "artist_name") val artistName: String,
    val album: String? = null,
    val genre: String? = null,
    @Json(name = "cover_image_url") val coverImageUrl: String? = null,
    @Json(name = "audio_url") val audioUrl: String,
    @Json(name = "duration_ms") val durationMs: Long? = null,
    val popularity: Int = 0,
    @Json(name = "is_local_music") val isLocalMusic: Boolean = false,
)

data class ArtistDto(
    val id: String,
    val name: String,
    @Json(name = "image_url") val imageUrl: String? = null,
    val bio: String? = null,
    val followers: Int = 0,
    @Json(name = "is_followed") val isFollowed: Boolean = false,
)

data class CatalogPlaylistDto(
    val id: String,
    val title: String,
    @Json(name = "cover_image_url") val coverImageUrl: String? = null,
    val category: String,
    @Json(name = "is_public") val isPublic: Boolean = true,
    @Json(name = "owner_user_id") val ownerUserId: String? = null,
    val description: String? = null,
)

data class HomeCarouselDto(
    @Json(name = "sort_order") val sortOrder: Int = 0,
    val headline: String? = null,
    @Json(name = "image_url") val imageUrl: String? = null,
    val song: SongDto? = null,
    val playlist: CatalogPlaylistDto? = null,
)

data class HomeFeedDto(
    val carousel: List<HomeCarouselDto> = emptyList(),
    @Json(name = "most_popular") val mostPopular: List<SongDto> = emptyList(),
    @Json(name = "new_releases") val newReleases: List<SongDto> = emptyList(),
    @Json(name = "global_playlists") val globalPlaylists: List<CatalogPlaylistDto> = emptyList(),
    @Json(name = "local_playlists") val localPlaylists: List<CatalogPlaylistDto> = emptyList(),
    @Json(name = "top_artists") val topArtists: List<ArtistDto> = emptyList(),
)

data class SongPageDto(
    val items: List<SongDto> = emptyList(),
    val page: Int = 1,
    val limit: Int = 50,
    val total: Int = 0,
)

data class ArtistPageDto(
    val items: List<ArtistDto> = emptyList(),
    val page: Int = 1,
    val limit: Int = 50,
    val total: Int = 0,
)

data class SearchResponseDto(
    val songs: List<SongDto> = emptyList(),
    val artists: List<ArtistDto> = emptyList(),
    val playlists: List<CatalogPlaylistDto> = emptyList(),
    val users: List<UserDto> = emptyList(),
)
