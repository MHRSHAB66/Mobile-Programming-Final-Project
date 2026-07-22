package com.example.project.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.project.R
import com.example.project.core.util.asClockTime
import com.example.project.domain.model.ChatMessage
import com.example.project.domain.model.MessageStatus
import com.example.project.ui.components.CoverImage
import com.example.project.ui.components.bounceClick
import com.example.project.ui.theme.LocalDimens
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    conversationId: String,
    onBack: () -> Unit,
    onPlaySharedSong: (String) -> Unit,
    contentAboveInput: (@Composable () -> Unit)? = null,
    viewModel: ChatDetailViewModel = koinViewModel(parameters = { parametersOf(conversationId) }),
) {
    val peer by viewModel.peer.collectAsStateWithLifecycle()
    val isTyping by viewModel.isPeerTyping.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val dimens = LocalDimens.current
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // reverseLayout: index 0 is the newest bubble (visual bottom). Keep it pinned above the input.
    val newestId = messages.firstOrNull()?.id
    LaunchedEffect(newestId, isTyping) {
        if (messages.isNotEmpty() || isTyping) {
            listState.animateScrollToItem(0)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(peer?.displayName ?: "", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = when {
                                isTyping -> stringResource(R.string.chat_typing)
                                peer?.isOnline == true -> stringResource(R.string.chat_online)
                                else -> stringResource(R.string.chat_offline)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isTyping) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                reverseLayout = true,
                contentPadding = PaddingValues(dimens.spaceM),
                verticalArrangement = Arrangement.spacedBy(dimens.spaceS),
            ) {
                if (isTyping) {
                    item(key = "typing") { TypingBubble() }
                }
                items(messages, key = { it.id }) { message ->
                    MessageBubble(message = message, onPlaySharedSong = onPlaySharedSong)
                }
            }

            contentAboveInput?.invoke()

            MessageInput(
                value = input,
                onValueChange = {
                    input = it
                    if (it.isNotBlank()) viewModel.onTyping()
                },
                onSend = {
                    viewModel.send(input)
                    input = ""
                },
            )
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage, onPlaySharedSong: (String) -> Unit) {
    val dimens = LocalDimens.current
    val isMine = message.isFromMe
    val bubbleColor = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMine) 16.dp else 4.dp,
                bottomEnd = if (isMine) 4.dp else 16.dp,
            ),
            modifier = Modifier.widthIn(max = 280.dp),
        ) {
            Column(modifier = Modifier.padding(dimens.spaceM)) {
                if (message.isSharedSong) {
                    SharedSongCard(message = message, onClick = { message.sharedSongId?.let(onPlaySharedSong) })
                } else {
                    Text(text = message.text, style = MaterialTheme.typography.bodyLarge, color = textColor)
                }
                Row(
                    modifier = Modifier.padding(top = dimens.spaceXs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                ) {
                    Text(
                        text = message.timestamp.asClockTime(),
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.7f),
                    )
                    if (isMine) {
                        StatusTicks(status = message.status, tint = textColor.copy(alpha = 0.9f))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusTicks(status: MessageStatus, tint: Color) {
    val dimens = LocalDimens.current
    val (icon, description) = when (status) {
        MessageStatus.SENDING -> Icons.Filled.AccessTime to R.string.cd_message_sending
        MessageStatus.SENT -> Icons.Filled.Done to R.string.cd_message_sent
        MessageStatus.READ -> Icons.Filled.DoneAll to R.string.cd_message_read
    }
    Icon(
        imageVector = icon,
        contentDescription = stringResource(description),
        tint = if (status == MessageStatus.READ) MaterialTheme.colorScheme.tertiary else tint,
        modifier = Modifier
            .padding(start = dimens.spaceXs)
            .size(14.dp),
    )
}

@Composable
private fun SharedSongCard(message: ChatMessage, onClick: () -> Unit) {
    val dimens = LocalDimens.current
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier
            .widthIn(max = 240.dp)
            .padding(bottom = dimens.spaceXs),
    ) {
        Row(
            modifier = Modifier
                .bounceClick(onClick = onClick)
                .padding(dimens.spaceS),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoverImage(
                url = message.sharedSongCover,
                contentDescription = message.sharedSongTitle,
                modifier = Modifier.size(48.dp),
                cornerRadius = 8,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = dimens.spaceS),
            ) {
                Text(
                    text = message.sharedSongTitle ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = message.sharedSongArtist ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                Icons.Filled.PlayCircle,
                contentDescription = stringResource(R.string.cd_play),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

@Composable
private fun TypingBubble() {
    val dimens = LocalDimens.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(
                text = stringResource(R.string.chat_typing),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = dimens.spaceM, vertical = dimens.spaceS),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageInput(value: String, onValueChange: (String) -> Unit, onSend: () -> Unit) {
    val dimens = LocalDimens.current
    Surface(tonalElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimens.spaceS),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.chat_message_hint)) },
                maxLines = 4,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                shape = MaterialTheme.shapes.large,
            )
            IconButton(onClick = onSend, enabled = value.isNotBlank()) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.cd_send),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
