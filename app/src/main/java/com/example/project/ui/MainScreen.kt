package com.example.project.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.project.R
import com.example.project.ui.components.AddToPlaylistSheet
import com.example.project.ui.components.MiniPlayer
import com.example.project.ui.navigation.AppNavHost
import com.example.project.ui.navigation.BottomBar
import com.example.project.ui.navigation.Routes
import com.example.project.ui.navigation.TopLevelTab
import com.example.project.ui.navigation.mainTabRoutes
import com.example.project.ui.nowplaying.LocalSharedTransitionScope
import com.example.project.ui.player.PlayerEffect
import com.example.project.ui.player.PlayerViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/**
 * Single-Activity host: bottom navigation on the five main tabs, a floating mini player on every
 * destination except Now Playing and Chat Detail, a shared snackbar host for one-time player
 * effects, and the [AppNavHost] for all destinations.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val playerViewModel: PlayerViewModel = koinViewModel()
    val mainViewModel: MainViewModel = koinViewModel()

    val avatarUrl by mainViewModel.avatarUrl.collectAsStateWithLifecycle()
    val playback by playerViewModel.playbackState.collectAsStateWithLifecycle()
    val addToPlaylistSong by playerViewModel.addToPlaylistSong.collectAsStateWithLifecycle()
    val myPlaylists by playerViewModel.myPlaylists.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    androidx.compose.runtime.LaunchedEffect(Unit) {
        playerViewModel.effects.collect { effect ->
            when (effect) {
                is PlayerEffect.Message -> snackbarHostState.showSnackbar(effect.text.asString(context))
                PlayerEffect.NeedsPremium -> {
                    val result = snackbarHostState.showSnackbar(
                        message = context.getString(R.string.premium_required_message),
                        actionLabel = context.getString(R.string.upgrade),
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        navController.navigate(Routes.PROFILE) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            }
        }
    }

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val showBottomBar = currentRoute in mainTabRoutes
    val isChatDetail = currentRoute == Routes.CHAT_DETAIL
    val showMiniPlayer =
        currentRoute != null &&
            currentRoute != Routes.NOW_PLAYING &&
            !isChatDetail &&
            playback.isActive

    // SharedTransitionLayout hosts the MiniPlayer → Now Playing cover animation (spec §5). The
    // scope is published via a CompositionLocal so the two cover composables (owned by the player
    // feature) can opt in without threading it through every navigation signature.
    SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    // Apply navigationBarsPadding only on screens that have no bottom bar and
                    // no chat input (which manages its own insets). Mahyar fix: issue #022.
                    Column(
                        modifier = when {
                            showBottomBar -> Modifier
                            isChatDetail -> Modifier
                            else -> Modifier.navigationBarsPadding()
                        },
                    ) {
                        // Keep this AnimatedVisibility in composition while navigating to Now
                        // Playing so its exit can drive the shared cover transition. Chat Detail
                        // renders its own MiniPlayer directly above the message input.
                        AnimatedVisibility(visible = showMiniPlayer) {
                            MiniPlayer(
                                state = playback,
                                onPlayPause = playerViewModel::togglePlayPause,
                                onPrevious = playerViewModel::previous,
                                onNext = playerViewModel::next,
                                onClick = { navController.navigate(Routes.NOW_PLAYING) },
                                animatedVisibilityScope = this@AnimatedVisibility,
                            )
                        }
                        AnimatedVisibility(visible = showBottomBar) {
                            BottomBar(
                                currentRoute = currentRoute,
                                onTabSelected = { tab: TopLevelTab ->
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                            )
                        }
                    }
                },
            ) { innerPadding ->
                AppNavHost(
                    navController = navController,
                    playerViewModel = playerViewModel,
                    avatarUrl = avatarUrl,
                    currentSongId = playback.currentSong?.id,
                    playback = playback,
                    contentPadding = innerPadding,
                    onShowMessage = { message -> scope.launch { snackbarHostState.showSnackbar(message) } },
                )
            }

            addToPlaylistSong?.let { song ->
                AddToPlaylistSheet(
                    song = song,
                    playlists = myPlaylists,
                    onDismiss = playerViewModel::dismissAddToPlaylist,
                    onPickPlaylist = { playlist ->
                        playerViewModel.addSongToPlaylist(playlist.id)
                    },
                )
            }
        }
    }
}
