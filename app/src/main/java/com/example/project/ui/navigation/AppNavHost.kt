package com.example.project.ui.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.project.domain.model.Song
import com.example.project.ui.artist.ArtistScreen
import com.example.project.ui.chat.ChatDetailScreen
import com.example.project.ui.chat.ChatListScreen
import com.example.project.ui.downloads.DownloadsScreen
import com.example.project.ui.followed.FollowedScreen
import com.example.project.ui.home.HomeScreen
import com.example.project.ui.library.LikedSongsScreen
import com.example.project.ui.library.RecentlyPlayedScreen
import com.example.project.ui.notifications.NotificationsScreen
import com.example.project.ui.nowplaying.NowPlayingScreen
import com.example.project.ui.player.PlayerViewModel
import com.example.project.ui.playlistdetail.PlaylistDetailScreen
import com.example.project.ui.playlists.PlaylistsScreen
import com.example.project.ui.profile.ProfileScreen
import com.example.project.ui.search.SearchScreen
import com.example.project.ui.settings.SettingsScreen
import com.example.project.ui.userprofile.UserProfileScreen
import androidx.compose.ui.Modifier
import androidx.compose.animation.AnimatedVisibility
import com.example.project.domain.model.PlaybackState
import com.example.project.ui.components.MiniPlayer

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppNavHost(
    navController: NavHostController,
    playerViewModel: PlayerViewModel,
    avatarUrl: String?,
    currentSongId: String?,
    playback: PlaybackState,
    contentPadding: PaddingValues,
    onShowMessage: (String) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
) {
    val onPlaySong: (List<Song>, Int) -> Unit = { list, index -> playerViewModel.playQueue(list, index) }
    val onPlayAll: (List<Song>) -> Unit = { playerViewModel.playQueue(it, 0) }
    val onShuffle: (List<Song>) -> Unit = { playerViewModel.playShuffled(it) }
    val onToggleLike: (Song) -> Unit = playerViewModel::onToggleLike

    fun navigateTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun topBar() = TopBarActions(
        avatarUrl = avatarUrl,
        onProfileClick = { navigateTab(Routes.PROFILE) },
        onNotificationsClick = { navController.navigate(Routes.NOTIFICATIONS) },
        onSettingsClick = { navController.navigate(Routes.SETTINGS) },
    )

    val openPlaylist: (String) -> Unit = { navController.navigate(Routes.playlistDetail(it)) }
    val openArtist: (String) -> Unit = { navController.navigate(Routes.artist(it)) }
    val openUser: (String) -> Unit = { navController.navigate(Routes.user(it)) }
    val back: () -> Unit = { navController.popBackStack() }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val detailScreenBottomPadding =
        if (currentRoute in mainTabRoutes) {
            0.dp
        } else {
            contentPadding.calculateBottomPadding()
        }

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = Modifier.padding(
            bottom = detailScreenBottomPadding
        )
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                topBar = topBar(),
                onPlaySong = onPlaySong,
                onOpenPlaylist = openPlaylist,
                onOpenArtist = openArtist,
                onOpenLiked = { navController.navigate(Routes.LIKED) },
                onOpenRecent = { navController.navigate(Routes.RECENT) },
                onOpenPlaylistsTab = { navigateTab(Routes.PLAYLISTS) },
                onOpenFollowed = { navController.navigate(Routes.FOLLOWED) },
                contentPadding = contentPadding,
            )
        }
        composable(Routes.SEARCH) {
            SearchScreen(
                topBar = topBar(),
                currentSongId = currentSongId,
                onPlaySong = onPlaySong,
                onToggleLike = onToggleLike,
                onOpenArtist = openArtist,
                onOpenPlaylist = openPlaylist,
                onOpenUser = openUser,
                contentPadding = contentPadding,
            )
        }
        composable(Routes.DOWNLOADS) {
            DownloadsScreen(
                topBar = topBar(),
                currentSongId = currentSongId,
                onPlaySong = onPlaySong,
                contentPadding = contentPadding,
            )
        }
        composable(Routes.PLAYLISTS) {
            PlaylistsScreen(
                topBar = topBar(),
                onOpenPlaylist = openPlaylist,
                contentPadding = contentPadding,
            )
        }
        composable(Routes.PROFILE) {
            ProfileScreen(
                topBar = topBar(),
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenChats = { navController.navigate(Routes.CHAT_LIST) },
                onOpenFollowed = { navController.navigate(Routes.FOLLOWED) },
                onOpenUser = openUser,
                onOpenPlaylist = openPlaylist,
                onShowMessage = onShowMessage,
                contentPadding = contentPadding,
            )
        }

        composable(Routes.SETTINGS) {
            // After logout the app root swaps to the Auth screen automatically (observes
            // isLoggedIn), so no manual navigation is needed here.
            SettingsScreen(onBack = back, onLoggedOut = {})
        }
        composable(Routes.NOTIFICATIONS) {
            NotificationsScreen(onBack = back)
        }
        composable(Routes.NOW_PLAYING) {
            NowPlayingScreen(
                playerViewModel = playerViewModel,
                onBack = back,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = this@composable,
            )
        }
        composable(Routes.LIKED) {
            LikedSongsScreen(
                currentSongId = currentSongId,
                onBack = back,
                onPlaySong = onPlaySong,
                onPlayAll = onPlayAll,
                onShuffle = onShuffle,
                onRemove = onToggleLike,
            )
        }
        composable(Routes.RECENT) {
            RecentlyPlayedScreen(
                currentSongId = currentSongId,
                onBack = back,
                onPlaySong = onPlaySong,
                onPlayAll = onPlayAll,
                onToggleLike = onToggleLike,
            )
        }
        composable(Routes.FOLLOWED) {
            FollowedScreen(onBack = back, onOpenUser = openUser)
        }
        composable(Routes.CHAT_LIST) {
            ChatListScreen(
                onBack = back,
                onOpenChat = { navController.navigate(Routes.chatDetail(it)) },
            )
        }

        composable(
            route = Routes.PLAYLIST_DETAIL,
            arguments = listOf(navArgument(Routes.Args.PLAYLIST_ID) { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString(Routes.Args.PLAYLIST_ID).orEmpty()
            PlaylistDetailScreen(
                playlistId = id,
                currentSongId = currentSongId,
                onBack = back,
                onPlaySong = onPlaySong,
                onPlayAll = onPlayAll,
                onShuffle = onShuffle,
                onToggleLike = onToggleLike,
            )
        }
        composable(
            route = Routes.ARTIST,
            arguments = listOf(navArgument(Routes.Args.ARTIST_ID) { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString(Routes.Args.ARTIST_ID).orEmpty()
            ArtistScreen(
                artistId = id,
                currentSongId = currentSongId,
                onBack = back,
                onPlaySong = onPlaySong,
                onToggleLike = onToggleLike,
            )
        }
        composable(
            route = Routes.USER,
            arguments = listOf(navArgument(Routes.Args.USER_ID) { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString(Routes.Args.USER_ID).orEmpty()
            UserProfileScreen(userId = id, onBack = back, onOpenPlaylist = openPlaylist)
        }
        composable(
            route = Routes.CHAT_DETAIL,
            arguments = listOf(
                navArgument(Routes.Args.CONVERSATION_ID) {
                    type = NavType.StringType
                }
            ),
        ) { entry ->
            val id = entry.arguments
                ?.getString(Routes.Args.CONVERSATION_ID)
                .orEmpty()

            ChatDetailScreen(
                conversationId = id,
                onBack = back,
                onPlaySharedSong = playerViewModel::playSongById,
                contentAboveInput = {
                    AnimatedVisibility(
                        visible = playback.isActive,
                    ) {
                        MiniPlayer(
                            state = playback,
                            onPlayPause = playerViewModel::togglePlayPause,
                            onNext = playerViewModel::next,
                            onClick = {
                                navController.navigate(Routes.NOW_PLAYING)
                            },
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = this@AnimatedVisibility,
                        )
                    }
                },
            )
        }
    }
}
