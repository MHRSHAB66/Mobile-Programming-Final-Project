package com.example.project

import android.app.Application
import com.example.project.data.remote.api.TokenProvider
import com.example.project.di.appKoinModules
import com.example.project.domain.repository.ChatRepository
import com.example.project.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class MelodyApp : Application() {

    private val chatRepository: ChatRepository by inject()
    private val settingsRepository: SettingsRepository by inject()
    private val tokenProvider: TokenProvider by inject()
    private val appScope: CoroutineScope by inject()

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@MelodyApp)
            modules(appKoinModules)
        }
        appScope.launch {
            tokenProvider.setToken(settingsRepository.restoreAccessToken())
            chatRepository.connect()
        }
    }
}
