package com.example.project.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.project.R
import com.example.project.domain.model.PlaybackState
import com.example.project.ui.nowplaying.playerCoverSharedBounds
import com.example.project.ui.theme.LocalDimens

/**
 * Floating mini player shown above the bottom navigation while a track is active. Tapping the
 * body opens Now Playing; play/pause and next are inline.
 *
 * [animatedVisibilityScope] comes from the `AnimatedVisibility` that wraps this in `MainScreen`;
 * combined with the shared-transition scope it animates the cover into the Now Playing disc.
 */
@Composable
fun MiniPlayer(
    state: PlaybackState,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    val song = state.currentSong ?: return
    val dimens = LocalDimens.current
    val progress by animateFloatAsState(state.progress, label = "miniProgress")
    val skipIconScale = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.spaceS),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 4.dp,
        shadowElevation = 12.dp,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick(onClick = onClick)
                    .padding(dimens.spaceS),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CoverImage(
                    url = song.coverImageUrl,
                    contentDescription = stringResource(R.string.cd_cover_art),
                    modifier = Modifier
                        .size(48.dp)
                        .playerCoverSharedBounds(animatedVisibilityScope),
                    cornerRadius = 8,
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = dimens.spaceM),
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = song.artistName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onPrevious, enabled = state.hasPrevious) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = stringResource(R.string.cd_previous),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.scale(scaleX = skipIconScale, scaleY = 1f),
                    )
                }
                IconButton(onClick = onPlayPause) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = stringResource(
                            if (state.isPlaying) R.string.cd_pause else R.string.cd_play
                        ),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                IconButton(onClick = onNext, enabled = state.hasNext) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = stringResource(R.string.cd_next),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.scale(scaleX = skipIconScale, scaleY = 1f),
                    )
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimens.spaceS),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}
