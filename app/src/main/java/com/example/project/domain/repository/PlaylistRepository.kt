package com.example.project.domain.repository

import androidx.paging.PagingData
import com.example.project.domain.model.Playlist
import com.example.project.domain.model.PlaylistType
import com.example.project.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    suspend fun getPlaylists(type: PlaylistType): List<Playlist>
    suspend fun getAllPlaylists(): List<Playlist>
    suspend fun getPlaylist(id: String): Playlist?
    suspend fun getPlaylistSongs(id: String): List<Song>

    /** Paging-ready song stream for long playlists (Playlist Detail screen). */
    fun getPlaylistSongsPaged(id: String): Flow<PagingData<Song>>
}
