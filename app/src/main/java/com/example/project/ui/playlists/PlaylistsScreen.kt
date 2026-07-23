package com.example.project.ui.playlists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    onShowMessage: (String) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: PlaylistsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val dimens = LocalDimens.current
    val context = LocalContext.current
    var showCreate by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is PlaylistsEffect.Message -> onShowMessage(effect.text.asString(context))
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().statusBarsPadding(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreate = true },
                modifier = Modifier.padding(bottom = contentPadding.calculateBottomPadding()),
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.playlists_create))
            }
        },
    ) { scaffoldPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(scaffoldPadding)) {
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
                section(R.string.playlists_user, state.user, onOpenPlaylist)
                section(R.string.playlists_global, state.global, onOpenPlaylist)
                section(R.string.playlists_local, state.local, onOpenPlaylist)
            }
        }
    }

    if (showCreate) {
        CreatePlaylistDialog(
            isCreating = state.isCreating,
            onDismiss = { showCreate = false },
            onConfirm = { title, isPublic ->
                viewModel.createPlaylist(title, isPublic)
                showCreate = false
            },
        )
    }
}

@Composable
private fun CreatePlaylistDialog(
    isCreating: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (title: String, isPublic: Boolean) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.playlists_create)) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    singleLine = true,
                    enabled = !isCreating,
                    label = { Text(stringResource(R.string.playlists_create_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Checkbox(
                        checked = isPublic,
                        onCheckedChange = { isPublic = it },
                        enabled = !isCreating,
                    )
                    Text(stringResource(R.string.playlists_public_label))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title, isPublic) },
                enabled = title.isNotBlank() && !isCreating,
            ) {
                Text(stringResource(R.string.playlists_create_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isCreating) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
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
            cornerRadius = 12,
        )
        Text(
            text = playlist.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = dimens.spaceS),
        )
    }
}
