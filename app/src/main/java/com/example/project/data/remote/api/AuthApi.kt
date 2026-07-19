package com.example.project.data.remote.api

import com.example.project.data.remote.api.dto.LoginRequestDto
import com.example.project.data.remote.api.dto.MessageResponseDto
import com.example.project.data.remote.api.dto.RegisterRequestDto
import com.example.project.data.remote.api.dto.TokenResponseDto
import com.example.project.data.remote.api.dto.UserDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequestDto): TokenResponseDto

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequestDto): TokenResponseDto

    @POST("auth/logout")
    suspend fun logout(): MessageResponseDto

    @GET("me")
    suspend fun me(): UserDto
}
