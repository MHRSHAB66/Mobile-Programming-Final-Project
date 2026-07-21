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
    ],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun likedSongDao(): LikedSongDao
    abstract fun recentlyPlayedDao(): RecentlyPlayedDao
    abstract fun downloadDao(): DownloadDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        const val NAME = "melodify.db"
    }
}
