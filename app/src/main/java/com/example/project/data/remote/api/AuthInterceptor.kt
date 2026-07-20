package com.example.project.data.remote.api

import okhttp3.Interceptor
import okhttp3.Response

/** Attaches `Authorization: Bearer <token>` when a session token is available. */
class AuthInterceptor(
    private val tokenProvider: TokenProvider,
    private val sessionExpiryHandler: SessionExpiryHandler,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenProvider.token
        val request = if (token.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }
        val response = chain.proceed(request)
        if (response.code == 401 && request.header("Authorization")?.startsWith("Bearer ") == true) {
            sessionExpiryHandler.onSessionExpired()
        }
        return response
    }
}

/** In-memory access token used by [AuthInterceptor]; kept in sync with DataStore. */
interface TokenProvider {
    val token: String?
    fun setToken(token: String?)
}

interface SessionExpiryHandler {
    fun onSessionExpired()
}

class InMemoryTokenProvider : TokenProvider {
    @Volatile
    override var token: String? = null
        private set

    override fun setToken(token: String?) {
        this.token = token
    }
}
