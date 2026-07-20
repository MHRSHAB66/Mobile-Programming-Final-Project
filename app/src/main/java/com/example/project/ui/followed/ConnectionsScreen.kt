package com.example.project.ui.followed

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.People
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
import com.example.project.ui.components.CircleImage
import com.example.project.ui.components.DetailTopBar
import com.example.project.ui.components.EmptyState
import com.example.project.ui.components.bounceClick
import com.example.project.ui.theme.LocalDimens
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ConnectionsScreen(
    userId: String,
    mode: String,
    onBack: () -> Unit,
    onOpenUser: (String) -> Unit,
    viewModel: ConnectionsViewModel = koinViewModel(
        parameters = { parametersOf(userId, mode) }
    ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val users = state.users
    val dimens = LocalDimens.current
    val title = stringResource(
        if (state.isFollowersMode) R.string.profile_followers else R.string.profile_following
    )

    Scaffold(
        topBar = { DetailTopBar(title = title, onBack = onBack) }
    ) { padding ->
        if (users.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.People,
                message = stringResource(
                    if (state.isFollowersMode) R.string.followers_empty else R.string.followed_empty
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            items(users, key = { it.id }) { user ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .bounceClick(onClick = { onOpenUser(user.id) })
                        .padding(horizontal = dimens.spaceL, vertical = dimens.spaceS),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircleImage(url = user.avatarUrl, contentDescription = user.displayName, sizeDp = 52)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = dimens.spaceM),
                    ) {
                        Text(
                            text = user.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = user.handle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (user.isFollowed) {
                        OutlinedButton(onClick = { viewModel.toggleFollow(user.id) }) {
                            Text(stringResource(R.string.following))
                        }
                    } else {
                        Button(onClick = { viewModel.toggleFollow(user.id) }) {
                            Text(stringResource(R.string.follow))
                        }
                    }
                }
            }
        }
    }
}
