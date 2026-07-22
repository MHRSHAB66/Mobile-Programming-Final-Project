package com.example.project.di

import androidx.room.Room
import com.example.project.BuildConfig
import com.example.project.data.local.datastore.settingsDataStore
import com.example.project.data.local.db.AppDatabase
import com.example.project.data.player.PlayerControllerImpl
import com.example.project.data.remote.api.ApiConfig
import com.example.project.data.remote.api.AuthApi
import com.example.project.data.remote.api.AuthInterceptor
import com.example.project.data.remote.api.CatalogApi
import com.example.project.data.remote.api.InMemoryTokenProvider
import com.example.project.data.remote.api.SessionExpiryHandler
import com.example.project.data.remote.api.SocialApi
import com.example.project.data.remote.api.TokenProvider
import com.example.project.data.remote.music.MelodifyCatalogDataSource
import com.example.project.data.remote.music.RemoteMusicDataSource
import com.example.project.data.remote.api.LibraryApi
import com.example.project.data.remote.api.ChatApi
import com.example.project.data.remote.api.NotificationsApi
import com.example.project.data.remote.socket.ChatSocket
import com.example.project.data.remote.socket.MelodifyChatSocket
import com.example.project.data.repository.AuthRepositoryImpl
import com.example.project.data.repository.ChatRepositoryImpl
import com.example.project.data.repository.DownloadRepositoryImpl
import com.example.project.data.repository.LibraryRepositoryImpl
import com.example.project.data.repository.MusicRepositoryImpl
import com.example.project.data.repository.NotificationRepositoryImpl
import com.example.project.data.repository.PlaylistRepositoryImpl
import com.example.project.data.repository.ProfileRepositoryImpl
import com.example.project.data.repository.SearchRepositoryImpl
import com.example.project.data.repository.SettingsRepositoryImpl
import com.example.project.data.repository.SocialRepositoryImpl
import com.example.project.domain.player.PlayerController
import com.example.project.domain.repository.AuthRepository
import com.example.project.domain.repository.ChatRepository
import com.example.project.domain.repository.DownloadRepository
import com.example.project.domain.repository.LibraryRepository
import com.example.project.domain.repository.MusicRepository
import com.example.project.domain.repository.NotificationRepository
import com.example.project.domain.repository.PlaylistRepository
import com.example.project.domain.repository.ProfileRepository
import com.example.project.domain.repository.SearchRepository
import com.example.project.domain.repository.SettingsRepository
import com.example.project.domain.repository.SocialRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/** Provides DataStore, Room, networking, the realtime socket, the player and all repositories. */
val dataModule = module {

    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    single { androidContext().settingsDataStore }
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }

    single<TokenProvider> { InMemoryTokenProvider() }
    single<SessionExpiryHandler> {
        val appScope: CoroutineScope = get()
        val tokenProvider: TokenProvider = get()
        val settingsRepository: SettingsRepository = get()
        object : SessionExpiryHandler {
            override fun onSessionExpired() {
                appScope.launch {
                    tokenProvider.setToken(null)
                    runCatching {
                        GlobalContext.get().get<SocialRepository>().clearSocialCache()
                    }
                    runCatching {
                        GlobalContext.get().get<ChatRepository>().clearChatCache()
                    }
                    runCatching {
                        GlobalContext.get().get<LibraryRepository>().clearLikesCache()
                    }
                    settingsRepository.logout()
                }
            }
        }
    }

    single {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    single {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(get(), get()))
            .addInterceptor(logging)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    single {
        Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(get())
            .addConverterFactory(MoshiConverterFactory.create(get()))
            .build()
    }

    single<AuthApi> { get<Retrofit>().create(AuthApi::class.java) }
    single<CatalogApi> { get<Retrofit>().create(CatalogApi::class.java) }
    single<SocialApi> { get<Retrofit>().create(SocialApi::class.java) }
    single<SocialRepository> { SocialRepositoryImpl(get(), get(), get(), get()) }

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

    single<ChatApi> { get<Retrofit>().create(ChatApi::class.java) }
    single<LibraryApi> { get<Retrofit>().create(LibraryApi::class.java) }
    single<NotificationsApi> { get<Retrofit>().create(NotificationsApi::class.java) }
    single<NotificationRepository> { NotificationRepositoryImpl(get()) }
    single<LibraryRepository> { LibraryRepositoryImpl(get(), get(), get(), get()) }
    single<ChatSocket> { MelodifyChatSocket(get(), get(), get(), get()) }
    single<ChatRepository> { ChatRepositoryImpl(get(), get(), get(), get(), get(), get()) }

    single<AuthRepository> { AuthRepositoryImpl(get(), get(), get(), get(), get(), get()) }
    single<ProfileRepository> { ProfileRepositoryImpl(androidContext(), get(), get()) }

    single<RemoteMusicDataSource> { MelodifyCatalogDataSource(get()) }

    single<PlayerController> { PlayerControllerImpl(androidContext()) }

    single<MusicRepository> { MusicRepositoryImpl(get(), get(), get(), get(), get()) }
    single<PlaylistRepository> { PlaylistRepositoryImpl(get(), get()) }
    single<SearchRepository> { SearchRepositoryImpl(get(), get(), get()) }
    single<DownloadRepository> { DownloadRepositoryImpl(androidContext(), get(), get()) }
}
