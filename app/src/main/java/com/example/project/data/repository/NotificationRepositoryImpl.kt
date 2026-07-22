package com.example.project.data.repository

import com.example.project.data.remote.api.NotificationsApi
import com.example.project.data.remote.api.dto.toDomain
import com.example.project.domain.model.AppNotification
import com.example.project.domain.repository.NotificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class NotificationRepositoryImpl(
    private val api: NotificationsApi,
) : NotificationRepository {

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())

    override fun observeNotifications(): Flow<List<AppNotification>> = _notifications.asStateFlow()

    override suspend fun refresh() = withContext(Dispatchers.IO) {
        runCatching {
            _notifications.value = api.getNotifications().map { it.toDomain() }
        }
        Unit
    }

    override suspend fun markRead(id: String) = withContext(Dispatchers.IO) {
        runCatching { api.markRead(id) }
        _notifications.value = _notifications.value.map {
            if (it.id == id) it.copy(isRead = true) else it
        }
    }

    override suspend fun markAllRead() = withContext(Dispatchers.IO) {
        runCatching { api.markAllRead() }
        _notifications.value = _notifications.value.map { it.copy(isRead = true) }
    }
}
