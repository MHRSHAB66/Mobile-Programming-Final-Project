package com.example.project.data.remote.socket

import com.example.project.data.mock.MockData
import com.example.project.domain.model.ChatMessage
import com.example.project.domain.model.MessageStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Simulates a realtime chat server using coroutines + a SharedFlow — no polling anywhere.
 *
 * When the client sends a message the "server":
 *  1. acknowledges delivery (SENT) after a short delay,
 *  2. marks it READ shortly after,
 *  3. sometimes shows the peer typing and then pushes an auto-reply.
 *
 * Swapping this for a real OkHttp WebSocket only means re-implementing [ChatSocket].
 */
class FakeChatSocket(
    private val scope: CoroutineScope,
) : ChatSocket {

    private val _incoming = MutableSharedFlow<SocketEvent>(extraBufferCapacity = 64)
    override val incoming: Flow<SocketEvent> = _incoming

    private val replies = listOf(
        "Love that one! 🎶", "Adding it to my playlist now.", "Great taste 😄",
        "Have you heard their new album?", "On repeat all day.", "Sending you one back!",
    )

    override suspend fun connect() {
        // A real socket would open the connection here. The fake is always "connected".
    }

    override fun send(command: SocketCommand) {
        when (command) {
            is SocketCommand.SendMessage -> simulateServerRoundTrip(command.message)
            is SocketCommand.Typing -> Unit // our own typing isn't echoed back
            is SocketCommand.MarkRead -> Unit
        }
    }

    private fun simulateServerRoundTrip(message: ChatMessage) {
        scope.launch {
            delay(500)
            _incoming.emit(SocketEvent.StatusChanged(message.conversationId, message.id, MessageStatus.SENT))
            delay(900)
            _incoming.emit(SocketEvent.StatusChanged(message.conversationId, message.id, MessageStatus.READ))

            // Peer occasionally replies to keep the conversation feeling live.
            if ((0..2).random() != 0) {
                delay(700)
                _incoming.emit(SocketEvent.Typing(message.conversationId, true))
                delay(1600)
                _incoming.emit(SocketEvent.Typing(message.conversationId, false))
                val peerId = MockData.seedConversations
                    .firstOrNull { it.id == message.conversationId }?.peer?.id ?: "u1"
                _incoming.emit(
                    SocketEvent.MessageReceived(
                        ChatMessage(
                            id = UUID.randomUUID().toString(),
                            conversationId = message.conversationId,
                            senderId = peerId,
                            text = replies.random(),
                            timestamp = System.currentTimeMillis(),
                            status = MessageStatus.SENT,
                            isFromMe = false,
                        )
                    )
                )
            }
        }
    }

    override fun close() {
        // A real socket would close the connection; nothing to release for the fake.
    }
}
