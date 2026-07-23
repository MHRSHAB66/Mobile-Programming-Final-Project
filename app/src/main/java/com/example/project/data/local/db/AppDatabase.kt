package com.example.project.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        LikedSongEntity::class,
        RecentlyPlayedEntity::class,
        DownloadEntity::class,
        SearchHistoryEntity::class,
        ChatMessageEntity::class,
        CachedSongEntity::class,
        CachedArtistEntity::class,
        CachedPlaylistEntity::class,
        CachedPlaylistSongEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun likedSongDao(): LikedSongDao
    abstract fun recentlyPlayedDao(): RecentlyPlayedDao
    abstract fun downloadDao(): DownloadDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun catalogCacheDao(): CatalogCacheDao

    companion object {
        const val NAME = "melodify.db"
    }
}
