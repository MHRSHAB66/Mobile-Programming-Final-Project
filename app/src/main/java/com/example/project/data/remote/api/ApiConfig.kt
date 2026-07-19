package com.example.project.data.remote.api

import android.net.Uri
import com.example.project.BuildConfig

/**
 * **Change the Melodify backend base URL here** (unless you set `apiBaseUrl` in
 * `gradle.properties`, which overrides this default via [BuildConfig.API_BASE_URL]).
 *
 * Emulator → host machine: `http://10.0.2.2:8000/`
 * Physical device on same Wi‑Fi: `http://YOUR_PC_LAN_IP:8000/`
 *
 * Always keep the trailing slash. All Retrofit services and media URLs (covers, audio,
 * avatars) must go through [BASE_URL] / [rewriteUrl] — do not hardcode hosts elsewhere.
 */
object ApiConfig {
    private const val DEFAULT_BASE_URL = "http://10.0.2.2:8000/"

    val BASE_URL: String
        get() = BuildConfig.API_BASE_URL.takeIf { it.isNotBlank() } ?: DEFAULT_BASE_URL

    /**
     * Remounts any backend-hosted absolute/relative media path onto [BASE_URL] so Coil
     * and ExoPlayer always hit the same host as Retrofit (from `gradle.properties`).
     * Also percent-encodes path segments (fixes Unicode filenames like `pathétique`).
     */
    fun rewriteUrl(url: String?): String {
        if (url.isNullOrBlank()) return ""
        val base = BASE_URL.trimEnd('/')
        val raw = url.trim()

        val path: String = when {
            raw.startsWith("http://", ignoreCase = true) ||
                raw.startsWith("https://", ignoreCase = true) -> {
                val uri = Uri.parse(raw)
                val p = uri.path.orEmpty()
                if (p.startsWith("/media/")) {
                    p
                } else {
                    // Non-media absolute URL: still rewrite known local backend hosts.
                    return rewriteKnownHost(raw, base)
                }
            }
            raw.startsWith("/media/") -> raw
            raw.startsWith("media/") -> "/$raw"
            else -> "/media/${raw.trimStart('/')}"
        }

        val encodedPath = path.split('/').joinToString("/") { segment ->
            if (segment.isEmpty()) {
                ""
            } else {
                // Decode first so already-encoded server paths aren't double-encoded.
                Uri.encode(Uri.decode(segment))
            }
        }
        return base + encodedPath
    }

    private fun rewriteKnownHost(url: String, base: String): String {
        val rewritten = url
            .replace("http://127.0.0.1:8000", base)
            .replace("http://localhost:8000", base)
            .replace("http://10.0.2.2:8000", base)
        if (rewritten == url) return url
        // Re-run so /media/ paths get encoding after host swap.
        return rewriteUrl(rewritten)
    }
}
