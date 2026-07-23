package com.example.project.domain.model

enum class NotificationType {
    FOLLOW,
    MESSAGE,
    SYSTEM,
    UNKNOWN,
}

data class AppNotification(
    val id: String,
    val type: NotificationType,
    val title: String,
    val body: String?,
    val refId: String?,
    val isRead: Boolean,
    val createdAt: Long,
)
