package com.example.project.data.remote.socket

import com.example.project.domain.model.ChatMessage
import com.example.project.domain.model.MessageStatus
import kotlinx.coroutines.flow.Flow

/** Events pushed from the server to the client over the realtime connection. */
sealed interface SocketEvent {
    data class MessageReceived(val message: ChatMessage) : SocketEvent
    data class StatusChanged(val messageId: String, val status: MessageStatus) : SocketEvent
    data class Typing(val conversationId: String, val isTyping: Boolean) : SocketEvent
}

/** Messages sent from the client to the server. */
sealed interface SocketCommand {
    data class SendMessage(val message: ChatMessage) : SocketCommand
    data class Typing(val conversationId: String) : SocketCommand
    data class MarkRead(val conversationId: String) : SocketCommand
}

/**
 * Realtime chat transport abstraction. Today this is backed by [FakeChatSocket] (Flow/Channel
 * based), but the contract matches a real OkHttp WebSocket: implement [connect]/[send]/[close]
 * and emit server frames on [incoming], and the rest of the app keeps working unchanged.
 */
interface ChatSocket {
    val incoming: Flow<SocketEvent>
    suspend fun connect()
    fun send(command: SocketCommand)
    fun close()
}
