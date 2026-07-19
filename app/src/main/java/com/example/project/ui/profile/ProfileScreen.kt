package com.example.project.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.project.R
import com.example.project.ui.components.AppTopBar
import com.example.project.ui.components.CircleImage
import com.example.project.ui.components.PlaylistCard
import com.example.project.ui.components.SectionHeader
import com.example.project.ui.components.bounceClick
import com.example.project.ui.navigation.TopBarActions
import com.example.project.ui.theme.LocalDimens
import org.koin.androidx.compose.koinViewModel

@Composable
fun ProfileScreen(
    topBar: TopBarActions,
    onOpenSettings: () -> Unit,
    onOpenChats: () -> Unit,
    onOpenFollowed: () -> Unit,
    onOpenUser: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onShowMessage: (String) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val dimens = LocalDimens.current
    val context = LocalContext.current

    val pickAvatar = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: "image/jpeg"
        val bytes = runCatching {
            resolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull()
        if (bytes == null) {
            onShowMessage(context.getString(R.string.avatar_change_failed))
        } else {
            viewModel.onAvatarImageSelected(bytes, mime)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ProfileEffect.Message -> onShowMessage(effect.text.asString(context))
            }
        }
    }

    val user = state.user
    Column(modifier = modifier.fillMaxSize().statusBarsPadding()) {
        AppTopBar(
            avatarUrl = topBar.avatarUrl,
            onProfileClick = topBar.onProfileClick,
            onNotificationsClick = topBar.onNotificationsClick,
            onSettingsClick = topBar.onSettingsClick,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                CircleImage(
                    url = state.avatarUrl,
                    contentDescription = stringResource(R.string.cd_profile_image),
                    sizeDp = 110,
                    modifier = Modifier.padding(dimens.spaceS),
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .bounceClick(
                            enabled = !state.isChangingAvatar,
                            onClick = {
                                pickAvatar.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly,
                                    ),
                                )
                            },
                        ),
                ) {
                    if (state.isChangingAvatar) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(7.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Icon(
                            Icons.Filled.CameraAlt,
                            contentDescription = stringResource(R.string.profile_change_avatar),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(7.dp),
                        )
                    }
                }
            }
            Text(
                text = user?.displayName ?: "",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = user?.handle ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            PremiumBadge(isPremium = user?.isPremium == true)

            UpgradeButton(
                isPremium = user?.isPremium == true,
                isUpgrading = state.isUpgrading,
                onUpgrade = viewModel::upgrade,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimens.spaceL, vertical = dimens.spaceS),
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceM),
            ) {
                OutlinedButton(onClick = onOpenChats, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.ChatBubble, contentDescription = null)
                    Text(
                        text = stringResource(R.string.profile_messages),
                        modifier = Modifier.padding(start = dimens.spaceS),
                    )
                }
                OutlinedButton(onClick = onOpenSettings, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Settings, contentDescription = null)
                    Text(
                        text = stringResource(R.string.profile_settings),
                        modifier = Modifier.padding(start = dimens.spaceS),
                    )
                }
            }

            if (state.followedUsers.isNotEmpty()) {
                SectionHeader(
                    title = stringResource(R.string.profile_followed),
                    onSeeAll = onOpenFollowed,
                )
                LazyRow(contentPadding = PaddingValues(horizontal = dimens.spaceM)) {
                    items(state.followedUsers, key = { it.id }) { followed ->
                        Column(
                            modifier = Modifier
                                .bounceClick(onClick = { onOpenUser(followed.id) })
                                .padding(dimens.spaceS),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CircleImage(url = followed.avatarUrl, contentDescription = followed.displayName, sizeDp = 72)
                            Text(
                                text = followed.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = dimens.spaceXs),
                            )
                        }
                    }
                }
            }

            if (state.publicPlaylists.isNotEmpty()) {
                SectionHeader(title = stringResource(R.string.profile_public_playlists))
                LazyRow(contentPadding = PaddingValues(horizontal = dimens.spaceM)) {
                    items(state.publicPlaylists, key = { it.id }) { playlist ->
                        PlaylistCard(playlist = playlist, onClick = { onOpenPlaylist(playlist.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumBadge(isPremium: Boolean) {
    val dimens = LocalDimens.current
    Surface(
        color = if (isPremium) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.padding(top = dimens.spaceS),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = dimens.spaceM, vertical = dimens.spaceXs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.WorkspacePremium,
                contentDescription = null,
                tint = if (isPremium) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(dimens.iconSmall),
            )
            Text(
                text = stringResource(if (isPremium) R.string.profile_premium_badge else R.string.profile_free_badge),
                style = MaterialTheme.typography.labelMedium,
                color = if (isPremium) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = dimens.spaceXs),
            )
        }
    }
}

@Composable
private fun UpgradeButton(isPremium: Boolean, isUpgrading: Boolean, onUpgrade: () -> Unit) {
    val dimens = LocalDimens.current
    Button(
        onClick = onUpgrade,
        enabled = !isUpgrading,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.spaceL, vertical = dimens.spaceM),
    ) {
        if (isUpgrading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Text(
                text = stringResource(R.string.upgrade_in_progress),
                modifier = Modifier.padding(start = dimens.spaceS),
            )
        } else {
            Icon(Icons.Filled.WorkspacePremium, contentDescription = null)
            Text(
                text = stringResource(if (isPremium) R.string.profile_renew else R.string.profile_upgrade),
                modifier = Modifier.padding(start = dimens.spaceS),
            )
        }
    }
}
