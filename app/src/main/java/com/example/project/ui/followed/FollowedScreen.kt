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

@Composable
fun FollowedScreen(
    onBack: () -> Unit,
    onOpenUser: (String) -> Unit,
    viewModel: FollowedViewModel = koinViewModel(),
) {
    val users by viewModel.users.collectAsStateWithLifecycle()
    val dimens = LocalDimens.current

    Scaffold(
        topBar = { DetailTopBar(title = stringResource(R.string.followed_title), onBack = onBack) }
    ) { padding ->
        if (users.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.People,
                message = stringResource(R.string.followed_empty),
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            return@Scaffold
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
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
                    OutlinedButton(onClick = { viewModel.unfollow(user.id) }) {
                        Text(stringResource(R.string.unfollow))
                    }
                }
            }
        }
    }
}
