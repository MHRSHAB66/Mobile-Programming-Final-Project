package com.example.project.data.remote.api

import com.example.project.data.remote.api.dto.AvatarUrlRequestDto
import com.example.project.data.remote.api.dto.LoginRequestDto
import com.example.project.data.remote.api.dto.MessageResponseDto
import com.example.project.data.remote.api.dto.PlaylistDto
import com.example.project.data.remote.api.dto.RegisterRequestDto
import com.example.project.data.remote.api.dto.TokenResponseDto
import com.example.project.data.remote.api.dto.UpdateProfileRequestDto
import com.example.project.data.remote.api.dto.UserDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import okhttp3.MultipartBody

interface AuthApi {
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequestDto): TokenResponseDto

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequestDto): TokenResponseDto

    @POST("auth/logout")
    suspend fun logout(): MessageResponseDto

    @GET("me")
    suspend fun me(): UserDto

    @PATCH("me")
    suspend fun updateMe(@Body body: UpdateProfileRequestDto): UserDto

    @POST("me/avatar/url")
    suspend fun setAvatarUrl(@Body body: AvatarUrlRequestDto): UserDto

    @Multipart
    @POST("me/avatar")
    suspend fun uploadAvatar(@Part file: MultipartBody.Part): UserDto

    @POST("me/premium/upgrade")
    suspend fun upgradePremium(): UserDto

    @GET("users/{userId}")
    suspend fun getUser(@Path("userId") userId: String): UserDto

    @GET("users/{userId}/playlists")
    suspend fun getUserPlaylists(@Path("userId") userId: String): List<PlaylistDto>
}
