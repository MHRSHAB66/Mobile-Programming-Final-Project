package com.example.project.domain.repository

import androidx.paging.PagingData
import com.example.project.domain.model.Playlist
import com.example.project.domain.model.PlaylistType
import com.example.project.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    suspend fun getPlaylists(type: PlaylistType): List<Playlist>
    suspend fun getAllPlaylists(): List<Playlist>
    suspend fun getMyPlaylists(): List<Playlist>
    suspend fun getPlaylist(id: String): Playlist?
    suspend fun getPlaylistSongs(id: String): List<Song>

    suspend fun createPlaylist(title: String, isPublic: Boolean = true): Result<Playlist>
    suspend fun addSongToPlaylist(playlistId: String, songId: String): Result<Unit>

    /** Paging-ready song stream for long playlists (Playlist Detail screen). */
    fun getPlaylistSongsPaged(id: String): Flow<PagingData<Song>>
}
