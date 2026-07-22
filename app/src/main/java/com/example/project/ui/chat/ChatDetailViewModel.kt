package com.example.project.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project.domain.model.ChatMessage
import com.example.project.domain.model.User
import com.example.project.domain.repository.ChatRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatDetailViewModel(
    private val conversationId: String,
    private val chatRepository: ChatRepository,
) : ViewModel() {

    /** Newest-first for reverseLayout LazyColumn; Room Flow updates ticks live. */
    val messages: StateFlow<List<ChatMessage>> = chatRepository.observeMessages(conversationId)
        .map { list -> list.asReversed() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isPeerTyping: StateFlow<Boolean> = chatRepository.observeTyping(conversationId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val peer: StateFlow<User?> = chatRepository.observeConversations()
        .map { conversations -> conversations.firstOrNull { it.id == conversationId }?.peer }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        chatRepository.setActiveConversation(conversationId)
        viewModelScope.launch {
            chatRepository.connect()
            chatRepository.syncMessages(conversationId)
            chatRepository.markConversationRead(conversationId)
        }
    }

    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { chatRepository.sendText(conversationId, trimmed) }
    }

    fun onTyping() {
        viewModelScope.launch { chatRepository.notifyTyping(conversationId) }
    }

    override fun onCleared() {
        chatRepository.setActiveConversation(null)
        super.onCleared()
    }
}
