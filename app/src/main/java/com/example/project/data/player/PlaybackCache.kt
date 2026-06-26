package com.example.project.data.player

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

/**
 * Process-wide ExoPlayer cache so streamed audio is stored on disk. Re-listening, seeking
 * or replaying a track reads from cache instead of consuming the network again
 * (ExoPlayer CacheDataSource requirement from the spec).
 */
@UnstableApi
object PlaybackCache {
    private const val MAX_BYTES = 256L * 1024 * 1024 // 256 MB
    @Volatile
    private var cache: SimpleCache? = null

    @Synchronized
    fun get(context: Context): SimpleCache {
        return cache ?: SimpleCache(
            File(context.applicationContext.cacheDir, "media_cache"),
            LeastRecentlyUsedCacheEvictor(MAX_BYTES),
            StandaloneDatabaseProvider(context.applicationContext),
        ).also { cache = it }
    }
}
