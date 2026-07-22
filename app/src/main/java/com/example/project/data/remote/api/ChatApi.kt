package com.example.project.data.remote.api

import com.example.project.data.remote.api.dto.ChatMessageDto
import com.example.project.data.remote.api.dto.ConversationDto
import com.example.project.data.remote.api.dto.CreateChatRequestDto
import com.example.project.data.remote.api.dto.MessagePageDto
import com.example.project.data.remote.api.dto.SendMessageRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ChatApi {
    @GET("chats")
    suspend fun listChats(): List<ConversationDto>

    @POST("chats")
    suspend fun createChat(@Body body: CreateChatRequestDto): ConversationDto

    @GET("chats/{chatId}/messages")
    suspend fun getMessages(
        @Path("chatId") chatId: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50,
    ): MessagePageDto

    @POST("chats/{chatId}/messages")
    suspend fun sendMessage(
        @Path("chatId") chatId: String,
        @Body body: SendMessageRequestDto,
    ): ChatMessageDto

    @POST("chats/{chatId}/read")
    suspend fun markRead(
        @Path("chatId") chatId: String,
        @Query("up_to_message_id") upToMessageId: String,
    ): Response<Unit>
}
