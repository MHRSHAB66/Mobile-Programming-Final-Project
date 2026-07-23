package com.example.project.domain.repository

import com.example.project.domain.model.AppNotification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun observeNotifications(): Flow<List<AppNotification>>
    suspend fun refresh()
    suspend fun markRead(id: String)
    suspend fun markAllRead()
}
