package com.example.project.data.local.db

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface LikedSongDao {
    @Query("SELECT * FROM liked_songs ORDER BY likedAt DESC")
    fun observeAll(): Flow<List<LikedSongEntity>>

    @Query("SELECT songId FROM liked_songs")
    fun observeIds(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM liked_songs WHERE songId = :songId)")
    suspend fun isLiked(songId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LikedSongEntity)

    @Query("DELETE FROM liked_songs WHERE songId = :songId")
    suspend fun delete(songId: String)
}

@Dao
interface RecentlyPlayedDao {
    @Query("SELECT * FROM recently_played ORDER BY playedAt DESC LIMIT 50")
    fun observeAll(): Flow<List<RecentlyPlayedEntity>>

    @Upsert
    suspend fun upsert(entity: RecentlyPlayedEntity)
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY addedAt DESC")
    fun observeAllByRecent(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads ORDER BY title COLLATE NOCASE ASC")
    fun observeAllByTitle(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads ORDER BY artistName COLLATE NOCASE ASC")
    fun observeAllByArtist(): Flow<List<DownloadEntity>>

    @Query("SELECT songId FROM downloads WHERE state = 'COMPLETED'")
    fun observeCompletedIds(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM downloads WHERE songId = :songId)")
    suspend fun exists(songId: String): Boolean

    @Query("SELECT localPath FROM downloads WHERE songId = :songId AND state = 'COMPLETED' LIMIT 1")
    suspend fun localPath(songId: String): String?

    @Upsert
    suspend fun upsert(entity: DownloadEntity)

    @Query("UPDATE downloads SET state = :state, progress = :progress, localPath = :localPath WHERE songId = :songId")
    suspend fun updateStatus(songId: String, state: String, progress: Int, localPath: String?)

    @Query("DELETE FROM downloads WHERE songId = :songId")
    suspend fun delete(songId: String)
}

@Dao
interface SearchHistoryDao {
    @Query("SELECT query FROM search_history ORDER BY createdAt DESC LIMIT 12")
    fun observeRecent(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE query = :query")
    suspend fun delete(query: String)

    @Query("DELETE FROM search_history")
    suspend fun clear()
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun observeForConversation(conversationId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp DESC")
    fun pagingForConversation(conversationId: String): PagingSource<Int, ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<ChatMessageEntity>)

    @Query("UPDATE chat_messages SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("UPDATE chat_messages SET status = 'READ' WHERE conversationId = :conversationId AND isFromMe = 1")
    suspend fun markMineRead(conversationId: String)

    @Query("SELECT COUNT(*) FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun count(conversationId: String): Int
}
