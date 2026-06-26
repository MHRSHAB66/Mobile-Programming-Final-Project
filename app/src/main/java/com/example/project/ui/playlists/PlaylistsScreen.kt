package com.example.project.ui.playlists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.project.R
import com.example.project.domain.model.Playlist
import com.example.project.ui.components.AppTopBar
import com.example.project.ui.components.CoverImage
import com.example.project.ui.components.bounceClick
import com.example.project.ui.navigation.TopBarActions
import com.example.project.ui.theme.LocalDimens
import org.koin.androidx.compose.koinViewModel

@Composable
fun PlaylistsScreen(
    topBar: TopBarActions,
    onOpenPlaylist: (String) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: PlaylistsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val dimens = LocalDimens.current

    Column(modifier = modifier.fillMaxSize().statusBarsPadding()) {
        AppTopBar(
            avatarUrl = topBar.avatarUrl,
            onProfileClick = topBar.onProfileClick,
            onNotificationsClick = topBar.onNotificationsClick,
            onSettingsClick = topBar.onSettingsClick,
        )
        Text(
            text = stringResource(R.string.playlists_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = dimens.spaceL, vertical = dimens.spaceS),
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceM),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceM),
            modifier = Modifier.padding(horizontal = dimens.spaceL),
        ) {
            section(R.string.playlists_global, state.global, onOpenPlaylist)
            section(R.string.playlists_local, state.local, onOpenPlaylist)
            section(R.string.playlists_user, state.user, onOpenPlaylist)
        }
    }
}

private fun androidx.compose.foundation.lazy.grid.LazyGridScope.section(
    titleRes: Int,
    playlists: List<Playlist>,
    onOpenPlaylist: (String) -> Unit,
) {
    if (playlists.isEmpty()) return
    item(span = { GridItemSpan(maxLineSpan) }) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(vertical = 8.dp),
        )
    }
    items(playlists, key = { it.id }) { playlist ->
        PlaylistGridCard(playlist = playlist, onClick = { onOpenPlaylist(playlist.id) })
    }
}

@Composable
private fun PlaylistGridCard(playlist: Playlist, onClick: () -> Unit) {
    val dimens = LocalDimens.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(onClick = onClick)
            .padding(bottom = dimens.spaceS),
    ) {
        CoverImage(
            url = playlist.coverImageUrl,
            contentDescription = playlist.title,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            cornerRadius = 16,
        )
        Text(
            text = playlist.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = dimens.spaceS),
        )
        Text(
            text = playlist.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
