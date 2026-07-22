package com.example.project.data.repository

import com.example.project.data.remote.api.AuthApi
import com.example.project.data.remote.api.ApiConfig
import com.example.project.data.remote.api.TokenProvider
import com.example.project.data.remote.api.dto.LoginRequestDto
import com.example.project.data.remote.api.dto.RegisterRequestDto
import com.example.project.data.remote.api.dto.TokenResponseDto
import com.example.project.data.remote.api.toAuthException
import com.example.project.domain.repository.AuthRepository
import com.example.project.domain.repository.ChatRepository
import com.example.project.domain.repository.SettingsRepository
import com.example.project.domain.repository.SocialRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepositoryImpl(
    private val authApi: AuthApi,
    private val settingsRepository: SettingsRepository,
    private val tokenProvider: TokenProvider,
    private val socialRepository: SocialRepository,
    private val chatRepository: ChatRepository,
) : AuthRepository {

    override suspend fun login(handle: String, password: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val response = authApi.login(
                    LoginRequestDto(
                        handle = handle.trim().removePrefix("@").lowercase(),
                        password = password,
                    ),
                )
                persistSession(response)
            }.recoverCatching { throw it.toAuthException() }
        }

    override suspend fun register(
        displayName: String,
        handle: String,
        password: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val response = authApi.register(
                RegisterRequestDto(
                    displayName = displayName.trim(),
                    handle = handle.trim().removePrefix("@").lowercase(),
                    password = password,
                ),
            )
            persistSession(response)
        }.recoverCatching { throw it.toAuthException() }
    }

    override suspend fun logout() {
        withContext(Dispatchers.IO) {
            runCatching { authApi.logout() }
            tokenProvider.setToken(null)
            socialRepository.clearSocialCache()
            chatRepository.clearChatCache()
            settingsRepository.logout()
        }
    }

    private suspend fun persistSession(response: TokenResponseDto) {
        socialRepository.clearSocialCache()
        tokenProvider.setToken(response.accessToken)
        val user = response.user
        settingsRepository.saveSession(
            userId = user.id,
            name = user.displayName,
            handle = "@${user.handle.removePrefix("@")}",
            avatarUrl = ApiConfig.rewriteUrl(user.avatarUrl),
            isPremium = user.isPremium,
            accessToken = response.accessToken,
        )
        socialRepository.refreshFollowing()
        chatRepository.connect()
    }
}
