package com.example.project.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.project.R

/** All navigation routes. Clean string routes (project isn't configured for type-safe nav). */
object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val DOWNLOADS = "downloads"
    const val PLAYLISTS = "playlists"
    const val PROFILE = "profile"

    const val SETTINGS = "settings"
    const val NOW_PLAYING = "now_playing"
    const val NOTIFICATIONS = "notifications"
    const val LIKED = "liked_songs"
    const val RECENT = "recently_played"
    const val FOLLOWED = "followed"
    const val FOLLOWED_ARTISTS = "followed_artists"
    const val CONNECTIONS = "connections/{userId}/{mode}"
    const val CHAT_LIST = "chats"

    const val PLAYLIST_DETAIL = "playlist/{playlistId}"
    const val ARTIST = "artist/{artistId}"
    const val USER = "user/{userId}"
    const val CHAT_DETAIL = "chat/{conversationId}"

    fun playlistDetail(id: String) = "playlist/$id"
    fun artist(id: String) = "artist/$id"
    fun user(id: String) = "user/$id"
    fun connections(userId: String, mode: String) = "connections/$userId/$mode"
    fun chatDetail(id: String) = "chat/$id"

    object Args {
        const val PLAYLIST_ID = "playlistId"
        const val ARTIST_ID = "artistId"
        const val USER_ID = "userId"
        const val MODE = "mode"
        const val CONVERSATION_ID = "conversationId"
    }
}

/** The five bottom-navigation tabs. */
enum class TopLevelTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    HOME(Routes.HOME, R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home),
    SEARCH(Routes.SEARCH, R.string.nav_search, Icons.Filled.Search, Icons.Outlined.Search),
    DOWNLOADS(Routes.DOWNLOADS, R.string.nav_downloads, Icons.Filled.Download, Icons.Outlined.Download),
    PLAYLISTS(Routes.PLAYLISTS, R.string.nav_playlists, Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic),
    PROFILE(Routes.PROFILE, R.string.nav_profile, Icons.Filled.Person, Icons.Outlined.Person),
}

val mainTabRoutes: Set<String> = TopLevelTab.entries.map { it.route }.toSet()
