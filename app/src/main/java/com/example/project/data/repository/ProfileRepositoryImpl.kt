package com.example.project.data.repository

import android.content.Context
import com.example.project.data.remote.api.ApiConfig
import com.example.project.data.remote.api.AuthApi
import com.example.project.data.remote.api.dto.AvatarUrlRequestDto
import com.example.project.data.remote.api.dto.UserDto
import com.example.project.data.remote.api.dto.toDomainPlaylist
import com.example.project.data.remote.api.dto.toDomainUser
import com.example.project.data.remote.api.toAuthException
import com.example.project.domain.model.Playlist
import com.example.project.domain.model.User
import com.example.project.domain.repository.ProfileRepository
import com.example.project.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.UUID

class ProfileRepositoryImpl(
    private val context: Context,
    private val authApi: AuthApi,
    private val settingsRepository: SettingsRepository,
) : ProfileRepository {

    override suspend fun refreshProfile(): Result<Unit> = withContext(Dispatchers.IO) {
        if (!hasToken()) return@withContext Result.success(Unit)
        runCatching {
            applyRemoteUser(authApi.me())
        }.recoverCatching { throw it.toAuthException() }
    }

    override suspend fun setAvatarUrl(avatarUrl: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (!hasToken()) {
                settingsRepository.updateProfileFields(avatarUrl = avatarUrl)
                return@withContext Result.success(Unit)
            }
            runCatching {
                applyRemoteUser(authApi.setAvatarUrl(AvatarUrlRequestDto(avatarUrl = avatarUrl)))
            }.recoverCatching { throw it.toAuthException() }
        }

    override suspend fun uploadAvatar(bytes: ByteArray, mimeType: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            val normalizedMime = normalizeMime(mimeType)
            if (bytes.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("empty image"))
            }
            if (bytes.size > MAX_AVATAR_BYTES) {
                return@withContext Result.failure(AvatarTooLargeException())
            }

            if (!hasToken()) {
                val localUri = saveLocalAvatar(bytes, normalizedMime)
                settingsRepository.updateProfileFields(avatarUrl = localUri)
                return@withContext Result.success(Unit)
            }

            runCatching {
                val ext = extensionFor(normalizedMime)
                val body = bytes.toRequestBody(normalizedMime.toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData(
                    name = "file",
                    filename = "avatar.$ext",
                    body = body,
                )
                applyRemoteUser(authApi.uploadAvatar(part))
            }.recoverCatching { throw it.toAuthException() }
        }

    override suspend fun upgradePremium(): Result<Unit> = withContext(Dispatchers.IO) {
        if (!hasToken()) {
            settingsRepository.setPremium(true)
            return@withContext Result.success(Unit)
        }
        runCatching {
            applyRemoteUser(authApi.upgradePremium())
        }.recoverCatching { throw it.toAuthException() }
    }

    override suspend fun getUser(userId: String): Result<User> = withContext(Dispatchers.IO) {
        runCatching {
            authApi.getUser(userId).toDomainUser()
        }.recoverCatching { throw it.toAuthException() }
    }

    override suspend fun getUserPublicPlaylists(userId: String): Result<List<Playlist>> =
        withContext(Dispatchers.IO) {
            runCatching {
                authApi.getUserPlaylists(userId).map { it.toDomainPlaylist() }
            }.recoverCatching { throw it.toAuthException() }
        }

    private suspend fun hasToken(): Boolean =
        !settingsRepository.restoreAccessToken().isNullOrBlank()

    private suspend fun applyRemoteUser(user: UserDto) {
        settingsRepository.updateProfileFields(
            userId = user.id,
            displayName = user.displayName,
            handle = if (user.handle.startsWith("@")) user.handle else "@${user.handle}",
            avatarUrl = rewriteMediaUrl(user.avatarUrl),
            isPremium = user.isPremium,
        )
    }

    private fun saveLocalAvatar(bytes: ByteArray, mimeType: String): String {
        val dir = File(context.filesDir, "avatars").apply { mkdirs() }
        val file = File(dir, "avatar_${UUID.randomUUID()}.${extensionFor(mimeType)}")
        file.writeBytes(bytes)
        return file.toURI().toString()
    }

    private fun rewriteMediaUrl(url: String?): String? {
        if (url.isNullOrBlank()) return url
        val apiBase = ApiConfig.BASE_URL.trimEnd('/')
        return url
            .replace("http://127.0.0.1:8000", apiBase)
            .replace("http://localhost:8000", apiBase)
    }

    private fun normalizeMime(mimeType: String): String {
        val mime = mimeType.lowercase().substringBefore(';').trim()
        return when (mime) {
            "image/jpg" -> "image/jpeg"
            in ALLOWED_MIME -> mime
            else -> "image/jpeg"
        }
    }

    private fun extensionFor(mimeType: String): String = when (mimeType) {
        "image/png" -> "png"
        "image/webp" -> "webp"
        else -> "jpg"
    }

    companion object {
        private const val MAX_AVATAR_BYTES = 5 * 1024 * 1024
        private val ALLOWED_MIME = setOf("image/jpeg", "image/png", "image/webp")
    }
}

class AvatarTooLargeException : Exception()
