package com.example.project.ui.followed

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.project.R
import com.example.project.core.util.asCompactCount
import com.example.project.ui.components.CircleImage
import com.example.project.ui.components.DetailTopBar
import com.example.project.ui.components.EmptyState
import com.example.project.ui.components.ErrorState
import com.example.project.ui.components.bounceClick
import com.example.project.ui.theme.LocalDimens
import org.koin.androidx.compose.koinViewModel

@Composable
fun FollowedArtistsScreen(
    onBack: () -> Unit,
    onOpenArtist: (String) -> Unit,
    viewModel: FollowedArtistsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val dimens = LocalDimens.current

    Scaffold(
        topBar = {
            DetailTopBar(
                title = stringResource(R.string.followed_artists_title),
                onBack = onBack,
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            state.error != null -> {
                ErrorState(
                    message = state.error!!.asString(),
                    onRetry = viewModel::load,
                    modifier = Modifier.fillMaxSize().padding(padding),
                )
            }

            state.artists.isEmpty() -> {
                EmptyState(
                    icon = Icons.Filled.MusicNote,
                    message = stringResource(R.string.followed_artists_empty),
                    modifier = Modifier.fillMaxSize().padding(padding),
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                ) {
                    items(state.artists, key = { it.id }) { artist ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .bounceClick(onClick = { onOpenArtist(artist.id) })
                                .padding(
                                    horizontal = dimens.spaceL,
                                    vertical = dimens.spaceS,
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircleImage(
                                url = artist.imageUrl,
                                contentDescription = artist.name,
                                sizeDp = 52,
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = dimens.spaceM),
                            ) {
                                Text(
                                    text = artist.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                                Text(
                                    text = stringResource(
                                        R.string.followers_count,
                                        artist.followers.asCompactCount(),
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            OutlinedButton(
                                onClick = { viewModel.unfollow(artist.id) },
                            ) {
                                Text(stringResource(R.string.unfollow))
                            }
                        }
                    }
                }
            }
        }
    }
}
