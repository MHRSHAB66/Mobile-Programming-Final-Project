package com.example.project.data.remote.api

import com.example.project.data.remote.api.dto.ArtistDto
import com.example.project.data.remote.api.dto.ArtistPageDto
import com.example.project.data.remote.api.dto.CatalogPlaylistDto
import com.example.project.data.remote.api.dto.HomeFeedDto
import com.example.project.data.remote.api.dto.SearchResponseDto
import com.example.project.data.remote.api.dto.SongDto
import com.example.project.data.remote.api.dto.SongPageDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface CatalogApi {
    @GET("home")
    suspend fun getHome(): HomeFeedDto

    @GET("songs")
    suspend fun getSongs(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 200,
    ): SongPageDto

    @GET("songs/{songId}")
    suspend fun getSong(@Path("songId") songId: String): SongDto

    @GET("artists")
    suspend fun getArtists(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 200,
    ): ArtistPageDto

    @GET("artists/{artistId}")
    suspend fun getArtist(@Path("artistId") artistId: String): ArtistDto

    @GET("artists/{artistId}/songs")
    suspend fun getArtistSongs(
        @Path("artistId") artistId: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 200,
    ): SongPageDto

    @GET("playlists")
    suspend fun getPlaylists(
        @Query("category") category: String? = null,
    ): List<CatalogPlaylistDto>

    @GET("playlists/{playlistId}")
    suspend fun getPlaylist(@Path("playlistId") playlistId: String): CatalogPlaylistDto

    @GET("playlists/{playlistId}/songs")
    suspend fun getPlaylistSongs(
        @Path("playlistId") playlistId: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 200,
    ): SongPageDto

    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("type") type: String = "all",
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 30,
    ): SearchResponseDto
}
