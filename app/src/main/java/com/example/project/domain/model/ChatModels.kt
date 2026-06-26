package com.example.project.domain.model

/** Delivery state of an outgoing message, rendered as clock / single tick / double tick. */
enum class MessageStatus { SENDING, SENT, READ }

/**
 * A direct message. A message is either plain [text] or a shared song (when [sharedSongId]
 * is set), in which case the UI renders a tappable mini song card.
 */
data class ChatMessage(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val text: String,
    val timestamp: Long,
    val status: MessageStatus,
    val isFromMe: Boolean,
    val sharedSongId: String? = null,
    val sharedSongTitle: String? = null,
    val sharedSongArtist: String? = null,
    val sharedSongCover: String? = null,
) {
    val isSharedSong: Boolean get() = sharedSongId != null
}

/** A 1:1 conversation summary for the chat list. */
data class Conversation(
    val id: String,
    val peer: User,
    val lastMessage: String,
    val lastTimestamp: Long,
    val unreadCount: Int,
)
