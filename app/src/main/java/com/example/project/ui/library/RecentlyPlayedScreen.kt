package com.example.project.ui.library

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.project.R
import com.example.project.domain.model.Song
import com.example.project.ui.components.DetailTopBar
import com.example.project.ui.components.EmptyState
import com.example.project.ui.components.SongRow
import org.koin.androidx.compose.koinViewModel

@Composable
fun RecentlyPlayedScreen(
    currentSongId: String?,
    onBack: () -> Unit,
    onPlaySong: (List<Song>, Int) -> Unit,
    onPlayAll: (List<Song>) -> Unit,
    onToggleLike: (Song) -> Unit,
    viewModel: RecentlyPlayedViewModel = koinViewModel(),
) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { DetailTopBar(title = stringResource(R.string.recently_played_title), onBack = onBack) }
    ) { padding ->
        if (songs.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.History,
                message = stringResource(R.string.recently_played_empty),
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            return@Scaffold
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                LibraryHeader(
                    title = stringResource(R.string.recently_played_title),
                    subtitle = "",
                    icon = Icons.Filled.History,
                    songCount = songs.size,
                    onPlayAll = { onPlayAll(songs) },
                )
            }
            itemsIndexed(songs, key = { _, s -> s.id }) { index, song ->
                SongRow(
                    song = song,
                    isCurrent = song.id == currentSongId,
                    onClick = { onPlaySong(songs, index) },
                    onToggleLike = onToggleLike,
                )
            }
        }
    }
}
