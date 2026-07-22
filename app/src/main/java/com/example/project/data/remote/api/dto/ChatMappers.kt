package com.example.project.data.remote.api.dto

import com.example.project.data.remote.api.ApiConfig
import com.example.project.domain.model.ChatMessage
import com.example.project.domain.model.Conversation
import com.example.project.domain.model.MessageStatus
import com.example.project.domain.model.User
import java.time.Instant

fun ConversationDto.toDomainConversation(): Conversation = Conversation(
    id = id,
    peer = User(
        id = peerId,
        displayName = peerDisplayName,
        handle = if (peerHandle.startsWith("@")) peerHandle else "@$peerHandle",
        avatarUrl = ApiConfig.rewriteUrl(peerAvatarUrl),
        isPremium = peerIsPremium,
        isOnline = peerIsOnline,
    ),
    lastMessage = lastMessage,
    lastTimestamp = lastTimestamp?.let { parseInstant(it) } ?: 0L,
    unreadCount = unreadCount,
)

fun ChatMessageDto.toDomainMessage(currentUserId: String): ChatMessage {
    val fromMe = senderId == currentUserId
    return ChatMessage(
        id = id,
        conversationId = chatId,
        senderId = senderId,
        text = text.orEmpty(),
        timestamp = parseInstant(createdAt),
        status = status.toMessageStatus(fromMe),
        isFromMe = fromMe,
        sharedSongId = song?.id,
        sharedSongTitle = song?.title,
        sharedSongArtist = song?.artistName,
        sharedSongCover = ApiConfig.rewriteUrl(song?.coverImageUrl),
    )
}

private fun String.toMessageStatus(fromMe: Boolean): MessageStatus = when (lowercase()) {
    "read" -> MessageStatus.READ
    "sent" -> if (fromMe) MessageStatus.SENT else MessageStatus.SENT
    else -> if (fromMe) MessageStatus.SENDING else MessageStatus.SENT
}

private fun parseInstant(iso: String): Long =
    runCatching { Instant.parse(iso).toEpochMilli() }.getOrDefault(System.currentTimeMillis())
