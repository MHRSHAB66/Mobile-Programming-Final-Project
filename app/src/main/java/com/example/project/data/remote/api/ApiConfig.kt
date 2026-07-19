package com.example.project.data.remote.api

import com.example.project.BuildConfig

/**
 * **Change the Melodify backend base URL here** (unless you set `apiBaseUrl` in
 * `gradle.properties`, which overrides this default via [BuildConfig.API_BASE_URL]).
 *
 * Emulator → host machine: `http://10.0.2.2:8000/`
 * Physical device on same Wi‑Fi: `http://YOUR_PC_LAN_IP:8000/`
 *
 * Always keep the trailing slash. All Retrofit services must use [BASE_URL] only —
 * do not hardcode hosts elsewhere.
 */
object ApiConfig {
    private const val DEFAULT_BASE_URL = "http://10.0.2.2:8000/"

    val BASE_URL: String
        get() = BuildConfig.API_BASE_URL.takeIf { it.isNotBlank() } ?: DEFAULT_BASE_URL
}
