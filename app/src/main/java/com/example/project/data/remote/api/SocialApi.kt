package com.example.project.data.remote.api

import com.example.project.data.remote.api.dto.ArtistDto
import com.example.project.data.remote.api.dto.MessageResponseDto
import com.example.project.data.remote.api.dto.UserDto
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface SocialApi {
    @POST("users/{userId}/follow")
    suspend fun followUser(@Path("userId") userId: String): MessageResponseDto

    @DELETE("users/{userId}/follow")
    suspend fun unfollowUser(@Path("userId") userId: String): MessageResponseDto

    @GET("me/following")
    suspend fun getFollowing(): List<UserDto>

    @GET("users/{userId}/following")
    suspend fun getUserFollowing(@Path("userId") userId: String): List<UserDto>

    @GET("users/{userId}/followers")
    suspend fun getUserFollowers(@Path("userId") userId: String): List<UserDto>

    @POST("artists/{artistId}/follow")
    suspend fun followArtist(@Path("artistId") artistId: String): MessageResponseDto

    @DELETE("artists/{artistId}/follow")
    suspend fun unfollowArtist(@Path("artistId") artistId: String): MessageResponseDto

    @GET("me/following/artists")
    suspend fun getFollowingArtists(): List<ArtistDto>
}
