package com.example.project.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.project.R
import com.example.project.domain.model.Artist
import com.example.project.domain.model.SearchFilter
import com.example.project.domain.model.Song
import com.example.project.domain.model.User
import com.example.project.ui.components.AppTopBar
import com.example.project.ui.components.CircleImage
import com.example.project.ui.components.CoverImage
import com.example.project.ui.components.EmptyState
import com.example.project.ui.components.SongRow
import com.example.project.ui.components.bounceClick
import com.example.project.ui.navigation.TopBarActions
import com.example.project.ui.theme.LocalDimens
import org.koin.androidx.compose.koinViewModel

@Composable
fun SearchScreen(
    topBar: TopBarActions,
    currentSongId: String?,
    onPlaySong: (List<Song>, Int) -> Unit,
    onToggleLike: (Song) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onOpenUser: (String) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = koinViewModel(),
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
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.spaceL),
            placeholder = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onQueryChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.clear))
                    }
                }
            },
            singleLine = true,
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onSearch = { viewModel.onSubmit() }
            ),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
        )

        FilterRow(selected = state.filter, onSelect = viewModel::onFilterChange)

        if (state.query.isBlank()) {
            HistoryList(
                history = state.history,
                onClick = viewModel::onHistoryClick,
                onRemove = viewModel::onRemoveHistory,
                onClearAll = viewModel::onClearHistory,
                contentPadding = contentPadding,
            )
        } else {
            ResultsList(
                state = state,
                currentSongId = currentSongId,
                onPlaySong = onPlaySong,
                onToggleLike = onToggleLike,
                onOpenArtist = onOpenArtist,
                onOpenPlaylist = onOpenPlaylist,
                onOpenUser = onOpenUser,
                onSubmit = viewModel::onSubmit,
                contentPadding = contentPadding,
            )
        }
    }
}

@Composable
private fun FilterRow(selected: SearchFilter, onSelect: (SearchFilter) -> Unit) {
    val dimens = LocalDimens.current
    val labels = mapOf(
        SearchFilter.SONG to R.string.search_filter_song,
        SearchFilter.ARTIST to R.string.search_filter_artist,
        SearchFilter.PLAYLIST to R.string.search_filter_playlist,
        SearchFilter.USER to R.string.search_filter_user,
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = dimens.spaceL, vertical = dimens.spaceS),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceS),
    ) {
        items(SearchFilter.entries) { filter ->
            FilterChip(
                selected = filter == selected,
                onClick = { onSelect(filter) },
                label = { Text(stringResource(labels.getValue(filter))) },
            )
        }
    }
}

@Composable
private fun HistoryList(
    history: List<String>,
    onClick: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClearAll: () -> Unit,
    contentPadding: PaddingValues,
) {
    val dimens = LocalDimens.current
    if (history.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.Search,
            message = stringResource(R.string.search_prompt),
        )
        return
    }
    LazyColumn(contentPadding = contentPadding) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimens.spaceL, vertical = dimens.spaceS),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.search_history),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                TextButton(onClick = onClearAll) {
                    Text(stringResource(R.string.search_clear_history))
                }
            }
        }
        items(history, key = { it }) { query ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick(onClick = { onClick(query) })
                    .padding(horizontal = dimens.spaceL, vertical = dimens.spaceM),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = query,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = dimens.spaceM),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                IconButton(onClick = { onRemove(query) }) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.cd_remove_history_item),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultsList(
    state: SearchUiState,
    currentSongId: String?,
    onPlaySong: (List<Song>, Int) -> Unit,
    onToggleLike: (Song) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onOpenUser: (String) -> Unit,
    onSubmit: () -> Unit,
    contentPadding: PaddingValues,
) {
    val results = state.results
    if (!state.isSearching && results.isEmpty) {
        EmptyState(
            icon = Icons.Filled.SearchOff,
            message = stringResource(R.string.search_no_results, state.query),
        )
        return
    }
    LazyColumn(contentPadding = contentPadding) {
        when (state.filter) {
            SearchFilter.SONG -> itemsIndexed(results.songs, key = { _, s -> s.id }) { index, song ->
                SongRow(
                    song = song,
                    isCurrent = song.id == currentSongId,
                    onClick = {
                        onPlaySong(results.songs, index)
                        onSubmit()
                    },
                    onToggleLike = onToggleLike,
                )
            }
            SearchFilter.ARTIST -> items(results.artists, key = { it.id }) { artist ->
                ArtistResultRow(artist) { onOpenArtist(artist.id); onSubmit() }
            }
            SearchFilter.PLAYLIST -> items(results.playlists, key = { it.id }) { playlist ->
                PlaylistResultRow(playlist.coverImageUrl, playlist.title, playlist.description) {
                    onOpenPlaylist(playlist.id); onSubmit()
                }
            }
            SearchFilter.USER -> items(results.users, key = { it.id }) { user ->
                UserResultRow(user) { onOpenUser(user.id); onSubmit() }
            }
        }
    }
}

@Composable
private fun ArtistResultRow(artist: Artist, onClick: () -> Unit) {
    val dimens = LocalDimens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(onClick = onClick)
            .padding(horizontal = dimens.spaceL, vertical = dimens.spaceS),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleImage(url = artist.imageUrl, contentDescription = artist.name, sizeDp = 48)
        Text(
            text = artist.name,
            modifier = Modifier.padding(horizontal = dimens.spaceM),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun PlaylistResultRow(cover: String, title: String, subtitle: String, onClick: () -> Unit) {
    val dimens = LocalDimens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(onClick = onClick)
            .padding(horizontal = dimens.spaceL, vertical = dimens.spaceS),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverImage(url = cover, contentDescription = title, modifier = Modifier.size(dimens.coverSmall), cornerRadius = 8)
        Column(modifier = Modifier.padding(horizontal = dimens.spaceM)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun UserResultRow(user: User, onClick: () -> Unit) {
    val dimens = LocalDimens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(onClick = onClick)
            .padding(horizontal = dimens.spaceL, vertical = dimens.spaceS),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleImage(url = user.avatarUrl, contentDescription = user.displayName, sizeDp = 48)
        Column(modifier = Modifier.padding(horizontal = dimens.spaceM)) {
            Text(user.displayName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Text(user.handle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
