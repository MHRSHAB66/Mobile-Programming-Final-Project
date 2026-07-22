package com.example.project.data.remote.api.dto

import com.example.project.domain.model.AppNotification
import com.example.project.domain.model.NotificationType
import com.squareup.moshi.Json
import java.time.Instant

data class NotificationDto(
    val id: String,
    val type: String,
    val title: String,
    val body: String? = null,
    @Json(name = "ref_id") val refId: String? = null,
    @Json(name = "is_read") val isRead: Boolean = false,
    @Json(name = "created_at") val createdAt: String,
)

fun NotificationDto.toDomain(): AppNotification = AppNotification(
    id = id,
    type = when (type.lowercase()) {
        "follow" -> NotificationType.FOLLOW
        "message" -> NotificationType.MESSAGE
        "system" -> NotificationType.SYSTEM
        else -> NotificationType.UNKNOWN
    },
    title = title,
    body = body,
    refId = refId,
    isRead = isRead,
    createdAt = runCatching { Instant.parse(createdAt).toEpochMilli() }
        .getOrDefault(System.currentTimeMillis()),
)
