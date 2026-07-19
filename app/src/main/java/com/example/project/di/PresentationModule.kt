package com.example.project.di

import com.example.project.ui.MainViewModel
import com.example.project.ui.artist.ArtistViewModel
import com.example.project.ui.auth.AuthViewModel
import com.example.project.ui.chat.ChatDetailViewModel
import com.example.project.ui.chat.ChatListViewModel
import com.example.project.ui.downloads.DownloadsViewModel
import com.example.project.ui.followed.FollowedViewModel
import com.example.project.ui.home.HomeViewModel
import com.example.project.ui.library.LikedSongsViewModel
import com.example.project.ui.library.RecentlyPlayedViewModel
import com.example.project.ui.player.PlayerViewModel
import com.example.project.ui.playlistdetail.PlaylistDetailViewModel
import com.example.project.ui.playlists.PlaylistsViewModel
import com.example.project.ui.profile.ProfileViewModel
import com.example.project.ui.search.SearchViewModel
import com.example.project.ui.settings.SettingsViewModel
import com.example.project.ui.userprofile.UserProfileViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/** Presentation layer: all ViewModels. */
val presentationModule = module {
    viewModel { AuthViewModel(get(), get()) }
    viewModel { MainViewModel(get(), get()) }
    viewModel { PlayerViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { HomeViewModel(get(), get()) }
    viewModel { SearchViewModel(get(), get()) }
    viewModel { DownloadsViewModel(get()) }
    viewModel { PlaylistsViewModel(get()) }
    viewModel { ProfileViewModel(get(), get(), get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get(), get()) }
    viewModel { LikedSongsViewModel(get()) }
    viewModel { RecentlyPlayedViewModel(get()) }
    viewModel { FollowedViewModel(get()) }
    viewModel { ChatListViewModel(get()) }

    // Parameterised ViewModels (receive a route argument via parametersOf).
    viewModel { (playlistId: String) -> PlaylistDetailViewModel(playlistId, get(), get()) }
    viewModel { (artistId: String) -> ArtistViewModel(artistId, get(), get()) }
    viewModel { (userId: String) -> UserProfileViewModel(userId, get(), get()) }
    viewModel { (conversationId: String) -> ChatDetailViewModel(conversationId, get()) }
}
