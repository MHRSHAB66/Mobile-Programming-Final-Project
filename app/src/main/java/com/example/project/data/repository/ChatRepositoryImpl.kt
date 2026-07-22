package com.example.project.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.example.project.data.local.db.ChatMessageDao
import com.example.project.data.local.db.toDomain
import com.example.project.data.local.db.toEntity
import com.example.project.data.remote.api.ChatApi
import com.example.project.data.remote.api.TokenProvider
import com.example.project.data.remote.api.dto.CreateChatRequestDto
import com.example.project.data.remote.api.dto.SendMessageRequestDto
import com.example.project.data.remote.api.dto.toDomainConversation
import com.example.project.data.remote.api.dto.toDomainMessage
import com.example.project.data.remote.socket.ChatSocket
import com.example.project.data.remote.socket.SocketCommand
import com.example.project.data.remote.socket.SocketEvent
import com.example.project.domain.model.ChatMessage
import com.example.project.domain.model.Conversation
import com.example.project.domain.model.MessageStatus
import com.example.project.domain.model.Song
import com.example.project.domain.repository.ChatRepository
import com.example.project.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

class ChatRepositoryImpl(
    private val dao: ChatMessageDao,
    private val chatApi: ChatApi,
    private val socket: ChatSocket,
    private val tokenProvider: TokenProvider,
    private val settingsRepository: SettingsRepository,
    private val scope: CoroutineScope,
) : ChatRepository {

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    private val typing = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    private val started = AtomicBoolean(false)
    private val activeConversationId = AtomicReference<String?>(null)
    private val lastTypingSentAt = ConcurrentHashMap<String, AtomicLong>()
    private val typingStopJobs = ConcurrentHashMap<String, Job>()

    private suspend fun currentUserId(): String =
        settingsRepository.settings.first().currentUserId

    override suspend fun connect() {
        if (tokenProvider.token.isNullOrBlank()) return
        if (!started.compareAndSet(false, true)) return
        socket.connect()
        refreshConversations()
        scope.launch {
            socket.incoming.collect { event -> handle(event) }
        }
    }

    private suspend fun refreshConversations() {
        runCatching {
            _conversations.value = chatApi.listChats().map { it.toDomainConversation() }
        }
    }

    private suspend fun handle(event: SocketEvent) {
        val meId = currentUserId()
        when (event) {
            is SocketEvent.MessageReceived -> {
                val message = event.message.let { m ->
                    m.copy(isFromMe = m.senderId == meId)
                }
                dao.insert(message.toEntity())
                typing.update { it + (message.conversationId to false) }
                val viewing = activeConversationId.get() == message.conversationId
                updateConversation(
                    message.conversationId,
                    summaryOf(message),
                    message.timestamp,
                    incrementUnread = !message.isFromMe && !viewing,
                )
                if (viewing && !message.isFromMe) {
                    markConversationRead(message.conversationId, message.id)
                }
            }
            is SocketEvent.StatusChanged -> {
                if (event.status == MessageStatus.READ) {
                    dao.markMineReadUpTo(event.conversationId, event.messageId)
                    dao.updateStatus(event.messageId, MessageStatus.READ.name)
                } else {
                    dao.updateStatus(event.messageId, event.status.name)
                }
            }
            is SocketEvent.Typing -> typing.update { it + (event.conversationId to event.isTyping) }
            is SocketEvent.PresenceChanged -> {
                _conversations.update { list ->
                    list.map { conversation ->
                        if (conversation.peer.id == event.userId) {
                            conversation.copy(peer = conversation.peer.copy(isOnline = event.isOnline))
                        } else {
                            conversation
                        }
                    }
                }
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
        val meId = currentUserId()
        val clientId = UUID.randomUUID().toString()
        val optimistic = ChatMessage(
            id = clientId,
            conversationId = conversationId,
            senderId = meId,
            text = text,
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.SENDING,
            isFromMe = true,
        )
        dao.insert(optimistic.toEntity())
        updateConversation(conversationId, text, optimistic.timestamp, incrementUnread = false)
        stopTyping(conversationId)

        runCatching {
            val dto = chatApi.sendMessage(
                conversationId,
                SendMessageRequestDto(text = text, clientMsgId = clientId),
            )
            val confirmed = dto.toDomainMessage(meId)
            dao.deleteById(clientId)
            dao.insert(confirmed.toEntity())
            updateConversation(conversationId, text, confirmed.timestamp, incrementUnread = false)
        }.onFailure {
            dao.updateStatus(clientId, MessageStatus.SENT.name)
        }
    }

    override suspend fun shareSong(conversationId: String, song: Song) {
        val meId = currentUserId()
        val clientId = UUID.randomUUID().toString()
        val optimistic = ChatMessage(
            id = clientId,
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
        dao.insert(optimistic.toEntity())
        updateConversation(
            conversationId,
            summaryOf(optimistic),
            optimistic.timestamp,
            incrementUnread = false,
        )

        runCatching {
            val dto = chatApi.sendMessage(
                conversationId,
                SendMessageRequestDto(songId = song.id, clientMsgId = clientId),
            )
            val confirmed = dto.toDomainMessage(meId)
            dao.deleteById(clientId)
            dao.insert(confirmed.toEntity())
            updateConversation(
                conversationId,
                summaryOf(confirmed),
                confirmed.timestamp,
                incrementUnread = false,
            )
        }
    }

    override suspend fun markConversationRead(conversationId: String) {
        val upTo = dao.latestIncomingMessageId(conversationId)
            ?: dao.latestMessageId(conversationId)
            ?: return
        markConversationRead(conversationId, upTo)
    }

    override suspend fun markConversationRead(conversationId: String, upToMessageId: String) {
        _conversations.update { list ->
            list.map { if (it.id == conversationId) it.copy(unreadCount = 0) else it }
        }
        runCatching {
            val response = chatApi.markRead(conversationId, upToMessageId)
            check(response.isSuccessful) { "markRead HTTP ${response.code()}" }
        }
    }

    override suspend fun notifyTyping(conversationId: String) {
        val now = System.currentTimeMillis()
        val last = lastTypingSentAt.getOrPut(conversationId) { AtomicLong(0L) }
        if (now - last.get() >= TYPING_THROTTLE_MS) {
            last.set(now)
            socket.send(SocketCommand.Typing(conversationId, isTyping = true))
        }
        typingStopJobs[conversationId]?.cancel()
        typingStopJobs[conversationId] = scope.launch {
            delay(TYPING_STOP_AFTER_MS)
            stopTyping(conversationId)
        }
    }

    private fun stopTyping(conversationId: String) {
        typingStopJobs.remove(conversationId)?.cancel()
        lastTypingSentAt[conversationId]?.set(0L)
        socket.send(SocketCommand.Typing(conversationId, isTyping = false))
    }

    override suspend fun openDirectMessage(peerUserId: String): Result<String> = runCatching {
        val dto = chatApi.createChat(CreateChatRequestDto(userId = peerUserId))
        val conversation = dto.toDomainConversation()
        _conversations.update { current ->
            if (current.any { it.id == conversation.id }) {
                current.map { if (it.id == conversation.id) conversation else it }
            } else {
                current + conversation
            }
        }
        conversation.id
    }

    override suspend fun syncMessages(conversationId: String) {
        val meId = currentUserId()
        val page = runCatching { chatApi.getMessages(conversationId) }.getOrNull() ?: return
        dao.insertAll(page.items.map { it.toDomainMessage(meId).toEntity() })
        refreshConversations()
    }

    override fun setActiveConversation(conversationId: String?) {
        activeConversationId.set(conversationId)
    }

    override fun clearChatCache() {
        _conversations.value = emptyList()
        typing.value = emptyMap()
        activeConversationId.set(null)
        started.set(false)
        socket.close()
        scope.launch { dao.deleteAll() }
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
            val existing = list.any { it.id == conversationId }
            if (!existing) {
                scope.launch { refreshConversations() }
                return@update list
            }
            list.map {
                if (it.id != conversationId) {
                    it
                } else {
                    val nextUnread = when {
                        activeConversationId.get() == conversationId -> 0
                        incrementUnread -> it.unreadCount + 1
                        else -> it.unreadCount
                    }
                    it.copy(
                        lastMessage = lastMessage,
                        lastTimestamp = timestamp,
                        unreadCount = nextUnread,
                    )
                }
            }
        }
    }

    companion object {
        private const val TYPING_THROTTLE_MS = 1_500L
        private const val TYPING_STOP_AFTER_MS = 2_500L
    }
}
