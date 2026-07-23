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

    @Query("SELECT * FROM liked_songs ORDER BY likedAt DESC")
    fun pagingAll(): PagingSource<Int, LikedSongEntity>

    @Query("SELECT songId FROM liked_songs")
    fun observeIds(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM liked_songs WHERE songId = :songId)")
    suspend fun isLiked(songId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LikedSongEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<LikedSongEntity>)

    @Query("DELETE FROM liked_songs WHERE songId = :songId")
    suspend fun delete(songId: String)

    @Query("DELETE FROM liked_songs")
    suspend fun deleteAll()
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
    @Query("SELECT * FROM downloads WHERE userId = :userId ORDER BY addedAt DESC")
    fun observeAllByRecent(userId: String): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE userId = :userId ORDER BY title COLLATE NOCASE ASC")
    fun observeAllByTitle(userId: String): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE userId = :userId ORDER BY artistName COLLATE NOCASE ASC")
    fun observeAllByArtist(userId: String): Flow<List<DownloadEntity>>

    @Query("SELECT songId FROM downloads WHERE userId = :userId AND state = 'COMPLETED'")
    fun observeCompletedIds(userId: String): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM downloads WHERE songId = :songId AND userId = :userId)")
    suspend fun exists(songId: String, userId: String): Boolean

    @Query("SELECT localPath FROM downloads WHERE songId = :songId AND userId = :userId AND state = 'COMPLETED' LIMIT 1")
    suspend fun localPath(songId: String, userId: String): String?

    @Upsert
    suspend fun upsert(entity: DownloadEntity)

    @Query("UPDATE downloads SET state = :state, progress = :progress, localPath = :localPath WHERE songId = :songId AND userId = :userId")
    suspend fun updateStatus(songId: String, userId: String, state: String, progress: Int, localPath: String?)

    @Query("DELETE FROM downloads WHERE songId = :songId AND userId = :userId")
    suspend fun delete(songId: String, userId: String)
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

    @Query(
        """
        UPDATE chat_messages SET status = 'READ'
        WHERE conversationId = :conversationId
          AND isFromMe = 1
          AND timestamp <= (
            SELECT timestamp FROM chat_messages WHERE id = :upToMessageId
          )
        """,
    )
    suspend fun markMineReadUpTo(conversationId: String, upToMessageId: String)

    @Query("SELECT COUNT(*) FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun count(conversationId: String): Int

    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAll()

    @Query(
        "SELECT id FROM chat_messages WHERE conversationId = :conversationId " +
            "ORDER BY timestamp DESC LIMIT 1",
    )
    suspend fun latestMessageId(conversationId: String): String?

    @Query(
        "SELECT id FROM chat_messages WHERE conversationId = :conversationId " +
            "AND isFromMe = 0 ORDER BY timestamp DESC LIMIT 1",
    )
    suspend fun latestIncomingMessageId(conversationId: String): String?
}

@Dao
interface CatalogCacheDao {
    @Query("SELECT * FROM cached_songs ORDER BY popularity DESC, title COLLATE NOCASE ASC")
    suspend fun getAllSongs(): List<CachedSongEntity>

    @Query("SELECT * FROM cached_songs WHERE id = :id LIMIT 1")
    suspend fun getSong(id: String): CachedSongEntity?

    @Query("SELECT * FROM cached_songs WHERE artistId = :artistId ORDER BY popularity DESC")
    suspend fun getSongsByArtist(artistId: String): List<CachedSongEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSongs(songs: List<CachedSongEntity>)

    @Query("DELETE FROM cached_songs")
    suspend fun clearSongs()

    @Query("SELECT * FROM cached_artists ORDER BY followers DESC")
    suspend fun getAllArtists(): List<CachedArtistEntity>

    @Query("SELECT * FROM cached_artists WHERE id = :id LIMIT 1")
    suspend fun getArtist(id: String): CachedArtistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertArtists(artists: List<CachedArtistEntity>)

    @Query("UPDATE cached_artists SET followers = :followers WHERE id = :id")
    suspend fun updateArtistFollowers(id: String, followers: Int)

    @Query("DELETE FROM cached_artists")
    suspend fun clearArtists()

    @Query("SELECT * FROM cached_playlists ORDER BY title COLLATE NOCASE ASC")
    suspend fun getAllPlaylists(): List<CachedPlaylistEntity>

    @Query("SELECT * FROM cached_playlists WHERE id = :id LIMIT 1")
    suspend fun getPlaylist(id: String): CachedPlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlaylists(playlists: List<CachedPlaylistEntity>)

    @Query("DELETE FROM cached_playlists")
    suspend fun clearPlaylists()

    @Query(
        """
        SELECT s.* FROM cached_songs s
        INNER JOIN cached_playlist_songs ps ON ps.songId = s.id
        WHERE ps.playlistId = :playlistId
        ORDER BY ps.position ASC
        """,
    )
    suspend fun getPlaylistSongs(playlistId: String): List<CachedSongEntity>

    @Query(
        """
        SELECT s.* FROM cached_songs s
        INNER JOIN cached_playlist_songs ps ON ps.songId = s.id
        WHERE ps.playlistId = :playlistId
        ORDER BY ps.position ASC
        """,
    )
    fun pagingPlaylistSongs(playlistId: String): PagingSource<Int, CachedSongEntity>

    @Query("SELECT * FROM cached_songs WHERE artistId = :artistId ORDER BY popularity DESC")
    fun pagingSongsByArtist(artistId: String): PagingSource<Int, CachedSongEntity>

    @Query("DELETE FROM cached_playlist_songs WHERE playlistId = :playlistId")
    suspend fun clearPlaylistSongs(playlistId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlaylistSongs(rows: List<CachedPlaylistSongEntity>)
}
