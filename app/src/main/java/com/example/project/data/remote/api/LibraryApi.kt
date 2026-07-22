package com.example.project.data.remote.api

import com.example.project.data.remote.api.dto.MessageResponseDto
import com.example.project.data.remote.api.dto.SongDto
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface LibraryApi {
    @GET("me/likes")
    suspend fun getLikedSongs(): List<SongDto>

    @POST("songs/{songId}/like")
    suspend fun likeSong(@Path("songId") songId: String): MessageResponseDto

    @DELETE("songs/{songId}/like")
    suspend fun unlikeSong(@Path("songId") songId: String): MessageResponseDto
}
