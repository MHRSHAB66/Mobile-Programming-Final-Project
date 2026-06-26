package com.example.project.ui.playlistdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.example.project.R
import com.example.project.domain.model.Song
import com.example.project.ui.components.CoverImage
import com.example.project.ui.components.DetailTopBar
import com.example.project.ui.components.SongRow
import com.example.project.ui.theme.LocalDimens
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    currentSongId: String?,
    onBack: () -> Unit,
    onPlaySong: (List<Song>, Int) -> Unit,
    onPlayAll: (List<Song>) -> Unit,
    onShuffle: (List<Song>) -> Unit,
    onToggleLike: (Song) -> Unit,
    viewModel: PlaylistDetailViewModel = koinViewModel(parameters = { parametersOf(playlistId) }),
) {
    val header by viewModel.header.collectAsStateWithLifecycle()
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val likedIds by viewModel.likedIds.collectAsStateWithLifecycle()
    val pagingItems = viewModel.pagedSongs.collectAsLazyPagingItems()
    val dimens = LocalDimens.current

    Scaffold(
        topBar = { DetailTopBar(title = header?.title ?: "", onBack = onBack) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(dimens.spaceL),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CoverImage(
                        url = header?.coverImageUrl,
                        contentDescription = header?.title,
                        modifier = Modifier.size(dimens.coverLarge),
                        cornerRadius = 16,
                    )
                    Text(
                        text = header?.title ?: "",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = dimens.spaceM),
                    )
                    Text(
                        text = header?.description ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = dimens.spaceM),
                        horizontalArrangement = Arrangement.spacedBy(dimens.spaceM),
                    ) {
                        Button(
                            onClick = { onPlayAll(songs) },
                            modifier = Modifier.weight(1f),
                            enabled = songs.isNotEmpty(),
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Text(stringResource(R.string.play_all), modifier = Modifier.padding(start = dimens.spaceXs))
                        }
                        OutlinedButton(
                            onClick = { onShuffle(songs) },
                            modifier = Modifier.weight(1f),
                            enabled = songs.isNotEmpty(),
                        ) {
                            Icon(Icons.Filled.Shuffle, contentDescription = null)
                            Text(stringResource(R.string.shuffle), modifier = Modifier.padding(start = dimens.spaceXs))
                        }
                    }
                }
            }

            items(
                count = pagingItems.itemCount,
                key = pagingItems.itemKey { it.id },
            ) { index ->
                val song = pagingItems[index] ?: return@items
                val decorated = song.copy(isLiked = song.id in likedIds)
                SongRow(
                    song = decorated,
                    isCurrent = song.id == currentSongId,
                    onClick = { onPlaySong(songs, index) },
                    onToggleLike = onToggleLike,
                )
            }
        }
    }
}
