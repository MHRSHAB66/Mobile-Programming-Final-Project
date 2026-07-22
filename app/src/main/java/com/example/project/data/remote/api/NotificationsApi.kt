package com.example.project.data.remote.api

import com.example.project.data.remote.api.dto.MessageResponseDto
import com.example.project.data.remote.api.dto.NotificationDto
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface NotificationsApi {
    @GET("notifications")
    suspend fun getNotifications(@Query("limit") limit: Int = 50): List<NotificationDto>

    @POST("notifications/{id}/read")
    suspend fun markRead(@Path("id") id: String): MessageResponseDto

    @POST("notifications/read-all")
    suspend fun markAllRead(): MessageResponseDto
}
