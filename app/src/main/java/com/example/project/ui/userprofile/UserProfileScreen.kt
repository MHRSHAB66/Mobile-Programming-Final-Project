package com.example.project.ui.userprofile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
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
import com.example.project.ui.components.PlaylistCard
import com.example.project.ui.components.SectionHeader
import com.example.project.ui.theme.LocalDimens
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun UserProfileScreen(
    userId: String,
    onBack: () -> Unit,
    onOpenPlaylist: (String) -> Unit,
    viewModel: UserProfileViewModel = koinViewModel(parameters = { parametersOf(userId) }),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val dimens = LocalDimens.current
    val user = state.user

    Scaffold(
        topBar = { DetailTopBar(title = user?.displayName ?: "", onBack = onBack) }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(dimens.spaceL),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircleImage(url = user?.avatarUrl, contentDescription = user?.displayName, sizeDp = 110)
                    Text(
                        text = user?.displayName ?: "",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = dimens.spaceS),
                    )
                    Text(
                        text = user?.handle ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.followers_count, (user?.followers ?: 0).asCompactCount()),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = dimens.spaceXs),
                    )
                    if (user?.isFollowed == true) {
                        OutlinedButton(
                            onClick = viewModel::toggleFollow,
                            modifier = Modifier.padding(top = dimens.spaceM),
                        ) { Text(stringResource(R.string.following)) }
                    } else {
                        Button(
                            onClick = viewModel::toggleFollow,
                            modifier = Modifier.padding(top = dimens.spaceM),
                        ) { Text(stringResource(R.string.follow)) }
                    }
                }
            }
            if (state.playlists.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.profile_public_playlists)) }
                item {
                    androidx.compose.foundation.lazy.LazyRow(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = dimens.spaceM)
                    ) {
                        items(state.playlists, key = { it.id }) { playlist ->
                            PlaylistCard(playlist = playlist, onClick = { onOpenPlaylist(playlist.id) })
                        }
                    }
                }
            }
        }
    }
}
