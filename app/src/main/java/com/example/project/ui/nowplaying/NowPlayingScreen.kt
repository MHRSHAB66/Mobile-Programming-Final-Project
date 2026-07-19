package com.example.project.ui.nowplaying

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.Share
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.project.R
import com.example.project.core.util.asTrackTime
import com.example.project.data.player.AudioSessionHolder
import com.example.project.domain.model.Conversation
import com.example.project.domain.model.RepeatMode
import com.example.project.ui.components.CoverImage
import com.example.project.ui.components.LikeButton
import com.example.project.ui.components.bounceClick
import com.example.project.ui.components.rememberAnimatedBrandGradient
import com.example.project.ui.components.rememberPulse
import com.example.project.ui.player.PlayerViewModel
import com.example.project.ui.theme.LocalDimens

@Composable
fun NowPlayingScreen(
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope? = null,
) {
    val state by playerViewModel.playbackState.collectAsStateWithLifecycle()
    val sleepSeconds by playerViewModel.sleepTimerSeconds.collectAsStateWithLifecycle()
    val conversations by playerViewModel.conversations.collectAsStateWithLifecycle()
    val audioSessionId by AudioSessionHolder.sessionId.collectAsStateWithLifecycle()
    val downloadedIds by playerViewModel.downloadedIds.collectAsStateWithLifecycle()
    val likedIds by playerViewModel.likedIds.collectAsStateWithLifecycle()
    val dimens = LocalDimens.current
    val song = state.currentSong

    // True once the track is available offline — either we're already playing the local file, or
    // its download just finished while it was streaming. Drives the offline label/button live (#019).
    val isOffline = song != null && (song.isDownloaded || song.id in downloadedIds)

    // Liked state read live from Room so the in-app heart and the notification Like action agree (#002).
    val isLiked = song != null && song.id in likedIds

    // RECORD_AUDIO lets the visualizer read the real playback FFT (issue #012). Ask once on entry;
    // if denied, the visualizer just keeps its decorative animation.
    val context = LocalContext.current
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasAudioPermission = granted }
    LaunchedEffect(Unit) {
        if (!hasAudioPermission) audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    // Auto-close Now Playing when playback fully stops (e.g. the sleep timer fired and cleared
    // the queue) so the user isn't left staring at an empty screen — issue #017.
    var hadSong by remember { mutableStateOf(false) }
    LaunchedEffect(song) {
        if (song != null) hadSong = true
        else if (hadSong) onBack()
    }

    val fallback = MaterialTheme.colorScheme.primary
    val albumColors by rememberAlbumColors(song?.coverImageUrl, fallback)
    val dominant = albumColors.firstOrNull() ?: fallback
    val background = MaterialTheme.colorScheme.background

    val animatedBackground = rememberAnimatedBrandGradient(
        colors = albumColors.map { it.copy(alpha = 0.60f) } + listOf(background, background),
        durationMillis = 5000,
        span = 1400f,
    )

    var showShareDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(animatedBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Keep content clear of the status bar, camera notch and the system nav buttons;
                // the gradient background still fills the whole screen behind them — issue #020.
                .safeDrawingPadding()
                .padding(horizontal = dimens.spaceL),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.now_playing),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = song?.album ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = { showShareDialog = true }, enabled = song != null) {
                    Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.cd_share_to_chat))
                }
            }

            Spacer(Modifier.height(dimens.spaceL))

            Box(contentAlignment = Alignment.Center) {
                // Soft, pulsing halo in the cover's dominant colour while playing.
                if (state.isPlaying) {
                    val pulse = rememberPulse()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.84f)
                            .aspectRatio(1f)
                            .scale(pulse)
                            .background(
                                Brush.radialGradient(
                                    listOf(dominant.copy(alpha = 0.55f), Color.Transparent)
                                ),
                                CircleShape,
                            )
                    )
                }
                RotatingDisc(
                    coverUrl = song?.coverImageUrl,
                    isPlaying = state.isPlaying,
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .aspectRatio(1f)
                        .playerCoverSharedBounds(animatedVisibilityScope),
                )
            }

            Spacer(Modifier.height(dimens.spaceL))

            AudioVisualizer(
                isPlaying = state.isPlaying,
                color = dominant,
                audioSessionId = audioSessionId,
                hasAudioPermission = hasAudioPermission,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            )

            Spacer(Modifier.height(dimens.spaceM))

            // Title + like
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song?.title ?: "",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = song?.artistName ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    // Clear textual indicator that this track is available offline — issue #009.
                    // Reacts live the moment the download finishes (isOffline) — issue #019.
                    if (isOffline) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.DownloadDone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = stringResource(R.string.cd_playing_offline),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }
                    }
                }
                if (isOffline) {
                    IconButton(onClick = {}, enabled = false) {
                        Icon(
                            imageVector = Icons.Filled.DownloadDone,
                            contentDescription = stringResource(R.string.cd_playing_offline),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                } else {
                    IconButton(
                        onClick = { song?.let(playerViewModel::onDownload) },
                        enabled = song != null,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = stringResource(R.string.download),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                LikeButton(
                    isLiked = isLiked,
                    onToggle = { song?.let(playerViewModel::onToggleLike) },
                    iconSize = 30.dp,
                )
            }

            SeekBar(
                positionMs = state.positionMs,
                durationMs = state.durationMs,
                onSeek = playerViewModel::seekTo,
            )

            val skipIconScale = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f

            // Transport controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Shuffle toggle
                IconButton(onClick = playerViewModel::toggleShuffle) {
                    Icon(
                        Icons.Filled.Shuffle,
                        contentDescription = stringResource(R.string.player_shuffle),
                        tint = if (state.isShuffled)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                }
                IconButton(onClick = playerViewModel::previous, enabled = state.hasPrevious) {
                    Icon(
                        Icons.Filled.SkipPrevious,
                        contentDescription = stringResource(R.string.cd_previous),
                        modifier = Modifier.size(36.dp).scale(scaleX = skipIconScale, scaleY = 1f),
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(72.dp),
                ) {
                    IconButton(onClick = playerViewModel::togglePlayPause) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = stringResource(
                                if (state.isPlaying) R.string.cd_pause else R.string.cd_play
                            ),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }
                IconButton(onClick = playerViewModel::next, enabled = state.hasNext) {
                    Icon(
                        Icons.Filled.SkipNext,
                        contentDescription = stringResource(R.string.cd_next),
                        modifier = Modifier.size(36.dp).scale(scaleX = skipIconScale, scaleY = 1f),
                    )
                }
                // Repeat cycle: OFF → ALL → ONE → OFF
                IconButton(onClick = playerViewModel::cycleRepeatMode) {
                    Icon(
                        imageVector = if (state.repeatMode == RepeatMode.ONE)
                            Icons.Filled.RepeatOne
                        else
                            Icons.Filled.Repeat,
                        contentDescription = stringResource(
                            when (state.repeatMode) {
                                RepeatMode.OFF -> R.string.player_repeat_off
                                RepeatMode.ALL -> R.string.player_repeat_all
                                RepeatMode.ONE -> R.string.player_repeat_one
                            }
                        ),
                        tint = if (state.repeatMode != RepeatMode.OFF)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Spacer(Modifier.height(dimens.spaceS))

            // Sleep timer + speed
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                SleepTimerControl(
                    activeSeconds = sleepSeconds,
                    onSelect = playerViewModel::setSleepTimer,
                )
                SpeedControl(
                    speed = state.speed,
                    onSelect = playerViewModel::setSpeed,
                )
            }
        }
    }

    if (showShareDialog && song != null) {
        ShareDialog(
            conversations = conversations,
            onDismiss = { showShareDialog = false },
            onShare = { conversationId ->
                playerViewModel.shareSongToConversation(conversationId, song)
                showShareDialog = false
            },
        )
    }
}

@Composable
private fun RotatingDisc(coverUrl: String?, isPlaying: Boolean, modifier: Modifier = Modifier) {
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                rotation.animateTo(
                    targetValue = rotation.value + 360f,
                    animationSpec = tween(durationMillis = 9000, easing = LinearEasing),
                )
                rotation.snapTo(rotation.value % 360f)
            }
        }
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Color.Black)
                .rotate(rotation.value),
            contentAlignment = Alignment.Center,
        ) {
            CoverImage(
                url = coverUrl,
                contentDescription = stringResource(R.string.cd_cover_art),
                modifier = Modifier
                    .fillMaxSize(0.92f)
                    .clip(CircleShape),
                cornerRadius = 0,
            )
            // CD center hole
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background)
            )
        }
    }
}

@Composable
private fun SeekBar(positionMs: Long, durationMs: Long, onSeek: (Long) -> Unit) {
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val sliderValue = if (dragging) dragValue else progress
    val shownPosition = if (dragging) (dragValue * durationMs).toLong() else positionMs

    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = sliderValue,
            onValueChange = {
                dragging = true
                dragValue = it
            },
            onValueChangeFinished = {
                onSeek((dragValue * durationMs).toLong())
                dragging = false
            },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = shownPosition.asTrackTime(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = durationMs.asTrackTime(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SleepTimerControl(activeSeconds: Int, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var showCustomDialog by remember { mutableStateOf(false) }
    var customInput by remember { mutableStateOf("") }

    // onSelect takes SECONDS. The 15-second option is there so the timer can be demoed quickly.
    val presets = listOf(
        0 to R.string.player_sleep_off,
        15 to R.string.player_sleep_15s,
        15 * 60 to R.string.player_sleep_15,
        30 * 60 to R.string.player_sleep_30,
        60 * 60 to R.string.player_sleep_60,
    )

    val label = when {
        activeSeconds <= 0 -> stringResource(R.string.player_sleep_timer)
        activeSeconds < 60 -> "${activeSeconds}s"
        else -> "${activeSeconds / 60} min"
    }

    Box {
        TextButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.Bedtime, contentDescription = stringResource(R.string.player_sleep_timer))
            Text(text = label, modifier = Modifier.padding(start = 6.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            presets.forEach { (seconds, labelRes) ->
                DropdownMenuItem(
                    text = { Text(stringResource(labelRes)) },
                    onClick = {
                        onSelect(seconds)
                        expanded = false
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.player_sleep_custom)) },
                onClick = {
                    expanded = false
                    customInput = ""
                    showCustomDialog = true
                },
            )
        }
    }

    if (showCustomDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text(stringResource(R.string.player_sleep_custom_title)) },
            text = {
                OutlinedTextField(
                    value = customInput,
                    onValueChange = { customInput = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.player_sleep_custom_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val minutes = customInput.toIntOrNull() ?: 0
                        if (minutes > 0) onSelect(minutes * 60)
                        showCustomDialog = false
                    }
                ) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun SpeedControl(speed: Float, onSelect: (Float) -> Unit) {
    val speeds = listOf(0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.Speed, contentDescription = stringResource(R.string.player_speed))
            Text(
                text = stringResource(R.string.player_speed_value, speed.toString().removeSuffix(".0")),
                modifier = Modifier.padding(start = 6.dp),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            speeds.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(R.string.player_speed_value, option.toString().removeSuffix(".0")),
                            color = if (option == speed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    onClick = { onSelect(option); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun ShareDialog(
    conversations: List<Conversation>,
    onDismiss: () -> Unit,
    onShare: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.cd_share_to_chat)) },
        text = {
            LazyColumn {
                items(conversations, key = { it.id }) { conversation ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .bounceClick(onClick = { onShare(conversation.id) })
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = conversation.peer.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.cd_send),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
