package com.example.project.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.project.R
import com.example.project.core.util.asTimeAgo
import com.example.project.domain.model.AppNotification
import com.example.project.domain.model.NotificationType
import com.example.project.ui.components.DetailTopBar
import com.example.project.ui.components.EmptyState
import com.example.project.ui.components.bounceClick
import com.example.project.ui.theme.LocalDimens
import org.koin.androidx.compose.koinViewModel

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    viewModel: NotificationsViewModel = koinViewModel(),
) {
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val dimens = LocalDimens.current
    val hasUnread = notifications.any { !it.isRead }

    Scaffold(
        topBar = {
            DetailTopBar(
                title = stringResource(R.string.notifications_title),
                onBack = onBack,
                actions = {
                    if (hasUnread) {
                        TextButton(onClick = viewModel::markAllRead) {
                            Text(stringResource(R.string.notifications_mark_all_read))
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (notifications.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.NotificationsNone,
                message = stringResource(R.string.notifications_empty),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = dimens.spaceS),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceXs),
        ) {
            items(notifications, key = { it.id }) { notification ->
                NotificationRow(
                    notification = notification,
                    onClick = { viewModel.onNotificationClick(notification) },
                )
            }
        }
    }
}

@Composable
private fun NotificationRow(
    notification: AppNotification,
    onClick: () -> Unit,
) {
    val dimens = LocalDimens.current
    val containerColor = if (!notification.isRead) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.spaceL)
            .bounceClick(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = containerColor,
    ) {
        Row(
            modifier = Modifier.padding(dimens.spaceM),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = notification.type.icon(),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = dimens.spaceM),
            ) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!notification.body.isNullOrBlank()) {
                    Text(
                        text = notification.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = notification.createdAt.asTimeAgo(),
                    modifier = Modifier.padding(top = dimens.spaceXs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (!notification.isRead) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}

private fun NotificationType.icon(): ImageVector = when (this) {
    NotificationType.FOLLOW -> Icons.Outlined.Person
    NotificationType.MESSAGE -> Icons.AutoMirrored.Filled.Message
    NotificationType.SYSTEM -> Icons.Outlined.Info
    NotificationType.UNKNOWN -> Icons.Filled.NotificationsNone
}
