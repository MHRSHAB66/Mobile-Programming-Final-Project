package com.example.project.data.remote.api.dto

import com.squareup.moshi.Json

data class CreateChatRequestDto(
    @Json(name = "user_id") val userId: String,
)

data class SendMessageRequestDto(
    val text: String? = null,
    @Json(name = "song_id") val songId: String? = null,
    @Json(name = "client_msg_id") val clientMsgId: String? = null,
)

data class SharedSongDto(
    val id: String,
    val title: String,
    @Json(name = "artist_name") val artistName: String,
    @Json(name = "cover_image_url") val coverImageUrl: String? = null,
)

data class ChatMessageDto(
    val id: String,
    @Json(name = "chat_id") val chatId: String,
    @Json(name = "sender_id") val senderId: String,
    val text: String? = null,
    val song: SharedSongDto? = null,
    @Json(name = "created_at") val createdAt: String,
    val status: String = "sent",
)

data class MessagePageDto(
    val items: List<ChatMessageDto>,
    val page: Int,
    val limit: Int,
    val total: Int,
)

data class ConversationDto(
    val id: String,
    @Json(name = "peer_id") val peerId: String,
    @Json(name = "peer_handle") val peerHandle: String,
    @Json(name = "peer_display_name") val peerDisplayName: String,
    @Json(name = "peer_avatar_url") val peerAvatarUrl: String? = null,
    @Json(name = "peer_is_premium") val peerIsPremium: Boolean = false,
    @Json(name = "peer_is_online") val peerIsOnline: Boolean = false,
    @Json(name = "last_message") val lastMessage: String = "",
    @Json(name = "last_timestamp") val lastTimestamp: String? = null,
    @Json(name = "unread_count") val unreadCount: Int = 0,
)

data class ChatSocketPayloadDto(
    val type: String,
    val message: ChatMessageDto? = null,
    @Json(name = "chat_id") val chatId: String? = null,
    @Json(name = "message_id") val messageId: String? = null,
    @Json(name = "user_id") val userId: String? = null,
    @Json(name = "is_online") val isOnline: Boolean? = null,
    @Json(name = "is_typing") val isTyping: Boolean? = null,
    val status: String? = null,
    val conversation: ConversationDto? = null,
)
