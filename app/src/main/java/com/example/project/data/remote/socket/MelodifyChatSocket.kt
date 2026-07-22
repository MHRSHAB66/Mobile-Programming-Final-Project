package com.example.project.data.remote.socket

import com.example.project.data.remote.api.ApiConfig
import com.example.project.data.remote.api.TokenProvider
import com.example.project.data.remote.api.dto.ChatSocketPayloadDto
import com.example.project.data.remote.api.dto.toDomainMessage
import com.example.project.domain.model.MessageStatus
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicReference

class MelodifyChatSocket(
    private val okHttpClient: OkHttpClient,
    private val tokenProvider: TokenProvider,
    private val moshi: Moshi,
    private val scope: CoroutineScope,
) : ChatSocket {

    private val payloadAdapter = moshi.adapter(ChatSocketPayloadDto::class.java)
    private val _incoming = MutableSharedFlow<SocketEvent>(extraBufferCapacity = 64)
    override val incoming: Flow<SocketEvent> = _incoming

    private val socketRef = AtomicReference<WebSocket?>(null)

    override suspend fun connect() {
        val token = tokenProvider.token ?: return
        if (socketRef.get() != null) return

        val request = Request.Builder()
            .url(ApiConfig.webSocketChatUrl(token))
            .build()

        val ws = okHttpClient.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onMessage(webSocket: WebSocket, text: String) {
                    scope.launch { handleFrame(text) }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    socketRef.compareAndSet(webSocket, null)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    socketRef.compareAndSet(webSocket, null)
                }
            },
        )
        socketRef.set(ws)
    }

    private suspend fun handleFrame(text: String) {
        val payload = runCatching { payloadAdapter.fromJson(text) }.getOrNull() ?: return
        when (payload.type) {
            "message.new" -> {
                val dto = payload.message ?: return
                _incoming.emit(
                    SocketEvent.MessageReceived(
                        dto.toDomainMessage("").copy(isFromMe = false),
                    ),
                )
            }
            "message.status" -> {
                val chatId = payload.chatId ?: return
                val id = payload.messageId ?: return
                val status = when (payload.status?.lowercase()) {
                    "read" -> MessageStatus.READ
                    else -> MessageStatus.SENT
                }
                _incoming.emit(SocketEvent.StatusChanged(chatId, id, status))
            }
            "typing" -> {
                val chatId = payload.chatId ?: return
                val isTyping = payload.isTyping ?: true
                _incoming.emit(SocketEvent.Typing(chatId, isTyping))
            }
            "presence.changed" -> {
                val userId = payload.userId ?: return
                val online = payload.isOnline ?: return
                _incoming.emit(SocketEvent.PresenceChanged(userId, online))
            }
            "chat.updated" -> Unit
        }
    }

    override fun send(command: SocketCommand) {
        val ws = socketRef.get() ?: return
        when (command) {
            is SocketCommand.Typing -> {
                val json = JSONObject()
                    .put("type", "typing")
                    .put("chat_id", command.conversationId)
                    .put("is_typing", command.isTyping)
                    .toString()
                ws.send(json)
            }
            is SocketCommand.SendMessage, is SocketCommand.MarkRead -> Unit
        }
    }

    override fun close() {
        socketRef.getAndSet(null)?.close(1000, "logout")
    }
}
