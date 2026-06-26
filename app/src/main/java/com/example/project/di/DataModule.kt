package com.example.project.di

import androidx.room.Room
import com.example.project.BuildConfig
import com.example.project.data.local.datastore.settingsDataStore
import com.example.project.data.local.db.AppDatabase
import com.example.project.data.player.PlayerControllerImpl
import com.example.project.data.remote.music.JamendoMusicDataSource
import com.example.project.data.remote.music.MockMusicDataSource
import com.example.project.data.remote.music.RemoteMusicDataSource
import com.example.project.data.remote.socket.ChatSocket
import com.example.project.data.remote.socket.FakeChatSocket
import com.example.project.data.repository.ChatRepositoryImpl
import com.example.project.data.repository.DownloadRepositoryImpl
import com.example.project.data.repository.LibraryRepositoryImpl
import com.example.project.data.repository.MusicRepositoryImpl
import com.example.project.data.repository.PlaylistRepositoryImpl
import com.example.project.data.repository.SearchRepositoryImpl
import com.example.project.data.repository.SettingsRepositoryImpl
import com.example.project.data.repository.SocialRepositoryImpl
import com.example.project.domain.player.PlayerController
import com.example.project.domain.repository.ChatRepository
import com.example.project.domain.repository.DownloadRepository
import com.example.project.domain.repository.LibraryRepository
import com.example.project.domain.repository.MusicRepository
import com.example.project.domain.repository.PlaylistRepository
import com.example.project.domain.repository.SearchRepository
import com.example.project.domain.repository.SettingsRepository
import com.example.project.domain.repository.SocialRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/** Provides DataStore, Room, the realtime socket, the player and all repositories. */
val dataModule = module {

    // Application-scoped coroutine scope for realtime/socket work.
    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    // DataStore + settings
    single { androidContext().settingsDataStore }
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }

    // Room database + DAOs
    single {
        Room.databaseBuilder(androidContext(), AppDatabase::class.java, AppDatabase.NAME)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }
    single { get<AppDatabase>().likedSongDao() }
    single { get<AppDatabase>().recentlyPlayedDao() }
    single { get<AppDatabase>().downloadDao() }
    single { get<AppDatabase>().searchHistoryDao() }
    single { get<AppDatabase>().chatMessageDao() }

    // Music catalogue source: Jamendo (real Creative-Commons music) when a client id is
    // configured, otherwise the in-memory mock source with stable royalty-free audio.
    single<RemoteMusicDataSource> {
        val clientId = BuildConfig.JAMENDO_CLIENT_ID
        if (clientId.isNotBlank()) JamendoMusicDataSource(clientId) else MockMusicDataSource()
    }

    // Realtime chat transport (fake WebSocket today; swap for OkHttp later)
    single<ChatSocket> { FakeChatSocket(get()) }

    // Player
    single<PlayerController> { PlayerControllerImpl(androidContext()) }

    // Repositories
    single<MusicRepository> { MusicRepositoryImpl(get(), get(), get()) }
    single<LibraryRepository> { LibraryRepositoryImpl(get(), get()) }
    single<PlaylistRepository> { PlaylistRepositoryImpl(get()) }
    single<SearchRepository> { SearchRepositoryImpl(get(), get()) }
    single<SocialRepository> { SocialRepositoryImpl(get()) }
    single<DownloadRepository> { DownloadRepositoryImpl(androidContext(), get(), get()) }
    single<ChatRepository> { ChatRepositoryImpl(get(), get(), get()) }
}
