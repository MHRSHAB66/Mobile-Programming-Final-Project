package com.example.project.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.project.R
import com.example.project.domain.model.Song
import com.example.project.ui.components.DetailTopBar
import com.example.project.ui.components.EmptyState
import com.example.project.ui.components.SongRow
import org.koin.androidx.compose.koinViewModel

@Composable
fun LikedSongsScreen(
    currentSongId: String?,
    onBack: () -> Unit,
    onPlaySong: (List<Song>, Int) -> Unit,
    onPlayAll: (List<Song>) -> Unit,
    onShuffle: (List<Song>) -> Unit,
    onRemove: (Song) -> Unit,
    viewModel: LikedSongsViewModel = koinViewModel(),
) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { DetailTopBar(title = stringResource(R.string.liked_songs_title), onBack = onBack) }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                LibraryHeader(
                    title = stringResource(R.string.liked_songs_title),
                    subtitle = stringResource(R.string.liked_songs_subtitle),
                    icon = Icons.Filled.Favorite,
                    songCount = songs.size,
                    onPlayAll = { onPlayAll(songs) },
                    onShuffle = { onShuffle(songs) },
                )
            }
            if (songs.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.Favorite,
                        message = stringResource(R.string.liked_songs_empty),
                    )
                }
            } else {
                itemsIndexed(songs, key = { _, s -> s.id }) { index, song ->
                    SwipeToRemove(onRemoved = { onRemove(song) }) {
                        SongRow(
                            song = song,
                            isCurrent = song.id == currentSongId,
                            onClick = { onPlaySong(songs, index) },
                            modifier = Modifier.background(MaterialTheme.colorScheme.background),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SwipeToRemove(onRemoved: () -> Unit, content: @Composable () -> Unit) {
    var removed by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { target ->
            if (target == SwipeToDismissBoxValue.EndToStart) {
                removed = true
                true
            } else false
        }
    )
    LaunchedEffect(removed) { if (removed) onRemoved() }
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.remove),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
        content = { content() },
    )
}
