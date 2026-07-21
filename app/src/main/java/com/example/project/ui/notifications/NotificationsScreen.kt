package com.example.project.ui.notifications

import androidx.annotation.StringRes
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
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.project.R
import com.example.project.ui.components.DetailTopBar
import com.example.project.ui.theme.LocalDimens

private data class NotificationPreview(
    @StringRes val titleRes: Int,
    @StringRes val messageRes: Int,
    @StringRes val timestampRes: Int,
    val icon: ImageVector,
    val unread: Boolean,
)

private val notificationPreviews = listOf(
    NotificationPreview(
        titleRes = R.string.notifications_follow_title,
        messageRes = R.string.notifications_follow_message,
        timestampRes = R.string.notifications_time_two_minutes,
        icon = Icons.Outlined.Person,
        unread = true,
    ),
    NotificationPreview(
        titleRes = R.string.notifications_playlist_title,
        messageRes = R.string.notifications_playlist_message,
        timestampRes = R.string.notifications_time_one_hour,
        icon = Icons.Outlined.LibraryMusic,
        unread = true,
    ),
    NotificationPreview(
        titleRes = R.string.notifications_download_title,
        messageRes = R.string.notifications_download_message,
        timestampRes = R.string.notifications_time_three_hours,
        icon = Icons.Filled.DownloadDone,
        unread = false,
    ),
    NotificationPreview(
        titleRes = R.string.notifications_release_title,
        messageRes = R.string.notifications_release_message,
        timestampRes = R.string.notifications_time_yesterday,
        icon = Icons.Filled.MusicNote,
        unread = false,
    ),
)

@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    val dimens = LocalDimens.current

    Scaffold(
        topBar = { DetailTopBar(title = stringResource(R.string.notifications_title), onBack = onBack) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = dimens.spaceS),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceXs),
        ) {
            items(notificationPreviews, key = { it.messageRes }) { notification ->
                NotificationRow(notification = notification)
            }
        }
    }
}

@Composable
private fun NotificationRow(notification: NotificationPreview) {
    val dimens = LocalDimens.current
    val containerColor = if (notification.unread) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.spaceL),
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
                    imageVector = notification.icon,
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
                    text = stringResource(notification.titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(notification.messageRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(notification.timestampRes),
                    modifier = Modifier.padding(top = dimens.spaceXs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (notification.unread) {
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
