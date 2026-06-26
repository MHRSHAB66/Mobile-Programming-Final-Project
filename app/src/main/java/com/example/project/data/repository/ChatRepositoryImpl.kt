package com.example.project.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.example.project.data.local.db.ChatMessageDao
import com.example.project.data.local.db.toDomain
import com.example.project.data.local.db.toEntity
import com.example.project.data.mock.MockData
import com.example.project.data.remote.socket.ChatSocket
import com.example.project.data.remote.socket.SocketCommand
import com.example.project.data.remote.socket.SocketEvent
import com.example.project.domain.model.ChatMessage
import com.example.project.domain.model.Conversation
import com.example.project.domain.model.MessageStatus
import com.example.project.domain.model.Song
import com.example.project.domain.repository.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class ChatRepositoryImpl(
    private val dao: ChatMessageDao,
    private val socket: ChatSocket,
    private val scope: CoroutineScope,
) : ChatRepository {

    private val meId = MockData.currentUser.id
    private val _conversations = MutableStateFlow(MockData.seedConversations)
    private val typing = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    private val started = AtomicBoolean(false)

    override suspend fun connect() {
        if (!started.compareAndSet(false, true)) return
        socket.connect()
        seedMessagesIfEmpty()
        scope.launch {
            socket.incoming.collect { event -> handle(event) }
        }
    }

    private suspend fun handle(event: SocketEvent) {
        when (event) {
            is SocketEvent.MessageReceived -> {
                dao.insert(event.message.toEntity())
                updateConversation(
                    event.message.conversationId,
                    summaryOf(event.message),
                    event.message.timestamp,
                    incrementUnread = true,
                )
            }
            is SocketEvent.StatusChanged -> dao.updateStatus(event.messageId, event.status.name)
            is SocketEvent.Typing -> typing.update { it + (event.conversationId to event.isTyping) }
        }
    }

    private suspend fun seedMessagesIfEmpty() {
        MockData.seedConversations.forEach { conversation ->
            if (dao.count(conversation.id) == 0) {
                val now = System.currentTimeMillis()
                dao.insertAll(
                    listOf(
                        ChatMessage(
                            id = UUID.randomUUID().toString(),
                            conversationId = conversation.id,
                            senderId = conversation.peer.id,
                            text = conversation.lastMessage,
                            timestamp = now - 120_000,
                            status = MessageStatus.READ,
                            isFromMe = false,
                        ).toEntity(),
                    )
                )
            }
        }
    }

    override fun observeConversations(): Flow<List<Conversation>> =
        _conversations.map { list -> list.sortedByDescending { it.lastTimestamp } }

    override fun observeMessagesPaged(conversationId: String): Flow<PagingData<ChatMessage>> =
        Pager(PagingConfig(pageSize = 30, enablePlaceholders = false)) {
            dao.pagingForConversation(conversationId)
        }.flow.map { pagingData -> pagingData.map { it.toDomain() } }

    override fun observeMessages(conversationId: String): Flow<List<ChatMessage>> =
        dao.observeForConversation(conversationId).map { list -> list.map { it.toDomain() } }

    override fun observeTyping(conversationId: String): Flow<Boolean> =
        typing.map { it[conversationId] ?: false }

    override suspend fun sendText(conversationId: String, text: String) {
        val message = ChatMessage(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            senderId = meId,
            text = text,
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.SENDING,
            isFromMe = true,
        )
        dao.insert(message.toEntity())
        updateConversation(conversationId, text, message.timestamp, incrementUnread = false)
        socket.send(SocketCommand.SendMessage(message))
    }

    override suspend fun shareSong(conversationId: String, song: Song) {
        val message = ChatMessage(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            senderId = meId,
            text = "",
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.SENDING,
            isFromMe = true,
            sharedSongId = song.id,
            sharedSongTitle = song.title,
            sharedSongArtist = song.artistName,
            sharedSongCover = song.coverImageUrl,
        )
        dao.insert(message.toEntity())
        updateConversation(conversationId, summaryOf(message), message.timestamp, incrementUnread = false)
        socket.send(SocketCommand.SendMessage(message))
    }

    override suspend fun markConversationRead(conversationId: String) {
        _conversations.update { list ->
            list.map { if (it.id == conversationId) it.copy(unreadCount = 0) else it }
        }
        socket.send(SocketCommand.MarkRead(conversationId))
    }

    override suspend fun notifyTyping(conversationId: String) {
        socket.send(SocketCommand.Typing(conversationId))
    }

    private fun summaryOf(message: ChatMessage): String =
        if (message.isSharedSong) "🎵 ${message.sharedSongTitle}" else message.text

    private fun updateConversation(
        conversationId: String,
        lastMessage: String,
        timestamp: Long,
        incrementUnread: Boolean,
    ) {
        _conversations.update { list ->
            list.map {
                if (it.id == conversationId) {
                    it.copy(
                        lastMessage = lastMessage,
                        lastTimestamp = timestamp,
                        unreadCount = if (incrementUnread) it.unreadCount + 1 else it.unreadCount,
                    )
                } else it
            }
        }
    }
}
