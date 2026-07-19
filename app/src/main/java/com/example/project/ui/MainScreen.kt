package com.example.project.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.project.R
import com.example.project.ui.components.MiniPlayer
import com.example.project.ui.navigation.AppNavHost
import com.example.project.ui.navigation.BottomBar
import com.example.project.ui.navigation.Routes
import com.example.project.ui.navigation.TopLevelTab
import com.example.project.ui.navigation.mainTabRoutes
import com.example.project.ui.player.PlayerEffect
import com.example.project.ui.player.PlayerViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.Modifier

/**
 * Single-Activity host: bottom navigation + floating mini player on the five main tabs, a
 * shared snackbar host for one-time player effects, and the [AppNavHost] for all destinations.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val playerViewModel: PlayerViewModel = koinViewModel()
    val mainViewModel: MainViewModel = koinViewModel()

    val avatarUrl by mainViewModel.avatarUrl.collectAsStateWithLifecycle()
    val playback by playerViewModel.playbackState.collectAsStateWithLifecycle()
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

    val currentRoute =
        navController.currentBackStackEntryAsState().value?.destination?.route

    val showBottomBar = currentRoute in mainTabRoutes
    val isChatDetail = currentRoute == Routes.CHAT_DETAIL

    val showGlobalMiniPlayer =
        playback.isActive &&
                currentRoute != Routes.NOW_PLAYING &&
                !isChatDetail

    SharedTransitionLayout {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                Column(
                    modifier = when {
                        showBottomBar -> Modifier
                        isChatDetail -> Modifier
                        else -> Modifier.navigationBarsPadding()
                    },
                ) {
                    AnimatedVisibility(visible = showGlobalMiniPlayer) {
                        MiniPlayer(
                            state = playback,
                            onPlayPause = playerViewModel::togglePlayPause,
                            onNext = playerViewModel::next,
                            onClick = {
                                navController.navigate(Routes.NOW_PLAYING)
                            },
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@AnimatedVisibility,
                        )
                    }

                    if (showBottomBar) {
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
                contentPadding = innerPadding,
                onShowMessage = { message -> scope.launch { snackbarHostState.showSnackbar(message) } },
                sharedTransitionScope = this@SharedTransitionLayout,
                playback = playback
            )
        }
    }
}
