package com.example.project.ui.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
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
import com.example.project.domain.model.DownloadItem
import com.example.project.domain.model.DownloadSort
import com.example.project.domain.model.DownloadState
import com.example.project.domain.model.Song
import com.example.project.ui.components.AppTopBar
import com.example.project.ui.components.EmptyState
import com.example.project.ui.components.SongRow
import com.example.project.ui.navigation.TopBarActions
import com.example.project.ui.theme.LocalDimens
import org.koin.androidx.compose.koinViewModel

@Composable
fun DownloadsScreen(
    topBar: TopBarActions,
    currentSongId: String?,
    onPlaySong: (List<Song>, Int) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: DownloadsViewModel = koinViewModel(),
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.spaceL, vertical = dimens.spaceS),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.downloads_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            SortMenu(current = state.sort, onSelect = viewModel::setSort)
        }

        if (state.items.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.DownloadDone,
                message = stringResource(R.string.downloads_empty),
            )
        } else {
            val songs = state.items.map { it.song }
            LazyColumn(contentPadding = contentPadding) {
                itemsIndexed(state.items, key = { _, item -> item.song.id }) { index, item ->
                    SwipeableDownloadRow(
                        item = item,
                        isCurrent = item.song.id == currentSongId,
                        onClick = { onPlaySong(songs, index) },
                        onDelete = { viewModel.remove(item.song.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SwipeableDownloadRow(
    item: DownloadItem,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    var dismissed by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { target ->
            if (target == SwipeToDismissBoxValue.EndToStart) {
                dismissed = true
                true
            } else false
        }
    )
    LaunchedEffect(dismissed) {
        if (dismissed) onDelete()
    }

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
                    contentDescription = stringResource(R.string.cd_swipe_delete),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
    ) {
        SongRow(
            song = item.song,
            isCurrent = isCurrent,
            onClick = onClick,
            modifier = Modifier.background(MaterialTheme.colorScheme.background),
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DownloadStatusIcon(item)
                    // Explicit delete button so removing a download doesn't rely on the
                    // (easy-to-miss, RTL-flipped) swipe gesture — issue #013.
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
        )
    }
}

@Composable
private fun DownloadStatusIcon(item: DownloadItem) {
    when (item.state) {
        DownloadState.COMPLETED -> Icon(
            Icons.Filled.CheckCircle,
            contentDescription = stringResource(R.string.downloaded),
            tint = MaterialTheme.colorScheme.primary,
        )
        DownloadState.DOWNLOADING, DownloadState.QUEUED -> Text(
            text = stringResource(R.string.downloading),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        DownloadState.FAILED -> Text(
            text = stringResource(R.string.download_failed),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun SortMenu(current: DownloadSort, onSelect: (DownloadSort) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val labels = mapOf(
        DownloadSort.RECENT to R.string.sort_recent,
        DownloadSort.TITLE to R.string.sort_title,
        DownloadSort.ARTIST to R.string.sort_artist,
    )
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.sort))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DownloadSort.entries.forEach { sort ->
                DropdownMenuItem(
                    text = { Text(stringResource(labels.getValue(sort))) },
                    onClick = {
                        onSelect(sort)
                        expanded = false
                    },
                )
            }
        }
    }
}
