package com.example.project.domain.repository

import androidx.paging.PagingData
import com.example.project.domain.model.ChatMessage
import com.example.project.domain.model.Conversation
import com.example.project.domain.model.Song
import kotlinx.coroutines.flow.Flow

/**
 * Real-time direct messaging. Incoming messages and typing/read events arrive through a
 * WebSocket abstraction (a fake, Flow-driven implementation is used until a real backend
 * exists). Message history is cached in Room for offline reading.
 */
interface ChatRepository {
    fun observeConversations(): Flow<List<Conversation>>

    /** Offline-first message history, Paging-ready for long chats. */
    fun observeMessagesPaged(conversationId: String): Flow<PagingData<ChatMessage>>

    /** Lightweight stream of all messages in a conversation (used for live updates). */
    fun observeMessages(conversationId: String): Flow<List<ChatMessage>>

    /** True while the peer is typing — driven by the real-time socket, never polling. */
    fun observeTyping(conversationId: String): Flow<Boolean>

    suspend fun sendText(conversationId: String, text: String)
    suspend fun shareSong(conversationId: String, song: Song)
    suspend fun markConversationRead(conversationId: String)
    suspend fun notifyTyping(conversationId: String)

    /** Opens the realtime connection (call from a coroutine; cancellation closes it). */
    suspend fun connect()
}
