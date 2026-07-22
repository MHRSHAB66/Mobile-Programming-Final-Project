package com.example.project.ui.artist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.example.project.R
import com.example.project.core.util.asCompactCount
import com.example.project.domain.model.Song
import com.example.project.ui.components.CoverImage
import com.example.project.ui.components.DetailTopBar
import com.example.project.ui.components.SectionHeader
import com.example.project.ui.components.SongRow
import com.example.project.ui.theme.LocalDimens
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ArtistScreen(
    artistId: String,
    currentSongId: String?,
    onBack: () -> Unit,
    onPlaySong: (List<Song>, Int) -> Unit,
    onToggleLike: (Song) -> Unit,
    viewModel: ArtistViewModel = koinViewModel(parameters = { parametersOf(artistId) }),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pagingItems = viewModel.pagedSongs.collectAsLazyPagingItems()
    val dimens = LocalDimens.current
    val artist = state.artist

    Scaffold(
        topBar = { DetailTopBar(title = artist?.name ?: "", onBack = onBack) }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                    CoverImage(
                        url = artist?.imageUrl,
                        contentDescription = artist?.name,
                        modifier = Modifier.fillMaxSize(),
                        cornerRadius = 0,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0.4f to Color.Transparent,
                                    1f to Color.Black.copy(alpha = 0.75f),
                                )
                            )
                    )
                    Column(modifier = Modifier.align(Alignment.BottomStart).padding(dimens.spaceL)) {
                        Text(
                            text = artist?.name ?: "",
                            style = MaterialTheme.typography.displaySmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = stringResource(R.string.followers_count, (artist?.followers ?: 0).asCompactCount()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f),
                        )
                    }
                }
            }
            item {
                if (state.isFollowed) {
                    OutlinedButton(
                        onClick = viewModel::toggleFollow,
                        modifier = Modifier.padding(horizontal = dimens.spaceL, vertical = dimens.spaceS),
                    ) { Text(stringResource(R.string.following)) }
                } else {
                    Button(
                        onClick = viewModel::toggleFollow,
                        modifier = Modifier.padding(horizontal = dimens.spaceL, vertical = dimens.spaceS),
                    ) { Text(stringResource(R.string.follow)) }
                }
            }
            item { SectionHeader(stringResource(R.string.top_songs)) }
            items(
                count = pagingItems.itemCount,
                key = pagingItems.itemKey { it.id },
            ) { index ->
                val song = pagingItems[index] ?: return@items
                val decorated = song.copy(isLiked = song.id in state.likedIds)
                SongRow(
                    song = decorated,
                    isCurrent = song.id == currentSongId,
                    onClick = { onPlaySong(state.songs, index) },
                    onToggleLike = onToggleLike,
                )
            }
        }
    }
}
